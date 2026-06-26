package com.example.scenex.views

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.scenex.databinding.FragmentTalentSchedulingBinding
import com.example.scenex.models.Booking
import com.example.scenex.models.HireRequest
import com.example.scenex.viewmodels.TalentScheduleViewModel
import com.example.scenex.views.adapter.BookingAdapter
import com.example.scenex.views.adapter.TimelineAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Apex Implementation: Unified Scheduling Handshake.
 * Combines Bookings, Direct Hires, and Active Applications into a single reactive timeline.
 */
class TalentSchedulingFragment : Fragment() {

    private var _binding: FragmentTalentSchedulingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TalentScheduleViewModel by viewModels()
    private lateinit var bookingAdapter: BookingAdapter
    private lateinit var timelineAdapter: TimelineAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTalentSchedulingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupToggles()
        setupStatusFilter()
        setupListeners()
        observeViewModel()
        setupNotificationBadge()

        updateToggleUI(isTimeline = true)
        viewModel.startListening()
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
        bookingAdapter = BookingAdapter { booking ->
            // APEX SMART NAVIGATION: Handles three distinct data sources automatically
            when {
                booking.type == "DIRECT_HIRE" || booking.id.startsWith("hire_") -> {
                    val intent = Intent(requireContext(), HireRequestDetailsActivity::class.java)
                    intent.putExtra("HIRE_REQUEST_ID", booking.id)
                    startActivity(intent)
                }
                booking.type == "APPLICATION" -> {
                    // Feedback for active casting call applications (Shortlisted/Pending)
                    Toast.makeText(requireContext(), "Status for ${booking.castingTitle}: ${booking.status}", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    val intent = Intent(requireContext(), TalentBookingDetailsActivity::class.java)
                    intent.putExtra("BOOKING_ID", booking.id)
                    startActivity(intent)
                }
            }
        }
        binding.rvRecruiterBookings.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = bookingAdapter
            isNestedScrollingEnabled = false
        }

        timelineAdapter = TimelineAdapter(isRecruiterView = false)
        binding.rvTimeline.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = timelineAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupStatusFilter() {
        val statuses = arrayOf("All Status", "Pending", "Confirmed", "Cancelled", "Requested Reschedule")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statuses)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStatusFilter.adapter = adapter

        binding.spinnerStatusFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = when (position) {
                    0 -> "ALL"
                    1 -> "PENDING"
                    2 -> "CONFIRMED"
                    3 -> "CANCELLED"
                    4 -> "RESCHEDULE_REQUESTED"
                    else -> "ALL"
                }
                viewModel.setStatusFilter(selected)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun observeViewModel() {
        viewModel.schedules.observe(viewLifecycleOwner) { list ->
            timelineAdapter.submitList(list ?: emptyList())
        }

        // TRIPLE HANDSHAKE OBSERVER: Regular Bookings + Direct Hires + Applications
        viewModel.bookings.observe(viewLifecycleOwner) { updateUnifiedListWrapper() }
        viewModel.hireRequests.observe(viewLifecycleOwner) { updateUnifiedListWrapper() }
        viewModel.applicationsAsBookings.observe(viewLifecycleOwner) { updateUnifiedListWrapper() }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.pbLoading.visibility = if (isLoading == true) View.VISIBLE else View.GONE
        }
    }

    private fun updateUnifiedListWrapper() {
        val bookings = viewModel.bookings.value ?: emptyList()
        val hireRequests = viewModel.hireRequests.value ?: emptyList()
        val applications = viewModel.applicationsAsBookings.value ?: emptyList()
        updateUnifiedList(bookings, hireRequests, applications)
    }

    private fun updateUnifiedList(bookings: List<Booking>, hires: List<HireRequest>, applications: List<Booking>) {
        // Map HireRequests to Booking model format
        val mappedHires = hires.map { hire ->
            Booking(
                id = hire.requestId,
                castingTitle = hire.projectTitle,
                status = hire.status,
                date = hire.createdAt,
                type = "DIRECT_HIRE" 
            )
        }
        
        // Combine all 3 sources into a single sorted high-performance list
        val unifiedList = (bookings + mappedHires + applications).sortedByDescending { it.date }
        bookingAdapter.submitList(unifiedList)
        
        if (unifiedList.isEmpty()) {
            binding.rvRecruiterBookings.visibility = View.GONE
            binding.layoutEmptyBookings.visibility = View.VISIBLE
        } else {
            binding.layoutEmptyBookings.visibility = View.GONE
            binding.rvRecruiterBookings.visibility = View.VISIBLE
        }
    }

    private fun setupToggles() {
        binding.btnTimelineView.setOnClickListener { updateToggleUI(isTimeline = true) }
        binding.btnBookingView.setOnClickListener { updateToggleUI(isTimeline = false) }
    }

    private fun updateToggleUI(isTimeline: Boolean) {
        binding.btnTimelineView.isSelected = isTimeline
        binding.btnBookingView.isSelected = !isTimeline
        binding.layoutTimelineMode.visibility = if (isTimeline) View.VISIBLE else View.GONE
        binding.layoutBookingMode.visibility = if (isTimeline) View.GONE else View.VISIBLE
        binding.txtHeaderSubtitle.text = "Manage your upcoming project bookings"
    }

    private fun setupListeners() {
        binding.btnNotification.setOnClickListener {
            val intent = Intent(requireContext(), NotificationsActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
