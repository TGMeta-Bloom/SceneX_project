package com.example.scenex.views.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.scenex.databinding.ItemScheduleCardBinding
import com.example.scenex.models.Schedule
import java.text.SimpleDateFormat
import java.util.*

class ScheduleAdapter(
    private val onEditClick: (Schedule) -> Unit,
    private val onCancelClick: (Schedule) -> Unit
) : RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder>() {

    private var items: List<Schedule> = emptyList()

    fun submitList(newList: List<Schedule>) {
        items = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val binding = ItemScheduleCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ScheduleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ScheduleViewHolder(private val binding: ItemScheduleCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(schedule: Schedule) {
            // Display Project Title
            binding.txtTitle.text = schedule.castingTitle
            binding.txtLocation.text = schedule.location.ifEmpty { "Location TBD" }
            
            // Handle Time Display Gracefully (Don't show dash if no end time)
            val timeDisplay = if (schedule.endTime.isNotEmpty()) {
                "${schedule.startTime} - ${schedule.endTime}"
            } else {
                schedule.startTime
            }
            binding.txtTime.text = timeDisplay
            
            // Format Date
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            binding.txtDate.text = sdf.format(Date(schedule.date))

            // Branding Sidebar
            binding.sideBar.setBackgroundColor(Color.parseColor("#880E4F"))

            // Action Listeners
            binding.btnEdit.setOnClickListener { onEditClick(schedule) }
            binding.btnCancel.setOnClickListener { onCancelClick(schedule) }
            
            // Logic: Hide edit for confirmed bookings if necessary, 
            // or use it to view details.
        }
    }
}
