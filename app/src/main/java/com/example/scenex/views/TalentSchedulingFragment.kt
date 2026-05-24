package com.example.scenex.views

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.scenex.databinding.FragmentTalentSchedulingBinding
import com.example.scenex.viewmodels.TalentScheduleViewModel
import com.example.scenex.views.adapter.ScheduleAdapter
import com.example.scenex.views.adapter.TimelineAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout

class TalentSchedulingFragment : Fragment() {

    private var _binding: FragmentTalentSchedulingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TalentScheduleViewModel by viewModels()
    private lateinit var scheduleAdapter: ScheduleAdapter
    private lateinit var timelineAdapter: TimelineAdapter

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
        setupTabs()
        setupToggle()
        setupListeners()
        observeViewModel()

        // 1. Initial State: Show Calendar View
        updateToggleUI(isCalendar = true)
        
        // 2. Start real-time Firestore listener
        viewModel.startListening()
        
        // 3. Apply default filter
        viewModel.applyFilter("UPCOMING")
    }

    private fun setupRecyclerViews() {
        scheduleAdapter = ScheduleAdapter(
            onEditClick = { schedule ->
                val intent = Intent(requireContext(), AddScheduleActivity::class.java)
                intent.putExtra("SCHEDULE_ID", schedule.id)
                startActivity(intent)
            },
            onCancelClick = { schedule ->
                showDeleteConfirmationDialog(schedule.id)
            }
        )
        binding.rvBookings.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = scheduleAdapter
        }

        timelineAdapter = TimelineAdapter()
        binding.rvTimeline.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = timelineAdapter
        }
    }

    private fun showDeleteConfirmationDialog(scheduleId: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cancel Schedule")
            .setMessage("Are you sure you want to cancel this schedule? This action cannot be undone.")
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Yes, Cancel") { dialog, _ ->
                viewModel.deleteSchedule(scheduleId)
                dialog.dismiss()
            }
            .show()
    }

    private fun setupToggle() {
        binding.btnCalendarView.setOnClickListener {
            updateToggleUI(isCalendar = true)
        }

        binding.btnListView.setOnClickListener {
            updateToggleUI(isCalendar = false)
        }
    }

    private fun updateToggleUI(isCalendar: Boolean) {
        binding.btnCalendarView.isSelected = isCalendar
        binding.btnListView.isSelected = !isCalendar

        binding.btnCalendarView.setTextColor(if (isCalendar) Color.WHITE else Color.BLACK)
        binding.btnListView.setTextColor(if (!isCalendar) Color.WHITE else Color.BLACK)

        if (isCalendar) {
            binding.layoutCalendarMode.visibility = View.VISIBLE
            binding.rvBookings.visibility = View.GONE
        } else {
            binding.layoutCalendarMode.visibility = View.GONE
            binding.rvBookings.visibility = View.VISIBLE
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val type = if (tab?.position == 0) "UPCOMING" else "PAST"
                // Switching filter now happens instantly in the ViewModel
                viewModel.applyFilter(type)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupListeners() {
        binding.btnNewSchedule.setOnClickListener {
            val intent = Intent(requireContext(), AddScheduleActivity::class.java)
            startActivity(intent)
        }
    }

    private fun observeViewModel() {
        viewModel.schedules.observe(viewLifecycleOwner) { schedules ->
            scheduleAdapter.submitList(schedules)
            timelineAdapter.submitList(schedules)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
