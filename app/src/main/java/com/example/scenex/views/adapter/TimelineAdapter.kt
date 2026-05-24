package com.example.scenex.views.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.scenex.databinding.ItemScheduleTimelineBinding
import com.example.scenex.models.Schedule

class TimelineAdapter : RecyclerView.Adapter<TimelineAdapter.TimelineViewHolder>() {

    private var items: List<Schedule> = emptyList()

    fun submitList(newList: List<Schedule>) {
        items = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimelineViewHolder {
        val binding = ItemScheduleTimelineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TimelineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TimelineViewHolder, position: Int) {
        holder.bind(items[position], position == items.size - 1)
    }

    override fun getItemCount(): Int = items.size

    inner class TimelineViewHolder(private val binding: ItemScheduleTimelineBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(schedule: Schedule, isLast: Boolean) {
            binding.txtTime.text = schedule.startTime
            binding.txtTitle.text = schedule.title
            binding.txtDetails.text = "${schedule.description}\n${schedule.location}\n${schedule.startTime} - ${schedule.endTime}"
            
            // Hide the bottom line for the last item to match the image
            binding.timelineLine.visibility = if (isLast) View.INVISIBLE else View.VISIBLE
        }
    }
}
