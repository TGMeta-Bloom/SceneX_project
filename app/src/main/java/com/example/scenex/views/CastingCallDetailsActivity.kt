package com.example.scenex.views

import android.os.Bundle
import android.widget.Toast
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.scenex.R
import com.example.scenex.models.CastingCall
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.gson.Gson
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CastingCallDetailsActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_casting_call_details)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }

        val castingCallJson = intent.getStringExtra("CASTING_CALL_JSON")
        // Note: If Timestamp fails to deserialize, consider making CastingCall Parcelable
        val call = try {
            Gson().fromJson(castingCallJson, CastingCall::class.java)
        } catch (e: Exception) {
            null
        }

        call?.let { populateDetails(it) } ?: run {
            Toast.makeText(this, "Error loading details", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun populateDetails(call: CastingCall) {
        with(call) {
            findViewById<TextView>(R.id.tvProjectTitleDetail).text = projectTitle
            findViewById<TextView>(R.id.tvRoleAndGender).text = "$characterName • $genderRequirement"

            // Highlights
            findViewById<TextView>(R.id.tvDeadline).text = "Deadline: $submissionDeadline"
            findViewById<TextView>(R.id.tvCategory).text = category
            findViewById<TextView>(R.id.tvCompDetail).text = compensation
            findViewById<TextView>(R.id.tvLangDetail).text = productionLanguage

            // Project Section
            findViewById<TextView>(R.id.tvProductionCompany).text = "Production: $productionCompany"
            findViewById<TextView>(R.id.tvDirector).text = "Director: $directorName"
            findViewById<TextView>(R.id.tvSynopsis).text = projectSynopsis

            // Role Section
            findViewById<TextView>(R.id.tvAgeRange).text = "Age: $minAge - $maxAge Years"
            findViewById<TextView>(R.id.tvExperience).text = "Level: $experienceLevel"
            findViewById<TextView>(R.id.tvSkills).text = "Required: $requiredSkills"
            findViewById<TextView>(R.id.tvCharacterBreakdown).text = characterBreakdown

            // Logistics Section
            findViewById<TextView>(R.id.tvAuditionInfo).text =
                "Audition: $auditionDate ($startTime - $endTime) @ $auditionLocation"
            findViewById<TextView>(R.id.tvShootInfo).text =
                "Shoot Starts: $firstDayOfShoot @ $shootLocation"

            // Image
            Glide.with(this@CastingCallDetailsActivity)
                .load(posterUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .centerCrop()
                .into(findViewById<ShapeableImageView>(R.id.ivPosterDetail))

            findViewById<MaterialButton>(R.id.btnApply).setOnClickListener {
                handleApplication(call)
            }
        }
    }

    private fun handleApplication(call: CastingCall) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Please login to apply", Toast.LENGTH_SHORT).show()
            return
        }

        val application = hashMapOf(
            "castingCallId" to call.id,
            "talentId" to userId,
            "status" to "pending",
            "appliedAt" to com.google.firebase.Timestamp.now()
        )

        firestore.collection("applications")
            .add(application)
            .addOnSuccessListener {
                Toast.makeText(this, "Application submitted successfully!", Toast.LENGTH_SHORT).show()
                findViewById<MaterialButton>(R.id.btnApply).isEnabled = false
                findViewById<MaterialButton>(R.id.btnApply).text = "Applied"
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to apply: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}