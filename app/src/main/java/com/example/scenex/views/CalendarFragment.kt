package com.example.scenex.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import com.example.scenex.R
import com.example.scenex.adapters.CalendarAdapter
import com.example.scenex.models.CalendarDay
import com.example.scenex.viewmodels.AvailabilityViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth

class CalendarFragment : Fragment() {

    private lateinit var calendarAdapter: CalendarAdapter
    private val viewModel: AvailabilityViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    
    private var targetTalentId: String = ""

    // UI Components
    private lateinit var btnCalendarView: TextView
    private lateinit var btnListView: TextView
    private lateinit var toggleContainer: ConstraintLayout
    private lateinit var vToggleSelector: View
    private lateinit var cardCalendar: View
    private lateinit var legendContainer: View
    private lateinit var listViewContainer: View
    private lateinit var calendarScrollView: View
    
    // Header Components
    private lateinit var ivHeaderCalendar: View
    private lateinit var tvScheduleHeader: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        targetTalentId = arguments?.getString("arg_talent_id") ?: auth.currentUser?.uid ?: ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_calendar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI(view)
        setupToggle(view)
        setupObservers()
        viewModel.refreshAvailabilityGrid(targetTalentId)
    }

    private fun setupUI(view: View) {
        val rvCalendar = view.findViewById<RecyclerView>(R.id.rvCalendar)
        ivHeaderCalendar = view.findViewById(R.id.ivHeaderCalendar)
        tvScheduleHeader = view.findViewById(R.id.tvScheduleHeader)
        calendarScrollView = view.findViewById(R.id.calendarScrollView)

        calendarAdapter = CalendarAdapter(emptyList()) { day ->
            if (day.events.isNotEmpty()) {
                showEventDetails(day)
            }
        }
        rvCalendar.adapter = calendarAdapter

        view.findViewById<View>(R.id.btnPrevMonth).setOnClickListener { 
            viewModel.navigateMonth(-1, targetTalentId) 
        }
        view.findViewById<View>(R.id.btnNextMonth).setOnClickListener { 
            viewModel.navigateMonth(1, targetTalentId) 
        }
    }

    private fun setupToggle(view: View) {
        btnCalendarView = view.findViewById(R.id.btnCalendarView)
        btnListView = view.findViewById(R.id.btnListView)
        toggleContainer = view.findViewById(R.id.toggleContainer)
        vToggleSelector = view.findViewById(R.id.vToggleSelector)
        cardCalendar = view.findViewById(R.id.cardCalendar)
        legendContainer = view.findViewById(R.id.legendContainer)
        listViewContainer = view.findViewById(R.id.listViewContainer)

        btnCalendarView.setOnClickListener {
            updateToggleState(isCalendar = true)
        }

        btnListView.setOnClickListener {
            updateToggleState(isCalendar = false)
            
            // Load the Dashboard Router
            if (childFragmentManager.findFragmentByTag("LIST_VIEW_CONTAINER") == null) {
                childFragmentManager.beginTransaction()
                    .replace(R.id.listViewContainer, ListViewContainerFragment(), "LIST_VIEW_CONTAINER")
                    .commit()
            }
        }
    }

    private fun updateToggleState(isCalendar: Boolean) {
        if (isCalendar) {
            // Restore Calendar View
            ivHeaderCalendar.visibility = View.VISIBLE
            tvScheduleHeader.visibility = View.VISIBLE
            toggleContainer.visibility = View.VISIBLE
            calendarScrollView.visibility = View.VISIBLE
            listViewContainer.visibility = View.GONE
            
            // Reset toggle selector position
            val constraintSet = ConstraintSet()
            constraintSet.clone(toggleContainer)
            constraintSet.connect(R.id.vToggleSelector, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.clear(R.id.vToggleSelector, ConstraintSet.END)
            TransitionManager.beginDelayedTransition(toggleContainer)
            constraintSet.applyTo(toggleContainer)
            
            btnCalendarView.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            btnListView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        } else {
            // FULL REDIRECT to Dashboard (Hide everything else)
            ivHeaderCalendar.visibility = View.GONE
            tvScheduleHeader.visibility = View.GONE
            toggleContainer.visibility = View.GONE 
            calendarScrollView.visibility = View.GONE
            
            listViewContainer.visibility = View.VISIBLE
        }
    }

    private fun showEventDetails(day: CalendarDay) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_day_details, null)
        
        view.findViewById<TextView>(R.id.tvSheetDate).text = "Schedule for ${day.dateString}"
        val container = view.findViewById<ViewGroup>(R.id.eventListContainer)

        day.events.forEach { event ->
            val itemView = layoutInflater.inflate(R.layout.item_event_detail, container, false)
            itemView.findViewById<TextView>(R.id.tvEventTitle).text = event.title
            itemView.findViewById<TextView>(R.id.tvEventTime).text = event.time
            itemView.findViewById<TextView>(R.id.tvEventDesc).text = event.description
            
            val indicator = itemView.findViewById<View>(R.id.vTypeIndicator)
            val color = when(event.type) {
                com.example.scenex.models.DayStatus.BUSY -> R.color.calendar_orange
                com.example.scenex.models.DayStatus.PENDING -> R.color.calendar_green
                com.example.scenex.models.DayStatus.CASTING_CALL -> R.color.calendar_blue
                else -> R.color.gray
            }
            indicator.setBackgroundResource(color)
            container.addView(itemView)
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun setupObservers() {
        viewModel.calendarDays.observe(viewLifecycleOwner) { days ->
            calendarAdapter.updateDays(days)
        }

        viewModel.currentMonthText.observe(viewLifecycleOwner) { text ->
            view?.findViewById<TextView>(R.id.tvCurrentMonth)?.text = text
        }

        viewModel.syncError.observe(viewLifecycleOwner) { err ->
            err?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
        }
    }
}
