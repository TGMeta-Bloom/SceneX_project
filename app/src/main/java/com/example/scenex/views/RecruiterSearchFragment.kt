package com.example.scenex.views

import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.example.scenex.R
import com.example.scenex.models.UserProfile
import com.example.scenex.viewmodels.SearchFilterViewModel
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RecruiterSearchFragment : Fragment() {

    private lateinit var ivProfileHeader: ShapeableImageView
    private lateinit var tvRecruiterName: TextView
    private lateinit var tvAppName: TextView
    private lateinit var etSearch: EditText
    private lateinit var btnSearchAction: View

    private val searchFilterViewModel: SearchFilterViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_recruiter_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ivProfileHeader = view.findViewById(R.id.ivProfileHeader)
        tvRecruiterName = view.findViewById(R.id.tvRecruiterName)
        tvAppName = view.findViewById(R.id.tvAppName)
        etSearch = view.findViewById(R.id.etSearch)
        btnSearchAction = view.findViewById(R.id.btnSearchAction)

        applyTextGradient(tvAppName)
        syncRecruiterData()
        setupQuickSearchTags(view)

        // 🎯 ELITE TRIGGER: Go directly to results hub with integrated filters
        btnSearchAction.setOnClickListener {
            val query = etSearch.text.toString().trim()
            searchFilterViewModel.updateQuery(query)
            navigateToResults()
        }
    }

    private fun setupQuickSearchTags(view: View) {
        val tagGroupIds = listOf(R.id.cgHistory, R.id.cgDiscovery)
        tagGroupIds.forEach { groupId ->
            val group = view.findViewById<ViewGroup>(groupId) ?: return@forEach
            for (i in 0 until group.childCount) {
                val tag = group.getChildAt(i) as? TextView ?: continue
                tag.setOnClickListener {
                    searchFilterViewModel.updateQuery(tag.text.toString())
                    navigateToResults()
                }
            }
        }
    }

    private fun navigateToResults() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, RecruiterSearchResultsFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun syncRecruiterData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("profiles").document(userId).get()
            .addOnSuccessListener { snapshot ->
                if (isAdded && snapshot.exists()) {
                    val profile = snapshot.toObject(UserProfile::class.java)
                    tvRecruiterName.text = profile?.fullName ?: "Recruiter"
                    Glide.with(this).load(profile?.effectiveAvatarUrl).circleCrop().into(ivProfileHeader)
                }
            }
    }

    private fun applyTextGradient(textView: TextView) {
        textView.post {
            val width = textView.paint.measureText(textView.text.toString())
            if (width > 0) {
                textView.paint.shader = LinearGradient(0f, 0f, width, 0f,
                    intArrayOf(Color.parseColor("#B0006D"), Color.parseColor("#4A0038")),
                    null, Shader.TileMode.CLAMP)
                textView.invalidate()
            }
        }
    }
}
