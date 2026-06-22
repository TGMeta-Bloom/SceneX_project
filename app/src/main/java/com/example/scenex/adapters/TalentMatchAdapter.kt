package com.example.scenex.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.scenex.R
import com.example.scenex.models.UserProfile
import com.google.android.material.imageview.ShapeableImageView

class TalentMatchAdapter(
    private var talents: List<UserProfile>,
    private val onProfileClick: (UserProfile) -> Unit,
    private val onHireClick: (UserProfile) -> Unit
) : RecyclerView.Adapter<TalentMatchAdapter.TalentViewHolder>() {

    class TalentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardContainer: View = view.findViewById(R.id.cardContainer)
        val ivProfile: ShapeableImageView = view.findViewById(R.id.ivTalentProfile)
        val tvName: TextView = view.findViewById(R.id.tvTalentName)
        val tvRole: TextView = view.findViewById(R.id.tvTalentRole)
        val tvMatch: TextView = view.findViewById(R.id.tvMatchPercentage)
        val tvLocation: TextView = view.findViewById(R.id.tvTalentLocation)
        val tvAvailability: TextView = view.findViewById(R.id.tvAvailability)
        val btnHire: View = view.findViewById(R.id.btnHire)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TalentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_talent_match, parent, false)
        return TalentViewHolder(view)
    }

    override fun onBindViewHolder(holder: TalentViewHolder, position: Int) {
        val talent = talents[position]

        holder.tvName.text = talent.fullName
        holder.tvRole.text = talent.spotlightCategory.ifEmpty { "Talent" }
        
        val matchPercent = (talent.calculated_score * 100).toInt()
        holder.tvMatch.text = "$matchPercent%"
        
        holder.tvLocation.text = talent.city.ifBlank { talent.province }.lowercase()

        holder.tvAvailability.text = if (talent.status == "active") "Available" else "Busy"
        holder.tvAvailability.setTextColor(
            if (talent.status == "active") holder.itemView.context.getColor(android.R.color.holo_green_dark)
            else holder.itemView.context.getColor(android.R.color.holo_red_dark)
        )

        Glide.with(holder.itemView.context)
            .load(talent.effectiveAvatarUrl)
            .placeholder(R.drawable.ic_profile_placeholder)
            .circleCrop()
            .into(holder.ivProfile)

        // 🎯 SEPARATE TRIGGERS
        holder.cardContainer.setOnClickListener { onProfileClick(talent) }
        holder.btnHire.setOnClickListener { onHireClick(talent) }
    }

    override fun getItemCount() = talents.size

    fun updateData(newList: List<UserProfile>) {
        talents = newList
        notifyDataSetChanged()
    }
}
