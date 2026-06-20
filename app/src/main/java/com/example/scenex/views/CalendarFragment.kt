package com.example.scenex.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.example.scenex.R
import com.example.scenex.adapters.CalendarAdapter
import com.example.scenex.models.CalendarDay
import com.example.scenex.viewmodels.AvailabilityViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth

/**
 * Member 1 Implementation: SceneX Dynamic Availability Grid (MVVM).
 * Now supports multi-event discovery via bottom sheet.
 */
class CalendarFragment : Fragment() {

    private lateinit var calendarAdapter: CalendarAdapter
    private val viewModel: AvailabilityViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    
    private var targetTalentId: String = ""

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
        setupObservers()
        viewModel.refreshAvailabilityGrid(targetTalentId)
    }

    private fun setupUI(view: View) {
        val rvCalendar = view.findViewById<RecyclerView>(R.id.rvCalendar)

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
