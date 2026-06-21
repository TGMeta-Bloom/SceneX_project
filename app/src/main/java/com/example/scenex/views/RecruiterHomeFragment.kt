package com.example.scenex.views

import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class RecruiterHomeFragment : Fragment() {

    private lateinit var ivProfileHeader: ShapeableImageView
    private lateinit var tvRecruiterName: TextView
    private lateinit var tvAppName: TextView
    private lateinit var ivNotification: ImageView
    
    private lateinit var rvRecommendedTalent: RecyclerView
    private lateinit var cvTalentExample: View
    private lateinit var fabCreateCasting: Button
    
    private var talentFeedListener: ListenerRegistration? = null
    private var identityListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_recruiter_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ivProfileHeader = view.findViewById(R.id.ivProfileHeader)
        tvRecruiterName = view.findViewById(R.id.tvRecruiterName)
        tvAppName = view.findViewById(R.id.tvAppName)
        ivNotification = view.findViewById(R.id.ivNotification)
        
        rvRecommendedTalent = view.findViewById(R.id.rvRecommendedTalent)
        cvTalentExample = view.findViewById(R.id.cvTalentExample)
        fabCreateCasting = view.findViewById(R.id.fabCreateCasting)

        rvRecommendedTalent.layoutManager = LinearLayoutManager(requireContext())

        // Setup FAB Click
        fabCreateCasting.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, CreateCastingFragment())
                .addToBackStack(null)
                .commit()
        }

        applyTextGradient(tvAppName)
        startIdentitySync()
        startLiveDiscoveryEngine()
    }

    private fun startIdentitySync() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        identityListener = FirebaseFirestore.getInstance().collection("profiles").document(userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    tvRecruiterName.text = snapshot.getString("fullName") ?: "Recruiter"
                    val img = snapshot.getString("profileImage") ?: snapshot.getString("profileImageUrl")
                    Glide.with(this).load(img).placeholder(R.drawable.ic_profile_placeholder).circleCrop().into(ivProfileHeader)
                }
            }
    }

    private fun startLiveDiscoveryEngine() {
        FirebaseFirestore.getInstance().collection("profiles")
            .whereEqualTo("userRole", "TALENT")
            .whereIn("verificationStatus", listOf("verified", "pending"))
            .orderBy("rankingScore", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshots, error ->
                if (error != null) { loadFailSafeTalent(); return@addSnapshotListener }
                if (snapshots != null) displayTalentList(snapshots.toObjects(UserProfile::class.java))
            }
    }

    private fun loadFailSafeTalent() {
        FirebaseFirestore.getInstance().collection("profiles")
            .whereEqualTo("userRole", "TALENT")
            .limit(30).get().addOnSuccessListener { snapshots ->
                val list = snapshots.toObjects(UserProfile::class.java).sortedByDescending { it.rankingScore }
                displayTalentList(list)
            }
    }

    private fun displayTalentList(list: List<UserProfile>) {
        if (list.isNotEmpty()) {
            cvTalentExample.visibility = View.GONE
            rvRecommendedTalent.visibility = View.VISIBLE
            rvRecommendedTalent.adapter = TalentAdapter(list) { talent ->
                val detailFragment = TalentDetailFragment.newInstance(talent)
                parentFragmentManager?.beginTransaction()
                    ?.replace(R.id.nav_host_fragment, detailFragment)
                    ?.addToBackStack(null)
                    ?.commit()
            }
        } else {
            cvTalentExample.visibility = View.VISIBLE
        }
    }

    private fun applyTextGradient(textView: TextView) {
        textView.post {
            val width = textView.paint.measureText(textView.text.toString())
            if (width > 0) {
                val textShader: Shader = LinearGradient(0f, 0f, width, 0f,
                    intArrayOf(Color.parseColor("#B0006D"), Color.parseColor("#4A0038")),
                    null, Shader.TileMode.CLAMP)
                textView.paint.shader = textShader
                textView.invalidate()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        talentFeedListener?.remove()
        identityListener?.remove()
    }
}
