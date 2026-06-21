package com.example.scenex.views.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.scenex.databinding.ItemRecruiterCastingCardBinding
import com.example.scenex.models.RecruiterCastingCallSchedule

class RecruiterCastingCallAdapter(
    private val onViewApplicantsClick: (RecruiterCastingCallSchedule) -> Unit
) : ListAdapter<RecruiterCastingCallSchedule, RecruiterCastingCallAdapter.CastingCallViewHolder>(DiffCallback) {

    inner class CastingCallViewHolder(private val binding: ItemRecruiterCastingCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RecruiterCastingCallSchedule) {
            binding.tvTitle.text = item.title
            binding.tvProduction.text = item.directorName.ifEmpty { "Director Not Assigned" }
            
            // NEW: Bind Audition Date
            binding.tvDate.text = item.date.ifEmpty { "Date TBD" }
            
            binding.tvTime.text = "${item.startTime} - ${item.endTime}"
            
            // This shows the shoot location as mapped in the ViewModel
            binding.tvLocation.text = item.location
            
            binding.tvStatus.text = item.status
            binding.tvTalentApplied.text = "Talents: ${item.appliedCount} applied"

            binding.btnViewApplicants.setOnClickListener { onViewApplicantsClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CastingCallViewHolder {
        val binding = ItemRecruiterCastingCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CastingCallViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CastingCallViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<RecruiterCastingCallSchedule>() {
        override fun areItemsTheSame(oldItem: RecruiterCastingCallSchedule, newItem: RecruiterCastingCallSchedule): Boolean {
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: RecruiterCastingCallSchedule, newItem: RecruiterCastingCallSchedule): Boolean {
            return oldItem == newItem
        }
    }
}
