package com.example.scenex.views

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.scenex.MainActivity
import com.example.scenex.R
import com.example.scenex.models.UserProfile
import com.example.scenex.viewmodels.TalentProfileViewModel
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.launch

class TalentProfileFragment : Fragment() {

    private val viewModel: TalentProfileViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_talent_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupListeners()
        viewModel.loadProfile()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.userProfile.collect { profile ->
                        profile?.let { populateUI(it) }
                    }
                }
                launch {
                    viewModel.portfolioImages.collect { images ->
                        updatePortfolioUI(images)
                    }
                }
            }
        }
    }

    private fun populateUI(profile: UserProfile) {
        val view = view ?: return

        view.findViewById<TextView>(R.id.tvTalentFullName).text = profile.fullName
        view.findViewById<TextView>(R.id.tvStageName).text = "aka '${profile.stageName}'"
        view.findViewById<TextView>(R.id.tvSpotlightCategory).text = profile.spotlightCategory
        view.findViewById<TextView>(R.id.tvTalentRegion).text = "${profile.city}, ${profile.province}"

        view.findViewById<TextView>(R.id.tvPhysicalSpecs).text = profile.physicalSpecs.takeIf { it.isNotBlank() } ?: "Physical Specs: Not listed"
        view.findViewById<TextView>(R.id.tvTalentBio).text = profile.bio.takeIf { it.isNotBlank() } ?: "No bio available."
        view.findViewById<TextView>(R.id.tvTalentAgeGender).text = "${profile.age} Yrs Old | ${profile.gender}"
        view.findViewById<TextView>(R.id.tvTalentStatusLanguages).text = "Languages: ${profile.languages}"

        view.findViewById<TextView>(R.id.tvShowreelLink).text = "Showreel: ${profile.showreelUrl}"
        view.findViewById<TextView>(R.id.tvSocialLinks).text = "Social: ${profile.socialMediaLinks}"

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
        
        // Keep the first two (Headshot/FullBody) if they are already there, or rebuild
        // For simplicity, let's just clear and rebuild the dynamic ones
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

        view.findViewById<Button>(R.id.btnTalentEditProfile).setOnClickListener {
            // Navigate to Edit screen (to be created)
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, EditTalentProfileFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<Button>(R.id.btnTalentLogout).setOnClickListener {
            viewModel.logout()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }
}
