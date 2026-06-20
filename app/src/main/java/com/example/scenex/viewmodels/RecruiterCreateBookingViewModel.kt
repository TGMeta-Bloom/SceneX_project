package com.example.scenex.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.scenex.models.Booking
import com.example.scenex.models.ImgBBResponse
import com.example.scenex.models.Notification
import com.example.scenex.models.Schedule
import com.example.scenex.network.ImgBBService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class RecruiterCreateBookingViewModel(private val state: SavedStateHandle) : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val IMGBB_API_KEY = "3555cbd369113d3b670cec87ddc281a3"

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.imgbb.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val imgBBService = retrofit.create(ImgBBService::class.java)

    // Persistent data using SavedStateHandle to prevent loss after picking image
    val selectedDate = state.getLiveData<Long>("selectedDate", 0L)
    val startTime = state.getLiveData<String>("startTime", "")
    val endTime = state.getLiveData<String>("endTime", "")
    val location = state.getLiveData<String>("location", "")
    val notes = state.getLiveData<String>("notes", "")
    
    // For Edit Mode
    private val _existingBooking = MutableLiveData<Booking?>()
    val existingBooking: LiveData<Booking?> = _existingBooking

    data class ConflictData(val hasConflict: Boolean, val suggestedSlots: List<String>)
    private val _conflictResult = MutableLiveData<ConflictData>()
    val conflictResult: LiveData<ConflictData> = _conflictResult

    private val _bookingStatus = MutableLiveData<Result<String>>()
    val bookingStatus: LiveData<Result<String>> = _bookingStatus

    private val _uploadStatus = MutableLiveData<String?>()
    val uploadStatus: LiveData<String?> = _uploadStatus

    private val _bannerImageUrl = state.getLiveData<String?>("bannerUrl", null)
    val bannerImageUrl: LiveData<String?> = _bannerImageUrl

    data class TimeSlot(val start: String, val end: String)

    fun loadBookingDetails(bookingId: String) {
        db.collection("bookings").document(bookingId).get()
            .addOnSuccessListener { doc ->
                val booking = doc.toObject(Booking::class.java)
                if (booking != null) {
                    _existingBooking.value = booking
                    selectedDate.value = booking.date
                    startTime.value = booking.startTime
                    endTime.value = booking.endTime
                    location.value = booking.location
                    notes.value = booking.notes
                    state["bannerUrl"] = booking.projectImageUrl
                }
            }
    }

    fun uploadBannerImage(file: File) {
        _uploadStatus.value = "UPLOADING"
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("image", file.name, requestFile)

        imgBBService.uploadImage(IMGBB_API_KEY, body).enqueue(object : Callback<ImgBBResponse> {
            override fun onResponse(call: Call<ImgBBResponse>, response: Response<ImgBBResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val url = response.body()?.data?.url
                    state["bannerUrl"] = url
                    _uploadStatus.value = "SUCCESS"
                } else {
                    _uploadStatus.value = "FAILED"
                }
            }
            override fun onFailure(call: Call<ImgBBResponse>, t: Throwable) {
                _uploadStatus.value = "ERROR"
            }
        })
    }

    private fun normalizeDate(date: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = date
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun timeToMinutes(timeString: String): Int {
        return try {
            val cleanTime = timeString.trim().uppercase()
            val sdf = SimpleDateFormat("hh:mm a", Locale.US)
            val date = sdf.parse(cleanTime)
            val cal = Calendar.getInstance().apply { time = date!! }
            cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        } catch (e: Exception) { -1 }
    }

    private fun isTimeOverlapping(s1: String, e1: String, s2: String, e2: String): Boolean {
        val start1 = timeToMinutes(s1)
        val end1 = timeToMinutes(e1)
        val start2 = timeToMinutes(s2)
        val end2 = timeToMinutes(e2)
        if (start1 == -1 || end1 == -1 || start2 == -1 || end2 == -1) return false
        return start1 < end2 && end1 > start2
    }

    fun checkAvailability(talentId: String, currentBookingId: String? = null) {
        val date = selectedDate.value ?: 0L
        val start = startTime.value ?: ""
        val end = endTime.value ?: ""
        if (talentId.isEmpty() || date == 0L || start.isEmpty() || end.isEmpty()) return
        val normalizedDate = normalizeDate(date)

        db.collection("schedules")
            .whereEqualTo("userId", talentId)
            .whereEqualTo("date", normalizedDate)
            .get()
            .addOnSuccessListener { scheduleSnapshot ->
                // Filter out the schedule that might be linked to the booking we are currently editing
                val confirmedSlots = scheduleSnapshot.toObjects(Schedule::class.java)
                    .filter { it.bookingId != currentBookingId }
                    .map { TimeSlot(it.startTime, it.endTime) }

                db.collection("bookings")
                    .whereEqualTo("talentId", talentId)
                    .whereEqualTo("date", normalizedDate)
                    .get()
                    .addOnSuccessListener { bookingSnapshot ->
                        val pendingSlots = bookingSnapshot.toObjects(Booking::class.java)
                            .filter { it.id != currentBookingId && (it.status == "PENDING" || it.status == "CONFIRMED") }
                            .map { TimeSlot(it.startTime, it.endTime) }
                        
                        val allOccupiedSlots = confirmedSlots + pendingSlots
                        val conflict = allOccupiedSlots.any { existing -> isTimeOverlapping(start, end, existing.start, existing.end) }
                        val suggested = if (conflict) calculateSuggestedSlots(allOccupiedSlots) else emptyList()
                        _conflictResult.value = ConflictData(conflict, suggested)
                    }
            }
    }

    private fun calculateSuggestedSlots(occupiedSlots: List<TimeSlot>): List<String> {
        val standardSlots = listOf(
            "08:00 AM - 09:00 AM", "09:00 AM - 10:00 AM", "10:00 AM - 11:00 AM",
            "11:00 AM - 12:00 PM", "12:00 PM - 01:00 PM", "01:00 PM - 02:00 PM",
            "02:00 PM - 03:00 PM", "03:00 PM - 04:00 PM", "04:00 PM - 05:00 PM"
        )
        return standardSlots.filter { slot ->
            val parts = slot.split(" - ")
            occupiedSlots.none { occupied -> isTimeOverlapping(parts[0], parts[1], occupied.start, occupied.end) }
        }
    }

    fun createBooking(
        castingCallId: String, castingTitle: String, talentId: String, talentName: String,
        spotlightCategory: String, recruiterName: String
    ) {
        val recruiterId = auth.currentUser?.uid ?: return
        val bannerUrl = _bannerImageUrl.value ?: ""
        val date = selectedDate.value ?: 0L
        val start = startTime.value ?: ""
        val end = endTime.value ?: ""
        val loc = location.value ?: ""
        val nts = notes.value ?: ""
        if (date == 0L || start.isEmpty() || end.isEmpty()) return

        val normalizedDate = normalizeDate(date)
        val docId = db.collection("bookings").document().id
        val booking = Booking(
            id = docId, castingCallId = castingCallId, castingTitle = castingTitle,
            recruiterId = recruiterId, recruiterName = recruiterName, talentId = talentId,
            name = talentName, role = spotlightCategory, projectImageUrl = bannerUrl,
            date = normalizedDate, startTime = start, endTime = end,
            location = loc, status = "PENDING", notes = nts,
            hasConflict = false, createdAt = System.currentTimeMillis()
        )

        val batch = db.batch()
        batch.set(db.collection("bookings").document(docId), booking)

        val notifId = db.collection("notifications").document().id
        val notification = Notification(
            notificationId = notifId, receiverId = talentId, senderId = recruiterId, senderName = recruiterName,
            title = "New Booking Request", message = "$recruiterName invited you for $castingTitle",
            type = "BOOKING_REQUEST", referenceId = docId, createdAt = System.currentTimeMillis()
        )
        batch.set(db.collection("notifications").document(notifId), notification)

        batch.commit()
            .addOnSuccessListener { _bookingStatus.value = Result.success(docId) }
            .addOnFailureListener { _bookingStatus.value = Result.failure(it) }
    }

    fun updateBooking(bookingId: String) {
        val recruiterId = auth.currentUser?.uid ?: return
        val date = selectedDate.value ?: 0L
        val start = startTime.value ?: ""
        val end = endTime.value ?: ""
        val loc = location.value ?: ""
        val nts = notes.value ?: ""
        val bannerUrl = _bannerImageUrl.value ?: ""

        if (date == 0L || start.isEmpty() || end.isEmpty()) return
        val normalizedDate = normalizeDate(date)

        val batch = db.batch()
        val bookingRef = db.collection("bookings").document(bookingId)

        val updates = mapOf(
            "date" to normalizedDate,
            "startTime" to start,
            "endTime" to end,
            "location" to loc,
            "notes" to nts,
            "projectImageUrl" to bannerUrl,
            "status" to "PENDING" // Move back to PENDING so Talent can accept/reject new time
        )
        batch.update(bookingRef, updates)

        // Fetch booking to get talent details for notification
        db.collection("bookings").document(bookingId).get().addOnSuccessListener { doc ->
            val b = doc.toObject(Booking::class.java) ?: return@addOnSuccessListener
            
            val notifId = db.collection("notifications").document().id
            val notification = Notification(
                notificationId = notifId,
                receiverId = b.talentId,
                senderId = recruiterId,
                senderName = b.recruiterName,
                title = "Booking Schedule Updated",
                message = "${b.recruiterName} has updated the schedule for ${b.castingTitle}. Please review.",
                type = "BOOKING_UPDATED",
                referenceId = bookingId,
                createdAt = System.currentTimeMillis()
            )
            batch.set(db.collection("notifications").document(notifId), notification)

            batch.commit()
                .addOnSuccessListener { _bookingStatus.value = Result.success(bookingId) }
                .addOnFailureListener { _bookingStatus.value = Result.failure(it) }
        }
    }
}
