package com.example.scenex.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.example.scenex.R
import com.example.scenex.viewmodels.SignupViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.imageview.ShapeableImageView

class SignupStep2Fragment : Fragment() {

    private val viewModel: SignupViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_signup_step2, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ivProfileImage = view.findViewById<ShapeableImageView>(R.id.ivProfileImage)
        val chipGroup = view.findViewById<ChipGroup>(R.id.chipGroupSpotlight)
        val btnNext = view.findViewById<Button>(R.id.btnNext)

        // Load the profile picture from Step 1, or use placeholder
        viewModel.profileImageUrl.observe(viewLifecycleOwner) { url ->
            Glide.with(this)
                .load(url)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .into(ivProfileImage)
        }

        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val selectedChip = view.findViewById<Chip>(checkedIds[0])
                viewModel.spotlightCategory = selectedChip.text.toString()
            }
        }

        btnNext.setOnClickListener {
            viewModel.updateSpotlightAndNavigate()
        }

        // Navigation logic
        viewModel.navigateToNextStep.observe(viewLifecycleOwner) { destination ->
            if (destination == "STEP3") {
                // Navigate to Step 3 (Professional Foundation)
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                    )
                    .replace(R.id.signupFragmentContainer, SignupStep3Fragment())
                    .addToBackStack(null)
                    .commit()
                
                // Clear the navigation event so it doesn't trigger again on back
                viewModel.clearNavigation()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
        }
    }
}
