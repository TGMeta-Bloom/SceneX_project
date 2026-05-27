package com.example.scenex.adapters

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.scenex.R
import com.example.scenex.models.UserProfile
import com.google.android.material.imageview.ShapeableImageView

/**
 * Senior Technical Implementation: Talent Discovery Adapter.
 * Optimized for Nested Identity Mapping and Navigation.
 */
class TalentAdapter(
    private val talentList: List<UserProfile>,
    private val onItemClicked: (UserProfile) -> Unit
) : RecyclerView.Adapter<TalentAdapter.TalentViewHolder>() {

    class TalentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivTalentPhoto: ShapeableImageView = view.findViewById(R.id.ivTalentPhoto)
        val tvTalentName: TextView = view.findViewById(R.id.tvTalentName)
        val tvTalentRole: TextView = view.findViewById(R.id.tvTalentRole)
        val tvVisibilityTier: TextView = view.findViewById(R.id.tvVisibilityTier)
        val tvYieldMeter: TextView = view.findViewById(R.id.tvYieldMeter)
        val tvRankingPoints: TextView = view.findViewById(R.id.tvRankingPoints)
        val tvLocation: TextView = view.findViewById(R.id.tvLocation)
        
        val tvPortfolioLink: TextView = view.findViewById(R.id.tvPortfolioLink)
        val tvShowreelLink: TextView = view.findViewById(R.id.tvShowreelLink)
        val tvSocialMatrix: TextView = view.findViewById(R.id.tvSocialMatrix)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TalentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_talent, parent, false)
        return TalentViewHolder(view)
    }

    override fun onBindViewHolder(holder: TalentViewHolder, position: Int) {
        val talent = talentList[position]
        
        // 1. IDENTITY ASSETS
        holder.tvTalentName.text = talent.fullName
        holder.tvTalentRole.text = talent.spotlightCategory.ifEmpty { "Actor" }
        holder.tvLocation.text = "Location : ${talent.city.ifEmpty { "Colombo" }}"

        // 2. IMAGE SYNC
        Glide.with(holder.itemView.context)
            .load(talent.effectiveAvatarUrl)
            .placeholder(R.drawable.ic_profile_placeholder)
            .error(R.drawable.ic_profile_placeholder)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .circleCrop()
            .into(holder.ivTalentPhoto)

        // 3. SYSTEM INTELLIGENCE SYNC
        val points = talent.rankingScore.toInt()
        val yield = talent.completenessScore.toInt()
        
        holder.tvRankingPoints.text = "⚡ $points Ranking Points"
        holder.tvYieldMeter.text = "📊 $yield% Yield Profile Quality"
        
        if (talent.visibility_tier.equals("FEATURED", ignoreCase = true)) {
            holder.tvVisibilityTier.visibility = View.VISIBLE
            holder.tvVisibilityTier.text = "[⭐ FEATURED]"
        } else {
            holder.tvVisibilityTier.visibility = View.GONE
        }

        // 4. INTERACTIVE MEDIA MATRIX
        setupLink(holder.tvPortfolioLink, talent.portfolioLink)
        setupLink(holder.tvShowreelLink, talent.showreelUrl)
        setupLink(holder.tvSocialMatrix, talent.socialMediaLinks)

        // 5. NAVIGATION TRIGGER
        holder.itemView.setOnClickListener {
            onItemClicked(talent)
        }
    }

    private fun setupLink(textView: TextView, url: String) {
        if (url.isNotBlank() && url.startsWith("http")) {
            textView.visibility = View.VISIBLE
            textView.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    it.context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(it.context, "Invalid link", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            textView.visibility = View.GONE
        }
    }

    override fun getItemCount() = talentList.size
}
