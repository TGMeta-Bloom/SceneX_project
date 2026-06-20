package com.example.scenex.views.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.scenex.databinding.ItemChatThreadBinding
import com.example.scenex.models.Booking
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class ChatThreadAdapter(private val onThreadClick: (Booking) -> Unit) :
    RecyclerView.Adapter<ChatThreadAdapter.ViewHolder>() {

    private var threads = mutableListOf<Booking>()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    fun submitList(newThreads: List<Booking>) {
        threads.clear()
        threads.addAll(newThreads)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChatThreadBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val booking = threads[position]
        holder.bind(booking)
    }

    override fun getItemCount(): Int = threads.size

    inner class ViewHolder(private val binding: ItemChatThreadBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(booking: Booking) {
            // Display Project Name as the primary title
            binding.tvName.text = booking.castingTitle
            
            // Show the other party's role/name in the subtitle
            val isRecruiter = currentUserId == booking.recruiterId
            val subtitle = if (isRecruiter) {
                "Talent: ${booking.name}"
            } else {
                "Recruiter: ${booking.recruiterName}"
            }
            binding.tvLastMessage.text = subtitle

            // Simple timestamp formatting
            val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
            binding.tvTime.text = sdf.format(Date(booking.createdAt))

            // Show a "Reschedule" indicator if status is RESCHEDULE_REQUESTED
            if (booking.status == "RESCHEDULE_REQUESTED") {
                binding.unreadIndicator.visibility = android.view.View.VISIBLE
                binding.tvLastMessage.text = "Negotiation Required 🟣"
            } else {
                binding.unreadIndicator.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener { onThreadClick(booking) }
        }
    }
}
