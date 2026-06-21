package com.example.scenex.views

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.scenex.R
import com.example.scenex.viewmodels.RecruiterProfileViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class EditRecruiterProfileFragment : Fragment(R.layout.fragment_edit_recruiter_profile) {

    private val viewModel: RecruiterProfileViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find Views
        val ivAvatar = view.findViewById<ShapeableImageView>(R.id.ivEditAvatar)
        val etFullName = view.findViewById<TextInputEditText>(R.id.etFullName)
        val etCompanyName = view.findViewById<TextInputEditText>(R.id.etCompanyName)
        val etRole = view.findViewById<TextInputEditText>(R.id.etRole)
        val etPhone = view.findViewById<TextInputEditText>(R.id.etPhone)
        val etExperience = view.findViewById<TextInputEditText>(R.id.etExperience)
        val etCity = view.findViewById<TextInputEditText>(R.id.etCity)
        val btnSave = view.findViewById<Button>(R.id.btnSaveChanges)
        val pbLoading = view.findViewById<FrameLayout>(R.id.pbEditLoading)

        // Back button
        view.findViewById<MaterialToolbar>(R.id.toolbarEditProfile).setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Load existing data
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.userProfile.collect { profile ->
                profile?.let {
                    etFullName.setText(it.fullName)
                    etCompanyName.setText(it.companyName)
                    etRole.setText(it.spotlightCategory.takeIf { it.isNotBlank() } ?: it.role)
                    etPhone.setText(it.phoneNumber)
                    etExperience.setText(it.experience)
                    etCity.setText(it.city)

                    // Load image using Glide (Display only, update removed)
                    Glide.with(this@EditRecruiterProfileFragment)
                        .load(it.effectiveAvatarUrl)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .into(ivAvatar)
                }
            }
        }

        // Save Button
        btnSave.setOnClickListener {
            pbLoading.visibility = View.VISIBLE

            // Updated call to match the refactored ViewModel (Text updates only)
            viewModel.updateRecruiterProfile(
                etFullName.text.toString(),
                etCompanyName.text.toString(),
                etPhone.text.toString(),
                etExperience.text.toString(),
                etRole.text.toString(),
                etCity.text.toString(),
                "" // Province
            ) { success: Boolean ->
                pbLoading.visibility = View.GONE
                if (success) {
                    Toast.makeText(requireContext(), "Profile Updated!", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                } else {
                    Toast.makeText(requireContext(), "Update Failed.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
