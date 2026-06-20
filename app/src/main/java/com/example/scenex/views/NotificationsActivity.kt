package com.example.scenex.views

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.scenex.databinding.ActivityNotificationsBinding
import com.example.scenex.models.Notification
import com.example.scenex.views.adapter.NotificationAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class NotificationsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationsBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Notifications"
        binding.toolbar.setNavigationOnClickListener { finish() }

        val adapter = NotificationAdapter { notif ->
            // Mark as read
            if (!notif.read) {
                db.collection("notifications").document(notif.notificationId).update("read", true)
            }

            // SMART NAVIGATION: Open correct screen based on notification type
            when (notif.type) {
                "BOOKING_REQUEST", "RESCHEDULE_APPROVED", "RESCHEDULE_REJECTED", "SCHEDULE_CONFIRMED" -> {
                    // These are for Talent
                    val intent = Intent(this, TalentBookingDetailsActivity::class.java)
                    intent.putExtra("BOOKING_ID", notif.referenceId)
                    startActivity(intent)
                }
                "BOOKING_ACCEPTED", "RESCHEDULE_REQUEST" -> {
                    // These are for Recruiter - Navigation removed for BOOKING_REJECTED
                    val intent = Intent(this, RecruiterBookingDetailsActivity::class.java)
                    intent.putExtra("BOOKING_ID", notif.referenceId)
                    startActivity(intent)
                }
                "NEW_MESSAGE" -> {
                    val intent = Intent(this, ChatActivity::class.java)
                    intent.putExtra("BOOKING_ID", notif.referenceId)
                    startActivity(intent)
                }
                "BOOKING_REJECTED" -> {
                    // No navigation for rejected bookings, only mark as read (handled above)
                    Log.d("Notifications", "Booking rejected: Staying on notification screen")
                }
            }
        }

        binding.rvNotifications.layoutManager = LinearLayoutManager(this)
        binding.rvNotifications.adapter = adapter

        val currentUserId = auth.currentUser?.uid ?: return
        
        // IMPORTANT: Listen to notifications for the current user
        db.collection("notifications")
            .whereEqualTo("receiverId", currentUserId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("FirestoreError", "Query failed: ${e.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.toObjects(Notification::class.java)
                    adapter.submitList(list)
                    binding.layoutEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                }
            }
    }
}
