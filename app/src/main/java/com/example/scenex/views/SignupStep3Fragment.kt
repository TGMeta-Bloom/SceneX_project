package com.example.scenex.views

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
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

class SignupStep3Fragment : Fragment() {

    private val viewModel: SignupViewModel by activityViewModels()
    private var selectedPlatform: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_signup_step3, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ivProfileImage = view.findViewById<ShapeableImageView>(R.id.ivProfileImage)
        val etQualification = view.findViewById<AutoCompleteTextView>(R.id.etQualification)
        val etLanguagesInput = view.findViewById<AutoCompleteTextView>(R.id.etLanguages)
        val chipGroupLanguages = view.findViewById<ChipGroup>(R.id.chipGroupLanguages)
        val etExperience = view.findViewById<AutoCompleteTextView>(R.id.etExperience)
        val etPortfolio = view.findViewById<EditText>(R.id.etPortfolio)
        
        // Social Media Components
        val btnFB = view.findViewById<ImageButton>(R.id.btnFacebook)
        val btnInsta = view.findViewById<ImageButton>(R.id.btnInstagram)
        val btnTikTok = view.findViewById<ImageButton>(R.id.btnTikTok)
        val btnTwitter = view.findViewById<ImageButton>(R.id.btnTwitter)
        val btnLinkedIn = view.findViewById<ImageButton>(R.id.btnLinkedIn)
        val btnAddSocial = view.findViewById<MaterialButton>(R.id.btnAddSocial)
        val etSocial = view.findViewById<EditText>(R.id.etSocial)
        val chipGroupAddedSocials = view.findViewById<ChipGroup>(R.id.chipGroupAddedSocials)
        
        val btnContinue = view.findViewById<Button>(R.id.btnContinue)

        setupDropdowns(etQualification, etLanguagesInput, chipGroupLanguages, etExperience)

        // Load profile picture
        viewModel.profileImageUrl.observe(viewLifecycleOwner) { url ->
            Glide.with(this)
                .load(url)
                .placeholder(R.drawable.ic_profile_placeholder)
                .into(ivProfileImage)
        }

        // --- Social Media logic ---
        val socialButtons = mapOf(
            "Facebook" to btnFB,
            "Instagram" to btnInsta,
            "TikTok" to btnTikTok,
            "Twitter" to btnTwitter,
            "LinkedIn" to btnLinkedIn
        )

        socialButtons.forEach { (platform, button) ->
            button?.setOnClickListener {
                // Reset backgrounds
                socialButtons.values.forEach { 
                    it?.setBackgroundResource(R.drawable.edittext_outline)
                    it?.imageTintList = null 
                }
                
                // Highlight selection
                button.setBackgroundResource(R.drawable.button_rounded_magenta)
                button.imageTintList = ColorStateList.valueOf(Color.WHITE)
                
                selectedPlatform = platform
                etSocial.hint = "Paste $platform profile link"
                etSocial.requestFocus()
            }
        }

        btnAddSocial.setOnClickListener {
            val link = etSocial.text.toString().trim()
            if (selectedPlatform.isEmpty()) {
                Toast.makeText(context, "Select a platform icon first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (link.isEmpty()) {
                etSocial.error = "Paste link here"
                return@setOnClickListener
            }

            addSocialTag(selectedPlatform, link, chipGroupAddedSocials)
            etSocial.text.clear()
        }

        btnContinue.setOnClickListener {
            viewModel.qualification = etQualification.text.toString().trim()
            viewModel.experience = etExperience.text.toString().trim()
            viewModel.portfolioLink = etPortfolio.text.toString().trim()
            
            // Join Languages
            val languages = mutableListOf<String>()
            for (i in 0 until chipGroupLanguages.childCount) {
                languages.add((chipGroupLanguages.getChildAt(i) as Chip).text.toString())
            }
            viewModel.languages = languages.joinToString(", ")

            // Join Social Links
            val socials = mutableListOf<String>()
            for (i in 0 until chipGroupAddedSocials.childCount) {
                socials.add((chipGroupAddedSocials.getChildAt(i) as Chip).text.toString())
            }
            viewModel.socialMediaLinks = socials.joinToString(" | ")

            viewModel.saveFoundationAndNavigate()
        }

        viewModel.navigateToNextStep.observe(viewLifecycleOwner) { destination ->
            when (destination) {
                "ACTOR_SPECS" -> navigateTo(ActorPhysicalSpecsFragment())
                "STEP5" -> navigateTo(SignupStep5Fragment())
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
        }
    }

    private fun addSocialTag(platform: String, link: String, chipGroup: ChipGroup) {
        val chip = Chip(requireContext())
        chip.text = "$platform: $link"
        chip.isCloseIconVisible = true
        chip.chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white))
        chip.chipStrokeColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary_magenta))
        chip.chipStrokeWidth = 2f
        chip.setOnCloseIconClickListener { chipGroup.removeView(chip) }
        chipGroup.addView(chip)
    }

    private fun setupDropdowns(q: AutoCompleteTextView, l: AutoCompleteTextView, cg: ChipGroup, e: AutoCompleteTextView) {
        val qualifications = arrayOf("High School Diploma", "Diploma in Performing Arts", "Bachelor’s Degree", "Master’s Degree", "Professional Certification")
        val languagesList = arrayOf("English", "Spanish", "French", "Mandarin", "Hindi", "Sinhala", "Tamil")
        val expLevels = arrayOf("Beginner: 0–1 year", "Junior: 1–3 years", "Intermediate: 3–5 years", "Professional: 5–10 years", "Expert: 10+ years")

        q.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, qualifications))
        q.setOnClickListener { q.showDropDown() }

        l.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, languagesList))
        l.setOnClickListener { l.showDropDown() }
        l.setOnItemClickListener { parent, _, pos, _ ->
            val lang = parent.getItemAtPosition(pos).toString()
            addChip(lang, cg)
            l.setText("")
        }

        e.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, expLevels))
        e.setOnClickListener { e.showDropDown() }
    }

    private fun addChip(text: String, chipGroup: ChipGroup) {
        for (i in 0 until chipGroup.childCount) {
            if ((chipGroup.getChildAt(i) as Chip).text == text) return
        }
        val chip = Chip(requireContext())
        chip.text = text
        chip.isCloseIconVisible = true
        chip.setOnCloseIconClickListener { chipGroup.removeView(chip) }
        chipGroup.addView(chip)
    }

    private fun navigateTo(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
            .replace(R.id.signupFragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
        viewModel.clearNavigation()
    }
}
