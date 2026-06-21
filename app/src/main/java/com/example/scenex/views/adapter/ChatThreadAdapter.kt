package com.example.scenex.views.adapter

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.scenex.R
import com.example.scenex.databinding.ItemChatThreadBinding
import com.example.scenex.models.Booking
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class ChatThreadAdapter(private val onThreadClick: (Booking) -> Unit) :
    RecyclerView.Adapter<ChatThreadAdapter.ViewHolder>() {

    private var threads = mutableListOf<Booking>()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    private val db = FirebaseFirestore.getInstance()

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
            // Display Project Name
            binding.tvName.text = booking.castingTitle
            
            // Subtitle logic
            val lastMsgText = booking.lastMessage.ifEmpty { "Tap to chat..." }
            binding.tvLastMessage.text = if (booking.status == "RESCHEDULE_REQUESTED") {
                "Negotiation Required: $lastMsgText"
            } else {
                lastMsgText
            }

            // PROFILE IMAGE: Fetch from 'profiles' collection
            val otherUserId = if (currentUserId == booking.recruiterId) booking.talentId else booking.recruiterId
            if (otherUserId.isNotEmpty()) {
                db.collection("profiles").document(otherUserId).get()
                    .addOnSuccessListener { doc ->
                        val imageUrl = doc.getString("profileImage") ?: ""
                        if (imageUrl.isNotEmpty()) {
                            Glide.with(binding.root.context)
                                .load(imageUrl)
                                .placeholder(R.drawable.ic_profile_placeholder)
                                .circleCrop()
                                .into(binding.imgProfile)
                        } else {
                            binding.imgProfile.setImageResource(R.drawable.ic_profile_placeholder)
                        }
                    }
                    .addOnFailureListener {
                        binding.imgProfile.setImageResource(R.drawable.ic_profile_placeholder)
                    }
            }

            // Timestamp
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            binding.tvTime.text = sdf.format(Date(booking.lastActivityTimestamp))

            val amISender = booking.lastMessageSenderId == currentUserId
            
            // UNREAD LOGIC (Blue Dot)
            val isUnread = !booking.isLastMessageSeen && !amISender
            
            // Always hide Seen badge for both sides as requested
            binding.tvSeenBadge.visibility = View.GONE

            if (isUnread) {
                // Show Blue Dot for new incoming message
                binding.unreadIndicator.visibility = View.VISIBLE
                binding.unreadIndicator.background = ContextCompat.getDrawable(binding.root.context, R.drawable.bg_unread_blue_dot)
                binding.tvName.setTypeface(null, Typeface.BOLD)
                binding.tvLastMessage.setTypeface(null, Typeface.BOLD)
            } else {
                // Message is read
                binding.unreadIndicator.visibility = View.GONE
                binding.tvName.setTypeface(null, Typeface.NORMAL)
                binding.tvLastMessage.setTypeface(null, Typeface.NORMAL)
            }

            binding.root.setOnClickListener { onThreadClick(booking) }
        }
    }
}
