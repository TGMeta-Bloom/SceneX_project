package com.example.scenex.views.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.scenex.R
import com.example.scenex.databinding.ItemApplicantTalentCardBinding
import com.example.scenex.models.ApplicantTalent

class ApplicantTalentAdapter(
    private val onShortlist: (ApplicantTalent) -> Unit,
    private val onReject: (ApplicantTalent) -> Unit,
    private val onProfileClick: (ApplicantTalent) -> Unit
) : ListAdapter<ApplicantTalent, ApplicantTalentAdapter.ApplicantViewHolder>(DiffCallback) {

    inner class ApplicantViewHolder(private val binding: ItemApplicantTalentCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ApplicantTalent) {
            binding.tvTalentName.text = item.fullName
            binding.tvSpotlightCategory.text = item.spotlightCategory
            
            // Load Avatar
            Glide.with(binding.ivTalentAvatar.context)
                .load(item.avatarUrl)
                .placeholder(R.drawable.ic_profile_placeholder)
                .circleCrop()
                .into(binding.ivTalentAvatar)

            // Status Badge Logic
            binding.tvStatusBadge.text = item.applicationStatus.uppercase()
            val badgeColor = when (item.applicationStatus.lowercase()) {
                "shortlisted" -> R.color.calendar_green
                "rejected" -> R.color.red
                else -> R.color.gradient_start
            }
            binding.tvStatusBadge.setBackgroundColor(ContextCompat.getColor(binding.root.context, badgeColor))

            // Action Listeners
            binding.btnShortlist.setOnClickListener { onShortlist(item) }
            binding.btnReject.setOnClickListener { onReject(item) }
            binding.root.setOnClickListener { onProfileClick(item) }
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
        override fun areItemsTheSame(oldItem: ApplicantTalent, newItem: ApplicantTalent) = 
            oldItem.talentId == newItem.talentId
            
        override fun areContentsTheSame(oldItem: ApplicantTalent, newItem: ApplicantTalent) = 
            oldItem == newItem
    }
}
