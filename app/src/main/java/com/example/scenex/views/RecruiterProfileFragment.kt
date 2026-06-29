package com.example.scenex.views

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.scenex.MainActivity
import com.example.scenex.R
import com.example.scenex.models.CastingCall
import com.example.scenex.models.UserProfile
import com.example.scenex.viewmodels.RecruiterProfileViewModel
import com.example.scenex.viewmodels.SignupViewModel
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Senior Implementation: Recruiter Profile with Unified Management Navigation.
 */
class RecruiterProfileFragment : Fragment() {

    private val viewModel: RecruiterProfileViewModel by activityViewModels()
    private val signupViewModel: SignupViewModel by activityViewModels()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var drawerLayout: DrawerLayout

    private val pickProfileImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            uriToFile(it)?.let { file ->
                Toast.makeText(requireContext(), "Uploading profile image...", Toast.LENGTH_SHORT).show()
                signupViewModel.uploadWorkImage(file) { url ->
                    if (url != null) {
                        // FIX: Update both profileImage and profileImageUrl to ensure UI updates
                        val updates = mapOf(
                            "profileImage" to url,
                            "profileImageUrl" to url
                        )
                        viewModel.updateProfileFields(updates) { success ->
                            if (success) {
                                Toast.makeText(requireContext(), "Profile image updated", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(requireContext(), "Upload failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_recruiter_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        drawerLayout = view.findViewById(R.id.recruiterDrawerLayout)
        setupObservers()
        setupListeners()
        viewModel.loadProfile()
    }

    private fun setupObservers() {
        val view = view ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.userProfile.collect { profile ->
                        profile?.let { populateUI(it, view) }
                    }
                }
                launch {
                    viewModel.castingCalls.collect { castings ->
                        updateCastingUI(castings, view)
                    }
                }
            }
        }
    }

    private fun populateUI(profile: UserProfile, view: View) {
        view.findViewById<TextView>(R.id.tvRecruiterName)?.text = profile.fullName ?: "Unknown"
        view.findViewById<TextView>(R.id.tvRole)?.text = profile.role?.takeIf { it.isNotBlank() } ?: "Director"
        view.findViewById<TextView>(R.id.tvCompanyName)?.text = profile.companyName?.takeIf { it.isNotBlank() } ?: "Independent Studio"
        view.findViewById<TextView>(R.id.tvCompanyBio)?.text = profile.bio?.takeIf { it.isNotBlank() } ?: "Agency summary not listed."

        val avatarView = view.findViewById<ShapeableImageView>(R.id.ivRecruiterAvatar)
        if (avatarView != null) {
            Glide.with(this)
                .load(profile.effectiveAvatarUrl)
                .placeholder(R.drawable.ic_profile_placeholder)
                .circleCrop()
                .into(avatarView)
        }

        view.findViewById<ImageView>(R.id.ivVerifiedBadge)?.visibility =
            if (profile.verificationStatus == "verified") View.VISIBLE else View.GONE

        val tvStatusValue = view.findViewById<TextView>(R.id.tvStatusValue)
        val switchAvailability = view.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.switchAvailability)
        val isAvailable = profile.status?.equals("Available", ignoreCase = true) ?: false

        tvStatusValue?.text = if (isAvailable) "Available" else "Unavailable"
        tvStatusValue?.setTextColor(if (isAvailable) ContextCompat.getColor(requireContext(), R.color.calendar_green) else android.graphics.Color.RED)

        switchAvailability?.setOnCheckedChangeListener(null)
        switchAvailability?.isChecked = isAvailable
        switchAvailability?.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateStatus(if (isChecked) "Available" else "Unavailable")
        }

        val experienceText = profile.experience?.toString()?.trim()
        view.findViewById<TextView>(R.id.tvExpValue)?.text = if (!experienceText.isNullOrBlank()) experienceText else "Professional"
    }

    private fun updateCastingUI(castings: List<CastingCall>, view: View) {
        view.findViewById<TextView>(R.id.tvStats)?.text = "Projects : ${castings.size}  |  Hired : 28  |"

        // 1. My Castings (History - Posters)
        val llPosters = view.findViewById<LinearLayout>(R.id.llMyCastingsPosters)
        llPosters?.removeAllViews()
        castings.forEach { call ->
            val posterView = layoutInflater.inflate(R.layout.item_profile_project_poster, llPosters, false)
            val ivPoster = posterView.findViewById<ImageView>(R.id.ivPoster)
            val posterUrl = call.posterUrl.ifBlank { R.color.gray }
            Glide.with(this).load(posterUrl).placeholder(R.color.gray).centerCrop().into(ivPoster)
            posterView.setOnClickListener { showCastingDetails(call) }
            llPosters?.addView(posterView)
        }

        // 2. Active Castings (Real-time Filtered List)
        val llList = view.findViewById<LinearLayout>(R.id.llCastingList)
        val tvNoCastings = view.findViewById<TextView>(R.id.tvNoCastings)
        llList?.removeAllViews()

        val activeList = castings.filter { isCastingActive(it.submissionDeadline) }

        if (activeList.isEmpty()) {
            tvNoCastings?.visibility = View.VISIBLE
        } else {
            tvNoCastings?.visibility = View.GONE
            activeList.forEach { casting ->
                val itemView = layoutInflater.inflate(R.layout.item_profile_casting, llList, false)
                itemView.findViewById<TextView>(R.id.tvCastingTitle)?.text = casting.projectTitle
                val tvStatus = itemView.findViewById<TextView>(R.id.tvCastingStatus)
                tvStatus?.text = "Open"
                tvStatus?.setTextColor(ContextCompat.getColor(requireContext(), R.color.calendar_green))

                itemView.findViewById<TextView>(R.id.tvCastingDetails)?.text =
                    "${casting.characterName} - ${casting.roleType}\nDeadline: ${casting.submissionDeadline}"

                itemView.setOnClickListener { showCastingDetails(casting) }
                llList?.addView(itemView)
            }
        }
        setupTalentPlaceholders(view)
    }

    private fun navigateToManagement(castingId: String) {
        if (castingId.isBlank()) return
        val intent = Intent(requireContext(), CastingManagementActivity::class.java)
        intent.putExtra("CASTING_ID", castingId)
        startActivity(intent)
    }

    /**
     * Senior Implementation: Professional Casting Details Dialog.
     * Reuses existing dialog_casting_details layout.
     */
    private fun showCastingDetails(casting: CastingCall) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_casting_details, null)

        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setView(dialogView)
            .create()

