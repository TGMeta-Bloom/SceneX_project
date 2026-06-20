package com.example.scenex.views

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.scenex.R
import com.example.scenex.databinding.FragmentRecruiterBookingBinding
import com.example.scenex.viewmodels.RecruiterCastingCallScheduleViewModel
import com.example.scenex.views.adapter.RecruiterCastingCallAdapter
import com.example.scenex.views.adapter.TimelineAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RecruiterBookingFragment : Fragment() {

    private var _binding: FragmentRecruiterBookingBinding? = null
    private val binding get() = _binding!!

    private lateinit var castingViewModel: RecruiterCastingCallScheduleViewModel
    private lateinit var castingAdapter: RecruiterCastingCallAdapter
    private lateinit var timelineAdapter: TimelineAdapter
    
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecruiterBookingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        castingViewModel = ViewModelProvider(this)[RecruiterCastingCallScheduleViewModel::class.java]

        setupRecyclerViews()
        setupToggles()
        setupListeners()
        observeViewModel()
        setupNotificationBadge()

        // Set default view to Timeline
        binding.btnTimelineView.performClick()
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

    private fun setupRecyclerViews() {
        // Casting Calls Adapter
        castingAdapter = RecruiterCastingCallAdapter { castingCall ->
            val fragment = RecruiterApplicantsFragment.newInstance(castingCall.id, castingCall.title)
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .addToBackStack(null)
                .commit()
        }
        binding.rvCastingCalls.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = castingAdapter
            isNestedScrollingEnabled = false
        }

        // Recruiter View: Show Talent name in timeline
        timelineAdapter = TimelineAdapter(isRecruiterView = true)
        binding.rvTimeline.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = timelineAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupListeners() {
        binding.ivNotification.setOnClickListener {
            startActivity(Intent(requireContext(), NotificationsActivity::class.java))
        }
    }

    private fun setupToggles() {
        binding.btnTimelineView.setOnClickListener {
            binding.btnTimelineView.isSelected = true
            binding.btnCastingView.isSelected = false
            binding.layoutTimelineMode.visibility = View.VISIBLE
            binding.layoutCastingMode.visibility = View.GONE
            castingViewModel.fetchTimelineSchedules()
        }

        binding.btnCastingView.setOnClickListener {
            binding.btnCastingView.isSelected = true
            binding.btnTimelineView.isSelected = false
            binding.layoutTimelineMode.visibility = View.GONE
            binding.layoutCastingMode.visibility = View.VISIBLE
            castingViewModel.fetchCastingCalls("ACTIVE")
        }
    }

    private fun observeViewModel() {
        castingViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.pbLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe Casting Calls
        castingViewModel.castingCalls.observe(viewLifecycleOwner) { calls ->
            if (calls.isNullOrEmpty()) {
                binding.rvCastingCalls.visibility = View.GONE
                binding.layoutEmptyStateCasting.visibility = View.VISIBLE
            } else {
                binding.layoutEmptyStateCasting.visibility = View.GONE
                binding.rvCastingCalls.visibility = View.VISIBLE
                castingAdapter.submitList(calls)
            }
        }

        // Observe Timeline Schedules
        castingViewModel.timelineSchedules.observe(viewLifecycleOwner) { schedules ->
            if (schedules.isNullOrEmpty()) {
                binding.rvTimeline.visibility = View.GONE
                binding.layoutEmptyStateTimeline.visibility = View.VISIBLE
            } else {
                binding.layoutEmptyStateTimeline.visibility = View.GONE
                binding.rvTimeline.visibility = View.VISIBLE
                timelineAdapter.submitList(schedules)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
