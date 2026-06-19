package com.example.scenex.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.scenex.R
import com.example.scenex.models.CalendarDay
import com.example.scenex.models.DayStatus

/**
 * Member 1: Reactive Calendar Grid Adapter.
 * Support for Multi-Dot Visualization (Orange, Green, Blue).
 */
class CalendarAdapter(
    private var days: List<CalendarDay>,
    private val onDayClick: (CalendarDay) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.DayViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = days[position]
        holder.tvDay.text = day.dayOfMonth
        
        // Dim days from non-current months
        holder.tvDay.alpha = if (day.isCurrentMonth) 1.0f else 0.3f

        // Clear existing dots
        holder.dotContainer.removeAllViews()

        if (day.isCurrentMonth && day.statuses.isNotEmpty()) {
            // Sort statuses to maintain consistent dot order: BUSY > PENDING > CASTING_CALL
            val sortedStatuses = day.statuses.sortedByDescending { 
                when(it) {
                    DayStatus.BUSY -> 3
                    DayStatus.PENDING -> 2
                    DayStatus.CASTING_CALL -> 1
                    DayStatus.FREE -> 0
                }
            }

            for (status in sortedStatuses) {
                if (status == DayStatus.FREE) continue
                
                val dot = View(holder.itemView.context).apply {
                    val size = (6 * resources.displayMetrics.density).toInt()
                    val margin = (2 * resources.displayMetrics.density).toInt()
                    layoutParams = LinearLayout.LayoutParams(size, size).apply {
                        setMargins(margin, 0, margin, 0)
                    }
                    background = ContextCompat.getDrawable(context, R.drawable.calendar_dot)
                    
                    val colorRes = when (status) {
                        DayStatus.BUSY -> R.color.calendar_orange
                        DayStatus.PENDING -> R.color.calendar_green
                        DayStatus.CASTING_CALL -> R.color.calendar_blue
                        else -> 0
                    }
                    if (colorRes != 0) {
                        backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, colorRes))
                    }
                }
                holder.dotContainer.addView(dot)
            }
        }

        holder.itemView.setOnClickListener { onDayClick(day) }
    }

    override fun getItemCount(): Int = days.size

    fun updateDays(newDays: List<CalendarDay>) {
        this.days = newDays
        notifyDataSetChanged()
    }

    class DayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDay: TextView = view.findViewById(R.id.tvDay)
        val dotContainer: LinearLayout = view.findViewById(R.id.dotContainer)
    }
}
