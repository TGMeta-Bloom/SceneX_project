package com.example.scenex.views

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import com.bumptech.glide.Glide
import com.example.scenex.R
import com.example.scenex.databinding.ActivityRecruiterBookingDetailsBinding
import com.example.scenex.models.Booking
import com.example.scenex.models.Notification
import com.example.scenex.models.Schedule
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

class RecruiterBookingDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecruiterBookingDetailsBinding
    private val db = FirebaseFirestore.getInstance()
    private var bookingId: String? = null
    private var bookingListener: ListenerRegistration? = null
    private var currentBooking: Booking? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecruiterBookingDetailsBinding.inflate(layoutInflater)
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
        
        val status = booking.status.uppercase()
        binding.detailStatusChip.text = status.replace("_", " ")
        val statusColor = when (status) {
            "PENDING" -> "#FFB300"
            "CONFIRMED" -> "#4CAF50"
            "CANCELLED", "REJECTED" -> "#F44336"
            "COMPLETED" -> "#2196F3"
            "RESCHEDULE_REQUESTED" -> "#9C27B0"
            else -> "#9E9E9E"
        }
        binding.detailStatusChip.chipBackgroundColor = ColorStateList.valueOf(statusColor.toColorInt())

        binding.txtDetailTalentName.text = booking.name
        fetchTalentProfile(booking.talentId)

        binding.txtBookingId.text = "Booking Ref: #BOK-${booking.id.take(6).uppercase()}"
        binding.txtRecruiterName.text = "Recruiter: ${booking.recruiterName}"
        binding.txtMyId.text = "Recruiter ID: ${booking.recruiterId.take(6).uppercase()}"
        binding.txtDetailNotes.text = booking.notes.ifEmpty { "No notes provided." }.let { "Notes: $it" }

        val sdf = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault())
        binding.txtDetailDate.text = sdf.format(Date(booking.date))
        binding.txtDetailTime.text = "${booking.startTime} - ${booking.endTime}"
        binding.txtDetailLocation.text = booking.location.ifEmpty { "Location not specified" }

        updateConflictCard(booking.hasConflict)
        updateActionButtons(status)
    }

    private fun checkLiveConflicts(booking: Booking) {
        if (booking.status != "PENDING" && booking.status != "CONFIRMED" && booking.status != "RESCHEDULE_REQUESTED") return

        db.collection("schedules")
            .whereEqualTo("userId", booking.talentId)
            .whereEqualTo("date", booking.date)
            .get()
            .addOnSuccessListener { snapshot ->
                val conflict = snapshot.toObjects(Schedule::class.java).any { 
                    it.bookingId != booking.id && isTimeOverlapping(booking.startTime, booking.endTime, it.startTime, it.endTime)
                }
                
                if (conflict) {
                    updateConflictCard(true)
                } else {
                    checkOtherPendingConflicts(booking)
                }
            }
    }

    private fun checkOtherPendingConflicts(booking: Booking) {
        db.collection("bookings")
            .whereEqualTo("talentId", booking.talentId)
            .whereEqualTo("date", booking.date)
            .get()
            .addOnSuccessListener { snapshot ->
                val conflict = snapshot.toObjects(Booking::class.java).any { existing ->
                    existing.id != booking.id &&
                    (existing.status == "PENDING" || existing.status == "CONFIRMED") &&
                    isTimeOverlapping(booking.startTime, booking.endTime, existing.startTime, existing.endTime)
                }
                updateConflictCard(conflict)
            }
    }

    private fun isTimeOverlapping(s1: String, e1: String, s2: String, e2: String): Boolean {
        val start1 = timeToMinutes(s1)
        val end1 = timeToMinutes(e1)
        val start2 = timeToMinutes(s2)
        val end2 = timeToMinutes(e2)
        if (start1 == 0 || start2 == 0) return false
        return start1 < end2 && start2 < end1
    }

    private fun timeToMinutes(timeString: String): Int {
        return try {
            val sdf = SimpleDateFormat("hh:mm a", Locale.US)
            val date = sdf.parse(timeString.trim().uppercase())
            val cal = Calendar.getInstance().apply { time = date!! }
            cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        } catch (_: Exception) { 0 }
    }

    private fun updateConflictCard(hasConflict: Boolean) {
        if (hasConflict) {
            binding.cardConflictStatus.setCardBackgroundColor(ColorStateList.valueOf("#FDEDEC".toColorInt()))
            binding.cardConflictStatus.strokeColor = "#F1948A".toColorInt()
            binding.imgConflictIcon.setImageResource(android.R.drawable.stat_sys_warning)
            binding.imgConflictIcon.imageTintList = ColorStateList.valueOf("#D32F2F".toColorInt())
            binding.txtConflictMessage.text = "⚠ Conflict detected with talent schedule"
            binding.txtConflictMessage.setTextColor("#D32F2F".toColorInt())
        } else {
            binding.cardConflictStatus.setCardBackgroundColor(ColorStateList.valueOf("#E8F5E9".toColorInt()))
            binding.cardConflictStatus.strokeColor = "#A5D6A7".toColorInt()
            binding.imgConflictIcon.setImageResource(android.R.drawable.checkbox_on_background)
            binding.imgConflictIcon.imageTintList = ColorStateList.valueOf("#2E7D32".toColorInt())
            binding.txtConflictMessage.text = "✅ No schedule conflict"
            binding.txtConflictMessage.setTextColor("#2E7D32".toColorInt())
        }
    }

    private fun updateActionButtons(status: String) {
        binding.layoutPendingActions.visibility = View.GONE
        binding.layoutConfirmedActions.visibility = View.GONE
        binding.layoutCancelledActions.visibility = View.GONE
        binding.layoutRescheduleActions.visibility = View.GONE

        when (status) {
            "PENDING" -> binding.layoutPendingActions.visibility = View.VISIBLE
            "CONFIRMED" -> binding.layoutConfirmedActions.visibility = View.VISIBLE
            "CANCELLED", "REJECTED" -> binding.layoutCancelledActions.visibility = View.VISIBLE
            "RESCHEDULE_REQUESTED" -> binding.layoutRescheduleActions.visibility = View.VISIBLE
        }
    }

    private fun fetchTalentProfile(talentId: String) {
        if (talentId.isEmpty()) return
        db.collection("profiles").document(talentId).get()
            .addOnSuccessListener { doc ->
                val profileUrl = doc.getString("profileImage") ?: doc.getString("profileImageUrl")
                if (!profileUrl.isNullOrEmpty()) {
                    Glide.with(this).load(profileUrl).circleCrop().into(binding.imgTalentProfile)
                }
            }
    }

    private fun setupButtons() {
        binding.btnConfirm.setOnClickListener { updateStatusWithNotification("CONFIRMED", "BOOKING_ACCEPTED") }
        binding.btnCancelPending.setOnClickListener { updateStatusWithNotification("CANCELLED", "BOOKING_REJECTED") }
        binding.btnMarkCompleted.setOnClickListener { updateStatus("COMPLETED") }
        binding.btnCancelConfirmed.setOnClickListener { updateStatus("CANCELLED") }
        
        binding.btnApproveReschedule.setOnClickListener { 
            // Instead of just approving, open the Edit flow to set the negotiated time
            val intent = Intent(this, EditBookingActivity::class.java)
            intent.putExtra("BOOKING_ID", bookingId)
            startActivity(intent)
        }
        
        binding.btnRejectReschedule.setOnClickListener { 
            updateStatusWithNotification("REJECTED", "RESCHEDULE_REJECTED") 
        }

        binding.btnDeleteBooking.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Booking")
                .setMessage("Permanently remove this record?")
                .setPositiveButton("Delete") { _, _ -> deleteBooking() }
                .setNegativeButton("Cancel", null)
                .show()
        }
        binding.btnChatTalent.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("BOOKING_ID", bookingId)
            intent.putExtra("OTHER_PARTY_NAME", currentBooking?.name)
            intent.putExtra("RECRUITER_ID", currentBooking?.recruiterId)
            intent.putExtra("TALENT_ID", currentBooking?.talentId)
            startActivity(intent)
        }
    }

    private fun updateStatus(newStatus: String) {
        bookingId?.let { id ->
            db.collection("bookings").document(id).update("status", newStatus)
                .addOnSuccessListener { Toast.makeText(this, "Status: $newStatus", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun updateStatusWithNotification(newStatus: String, notifType: String) {
        val booking = currentBooking ?: return
        val batch = db.batch()
        
        val bookingRef = db.collection("bookings").document(booking.id)
        batch.update(bookingRef, "status", newStatus)

        val notifId = db.collection("notifications").document().id
        val message = when(notifType) {
            "RESCHEDULE_REJECTED" -> "The recruiter declined your reschedule request for ${booking.castingTitle}."
            else -> "Booking status updated to $newStatus"
        }

        val notification = Notification(
            notificationId = notifId,
            receiverId = booking.talentId,
            senderId = booking.recruiterId,
            senderName = booking.recruiterName,
            title = notifType.replace("_", " "),
            message = message,
            type = notifType,
            referenceId = booking.id,
            createdAt = System.currentTimeMillis()
        )
        batch.set(db.collection("notifications").document(notifId), notification)

        batch.commit().addOnSuccessListener {
            Toast.makeText(this, "Status Updated & Talent Notified", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteBooking() {
        bookingId?.let { id ->
            db.collection("bookings").document(id).delete()
                .addOnSuccessListener { finish() }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bookingListener?.remove()
    }
}
