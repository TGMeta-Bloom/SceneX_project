package com.example.scenex.views.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.scenex.databinding.ItemApplicantTalentCardBinding
import com.example.scenex.models.ApplicantTalent

class ApplicantTalentAdapter(
    private val onSelectClick: (ApplicantTalent) -> Unit
) : ListAdapter<ApplicantTalent, ApplicantTalentAdapter.ApplicantViewHolder>(DiffCallback) {

    inner class ApplicantViewHolder(private val binding: ItemApplicantTalentCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ApplicantTalent) {
            binding.tvTalentName.text = item.fullName
            binding.tvSpotlightCategory.text = item.spotlightCategory
            
            binding.btnAction.setOnClickListener { onSelectClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplicantViewHolder {
        val binding = ItemApplicantTalentCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ApplicantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ApplicantViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ApplicantTalent>() {
        override fun areItemsTheSame(oldItem: ApplicantTalent, newItem: ApplicantTalent): Boolean {
            return oldItem.talentId == newItem.talentId
        }
        override fun areContentsTheSame(oldItem: ApplicantTalent, newItem: ApplicantTalent): Boolean {
            return oldItem == newItem
        }
    }
}
