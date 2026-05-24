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
import com.bumptech.glide.Glide
import com.example.scenex.R
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Senior Architect Implementation: Recruiter Dashboard Home.
 * Header synchronized with Talent Dashboard branding and visual functions.
 */
class RecruiterHomeFragment : Fragment() {

    private lateinit var ivProfileHeader: ShapeableImageView
    private lateinit var tvRecruiterName: TextView
    private lateinit var tvAppName: TextView
    private lateinit var ivNotification: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_recruiter_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Header Components
        ivProfileHeader = view.findViewById(R.id.ivProfileHeader)
        tvRecruiterName = view.findViewById(R.id.tvRecruiterName)
        tvAppName = view.findViewById(R.id.tvAppName)
        ivNotification = view.findViewById(R.id.ivNotification)

        // 1. Sync Branding Colors: Apply primary gradient to App Name
        applyTextGradient(tvAppName)

        // 2. Load Dashboard Handshake Data
        loadRecruiterData()
    }

    private fun loadRecruiterData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("profiles").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val name = document.getString("name") ?: document.getString("fullName") ?: "Recruiter"
                    val profileImage = document.getString("profileImage") ?: document.getString("profileImageUrl") ?: ""

                    // Update Greeting
                    tvRecruiterName.text = name

                    // 3. Sync Image Functions: Load with placeholders and circular crop
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
     * Senior Branding Function: Applies the primary gradient (#B0006D to #4A0038).
     * Ensures consistent visual fidelity across the entire platform.
     */
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
