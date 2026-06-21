package com.example.scenex.views

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.scenex.MainActivity
import com.example.scenex.R
import com.example.scenex.models.CastingCall
import com.example.scenex.models.UserProfile
import com.example.scenex.viewmodels.RecruiterProfileViewModel
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Senior Implementation: Recruiter Profile with Robust Date Filtering and Premium UI.
 */
class RecruiterProfileFragment : Fragment() {

    private val viewModel: RecruiterProfileViewModel by activityViewModels()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_recruiter_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupClickListeners()
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

        // 🎯 Robust filtering for active castings
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
                
                // Formatted display matching your screenshot
                itemView.findViewById<TextView>(R.id.tvCastingDetails)?.text =
                    "${casting.characterName} - ${casting.roleType}\nDeadline: ${casting.submissionDeadline}\nLocation: ${casting.auditionLocation}"

                itemView.setOnClickListener { showCastingDetails(casting) }
                llList?.addView(itemView)
            }
        }
        setupTalentPlaceholders(view)
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
        return false // If unparsable, we treat as inactive to keep the UI clean
    }

    private fun showCastingDetails(casting: CastingCall) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_casting_details, null)
        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setView(dialogView)
            .create()

        dialogView.findViewById<TextView>(R.id.tvDialogProjectTitle).text = casting.projectTitle
        dialogView.findViewById<TextView>(R.id.tvDialogProductionType).text = "${casting.productionType} • Directed by ${casting.directorName}"
        dialogView.findViewById<TextView>(R.id.tvDialogRoleInfo).text = "${casting.characterName} (${casting.roleType})"
        dialogView.findViewById<TextView>(R.id.tvDialogSynopsis).text = casting.projectSynopsis
        dialogView.findViewById<TextView>(R.id.tvDialogDeadline).text = casting.submissionDeadline
        dialogView.findViewById<TextView>(R.id.tvDialogLocation).text = casting.auditionLocation
        dialogView.findViewById<TextView>(R.id.tvDialogCompensation)?.text = casting.compensation.ifBlank { "TBD" }
        dialogView.findViewById<TextView>(R.id.tvDialogShootLocation)?.text = casting.shootLocation.ifBlank { "Not Specified" }

        val ivPoster = dialogView.findViewById<ImageView>(R.id.ivDialogPoster)
        if (casting.posterUrl.isNotBlank()) {
            ivPoster.visibility = View.VISIBLE
            Glide.with(this).load(casting.posterUrl).centerCrop().into(ivPoster)
        } else {
            ivPoster.visibility = View.GONE
        }

        dialogView.findViewById<View>(R.id.btnDialogClose).setOnClickListener { dialog.dismiss() }
        dialog.show()
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

    private fun setupClickListeners() {
        val view = view ?: return
        view.findViewById<Button>(R.id.btnRecruiterLogout)?.setOnClickListener {
            viewModel.logout()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnQuickEdit)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, EditRecruiterProfileFragment())
                .addToBackStack(null)
                .commit()
        }
        view.findViewById<TextView>(R.id.tvDeleteAccount)?.setOnClickListener {
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
    }
}
