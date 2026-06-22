package com.example.scenex.views

import android.content.Context
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
import com.google.android.material.chip.ChipGroup
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RecruiterSearchFragment : Fragment() {

    private lateinit var ivProfileHeader: ShapeableImageView
    private lateinit var tvRecruiterName: TextView
    private lateinit var tvAppName: TextView
    private lateinit var etSearch: EditText
    private lateinit var btnSearchAction: View
    private lateinit var cgHistory: ChipGroup
    private lateinit var cgDiscovery: ChipGroup

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
        cgHistory = view.findViewById(R.id.cgHistory)
        cgDiscovery = view.findViewById(R.id.cgDiscovery)

        applyTextGradient(tvAppName)
        syncRecruiterData()
        loadRecentSearches()
        setupDiscoveryTags()

        // 🎯 Trigger Search: Go straight to live results
        btnSearchAction.setOnClickListener {
            val query = etSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                saveRecentSearch(query)
                searchFilterViewModel.updateQuery(query)
            } else {
                searchFilterViewModel.updateQuery("") // Show all if empty
            }
            navigateToResults()
        }
    }

    private fun setupDiscoveryTags() {
        cgDiscovery.removeAllViews()
        val roles = listOf("Actor", "Singer", "Dancer", "Other")
        roles.forEach { role ->
            val tagView = LayoutInflater.from(requireContext()).inflate(R.layout.item_quick_tag, cgDiscovery, false) as TextView
            tagView.text = role
            tagView.setOnClickListener {
                searchFilterViewModel.updateQuery(role)
                navigateToResults()
            }
            cgDiscovery.addView(tagView)
        }
    }

    private fun saveRecentSearch(query: String) {
        val prefs = requireContext().getSharedPreferences("scenex_history", Context.MODE_PRIVATE)
        val history = prefs.getStringSet("searches", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        history.add(query)
        prefs.edit().putStringSet("searches", history).apply()
    }

    private fun loadRecentSearches() {
        val prefs = requireContext().getSharedPreferences("scenex_history", Context.MODE_PRIVATE)
        val history = prefs.getStringSet("searches", emptySet())?.toList() ?: emptyList()
        
        cgHistory.removeAllViews()
        history.takeLast(5).reversed().forEach { query ->
            val tagView = LayoutInflater.from(requireContext()).inflate(R.layout.item_quick_tag, cgHistory, false) as TextView
            tagView.text = query
            tagView.setOnClickListener {
                searchFilterViewModel.updateQuery(query)
                navigateToResults()
            }
            cgHistory.addView(tagView)
        }
    }

    private fun navigateToResults() {
        // 🎯 Skip fragment_search_filter and go to RecruiterSearchResultsFragment
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
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
