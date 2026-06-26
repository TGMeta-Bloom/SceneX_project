package com.example.scenex.views

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.scenex.databinding.ActivityCastingManagementBinding
import com.example.scenex.models.UserProfile
import com.example.scenex.viewmodels.CastingManagementViewModel
import com.example.scenex.views.adapter.ApplicantTalentAdapter
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CastingManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCastingManagementBinding
    private val viewModel: CastingManagementViewModel by viewModels()
    private lateinit var applicantAdapter: ApplicantTalentAdapter
    private var currentCastingId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCastingManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentCastingId = intent.getStringExtra("CASTING_ID")
        if (currentCastingId.isNullOrBlank()) {
            Toast.makeText(this, "Error: Invalid Casting ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        observeViewModel()
        viewModel.loadCastingData(currentCastingId!!)
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbar.setNavigationOnClickListener { finish() }

        applicantAdapter = ApplicantTalentAdapter(
            onShortlist = { applicant ->
                viewModel.updateStatus(applicant.talentId, currentCastingId!!, "shortlisted")
                Toast.makeText(this, "${applicant.fullName} Shortlisted", Toast.LENGTH_SHORT).show()
            },
            onReject = { applicant ->
                viewModel.updateStatus(applicant.talentId, currentCastingId!!, "rejected")
                Toast.makeText(this, "Application Rejected", Toast.LENGTH_SHORT).show()
            },
            onProfileClick = { applicant ->
                // 🎯 Senior Implementation: Instant Profile Discovery
                lifecycleScope.launch {
                    try {
                        val profileDoc = FirebaseFirestore.getInstance()
                            .collection("profiles")
                            .document(applicant.talentId)
                            .get()
                            .await()
                        
                        val userProfile = profileDoc.toObject(UserProfile::class.java)?.copy(userId = profileDoc.id)
                        
                        if (userProfile != null) {
                            val detailFragment = TalentDetailFragment.newInstance(userProfile)
                            supportFragmentManager.beginTransaction()
                                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                                .replace(android.R.id.content, detailFragment) // Overlays full screen
                                .addToBackStack(null)
                                .commit()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@CastingManagementActivity, "Error loading profile", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )

        binding.rvApplicants.apply {
            layoutManager = LinearLayoutManager(this@CastingManagementActivity)
            adapter = applicantAdapter
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.castingCall.collect { call ->
                        call?.let {
                            binding.tvManageTitle.text = it.projectTitle
                            binding.tvManageSubtitle.text = "${it.characterName} • ${it.auditionLocation}"
                        }
                    }
                }

                launch {
                    viewModel.applicants.collect { list ->
                        applicantAdapter.submitList(list)
                        updateEmptyState(list.isEmpty(), viewModel.isLoading.value)
                    }
                }

                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.pbApplicants.visibility = if (isLoading) View.VISIBLE else View.GONE
                        updateEmptyState(viewModel.applicants.value.isEmpty(), isLoading)
                    }
                }

                launch {
                    viewModel.error.collect { error ->
                        error?.let {
                            Toast.makeText(this@CastingManagementActivity, it, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean, isLoading: Boolean) {
        binding.tvEmptyApplicants.visibility = if (isEmpty && !isLoading) View.VISIBLE else View.GONE
    }
}
