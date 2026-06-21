package com.example.scenex.views.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.scenex.databinding.ItemMessageReceivedBinding
import com.example.scenex.databinding.ItemMessageSentBinding
import com.example.scenex.models.Message
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val messages = mutableListOf<Message>()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    fun submitList(newList: List<Message>) {
        messages.clear()
        messages.addAll(newList)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val binding = ItemMessageSentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            SentViewHolder(binding)
        } else {
            val binding = ItemMessageReceivedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ReceivedViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        if (holder is SentViewHolder) holder.bind(message)
        else if (holder is ReceivedViewHolder) holder.bind(message)
    }

    override fun getItemCount(): Int = messages.size

    inner class SentViewHolder(private val binding: ItemMessageSentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(chatMessage: Message) {
            binding.tvMessage.text = chatMessage.message
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            binding.tvTime.text = sdf.format(Date(chatMessage.timestamp))
            
            // Handle Reschedule Type UI
            if (chatMessage.type == "RESCHEDULE") {
                binding.tvMessage.text = "🟣 RESCHEDULE REQUEST:\n${chatMessage.message}"
            }
        }
    }

    inner class ReceivedViewHolder(private val binding: ItemMessageReceivedBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(chatMessage: Message) {
            binding.tvMessage.text = chatMessage.message
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            binding.tvTime.text = sdf.format(Date(chatMessage.timestamp))

            if (chatMessage.type == "RESCHEDULE") {
                binding.tvMessage.text = "🟣 RESCHEDULE REQUEST:\n${chatMessage.message}"
            }
        }
    }
}
