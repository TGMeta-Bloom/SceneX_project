package com.example.scenex.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.example.scenex.R
import com.example.scenex.viewmodels.SignupViewModel
import com.google.android.material.imageview.ShapeableImageView

class SignupStep3Fragment : Fragment() {

    private val viewModel: SignupViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_signup_step3, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ivProfileImage = view.findViewById<ShapeableImageView>(R.id.ivProfileImage)
        val etQualification = view.findViewById<EditText>(R.id.etQualification)
        val etLanguages = view.findViewById<EditText>(R.id.etLanguages)
        val etExperience = view.findViewById<EditText>(R.id.etExperience)
        val etPortfolio = view.findViewById<EditText>(R.id.etPortfolio)
        val etSocial = view.findViewById<EditText>(R.id.etSocial)
        val btnContinue = view.findViewById<Button>(R.id.btnContinue)

        // Load profile picture
        viewModel.profileImageUrl.observe(viewLifecycleOwner) { url ->
            Glide.with(this)
                .load(url)
                .placeholder(R.drawable.ic_profile_placeholder)
                .into(ivProfileImage)
        }

        btnContinue.setOnClickListener {
            viewModel.qualification = etQualification.text.toString().trim()
            viewModel.languages = etLanguages.text.toString().trim()
            viewModel.experience = etExperience.text.toString().trim()
            viewModel.portfolioLink = etPortfolio.text.toString().trim()
            viewModel.socialMediaLinks = etSocial.text.toString().trim()

            viewModel.saveFoundationAndNavigate()
        }

        viewModel.navigateToNextStep.observe(viewLifecycleOwner) { destination ->
            when (destination) {
                "ACTOR_SPECS" -> {
                    parentFragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                        .replace(R.id.signupFragmentContainer, ActorPhysicalSpecsFragment())
                        .addToBackStack(null)
                        .commit()
                    viewModel.clearNavigation()
                }
                "EQUIPMENT_GALLERY" -> {
                    Toast.makeText(requireContext(), "Routing to Photographer Equipment...", Toast.LENGTH_SHORT).show()
                    viewModel.clearNavigation()
                }
                "COMPLETE" -> {
                    Toast.makeText(requireContext(), "Signup Complete!", Toast.LENGTH_LONG).show()
                    viewModel.clearNavigation()
                }
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
        }
    }
}
