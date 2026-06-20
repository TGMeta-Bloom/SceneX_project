package com.example.scenex.views.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.scenex.databinding.ItemScheduleTimelineBinding
import com.example.scenex.models.Schedule
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class TimelineAdapter(private val isRecruiterView: Boolean = true) : RecyclerView.Adapter<TimelineAdapter.TimelineViewHolder>() {

    private var items: List<Schedule> = emptyList()
    private val dateFormatter = SimpleDateFormat("dd MMM", Locale.getDefault())
    private val db = FirebaseFirestore.getInstance()
    
    // Cache to prevent flickering and redundant Firestore calls
    private val nameCache = mutableMapOf<String, String>()

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
            // 1. Left side shows Date
            val dateLabel = if (schedule.date != 0L) dateFormatter.format(Date(schedule.date)) else ""
            binding.txtTime.text = dateLabel

            // 2. Main Title
            binding.txtTitle.text = schedule.castingTitle

            // 3. The "Who" factor (Purple Label)
            if (isRecruiterView) {
                // Recruiter sees Talent's name
                binding.txtTalentName.text = "Talent: ${schedule.name}"
            } else {
                // Talent Dashboard: Fetch Recruiter Name from 'profiles' collection
                val recruiterId = schedule.recruiterId
                if (recruiterId.isNotEmpty()) {
                    if (nameCache.containsKey(recruiterId)) {
                        binding.txtTalentName.text = "Recruiter: ${nameCache[recruiterId]}"
                    } else {
                        // Display generic label while loading
                        binding.txtTalentName.text = "Recruiter: ..."
                        
                        // Fetch the actual fullName from profiles collection
                        db.collection("profiles").document(recruiterId).get()
                            .addOnSuccessListener { doc ->
                                val fullName = doc.getString("fullName") ?: doc.getString("name") ?: "Official Recruiter"
                                nameCache[recruiterId] = fullName
                                binding.txtTalentName.text = "Recruiter: $fullName"
                            }
                    }
                } else {
                    binding.txtTalentName.text = "Recruiter"
                }
            }

            // 4. Details: Time Range, Role, Location (Date removed)
            val timeRange = if (schedule.endTime.isNotEmpty()) {
                "${schedule.startTime} - ${schedule.endTime}"
            } else {
                schedule.startTime
            }
            
            binding.txtDetails.text = "${timeRange}\n${schedule.role}\n${schedule.location}"

            // Timeline visual logic
            binding.timelineLine.visibility = if (isLast) View.INVISIBLE else View.VISIBLE
        }
    }
}