        // 1. Bind Basic Fields
        dialogView.findViewById<TextView>(R.id.tvDialogProjectTitle)?.text = casting.projectTitle
        dialogView.findViewById<TextView>(R.id.tvDialogProductionType)?.text =
            "${casting.productionType} • Directed by ${casting.directorName}"
        dialogView.findViewById<TextView>(R.id.tvDialogRoleInfo)?.text =
            "${casting.characterName} (${casting.roleType})"
        dialogView.findViewById<TextView>(R.id.tvDialogSynopsis)?.text = casting.projectSynopsis
        dialogView.findViewById<TextView>(R.id.tvDialogDeadline)?.text = casting.submissionDeadline
        dialogView.findViewById<TextView>(R.id.tvDialogLocation)?.text = casting.auditionLocation
        dialogView.findViewById<TextView>(R.id.tvDialogCompensation)?.text = casting.compensation
        dialogView.findViewById<TextView>(R.id.tvDialogShootLocation)?.text = casting.shootLocation

        // 2. Image Handling (Glide)
        val ivPoster = dialogView.findViewById<ImageView>(R.id.ivDialogPoster)
        if (ivPoster != null) {
            if (casting.posterUrl.isNotBlank()) {
                ivPoster.visibility = View.VISIBLE
                Glide.with(this)
                    .load(casting.posterUrl)
                    .centerCrop()
                    .into(ivPoster)
            } else {
                ivPoster.visibility = View.GONE
            }
        }

