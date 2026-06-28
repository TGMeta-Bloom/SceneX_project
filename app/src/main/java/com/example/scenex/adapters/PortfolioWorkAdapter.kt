package com.example.scenex.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.scenex.R
import com.example.scenex.models.PortfolioWork

class PortfolioWorkAdapter(
    private var works: List<PortfolioWork>,
    private val showOptions: Boolean = true,
    private val onAction: (PortfolioWork, String) -> Unit
) : RecyclerView.Adapter<PortfolioWorkAdapter.WorkViewHolder>() {

    class WorkViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPoster: ImageView = view.findViewById(R.id.ivPoster)
        val tvWorkTitle: TextView = view.findViewById(R.id.tvWorkTitle)
        val tvYear: TextView = view.findViewById(R.id.tvYear)
        val tvRoleType: TextView = view.findViewById(R.id.tvRoleType)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        val ivMore: ImageView = view.findViewById(R.id.ivMoreOptions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_portfolio_work_small, parent, false)
        return WorkViewHolder(view)
    }

    override fun onBindViewHolder(holder: WorkViewHolder, position: Int) {
        val work = works[position]
        holder.tvWorkTitle.text = work.title
        holder.tvYear.text = work.year
        holder.tvRoleType.text = "${work.rolePlayed} | ${work.projectType}"
        holder.tvDescription.text = work.description

        if (!work.imageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(work.imageUrl)
                .centerCrop()
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .into(holder.ivPoster)
        } else {
            holder.ivPoster.setImageResource(R.drawable.ic_profile_placeholder)
            holder.ivPoster.scaleType = ImageView.ScaleType.CENTER_CROP
        }

        holder.ivMore.visibility = if (showOptions) View.VISIBLE else View.GONE
        holder.ivMore.setOnClickListener {
            val popup = android.widget.PopupMenu(holder.itemView.context, holder.ivMore)
            popup.menu.add("View Details")
            popup.menu.add("Edit")
            popup.menu.add("Delete")

            popup.setOnMenuItemClickListener { item ->
                onAction(work, item.title.toString())
                true
            }
            popup.show()
        }

        holder.itemView.setOnClickListener {
            onAction(work, "View Details")
        }
    }

    override fun getItemCount() = works.size

    fun updateWorks(newWorks: List<PortfolioWork>) {
        this.works = newWorks
        notifyDataSetChanged()
    }
}
