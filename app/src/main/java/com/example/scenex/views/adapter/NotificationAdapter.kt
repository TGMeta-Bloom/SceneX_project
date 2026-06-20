package com.example.scenex.views.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.scenex.databinding.ItemNotificationBinding
import com.example.scenex.models.Notification
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(private val onItemClick: (Notification) -> Unit) :
    ListAdapter<Notification, NotificationAdapter.NotifViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotifViewHolder {
        return NotifViewHolder(
            ItemNotificationBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: NotifViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    class NotifViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(notif: Notification) {
            binding.txtNotifTitle.text = notif.title
            binding.txtNotifMessage.text = notif.message
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            binding.txtNotifTime.text = sdf.format(Date(notif.createdAt))
            
            // Show unread indicator if not read
            binding.viewUnreadIndicator.visibility = if (notif.read) View.GONE else View.VISIBLE
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Notification>() {
        override fun areItemsTheSame(oldItem: Notification, newItem: Notification) =
            oldItem.notificationId == newItem.notificationId

        override fun areContentsTheSame(oldItem: Notification, newItem: Notification) =
            oldItem == newItem
    }
}