        // 3. Action Buttons (Restored from Snippet 1)
        dialogView.findViewById<View>(R.id.btnCastingMenu)?.setOnClickListener { view ->
            val popup = PopupMenu(requireContext(), view)
            popup.menu.add("Edit")
            popup.menu.add("Delete")

            popup.setOnMenuItemClickListener { item ->
                when (item.title) {
                    "Edit" -> {
                        dialog.dismiss()
                        val editFragment = CreateCastingFragment().apply {
                            arguments = Bundle().apply {
                                putString("CASTING_CALL_JSON", Gson().toJson(casting))
                            }
                        }
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.nav_host_fragment, editFragment)
                            .addToBackStack(null)
                            .commit()
                    }
                    "Delete" -> {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Delete Casting Call")
                            .setMessage("Are you sure you want to delete this casting call?")
                            .setPositiveButton("Delete") { _, _ ->
                                viewModel.deleteCastingCall(casting.id)
                                dialog.dismiss()
                                Toast.makeText(requireContext(), "Casting Deleted", Toast.LENGTH_SHORT).show()
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                }
                true
            }
            popup.show()
        }

        dialogView.findViewById<View>(R.id.btnViewApplicants)?.setOnClickListener {
            dialog.dismiss()

            // RESTORED: Navigate back to the original CastingManagementActivity
            navigateToManagement(casting.id)
        }

        dialogView.findViewById<View>(R.id.btnDialogClose)?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun isCastingActive(deadline: String?): Boolean {
        if (deadline.isNullOrBlank()) return true
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.time
        val cleaned = deadline.trim().replace(".", "").replace(Regex("\\s+"), " ")
        val formats = listOf("dd MMM yyyy", "d MMM yyyy", "dd MMMM yyyy", "d MMMM yyyy", "yyyy-MM-dd", "dd/MM/yyyy", "MMM dd, yyyy")
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US)
                sdf.isLenient = false
                val date = sdf.parse(cleaned)
                if (date != null) return !date.before(today)
            } catch (e: Exception) { continue }
        }
        return false
    }

    private fun setupTalentPlaceholders(view: View) {
        val grid = view.findViewById<GridLayout>(R.id.glShortlisted) ?: return
        if (grid.childCount > 0) return
        for (i in 1..4) {
            val iv = ShapeableImageView(requireContext()).apply {
                layoutParams = ViewGroup.MarginLayoutParams(160, 160).apply { setMargins(10, 10, 10, 10) }
                scaleType = ImageView.ScaleType.CENTER_CROP
                shapeAppearanceModel = com.google.android.material.shape.ShapeAppearanceModel.builder()
                    .setAllCorners(com.google.android.material.shape.CornerFamily.ROUNDED, 100f).build()
                setImageResource(R.drawable.ic_profile_placeholder)
                alpha = 0.8f
            }
            grid.addView(iv)
        }
    }

    private fun setupListeners() {
        val view = view ?: return
        val navigationView = view.findViewById<NavigationView>(R.id.recruiterSettingsDrawer)

        // Edit Profile Image
        view.findViewById<View>(R.id.btnEditRecruiterAvatar)?.setOnClickListener {
            pickProfileImageLauncher.launch("image/*")
        }

        // Open Sidebar
        view.findViewById<View>(R.id.ivSettings)?.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        // Sidebar Item Clicks
        navigationView?.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_edit_profile -> {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment, EditRecruiterProfileFragment())
                        .addToBackStack(null)
                        .commit()
                }
                R.id.menu_signout -> performLogout()
                R.id.menu_delete_account -> confirmDeleteAccount()
            }
            drawerLayout.closeDrawer(GravityCompat.END)
            true
        }
    }

    private fun openUrl(url: String) {
        if (url.isBlank() || url.lowercase().contains("not listed") || url.length < 4) {
            Toast.makeText(requireContext(), "Link not available", Toast.LENGTH_SHORT).show()
            return
        }

        var formattedUrl = url.trim()
        if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
            formattedUrl = "https://$formattedUrl"
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(formattedUrl))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Invalid link format", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performLogout() {
        viewModel.logout()
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    private fun confirmDeleteAccount() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Profile?")
            .setMessage("Permanently remove your account and all data?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteProfile { success ->
                    if (success) {
                        val intent = Intent(requireContext(), RoleSelectActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        requireActivity().finish()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val contentResolver = requireContext().contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return null

            // Load bitmap for compression
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            val file = File(requireContext().cacheDir, "temp_recruiter_avatar_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)

            // Compress to 70% quality to reduce upload size
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
            outputStream.close()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}