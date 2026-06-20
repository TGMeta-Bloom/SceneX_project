package com.example.scenex.views

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.scenex.R
import com.example.scenex.databinding.ActivityChatBinding
import com.example.scenex.models.Booking
import com.example.scenex.models.Message
import com.example.scenex.models.Notification
import com.example.scenex.views.adapter.MessageAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: MessageAdapter
    
    private var bookingId: String? = null
    private var otherPartyName: String? = null
    private var receiverId: String? = null
    
    private var receiverListener: ListenerRegistration? = null
    private var bookingListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bookingId = intent.getStringExtra("BOOKING_ID")
        otherPartyName = intent.getStringExtra("OTHER_PARTY_NAME")
        
        val recruiterId = intent.getStringExtra("RECRUITER_ID")
        val talentId = intent.getStringExtra("TALENT_ID")
        val currentUserId = auth.currentUser?.uid
        receiverId = if (currentUserId == recruiterId) talentId else recruiterId

        setupUI()
        setupRecyclerView()
        listenForMessages()
        loadReceiverProfile()
        listenToBookingStatus()
        updateMyPresence(true)
    }

    private fun setupUI() {
        binding.tvChatName.text = otherPartyName ?: "Chat"
        binding.btnBack.setOnClickListener { finish() }
        
        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
            }
        }

        binding.btnEditBooking.setOnClickListener {
            // Navigate to Edit Booking Screen
            val intent = Intent(this, EditBookingActivity::class.java)
            intent.putExtra("BOOKING_ID", bookingId)
            startActivity(intent)
        }

        binding.rvMessages.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            if (bottom < oldBottom) {
                scrollToBottom()
            }
        }
    }

    private fun listenToBookingStatus() {
        val bId = bookingId ?: return
        val currentUserId = auth.currentUser?.uid ?: return

        bookingListener = db.collection("bookings").document(bId)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val booking = snapshot.toObject(Booking::class.java)
                if (booking != null) {
                    // Business Rule: Show Edit button only if status is RESCHEDULE_REQUESTED and user is the Recruiter
                    if (booking.status == "RESCHEDULE_REQUESTED" && booking.recruiterId == currentUserId) {
                        binding.btnEditBooking.visibility = View.VISIBLE
                    } else {
                        binding.btnEditBooking.visibility = View.GONE
                    }
                }
            }
    }

    private fun loadReceiverProfile() {
        val rId = receiverId ?: return
        receiverListener = db.collection("profiles").document(rId)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val imageUrl = snapshot.getString("profileImage") ?: ""
                if (imageUrl.isNotEmpty()) {
                    Glide.with(this).load(imageUrl).placeholder(R.drawable.ic_profile_placeholder).into(binding.imgChatProfile)
                }
                val receiverInThisChat = snapshot.getString("currentChatWith") == bookingId
                binding.tvOnlineStatus.visibility = if (receiverInThisChat) View.VISIBLE else View.GONE
            }
    }

    private fun updateMyPresence(isEntering: Boolean) {
        val currentUserId = auth.currentUser?.uid ?: return
        db.collection("profiles").document(currentUserId).update("currentChatWith", if (isEntering) bookingId else null)
    }

    private fun setupRecyclerView() {
        adapter = MessageAdapter()
        binding.rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = adapter
    }

    private fun listenForMessages() {
        val id = bookingId ?: return
        db.collection("messages")
            .whereEqualTo("bookingId", id)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val messageList = snapshot.toObjects(Message::class.java)
                    adapter.submitList(messageList)
                    scrollToBottom()
                }
            }
    }

    private fun scrollToBottom() {
        if (adapter.itemCount > 0) {
            binding.rvMessages.postDelayed({
                binding.rvMessages.scrollToPosition(adapter.itemCount - 1)
            }, 100)
        }
    }

    private fun sendMessage(text: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val bId = bookingId ?: return
        val rId = receiverId ?: return

        val batch = db.batch()

        // 1. Save Message
        val messageRef = db.collection("messages").document()
        val newMessage = Message(
            messageId = messageRef.id,
            bookingId = bId,
            senderId = currentUserId,
            receiverId = rId,
            message = text,
            timestamp = System.currentTimeMillis(),
            status = "SENT",
            type = "NORMAL"
        )
        batch.set(messageRef, newMessage)

        // 2. Send Notification to Receiver
        val notifId = db.collection("notifications").document().id
        val notification = Notification(
            notificationId = notifId,
            receiverId = rId,
            senderId = currentUserId,
            senderName = "New Message", 
            title = "New Message",
            message = text.take(50) + if (text.length > 50) "..." else "",
            type = "NEW_MESSAGE",
            referenceId = bId,
            createdAt = System.currentTimeMillis()
        )
        batch.set(db.collection("notifications").document(notifId), notification)

        binding.etMessage.setText("")
        batch.commit().addOnSuccessListener {
            scrollToBottom()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        updateMyPresence(false)
        receiverListener?.remove()
        bookingListener?.remove()
    }
}
