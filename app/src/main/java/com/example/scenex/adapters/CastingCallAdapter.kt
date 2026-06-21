package com.example.scenex.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.scenex.R
import com.example.scenex.models.CastingCall
import com.google.android.material.imageview.ShapeableImageView

class CastingCallAdapter(
    private var castingCalls: List<CastingCall>,
    private val onItemClick: (CastingCall) -> Unit
) : RecyclerView.Adapter<CastingCallAdapter.CastingViewHolder>() {

    class CastingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPoster: ShapeableImageView = view.findViewById(R.id.ivPoster)
        val tvProjectTitle: TextView = view.findViewById(R.id.tvProjectTitle)
        val tvRoleName: TextView = view.findViewById(R.id.tvRoleName)
        val tvProductionCompany: TextView = view.findViewById(R.id.tvProductionCompany)
        val tvDeadline: TextView = view.findViewById(R.id.tvDeadline)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CastingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_casting_call, parent, false)
        return CastingViewHolder(view)
    }

    override fun onBindViewHolder(holder: CastingViewHolder, position: Int) {
        val call = castingCalls[position]
        
        holder.tvProjectTitle.text = call.projectTitle
        holder.tvRoleName.text = "${call.characterName} - ${call.genderRequirement}"
        holder.tvProductionCompany.text = call.productionCompany
        holder.tvDeadline.text = "Deadline: ${call.submissionDeadline}"
        holder.tvCategory.text = call.category

        Glide.with(holder.itemView.context)
            .load(call.posterUrl)
            .placeholder(R.drawable.ic_image_placeholder)
            .error(R.drawable.ic_image_placeholder)
            .into(holder.ivPoster)

        holder.itemView.setOnClickListener { onItemClick(call) }
    }

    override fun getItemCount() = castingCalls.size

    fun updateData(newCalls: List<CastingCall>) {
        this.castingCalls = newCalls
        notifyDataSetChanged()
    }
}
