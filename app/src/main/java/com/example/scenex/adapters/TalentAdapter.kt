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

class TalentAdapter(private val talentList: List<UserProfile>) :
    RecyclerView.Adapter<TalentAdapter.TalentViewHolder>() {

    class TalentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivTalentPhoto: ShapeableImageView = view.findViewById(R.id.ivTalentPhoto)
        val tvTalentName: TextView = view.findViewById(R.id.tvTalentName)
        val tvTalentRole: TextView = view.findViewById(R.id.tvTalentRole)
        val tvMatch: TextView = view.findViewById(R.id.tvMatch)
        val tvLocation: TextView = view.findViewById(R.id.tvLocation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TalentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_talent, parent, false)
        return TalentViewHolder(view)
    }

    override fun onBindViewHolder(holder: TalentViewHolder, position: Int) {
        val talent = talentList[position]
        holder.tvTalentName.text = talent.fullName
        holder.tvTalentRole.text = talent.spotlightCategory
        holder.tvMatch.text = "Match : ${talent.rankingScore}%"
        holder.tvLocation.text = "Location : ${talent.city}"

        if (talent.profileImage.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(talent.profileImage)
                .placeholder(R.drawable.ic_profile_placeholder)
                .circleCrop()
                .into(holder.ivTalentPhoto)
        }
    }

    override fun getItemCount() = talentList.size
}
