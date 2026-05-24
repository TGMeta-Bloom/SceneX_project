package com.example.scenex.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.example.scenex.R
import com.example.scenex.viewmodels.TalentHomeViewModel
import com.google.android.material.imageview.ShapeableImageView

/**
 * Fragment to display and manage the User's Professional Profile.
 */
class ProfileFragment : Fragment() {

    // Reusing TalentHomeViewModel as it already has profile data fetching logic
    private val viewModel: TalentHomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ivProfileLarge = view.findViewById<ShapeableImageView>(R.id.ivProfileLarge)
        val tvProfileName = view.findViewById<TextView>(R.id.tvProfileName)

        // Observe profile data to update the profile screen
        viewModel.profileData.observe(viewLifecycleOwner) { data ->
            data?.let {
                val imageUrl = it["profileImage"] as? String ?: it["profileImageUrl"] as? String
                val name = it["fullName"] as? String ?: it["name"] as? String ?: "User Name"

                tvProfileName.text = name

                Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .circleCrop()
                    .into(ivProfileLarge)
            }
        }

        viewModel.fetchProfileData()
    }
}
