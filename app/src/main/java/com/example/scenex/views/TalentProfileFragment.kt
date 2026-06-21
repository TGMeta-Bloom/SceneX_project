package com.example.scenex.views

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.scenex.MainActivity
import com.example.scenex.R
import com.example.scenex.models.UserProfile
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TalentProfileFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_talent_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadTalentInformationRecords()
        setupListeners()
    }

    private fun loadTalentInformationRecords() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("profiles").document(uid).get()
            .addOnSuccessListener { snapshot ->
                val record = snapshot.toObject(UserProfile::class.java)
                record?.let { populateUI(it) }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error fetching profile database map", Toast.LENGTH_SHORT).show()
            }
    }

    private fun populateUI(profile: UserProfile) {
        val view = view ?: return

        view.findViewById<TextView>(R.id.tvTalentFullName).text = profile.fullName
        view.findViewById<TextView>(R.id.tvStageName).text = "aka '${profile.stageName}'"
        view.findViewById<TextView>(R.id.tvSpotlightCategory).text = profile.spotlightCategory
        view.findViewById<TextView>(R.id.tvTalentRegion).text = "${profile.city}, ${profile.province}"

        view.findViewById<TextView>(R.id.tvPhysicalSpecs).text = "Physical Parameters: ${profile.physicalSpecs}"
        view.findViewById<TextView>(R.id.tvTalentBio).text = "Biography: ${profile.bio}"
        view.findViewById<TextView>(R.id.tvTalentAgeGender).text = "Parameters: ${profile.age} Yrs Old | ${profile.gender}"
        view.findViewById<TextView>(R.id.tvTalentStatusLanguages).text = "Languages: ${profile.languages} | status: ${profile.relationshipStatus}"

        view.findViewById<TextView>(R.id.tvShowreelLink).text = "Showreel Link: ${profile.showreelUrl}"
        view.findViewById<TextView>(R.id.tvSocialLinks).text = "Social Assets: ${profile.socialMediaLinks}"

        // Load Core Identity Asset Frames using Glide framework
        val avatar = view.findViewById<ShapeableImageView>(R.id.ivTalentAvatar)
        if (avatar != null) {
            Glide.with(this).load(profile.effectiveAvatarUrl).circleCrop().into(avatar)
        }

        val ivHeadshot = view.findViewById<ImageView>(R.id.ivHeadshotPhoto)
        if (ivHeadshot != null && profile.headshotUrl.isNotBlank()) {
            Glide.with(this).load(profile.headshotUrl).centerCrop().into(ivHeadshot)
        }

        val ivFullBody = view.findViewById<ImageView>(R.id.ivFullBodyPhoto)
        if (ivFullBody != null && profile.fullBodyUrl.isNotBlank()) {
            Glide.with(this).load(profile.fullBodyUrl).centerCrop().into(ivFullBody)
        }
    }

    private fun setupListeners() {
        val view = view ?: return

        view.findViewById<Button>(R.id.btnTalentEditProfile).setOnClickListener {
            Toast.makeText(context, "Opening personal identity modifications layout form...", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<Button>(R.id.btnUpdatePortfolioAssets).setOnClickListener {
            Toast.makeText(context, "Opening Studio Camera media asset selector", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<Button>(R.id.btnTalentChangePassword).setOnClickListener {
            Toast.makeText(context, "Authentication change query sent successfully.", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<Button>(R.id.btnTalentLogout).setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}