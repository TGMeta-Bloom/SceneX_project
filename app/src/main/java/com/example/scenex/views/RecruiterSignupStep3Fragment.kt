package com.example.scenex.views

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.example.scenex.R
import com.example.scenex.viewmodels.SignupViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.imageview.ShapeableImageView

class RecruiterSignupStep3Fragment : Fragment() {

    private val viewModel: SignupViewModel by activityViewModels()
    private lateinit var ivProfileImage: ShapeableImageView
    
    private val proofLinksList = mutableListOf<String>()
    private var selectedPlatform = "YouTube" 

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_recruiter_signup_step3, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Header Handshake
        ivProfileImage = view.findViewById(R.id.ivProfileImage)
        
        // Form Components
        val etCompanyName = view.findViewById<EditText>(R.id.etCompanyName)
        val etExperience = view.findViewById<EditText>(R.id.etExperience)
        val etProofLink = view.findViewById<EditText>(R.id.etProofLink)
        val chipGroupProofs = view.findViewById<ChipGroup>(R.id.chipGroupProofs)
        val btnAddProof = view.findViewById<MaterialButton>(R.id.btnAddProof)
        val btnNext = view.findViewById<Button>(R.id.btnNext)

        val btnFB = view.findViewById<ImageButton>(R.id.btnFacebook)
        val btnInsta = view.findViewById<ImageButton>(R.id.btnInstagram)
        val btnYouTube = view.findViewById<ImageButton>(R.id.btnYouTube)
        val btnVimeo = view.findViewById<ImageButton>(R.id.btnVimeo)

        val socialButtons = mapOf(
            "Facebook" to btnFB,
            "Instagram" to btnInsta,
            "YouTube" to btnYouTube,
            "Vimeo" to btnVimeo
        )

        fun selectIcon(platformName: String) {
            selectedPlatform = platformName
            socialButtons.forEach { (name, button) ->
                if (name == platformName) {
                    button.setBackgroundResource(R.drawable.button_rounded_magenta)
                    button.imageTintList = ColorStateList.valueOf(Color.WHITE)
                } else {
                    button.setBackgroundResource(R.drawable.edittext_outline)
                    button.imageTintList = null 
                }
            }
            etProofLink.hint = "Paste $platformName link here"
            etProofLink.requestFocus()
        }

        // Default Selection
        selectIcon("YouTube")

        btnFB.setOnClickListener { selectIcon("Facebook") }
        btnInsta.setOnClickListener { selectIcon("Instagram") }
        btnYouTube.setOnClickListener { selectIcon("YouTube") }
        btnVimeo.setOnClickListener { selectIcon("Vimeo") }

        btnAddProof.setOnClickListener {
            val link = etProofLink.text.toString().trim().lowercase()
            
            if (link.isEmpty()) {
                Toast.makeText(context, "Please enter a link first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Logic: Accept major platforms or verified production domains
            val isValid = when {
                link.contains("youtube.com") || link.contains("youtu.be") -> true
                link.contains("vimeo.com") -> true
                link.contains("facebook.com") || link.contains("fb.watch") -> true
                link.contains("instagram.com") -> true
                link.contains(".lk") || link.contains(".com") || link.contains(".tv") -> true 
                else -> false
            }

            if (!isValid) {
                etProofLink.error = "Please enter a valid industry proof link"
                return@setOnClickListener
            }

            val finalEntry = "${selectedPlatform}: $link"
            if (proofLinksList.contains(finalEntry)) {
                Toast.makeText(context, "This link is already added", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            proofLinksList.add(finalEntry)
            
            // Premium Chip Generation
            val chip = Chip(requireContext()).apply {
                text = finalEntry
                isCloseIconVisible = true
                chipBackgroundColor = ColorStateList.valueOf(Color.WHITE)
                chipStrokeColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary_magenta))
                chipStrokeWidth = 2f
                setOnCloseIconClickListener { 
                    chipGroupProofs.removeView(this)
                    proofLinksList.remove(finalEntry)
                }
            }
            chipGroupProofs.addView(chip)
            etProofLink.text.clear()
        }

        btnNext.setOnClickListener {
            val company = etCompanyName.text.toString().trim()
            val exp = etExperience.text.toString().trim()
            
            if (company.isEmpty()) {
                etCompanyName.error = "Production name required"
                return@setOnClickListener
            }
            if (proofLinksList.isEmpty()) {
                Toast.makeText(context, "Add at least one production proof link", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            viewModel.saveRecruiterExperienceAndNavigate(company, proofLinksList, exp)
        }

        // Profile Sync
        viewModel.profileImageUrl.observe(viewLifecycleOwner) { url ->
            if (!url.isNullOrEmpty()) {
                Glide.with(this)
                    .load(url)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .circleCrop()
                    .into(ivProfileImage)
            }
        }

        // Navigation Switchboard
        viewModel.navigateToNextStep.observe(viewLifecycleOwner) { destination ->
            if (destination == "RECRUITER_VERIFICATION") {
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.signupFragmentContainer, RecruiterVerificationFragment())
                    .addToBackStack(null)
                    .commit()
                viewModel.clearNavigation()
            }
        }
    }
}
