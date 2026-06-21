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
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class EditTalentProfileFragment : Fragment(R.layout.fragment_edit_talent_profile) {

    private val viewModel: TalentProfileViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find Views
        val etFullName = view.findViewById<TextInputEditText>(R.id.etFullName)
        val etStageName = view.findViewById<TextInputEditText>(R.id.etStageName)
        val etBio = view.findViewById<TextInputEditText>(R.id.etBio)
        val etHeight = view.findViewById<AutoCompleteTextView>(R.id.etHeight)
        val etBuild = view.findViewById<AutoCompleteTextView>(R.id.etBuild)
        val etGender = view.findViewById<AutoCompleteTextView>(R.id.etGender)
        val etExperience = view.findViewById<AutoCompleteTextView>(R.id.etExperience)
        val etQualification = view.findViewById<AutoCompleteTextView>(R.id.etQualification)
        val etLanguages = view.findViewById<TextInputEditText>(R.id.etLanguages)
        val etShowreelUrl = view.findViewById<TextInputEditText>(R.id.etShowreelUrl)
        val etSocialLinks = view.findViewById<TextInputEditText>(R.id.etSocialLinks)
        val btnSave = view.findViewById<Button>(R.id.btnSaveTalentChanges)
        val pbLoading = view.findViewById<FrameLayout>(R.id.pbTalentEditLoading)

        // 1. Setup User-Friendly Dropdowns
        setupDropdowns(view)

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
                    etBio.setText(it.bio)
                    etLanguages.setText(it.languages)
                    etShowreelUrl.setText(it.showreelUrl)
                    etSocialLinks.setText(it.socialMediaLinks)
                    
                    // Note: Height/Build parsing from physicalSpecs if needed, 
                    // or use raw data if available in Firestore keys
                    etGender.setText(it.gender, false)
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
                "bio" to etBio.text.toString(),
                "gender" to g,
                "physicalSpecs" to specString,
                "languages" to etLanguages.text.toString(),
                "highest_qualification" to etQualification.text.toString(),
                "experience_level" to etExperience.text.toString(),
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

    private fun setupDropdowns(v: View) {
        val heights = (140..215).map { "$it cm" }.toTypedArray()
        val builds = arrayOf("Slim", "Athletic", "Average", "Muscular", "Heavyset", "Petite", "Plus-sized")
        val genders = arrayOf("Male", "Female", "Non-binary", "Other")
        val exps = arrayOf("Beginner", "Intermediate", "Professional: 5–10 years", "Veteran: 10+ years")
        val quals = arrayOf("School Level", "Diploma", "Bachelor’s Degree", "Master’s Degree", "PhD")

        bindAdapter(v.findViewById(R.id.etHeight), heights)
        bindAdapter(v.findViewById(R.id.etBuild), builds)
        bindAdapter(v.findViewById(R.id.etGender), genders)
        bindAdapter(v.findViewById(R.id.etExperience), exps)
        bindAdapter(v.findViewById(R.id.etQualification), quals)
    }

    private fun bindAdapter(view: AutoCompleteTextView, items: Array<String>) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, items)
        view.setAdapter(adapter)
        view.setOnClickListener { view.showDropDown() }
    }
}
