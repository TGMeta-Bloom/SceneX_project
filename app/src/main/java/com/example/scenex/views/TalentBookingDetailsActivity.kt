package com.example.scenex.views

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.scenex.MainActivity
import com.example.scenex.databinding.ActivityTalentBookingDetailsBinding
import com.example.scenex.models.Booking
import com.example.scenex.models.Message
import com.example.scenex.models.Notification
import com.example.scenex.models.Schedule
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

class TalentBookingDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTalentBookingDetailsBinding
    private val db = FirebaseFirestore.getInstance()
    private var bookingId: String? = null
    private var bookingListener: ListenerRegistration? = null
    private var currentBooking: Booking? = null
    private var hasConflictDetected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTalentBookingDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""
        binding.toolbar.setNavigationOnClickListener { finish() }

        bookingId = intent.getStringExtra("BOOKING_ID")
        
        if (bookingId != null) {
            startBookingListener(bookingId!!)
        } else {
            Toast.makeText(this, "Error: Booking data not found", Toast.LENGTH_SHORT).show()
            finish()
        }

        setupButtons()
    }

    private fun startBookingListener(id: String) {
        bookingListener = db.collection("bookings").document(id)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener

                if (snapshot != null && snapshot.exists()) {
                    val booking = snapshot.toObject(Booking::class.java)
                    booking?.let { 
                        currentBooking = it
                        updateUI(it)
                        checkLiveConflicts(it)
                    }
                } else if (snapshot != null && !snapshot.exists()) {
                    finish()
                }
            }
    }

    private fun updateUI(booking: Booking) {
        if (booking.projectImageUrl.isNotEmpty()) {
            Glide.with(this).load(booking.projectImageUrl).centerCrop().into(binding.imgProjectBanner)
        }
        binding.txtDetailProjectTitle.text = booking.castingTitle
        binding.txtRecruiterName.text = if (booking.recruiterName.isNotEmpty()) booking.recruiterName else "Official Recruiter"
        
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        binding.txtDetailDate.text = sdf.format(Date(booking.date))
        binding.txtDetailTime.text = "${booking.startTime} - ${booking.endTime}"
        binding.txtDetailLocation.text = if (booking.location.isNotEmpty()) booking.location else "Not specified"
        binding.txtCategory.text = booking.role
        
        val status = booking.status.uppercase()
        binding.txtStatus.text = status.replace("_", " ")
        
        val statusColor = when (status) {
            "PENDING" -> "#FFB300"
            "CONFIRMED" -> "#4CAF50"
            "CANCELLED", "REJECTED" -> "#F44336"
            "COMPLETED" -> "#2196F3"
            "RESCHEDULE_REQUESTED" -> "#9C27B0" 
            else -> "#9E9E9E"
        }
        binding.txtStatus.setTextColor(Color.parseColor(statusColor))

        binding.layoutActionButtons.visibility = if (status == "PENDING") View.VISIBLE else View.GONE
        
        binding.txtDetailNotes.text = if (booking.notes.isNotEmpty()) booking.notes else "No notes provided."
        
        binding.txtSuccessTitle.text = booking.castingTitle
        binding.txtSuccessDate.text = sdf.format(Date(booking.date))
        binding.txtSuccessTime.text = "${booking.startTime} - ${booking.endTime}"
    }

    private fun checkLiveConflicts(booking: Booking) {
        if (booking.status != "PENDING" && booking.status != "CONFIRMED" && booking.status != "RESCHEDULE_REQUESTED") {
            setConflictState(false)
            return
        }

        val talentId = booking.talentId
        val date = booking.date
        val bookingId = booking.id

        db.collection("schedules")
            .whereEqualTo("userId", talentId)
            .whereEqualTo("date", date)
            .get()
            .addOnSuccessListener { snapshot ->
                val hasScheduleConflict = snapshot.toObjects(Schedule::class.java).any { 
                    it.bookingId != bookingId && isTimeOverlapping(booking.startTime, booking.endTime, it.startTime, it.endTime)
                }
                
                if (hasScheduleConflict) {
                    setConflictState(true)
                } else {
                    db.collection("bookings")
                        .whereEqualTo("talentId", talentId)
                        .whereEqualTo("date", date)
                        .get()
                        .addOnSuccessListener { bSnapshot ->
                            val hasBookingConflict = bSnapshot.toObjects(Booking::class.java).any {
                                it.id != bookingId && 
                                (it.status == "PENDING" || it.status == "CONFIRMED") &&
                                isTimeOverlapping(booking.startTime, booking.endTime, it.startTime, it.endTime)
                            }
                            setConflictState(hasBookingConflict)
                        }
                }
            }
    }

    private fun setConflictState(hasConflict: Boolean) {
        hasConflictDetected = hasConflict
        if (hasConflict) {
            binding.cardConflictStatus.visibility = View.VISIBLE
            binding.btnAccept.text = "Accept (Conflict Detected)"
        } else {
            binding.cardConflictStatus.visibility = View.GONE
            binding.btnAccept.text = "Accept"
        }
    }

    private fun isTimeOverlapping(s1: String, e1: String, s2: String, e2: String): Boolean {
        val start1 = timeToMinutes(s1)
        val end1 = timeToMinutes(e1)
        val start2 = timeToMinutes(s2)
        val end2 = timeToMinutes(e2)
        if (start1 == -1 || start2 == -1) return false
        return start1 < end2 && start2 < end1
    }

    private fun timeToMinutes(timeString: String): Int {
        return try {
            val sdf = SimpleDateFormat("hh:mm a", Locale.US)
            val date = sdf.parse(timeString.trim().uppercase())
            val cal = Calendar.getInstance().apply { time = date!! }
            cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        } catch (e: Exception) { -1 }
    }

    private fun setupButtons() {
        binding.btnAccept.setOnClickListener {
            if (hasConflictDetected) {
                AlertDialog.Builder(this)
                    .setTitle("Schedule Conflict")
                    .setMessage("You have an existing booking at this time. Are you sure you want to accept this anyway?")
                    .setPositiveButton("Accept Anyway") { _, _ -> currentBooking?.let { acceptBooking(it) } }
                    .setNegativeButton("Review Schedule", null)
                    .show()
            } else {
                currentBooking?.let { acceptBooking(it) }
            }
        }

        binding.btnReject.setOnClickListener { 
            AlertDialog.Builder(this)
                .setTitle("Reject Booking")
                .setMessage("Are you sure you want to reject this booking?")
                .setPositiveButton("Reject") { _, _ -> rejectBooking() }
                .setNegativeButton("No", null)
                .show()
        }

        binding.btnRequestReschedule.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Negotiate New Time?")
                .setMessage("This will notify the recruiter. You should then send them a message to propose a time that works for you. Proceed?")
                .setPositiveButton("Yes, Notify Recruiter") { _, _ -> initiateReschedule() }
                .setNegativeButton("Not Now", null)
                .show()
        }

        binding.btnViewTimeline.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("OPEN_TAB", "TIMELINE")
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }
    }

    private fun initiateReschedule() {
        val booking = currentBooking ?: return
        if (booking.recruiterId.isEmpty()) {
            Toast.makeText(this, "Cannot notify recruiter (Missing ID)", Toast.LENGTH_SHORT).show()
            return
        }

        val batch = db.batch()
        batch.update(db.collection("bookings").document(booking.id), "status", "RESCHEDULE_REQUESTED")

        val notifId = db.collection("notifications").document().id
        val notification = Notification(
            notificationId = notifId,
            receiverId = booking.recruiterId,
            senderId = booking.talentId,
            senderName = booking.name,
            title = "Reschedule Requested",
            message = "${booking.name} requested to reschedule the booking for ${booking.castingTitle}.",
            type = "RESCHEDULE_REQUEST",
            referenceId = booking.id,
            createdAt = System.currentTimeMillis()
        ).apply { read = false }
        batch.set(db.collection("notifications").document(notifId), notification)

        val messageRef = db.collection("messages").document()
        val chatMessage = Message(
            messageId = messageRef.id,
            bookingId = booking.id,
            senderId = booking.talentId,
            receiverId = booking.recruiterId,
            message = "I love this project, but I have a time clash. Can we move the schedule?",
            timestamp = System.currentTimeMillis(),
            status = "SENT",
            type = "RESCHEDULE"
        )
        batch.set(messageRef, chatMessage)

        batch.commit().addOnSuccessListener { showRescheduleSuccessDialog() }
    }

    private fun showRescheduleSuccessDialog() {
        AlertDialog.Builder(this)
            .setTitle("Request Sent!")
            .setMessage("The reschedule request has been sent. Open your Inbox to negotiate with the recruiter.")
            .setPositiveButton("Go to Inbox") { _, _ -> 
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("OPEN_TAB", "INBOX")
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(intent)
                finish() 
            }
            .setCancelable(false)
            .show()
    }

    private fun acceptBooking(booking: Booking) {
        if (booking.recruiterId.isEmpty()) {
            Toast.makeText(this, "Error: Recruiter data not found", Toast.LENGTH_SHORT).show()
            return
        }

        // Fetch the recruiter's actual fullName from the userprofile collection
        db.collection("userprofile").document(booking.recruiterId).get().addOnSuccessListener { profileDoc ->
            val recruiterName = profileDoc.getString("fullName") ?: booking.recruiterName.ifEmpty { "Official Recruiter" }

            val batch = db.batch()
            batch.update(db.collection("bookings").document(booking.id), "status", "CONFIRMED")
            
            val scheduleRef = db.collection("schedules").document()
            val schedule = Schedule(
                id = scheduleRef.id,
                bookingId = booking.id,
                castingCallId = booking.castingCallId,
                castingTitle = booking.castingTitle,
                recruiterId = booking.recruiterId,
                recruiterName = recruiterName, // Updated with fullName from userprofile
                userId = booking.talentId,
                name = booking.name,
                role = booking.role,
                date = booking.date,
                startTime = booking.startTime,
                endTime = booking.endTime,
                location = booking.location,
                status = "CONFIRMED",
                notes = booking.notes,
                createdAt = System.currentTimeMillis()
            )
            batch.set(scheduleRef, schedule)

            // Notification to Recruiter
            val recNotifId = db.collection("notifications").document().id
            val recNotification = Notification(
                notificationId = recNotifId,
                receiverId = booking.recruiterId,
                senderId = booking.talentId,
                senderName = booking.name,
                title = "Booking Accepted",
                message = "${booking.name} accepted your booking request for ${booking.castingTitle}.",
                type = "BOOKING_ACCEPTED",
                referenceId = booking.id,
                createdAt = System.currentTimeMillis()
            ).apply { read = false }
            batch.set(db.collection("notifications").document(recNotifId), recNotification)

            // Notification to Talent (Self)
            val talNotifId = db.collection("notifications").document().id
            val talNotification = Notification(
                notificationId = talNotifId,
                receiverId = booking.talentId,
                senderId = booking.recruiterId,
                senderName = recruiterName,
                title = "Schedule Confirmed",
                message = "Your interview has been scheduled for ${booking.castingTitle}.",
                type = "SCHEDULE_CONFIRMED",
                referenceId = booking.id,
                createdAt = System.currentTimeMillis()
            ).apply { read = false }
            batch.set(db.collection("notifications").document(talNotifId), talNotification)

            batch.commit().addOnSuccessListener { binding.layoutSuccessOverlay.visibility = View.VISIBLE }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to confirm schedule. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun rejectBooking() {
        val booking = currentBooking ?: return
        if (booking.recruiterId.isEmpty()) {
            Log.e("RejectBooking", "RecruiterId is missing in Booking document!")
            Toast.makeText(this, "Cannot notify recruiter (Missing ID)", Toast.LENGTH_SHORT).show()
            return
        }

        val batch = db.batch()
        batch.update(db.collection("bookings").document(booking.id), "status", "REJECTED")

        val notifId = db.collection("notifications").document().id
        val notification = Notification(
            notificationId = notifId,
            receiverId = booking.recruiterId,
            senderId = booking.talentId,
            senderName = booking.name,
            title = "Booking Rejected",
            message = "${booking.name} rejected your booking request for ${booking.castingTitle}.",
            type = "BOOKING_REJECTED",
            referenceId = booking.id,
            createdAt = System.currentTimeMillis()
        ).apply { read = false }
        batch.set(db.collection("notifications").document(notifId), notification)

        batch.commit().addOnSuccessListener {
            Toast.makeText(this, "Booking Rejected & Recruiter Notified", Toast.LENGTH_SHORT).show()
            finish()
        }.addOnFailureListener { e ->
            Log.e("RejectBooking", "Batch failed: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bookingListener?.remove()
    }
}
