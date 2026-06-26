package com.example.scenex.views

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.scenex.R
import com.example.scenex.databinding.FragmentRecruiterApplicantsBinding
import com.example.scenex.viewmodels.RecruiterApplicantsViewModel
import com.example.scenex.views.adapter.ApplicantTalentAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RecruiterApplicantsFragment : Fragment() {

    private var _binding: FragmentRecruiterApplicantsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: RecruiterApplicantsViewModel
    private lateinit var adapter: ApplicantTalentAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private var castingCallId: String? = null
    private var projectTitle: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            castingCallId = it.getString(ARG_CASTING_ID)
            projectTitle = it.getString(ARG_PROJECT_TITLE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecruiterApplicantsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[RecruiterApplicantsViewModel::class.java]
        
        setupToolbar()
        setupRecyclerView()
        observeViewModel()
        setupNotificationBadge()
        
        castingCallId?.let { viewModel.fetchApplicants(it) }
    }

    private fun setupNotificationBadge() {
        val currentUserId = auth.currentUser?.uid ?: return
        db.collection("notifications")
            .whereEqualTo("receiverId", currentUserId)
            .whereEqualTo("read", false)
            .addSnapshotListener { snapshot, _ ->
                if (_binding == null) return@addSnapshotListener
                val count = snapshot?.size() ?: 0
                if (count > 0) {
                    binding.txtNotifCount.visibility = View.VISIBLE
                    binding.txtNotifCount.text = if (count > 9) "9+" else count.toString()
                } else {
                    binding.txtNotifCount.visibility = View.GONE
                }
            }
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { 
            requireActivity().supportFragmentManager.popBackStack()
        }
        
        binding.btnNotifications.setOnClickListener {
            startActivity(Intent(requireContext(), NotificationsActivity::class.java))
        }

        binding.tvProjectTitle.text = projectTitle ?: "Casting Applicants"
    }

    private fun setupRecyclerView() {
        adapter = ApplicantTalentAdapter(
            onShortlist = { applicant ->
                // Placeholder for shortlist action in this view
                Toast.makeText(context, "Shortlisted ${applicant.fullName}", Toast.LENGTH_SHORT).show()
            },
            onReject = { applicant ->
                // Placeholder for reject action in this view
                Toast.makeText(context, "Rejected ${applicant.fullName}", Toast.LENGTH_SHORT).show()
            },
            onProfileClick = { applicant ->
                val createBookingFragment = RecruiterCreateBookingFragment.newInstance(
                    talentId = applicant.talentId,
                    talentName = applicant.fullName,
                    spotlightCategory = applicant.spotlightCategory,
                    castingCallId = castingCallId ?: "",
                    castingTitle = projectTitle ?: ""
                )
                
                requireActivity().supportFragmentManager.beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.nav_host_fragment, createBookingFragment)
                    .addToBackStack(null)
                    .commit()
            }
        )
        binding.rvApplicants.layoutManager = LinearLayoutManager(context)
        binding.rvApplicants.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.pbLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        viewModel.applicants.observe(viewLifecycleOwner) { applicants ->
            if (applicants.isNullOrEmpty()) {
                binding.rvApplicants.visibility = View.GONE
                binding.layoutEmptyState.visibility = View.VISIBLE
            } else {
                binding.layoutEmptyState.visibility = View.GONE
                binding.rvApplicants.visibility = View.VISIBLE
                adapter.submitList(applicants)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_CASTING_ID = "casting_id"
        private const val ARG_PROJECT_TITLE = "project_title"
        fun newInstance(castingId: String, title: String) = RecruiterApplicantsFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_CASTING_ID, castingId)
                putString(ARG_PROJECT_TITLE, title)
            }
        }
    }
}
