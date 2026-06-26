package com.example.scenex.views

import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_casting_call_details)

        firestore = FirebaseFirestore.getInstance()

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }

        val castingCallJson = intent.getStringExtra("CASTING_CALL_JSON")
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
        val tvTitle = findViewById<TextView>(R.id.tvProjectTitleDetail)
        val tvRole = findViewById<TextView>(R.id.tvRoleAndGender)
        val tvProdCompany = findViewById<TextView>(R.id.tvProductionCompany)
        val tvExperience = findViewById<TextView>(R.id.tvExperience)
        val tvSkills = findViewById<TextView>(R.id.tvSkills)
        
        with(call) {
            tvTitle.text = projectTitle
            tvRole.text = "$characterName • $genderRequirement"

            applyTextGradient(tvTitle)
            applyTextGradient(tvRole)
            applyTextGradient(tvProdCompany)

            findViewById<TextView>(R.id.tvDeadline).text = "Deadline: $submissionDeadline"
            findViewById<TextView>(R.id.tvCategory).text = category
            findViewById<TextView>(R.id.tvCompDetail).text = compensation
            findViewById<TextView>(R.id.tvLangDetail).text = productionLanguage

            tvProdCompany.text = "Production: $productionCompany"
            findViewById<TextView>(R.id.tvDirector).text = "Director: $directorName"
            findViewById<TextView>(R.id.tvSynopsis).text = projectSynopsis

            findViewById<TextView>(R.id.tvAgeRange).text = "Age: $minAge - $maxAge Years"
            findViewById<TextView>(R.id.tvExperience).text = "Level: $experienceLevel"
            findViewById<TextView>(R.id.tvSkills).text = "Required: $requiredSkills"
            findViewById<TextView>(R.id.tvCharacterBreakdown).text = characterBreakdown

            findViewById<TextView>(R.id.tvAuditionInfo).text =
                "Audition: $auditionDate ($startTime - $endTime) @ $auditionLocation"
            findViewById<TextView>(R.id.tvShootInfo).text =
                "Shoot Starts: $firstDayOfShoot @ $shootLocation"

            Glide.with(this@CastingCallDetailsActivity)
                .load(posterUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .centerCrop()
                .into(findViewById<ShapeableImageView>(R.id.ivPosterDetail))

            findViewById<MaterialButton>(R.id.btnApply).setOnClickListener {
                handleApplication(call)
            }
        }
    }

    private fun applyTextGradient(textView: TextView) {
        textView.post {
            val width = textView.paint.measureText(textView.text.toString())
            if (width > 0) {
                val textShader: Shader = LinearGradient(
                    0f, 0f, width, 0f,
                    intArrayOf(
                        ContextCompat.getColor(this, R.color.gradient_start),
                        ContextCompat.getColor(this, R.color.gradient_end)
                    ), null, Shader.TileMode.CLAMP
                )
                textView.paint.shader = textShader
                textView.invalidate()
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
            "projectTitle" to call.projectTitle,
            "posterUrl" to call.posterUrl,
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
