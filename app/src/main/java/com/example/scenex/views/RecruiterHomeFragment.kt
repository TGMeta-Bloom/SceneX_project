package com.example.scenex.views

import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.scenex.R
import com.example.scenex.adapters.TalentAdapter
import com.example.scenex.models.UserProfile
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

/**
 * Senior Architect Implementation: Recruiter Dashboard Home.
 * Integrated with the Visibility Prioritization Engine for Recommended Talent.
 */
class RecruiterHomeFragment : Fragment() {

    private lateinit var ivProfileHeader: ShapeableImageView
    private lateinit var tvRecruiterName: TextView
    private lateinit var tvAppName: TextView
    private lateinit var ivNotification: ImageView
    
    private lateinit var rvRecommendedTalent: RecyclerView
    private lateinit var cvTalentExample: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_recruiter_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Components
        ivProfileHeader = view.findViewById(R.id.ivProfileHeader)
        tvRecruiterName = view.findViewById(R.id.tvRecruiterName)
        tvAppName = view.findViewById(R.id.tvAppName)
        ivNotification = view.findViewById(R.id.ivNotification)
        
        rvRecommendedTalent = view.findViewById(R.id.rvRecommendedTalent)
        cvTalentExample = view.findViewById(R.id.cvTalentExample)

        rvRecommendedTalent.layoutManager = LinearLayoutManager(requireContext())

        // 1. Sync Branding Colors: Apply primary gradient to App Name
        applyTextGradient(tvAppName)

        // 2. Load Dashboard Handshake Data
        loadRecruiterData()

        // 3. Populate Recommended Talent using Visibility Prioritization Score
        loadRecommendedTalent()
    }

    private fun loadRecruiterData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("profiles").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val name = document.getString("fullName") ?: "Recruiter"
                    val profileImage = document.getString("profileImage") ?: document.getString("profileImageUrl") ?: ""

                    tvRecruiterName.text = name

                    if (profileImage.isNotEmpty()) {
                        Glide.with(this)
                            .load(profileImage)
                            .placeholder(R.drawable.ic_profile_placeholder)
                            .error(R.drawable.ic_profile_placeholder)
                            .circleCrop()
                            .into(ivProfileHeader)
                    }
                }
            }
    }

    /**
     * DYNAMIC CLOUD OUTPUT: Recommended Talent Feed
     * Executes optimized query using pre-compiled rankingScore from the Weighted Engine.
     */
    private fun loadRecommendedTalent() {
        FirebaseFirestore.getInstance().collection("profiles")
            .whereEqualTo("userRole", "TALENT")
            .whereEqualTo("verificationStatus", "verified")
            .orderBy("rankingScore", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { documents ->
                val talentList = mutableListOf<UserProfile>()
                for (doc in documents) {
                    val talent = doc.toObject(UserProfile::class.java)
                    talentList.add(talent)
                }

                if (talentList.isNotEmpty()) {
                    // Hide static example card and show dynamic feed
                    cvTalentExample.visibility = View.GONE
                    rvRecommendedTalent.adapter = TalentAdapter(talentList)
                }
            }
            .addOnFailureListener { e ->
                // Fallback to static example if query fails (usually due to missing index)
                cvTalentExample.visibility = View.VISIBLE
            }
    }

    private fun applyTextGradient(textView: TextView) {
        textView.post {
            val width = textView.paint.measureText(textView.text.toString())
            if (width > 0) {
                val textShader: Shader = LinearGradient(
                    0f, 0f, width, 0f,
                    intArrayOf(Color.parseColor("#B0006D"), Color.parseColor("#4A0038")),
                    null, Shader.TileMode.CLAMP
                )
                textView.paint.shader = textShader
                textView.invalidate()
            }
        }
    }
}
