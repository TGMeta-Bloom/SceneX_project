package com.example.scenex.views

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.scenex.R
import com.example.scenex.viewmodels.TalentProfileViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class EditTalentProfileFragment : Fragment(R.layout.fragment_edit_talent_profile) {

    private val viewModel: TalentProfileViewModel by activityViewModels()
    private val currentSkills = mutableListOf<String>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find Views
        val etFullName = view.findViewById<TextInputEditText>(R.id.etFullName)
        val etStageName = view.findViewById<TextInputEditText>(R.id.etStageName)
        val etPhone = view.findViewById<TextInputEditText>(R.id.etPhone)
        val etBio = view.findViewById<TextInputEditText>(R.id.etBio)

        val etProvince = view.findViewById<AutoCompleteTextView>(R.id.etProvince)
        val etCity = view.findViewById<AutoCompleteTextView>(R.id.etCity)

        val etHeight = view.findViewById<AutoCompleteTextView>(R.id.etHeight)
        val etBuild = view.findViewById<AutoCompleteTextView>(R.id.etBuild)
        val etAge = view.findViewById<TextInputEditText>(R.id.etAge)
        val etGender = view.findViewById<AutoCompleteTextView>(R.id.etGender)
        val etHair = view.findViewById<AutoCompleteTextView>(R.id.etHair)
        val etEyes = view.findViewById<AutoCompleteTextView>(R.id.etEyes)

        val etSpotlight = view.findViewById<AutoCompleteTextView>(R.id.etSpotlight)
        val etExperience = view.findViewById<AutoCompleteTextView>(R.id.etExperience)
        val etQualification = view.findViewById<AutoCompleteTextView>(R.id.etQualification)
        val etLanguages = view.findViewById<TextInputEditText>(R.id.etLanguages)

        val tilNewSkill = view.findViewById<TextInputLayout>(R.id.tilNewSkill)
        val etNewSkill = view.findViewById<AutoCompleteTextView>(R.id.etNewSkill)
        val cgEditSkills = view.findViewById<ChipGroup>(R.id.cgEditSkills)

        val etShowreelUrl = view.findViewById<TextInputEditText>(R.id.etShowreelUrl)
        val etSocialLinks = view.findViewById<TextInputEditText>(R.id.etSocialLinks)
        val btnSave = view.findViewById<Button>(R.id.btnSaveTalentChanges)
        val pbLoading = view.findViewById<FrameLayout>(R.id.pbTalentEditLoading)

        // 1. Setup User-Friendly Dropdowns
        setupDropdowns(view)
        viewModel.loadProfile()

        //  Add Skill Logic
        tilNewSkill?.setEndIconOnClickListener {
            val skill = etNewSkill?.text.toString().trim()
            if (skill.isNotEmpty() && !currentSkills.contains(skill)) {
                currentSkills.add(skill)
                addSkillChip(skill, cgEditSkills)
                etNewSkill?.setText("")
            }
        }

        etNewSkill?.setOnItemClickListener { parent, _, position, _ ->
            val selected = parent.getItemAtPosition(position) as String
            if (!currentSkills.contains(selected)) {
                currentSkills.add(selected)
                addSkillChip(selected, cgEditSkills)
            }
            etNewSkill.setText("")
        }

        // Back button
        view.findViewById<MaterialToolbar>(R.id.toolbarEditTalent).setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 2. Load Existing Data
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.userProfile.collect { profile ->
                profile?.let {
                    etFullName.setText(it.fullName)
                    etStageName.setText(it.stageName)
                    etPhone.setText(it.phoneNumber)
                    etBio.setText(it.bio)

                    etProvince.setText(it.province, false)
                    etCity.setText(it.city, false)

                    etAge.setText(it.age.takeIf { a -> a > 0 }?.toString() ?: "")
                    etGender.setText(it.gender, false)
                    etHair.setText(it.hairColor, false)
                    etEyes.setText(it.eyeColor, false)

                    etSpotlight.setText(it.spotlightCategory, false)
                    etExperience.setText(it.experience, false)
                    etQualification.setText(it.qualification, false)
                    etLanguages.setText(it.languages)

                    etShowreelUrl.setText(it.showreelUrl)
                    etSocialLinks.setText(it.socialMediaLinks)

                    //  Parse physicalSpecs back into Height/Build
                    try {
                        val specs = it.physicalSpecs
                        if (specs.contains("|")) {
                            val parts = specs.split("|")
                            parts.forEach { part ->
                                val pair = part.trim().split(":")
                                if (pair.size == 2) {
                                    val key = pair[0].trim().lowercase()
                                    val value = pair[1].trim()
                                    when (key) {
                                        "height" -> etHeight.setText(value, false)
                                        "build" -> etBuild.setText(value, false)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) { e.printStackTrace() }

                    // Load Skills
                    currentSkills.clear()
                    cgEditSkills.removeAllViews()
                    val combinedSkills = (it.accents + it.otherSkills).distinct()
                    combinedSkills.forEach { skill ->
                        currentSkills.add(skill)
                        addSkillChip(skill, cgEditSkills)
                    }
                }
            }
        }

        // 3. Save Logic
        btnSave.setOnClickListener {
            val name = etFullName.text.toString().trim()
            if (name.isEmpty()) {
                etFullName.error = "Name is required"
                return@setOnClickListener
            }

            pbLoading.visibility = View.VISIBLE

            // Constructing physicalSpecs string for UI compatibility
            val h = etHeight.text.toString()
            val b = etBuild.text.toString()
            val g = etGender.text.toString()
            val specString = "Height: $h | Build: $b | Gender: $g"

            val updates = mapOf(
                "fullName" to name,
                "stageName" to etStageName.text.toString(),
                "phoneNumber" to etPhone.text.toString(),
                "bio" to etBio.text.toString(),
                "province" to etProvince.text.toString(),
                "city" to etCity.text.toString(),
                "age" to (etAge.text.toString().toIntOrNull() ?: 0),
                "gender" to g,
                "hairColor" to etHair.text.toString(),
                "eyeColor" to etEyes.text.toString(),
                "physicalSpecs" to specString,
                "spotlightCategory" to etSpotlight.text.toString(),
                "languages" to etLanguages.text.toString(),
                "highest_qualification" to etQualification.text.toString(),
                "experience_level" to etExperience.text.toString(),
                "accents" to currentSkills,
                "showreelUrl" to etShowreelUrl.text.toString(),
                "socialMediaLinks" to etSocialLinks.text.toString()
            )

            viewModel.updateTalentProfile(updates) { success ->
                pbLoading.visibility = View.GONE
                if (success) {
                    Toast.makeText(requireContext(), "Portfolio Updated!", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                } else {
                    Toast.makeText(requireContext(), "Update Failed.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun addSkillChip(skill: String, chipGroup: ChipGroup) {
        val chip = Chip(requireContext()).apply {
            text = skill
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                currentSkills.remove(skill)
                chipGroup.removeView(this)
            }
        }
        chipGroup.addView(chip)
    }

    private fun setupDropdowns(v: View) {
        val provinces = arrayOf("Western", "Central", "Southern", "North Western", "Sabaragamuwa", "Eastern", "Uva", "North Central", "Northern")
        val cities = arrayOf("Colombo", "Kandy", "Galle", "Negombo", "Jaffna", "Matara", "Gampaha", "Kurunegala")

        val heights = (140..215).map { "$it cm" }.toTypedArray()
        val builds = arrayOf("Slim", "Athletic", "Average", "Muscular", "Heavyset", "Petite", "Plus-sized")
        val genders = arrayOf("Male", "Female", "Non-binary", "Other")
        val hair = arrayOf("Black", "Brown", "Blonde", "Auburn", "Grey", "Bald", "Other")
        val eyes = arrayOf("Brown", "Black", "Blue", "Green", "Hazel", "Grey")

        val spots = arrayOf("Actor", "Model", "Voice Artist", "Dancer", "Singer", "Stunt Performer", "Presenter")
        val exps = arrayOf("Beginner", "Intermediate", "Professional: 5–10 years", "Veteran: 10+ years")
        val quals = arrayOf("School Level", "Diploma", "Bachelor’s Degree", "Master’s Degree", "PhD")

        val skills = arrayOf(
            "Singing", "Dancing", "Martial Arts", "Swimming", "Horse Riding",
            "Stunt Performance", "Voice Acting", "Modeling", "Musical Instruments",
            "British Accent", "American Accent", "Southern Accent", "Jaffna Tamil",
            "Batticaloa Tamil", "Up-country Accent", "Sinhala (Fluent)", "English (Fluent)"
        )

        bindAdapter(v.findViewById(R.id.etProvince), provinces)
        bindAdapter(v.findViewById(R.id.etCity), cities)
        bindAdapter(v.findViewById(R.id.etHeight), heights)
        bindAdapter(v.findViewById(R.id.etBuild), builds)
        bindAdapter(v.findViewById(R.id.etGender), genders)
        bindAdapter(v.findViewById(R.id.etHair), hair)
        bindAdapter(v.findViewById(R.id.etEyes), eyes)
        bindAdapter(v.findViewById(R.id.etSpotlight), spots)
        bindAdapter(v.findViewById(R.id.etExperience), exps)
        bindAdapter(v.findViewById(R.id.etQualification), quals)
        bindAdapter(v.findViewById(R.id.etNewSkill), skills)
    }

    private fun bindAdapter(view: AutoCompleteTextView, items: Array<String>) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, items)
        view.setAdapter(adapter)
        view.setOnClickListener { view.showDropDown() }
    }
}
