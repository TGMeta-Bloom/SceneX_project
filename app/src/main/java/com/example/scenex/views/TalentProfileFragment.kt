package com.example.scenex.views

import android.content.Intent
import android.os.Bundle
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.scenex.MainActivity
import com.example.scenex.R
import com.example.scenex.adapters.PortfolioWorkAdapter
import com.example.scenex.models.PortfolioWork
import com.example.scenex.models.UserProfile
import com.example.scenex.viewmodels.AvailabilityViewModel
import com.example.scenex.viewmodels.SignupViewModel
import com.example.scenex.viewmodels.TalentProfileViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class TalentProfileFragment : Fragment() {

    private val profileViewModel: TalentProfileViewModel by activityViewModels()
    private val availabilityViewModel: AvailabilityViewModel by viewModels()
    private val signupViewModel: SignupViewModel by activityViewModels()

    private lateinit var switchManualAvailability: MaterialSwitch
    private lateinit var tvAvailabilityBadge: TextView
    private lateinit var creditsAdapter: PortfolioWorkAdapter
    private lateinit var drawerLayout: DrawerLayout

    private var ivDialogPreview: ShapeableImageView? = null
    private var btnDialogSave: Button? = null
    private var selectedWorkImageUrl: String? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            uriToFile(it)?.let { file ->
                ivDialogPreview?.alpha = 0.5f
                btnDialogSave?.isEnabled = false
                btnDialogSave?.text = "Uploading..."

                signupViewModel.uploadWorkImage(file) { url ->
                    ivDialogPreview?.alpha = 1.0f
                    btnDialogSave?.isEnabled = true
                    btnDialogSave?.text = "Add Credit"

                    if (url != null) {
                        selectedWorkImageUrl = url
                        ivDialogPreview?.let { preview ->
                            Glide.with(this).load(url).into(preview)
                        }
                    } else {
                        Toast.makeText(requireContext(), "Image upload failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private val pickProfileImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            uriToFile(it)?.let { file ->
                Toast.makeText(requireContext(), "Uploading profile image...", Toast.LENGTH_SHORT).show()
                signupViewModel.uploadWorkImage(file) { url ->
                    if (url != null) {
                        //  FIX: Update both profileImage and profileImageUrl to ensure UI updates
                        val updates = mapOf(
                            "profileImage" to url,
                            "profileImageUrl" to url
                        )
                        profileViewModel.updateTalentProfile(updates) { success ->
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
        return inflater.inflate(R.layout.fragment_talent_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        switchManualAvailability = view.findViewById(R.id.switchAvailability)
        tvAvailabilityBadge = view.findViewById(R.id.tvAvailabilityBadge)

        val rvCredits = view.findViewById<RecyclerView>(R.id.rvProfessionalCredits)
        creditsAdapter = PortfolioWorkAdapter(works = emptyList()) { work, action ->
            handleWorkAction(work, action)
        }
        rvCredits.layoutManager = LinearLayoutManager(requireContext())
        rvCredits.adapter = creditsAdapter

        setupObservers()
        setupListeners()

        profileViewModel.loadProfile()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    profileViewModel.userProfile.collect { profile ->
                        profile?.let {
                            populateUI(it)
                            // Resolved: Trigger Intelligence Engine Calculation
                            availabilityViewModel.resolveCombinedStatus(it.userId)
                        }
                    }
                }
                launch {
                    profileViewModel.portfolioImages.collect { images ->
                        updatePortfolioUI(images)
                    }
                }
            }
        }

        // Intelligence Logic Observer
        availabilityViewModel.manualStatusPreference.observe(viewLifecycleOwner) { status ->
            switchManualAvailability.setOnCheckedChangeListener(null)
            switchManualAvailability.isChecked = status == "AVAILABLE"
            attachSwitchListener()
        }

        availabilityViewModel.calculatedStatus.observe(viewLifecycleOwner) { statusText ->
            tvAvailabilityBadge.text = statusText

            when {
                statusText.contains("Available") -> tvAvailabilityBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.calendar_green))
                statusText.contains("Busy") -> tvAvailabilityBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.calendar_orange))
                else -> tvAvailabilityBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_dark))
            }
        }
    }

    private fun attachSwitchListener() {
        switchManualAvailability.setOnCheckedChangeListener { _, isChecked ->
            val status = if (isChecked) "AVAILABLE" else "UNAVAILABLE"
            availabilityViewModel.updateManualOverride(status)
        }
    }

    private fun populateUI(profile: UserProfile) {
        val view = view ?: return

        view.findViewById<TextView>(R.id.tvTalentFullName).text = profile.fullName

        // Removed "aka" as per requirement
        val tvStageName = view.findViewById<TextView>(R.id.tvStageName)
        if (profile.stageName.isNullOrBlank() || profile.stageName == profile.fullName) {
            tvStageName.visibility = View.GONE
        } else {
            tvStageName.visibility = View.VISIBLE
            tvStageName.text = profile.stageName
        }

        view.findViewById<TextView>(R.id.tvSpotlightCategory).text = profile.spotlightCategory
        view.findViewById<TextView>(R.id.tvTalentRegion).text = "${profile.city}, ${profile.province}"

        // Appearance
        view.findViewById<TextView>(R.id.tvTalentAgeGender).text = "${profile.age} Yrs | ${profile.gender}"
        view.findViewById<TextView>(R.id.tvPhysicalSpecs).text = profile.physicalSpecs.takeIf { it.isNotBlank() } ?: "Physical Specs: Not listed"
        view.findViewById<TextView>(R.id.tvHairEyeColor).text = "Hair: ${profile.hairColor.takeIf { it.isNotBlank() } ?: "N/A"} | Eyes: ${profile.eyeColor.takeIf { it.isNotBlank() } ?: "N/A"}"
        view.findViewById<TextView>(R.id.tvTalentBio).text = profile.bio.takeIf { it.isNotBlank() } ?: "No bio available."

        // Professional Highlights
        view.findViewById<TextView>(R.id.tvExperienceLevel).text = "Experience: ${profile.experience.takeIf { it.isNotBlank() } ?: "Beginner"}"
        view.findViewById<TextView>(R.id.tvQualification).text = "Qualification: ${profile.qualification.takeIf { it.isNotBlank() } ?: "School Level"}"
        view.findViewById<TextView>(R.id.tvTalentLanguages).text = "Languages: ${profile.languages}"

        // Skills & Accents (Chips)
        val cgSkills = view.findViewById<ChipGroup>(R.id.cgSkillsAccents)
        cgSkills.removeAllViews()

        val allSkills = (profile.accents + profile.otherSkills).distinct()
        if (allSkills.isEmpty()) {
            val emptyChip = Chip(requireContext()).apply { text = "No specific skills listed" }
            cgSkills.addView(emptyChip)
        } else {
            allSkills.forEach { skill ->
                val chip = Chip(requireContext(), null, com.google.android.material.R.style.Widget_Material3_Chip_Suggestion).apply {
                    text = skill
                    isClickable = false
                    setChipBackgroundColorResource(android.R.color.transparent)
                    setChipStrokeColorResource(R.color.primary_magenta)
                    chipStrokeWidth = 1f
                }
                cgSkills.addView(chip)
            }
        }

        view.findViewById<TextView>(R.id.tvShowreelLink).text = "Showreel: ${profile.showreelUrl}"
        view.findViewById<TextView>(R.id.tvSocialLinks).text = "Social: ${profile.socialMediaLinks}"

        // Update Credits
        creditsAdapter.updateWorks(profile.portfolioWorks)

        val avatar = view.findViewById<ShapeableImageView>(R.id.ivTalentAvatar)
        Glide.with(this).load(profile.effectiveAvatarUrl)
            .placeholder(R.drawable.ic_profile_placeholder)
            .circleCrop().into(avatar)

        val ivHeadshot = view.findViewById<ImageView>(R.id.ivHeadshotPhoto)
        if (profile.headshotUrl.isNotBlank()) {
            Glide.with(this).load(profile.headshotUrl).centerCrop().into(ivHeadshot)
        }

        val ivFullBody = view.findViewById<ImageView>(R.id.ivFullBodyPhoto)
        if (profile.fullBodyUrl.isNotBlank()) {
            Glide.with(this).load(profile.fullBodyUrl).centerCrop().into(ivFullBody)
        }
    }

    private fun updatePortfolioUI(images: List<String>) {
        val llPortfolio = view?.findViewById<LinearLayout>(R.id.llPortfolioImages) ?: return
        val childCount = llPortfolio.childCount
        if (childCount > 2) {
            llPortfolio.removeViews(2, childCount - 2)
        }

        images.forEach { url ->
            val iv = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(360, ViewGroup.LayoutParams.MATCH_PARENT).apply {
                    setMargins(0, 0, 12, 0)
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(resources.getColor(R.color.gray))
            }
            Glide.with(this).load(url).centerCrop().into(iv)
            llPortfolio.addView(iv)
        }
    }

    private fun setupListeners() {
        val view = view ?: return
        drawerLayout = view.findViewById(R.id.drawerLayout)
        val navigationView = view.findViewById<NavigationView>(R.id.settingsDrawer)

        // Edit Profile Image
        view.findViewById<View>(R.id.btnEditTalentAvatar)?.setOnClickListener {
            pickProfileImageLauncher.launch("image/*")
        }

        // Quick Edit Skills
        view.findViewById<View>(R.id.btnEditSkills)?.setOnClickListener {
            navigateToEditProfile()
        }

        // Open Sidebar
        view.findViewById<View>(R.id.btnOpenSettings).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        // Media & Social Link Clicks
        view.findViewById<TextView>(R.id.tvShowreelLink).setOnClickListener {
            val text = (it as TextView).text.toString()
            val url = text.substringAfter("Showreel: ").trim()
            openUrl(url)
        }

        view.findViewById<TextView>(R.id.tvSocialLinks).setOnClickListener {
            val text = (it as TextView).text.toString()
            val url = text.substringAfter("Social: ").trim()
            openUrl(url)
        }

        // Media & Social Link Clicks
        view.findViewById<TextView>(R.id.tvShowreelLink).setOnClickListener {
            val text = (it as TextView).text.toString()
            val url = text.substringAfter("Showreel: ").trim()
            openUrl(url)
        }

        view.findViewById<TextView>(R.id.tvSocialLinks).setOnClickListener {
            val text = (it as TextView).text.toString()
            val url = text.substringAfter("Social: ").trim()
            openUrl(url)
        }

        // Sidebar Item Clicks
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_edit_profile -> navigateToEditProfile()
                R.id.menu_signout -> performLogout()
                R.id.menu_delete_account -> confirmDeleteAccount()
            }
            drawerLayout.closeDrawer(GravityCompat.END)
            true
        }

        view.findViewById<Button>(R.id.btnProfileAddWork).setOnClickListener {
            showAddWorkDialog(null)
        }
    }

    private fun navigateToEditProfile() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, EditTalentProfileFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun performLogout() {
        profileViewModel.logout()
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    private fun confirmDeleteAccount() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to permanently delete your account and all data? This cannot be undone.")
            .setPositiveButton("Delete Permanently") { _, _ ->
                profileViewModel.deleteAccount { success ->
                    if (success) {
                        Toast.makeText(requireContext(), "Account Deleted", Toast.LENGTH_SHORT).show()
                        val intent = Intent(requireContext(), LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        requireActivity().finish()
                    } else {
                        Toast.makeText(requireContext(), "Failed to delete account.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handleWorkAction(work: PortfolioWork, action: String) {
        when (action) {
            "View Details" -> showWorkDetailsDialog(work)
            "Edit" -> showAddWorkDialog(work)
            "Delete" -> confirmDeleteWork(work)
        }
    }

    private fun showWorkDetailsDialog(work: PortfolioWork) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_portfolio_work, null)
        val dialog = AlertDialog.Builder(requireContext(), R.style.SceneX_Dialog_Rounded)
            .setView(dialogView)
            .create()

        dialogView.findViewById<TextView>(R.id.dialogTitle).text = "Credit Details"
        val ivPreview = dialogView.findViewById<ShapeableImageView>(R.id.ivWorkImagePreview)
        val etTitle = dialogView.findViewById<EditText>(R.id.etWorkTitle)
        val etProjectType = dialogView.findViewById<EditText>(R.id.etProjectType)
        val etRole = dialogView.findViewById<EditText>(R.id.etRolePlayed)
        val etYear = dialogView.findViewById<EditText>(R.id.etYear)
        val etDesc = dialogView.findViewById<EditText>(R.id.etDescription)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveWork)

        // Fill data
        etTitle.setText(work.title)
        etProjectType.setText(work.projectType)
        etRole.setText(work.rolePlayed)
        etYear.setText(work.year)
        etDesc.setText(work.description)

        Glide.with(this).load(work.imageUrl).placeholder(R.drawable.ic_profile_placeholder).into(ivPreview)

        // Make read-only
        etTitle.isEnabled = false
        etProjectType.isEnabled = false
        etRole.isEnabled = false
        etYear.isEnabled = false
        etDesc.isEnabled = false
        dialogView.findViewById<TextView>(R.id.tvAddImageLabel).visibility = View.GONE
        ivPreview.isClickable = false

        btnSave.text = "Close"
        btnSave.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun confirmDeleteWork(work: PortfolioWork) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Credit")
            .setMessage("Are you sure you want to remove '${work.title}'?")
            .setPositiveButton("Delete") { _, _ ->
                profileViewModel.deletePortfolioWork(work) { success ->
                    if (success) Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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

    private fun showAddWorkDialog(editWork: PortfolioWork? = null) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_portfolio_work, null)
        val dialog = AlertDialog.Builder(requireContext(), R.style.SceneX_Dialog_Rounded)
            .setView(dialogView)
            .create()

        if (editWork != null) {
            dialogView.findViewById<TextView>(R.id.dialogTitle).text = "Edit Professional Credit"
        }

        ivDialogPreview = dialogView.findViewById(R.id.ivWorkImagePreview)
        val tvAddImage = dialogView.findViewById<TextView>(R.id.tvAddImageLabel)
        val etTitle = dialogView.findViewById<EditText>(R.id.etWorkTitle)
        val etProjectType = dialogView.findViewById<EditText>(R.id.etProjectType)
        val etRole = dialogView.findViewById<EditText>(R.id.etRolePlayed)
        val etYear = dialogView.findViewById<EditText>(R.id.etYear)
        val etDesc = dialogView.findViewById<EditText>(R.id.etDescription)
        btnDialogSave = dialogView.findViewById(R.id.btnSaveWork)

        selectedWorkImageUrl = editWork?.imageUrl

        if (editWork != null) {
            etTitle.setText(editWork.title)
            etProjectType.setText(editWork.projectType)
            etRole.setText(editWork.rolePlayed)
            etYear.setText(editWork.year)
            etDesc.setText(editWork.description)
            Glide.with(this).load(editWork.imageUrl).placeholder(R.drawable.ic_profile_placeholder).into(ivDialogPreview!!)
            btnDialogSave?.text = "Update Credit"
        }

        val pickAction = View.OnClickListener {
            pickImageLauncher.launch("image/*")
        }
        ivDialogPreview?.setOnClickListener(pickAction)
        tvAddImage.setOnClickListener(pickAction)

        btnDialogSave?.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val type = etProjectType.text.toString().trim()
            val role = etRole.text.toString().trim()
            val year = etYear.text.toString().trim()
            val desc = etDesc.text.toString().trim()

            if (title.isEmpty() || type.isEmpty() || role.isEmpty() || year.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill mandatory fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val work = PortfolioWork(
                title = title,
                projectType = type,
                rolePlayed = role,
                year = year,
                description = desc,
                imageUrl = selectedWorkImageUrl
            )

            if (editWork != null) {
                profileViewModel.editPortfolioWork(editWork, work) { success ->
                    if (success) {
                        Toast.makeText(requireContext(), "Updated!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                }
            } else {
                profileViewModel.addPortfolioWork(work) { success ->
                    if (success) {
                        Toast.makeText(requireContext(), "Credit added successfully!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(requireContext(), "Failed to add credit.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun uriToFile(uri: android.net.Uri): File? {
        return try {
            val contentResolver = requireContext().contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return null

            // Load bitmap for compression
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            val file = File(requireContext().cacheDir, "temp_profile_image_${System.currentTimeMillis()}.jpg")
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
