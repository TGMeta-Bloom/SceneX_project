package com.example.scenex.views.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.scenex.databinding.ItemScheduleCardBinding
import com.example.scenex.models.Schedule

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
            binding.txtTitle.text = schedule.title
            binding.txtProject.text = schedule.description
            binding.txtLocation.text = schedule.location
            binding.txtTime.text = "${schedule.startTime} - ${schedule.endTime}"

            val color = when (schedule.type.uppercase()) {
                "AUDITION" -> "#2196F3"
                "SHOOT" -> "#FF5722"
                else -> "#4CAF50"
            }
            binding.sideBar.setBackgroundColor(Color.parseColor(color))

            binding.btnEdit.setOnClickListener { onEditClick(schedule) }
            binding.btnCancel.setOnClickListener { onCancelClick(schedule) }
        }
    }
}
