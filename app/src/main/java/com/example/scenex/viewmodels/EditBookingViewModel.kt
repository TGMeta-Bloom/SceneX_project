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

class EditBookingViewModel(private val state: SavedStateHandle) : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val IMGBB_API_KEY = "3555cbd369113d3b670cec87ddc281a3"

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.imgbb.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val imgBBService = retrofit.create(ImgBBService::class.java)

    val projectTitle = state.getLiveData<String>("projectTitle", "")
    val talentName = state.getLiveData<String>("talentName", "")
    val category = state.getLiveData<String>("category", "")
    val selectedDate = state.getLiveData<Long>("selectedDate", 0L)
    val startTime = state.getLiveData<String>("startTime", "")
    val endTime = state.getLiveData<String>("endTime", "")
    val location = state.getLiveData<String>("location", "")
    val notes = state.getLiveData<String>("notes", "")
    val bannerImageUrl = state.getLiveData<String?>("bannerUrl", null)

    private val _existingBooking = MutableLiveData<Booking?>()
    val existingBooking: LiveData<Booking?> = _existingBooking

    data class ConflictData(val hasConflict: Boolean, val suggestedSlots: List<String>)
    private val _conflictResult = MutableLiveData<ConflictData>()
    val conflictResult: LiveData<ConflictData> = _conflictResult

    private val _updateStatus = MutableLiveData<Result<String>>()
    val updateStatus: LiveData<Result<String>> = _updateStatus

    private val _uploadStatus = MutableLiveData<String?>()
    val uploadStatus: LiveData<String?> = _uploadStatus

    data class TimeSlot(val start: String, val end: String)

    fun loadBookingDetails(bookingId: String) {
        db.collection("bookings").document(bookingId).get()
            .addOnSuccessListener { doc ->
                val booking = doc.toObject(Booking::class.java)
                if (booking != null) {
                    _existingBooking.value = booking
                    projectTitle.value = booking.castingTitle
                    talentName.value = booking.name
                    category.value = booking.role
                    selectedDate.value = booking.date
                    startTime.value = booking.startTime
                    endTime.value = booking.endTime
                    location.value = booking.location
                    notes.value = booking.notes
                    bannerImageUrl.value = booking.projectImageUrl
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
                    bannerImageUrl.value = url
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

    fun checkAvailability(talentId: String, currentBookingId: String) {
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
                val confirmedSlots = scheduleSnapshot.toObjects(Schedule::class.java)
                    .filter { s: Schedule -> s.bookingId != currentBookingId }
                    .map { s: Schedule -> TimeSlot(s.startTime, s.endTime) }

                db.collection("bookings")
                    .whereEqualTo("talentId", talentId)
                    .whereEqualTo("date", normalizedDate)
                    .get()
                    .addOnSuccessListener { bookingSnapshot ->
                        val pendingSlots = bookingSnapshot.toObjects(Booking::class.java)
                            .filter { b: Booking -> b.id != currentBookingId && (b.status == "PENDING" || b.status == "CONFIRMED") }
                            .map { b: Booking -> TimeSlot(b.startTime, b.endTime) }
                        
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

    fun updateBooking(bookingId: String) {
        val recruiterId = auth.currentUser?.uid ?: return
        val title = projectTitle.value ?: ""
        val tName = talentName.value ?: ""
        val cat = category.value ?: ""
        val date = selectedDate.value ?: 0L
        val start = startTime.value ?: ""
        val end = endTime.value ?: ""
        val loc = location.value ?: ""
        val nts = notes.value ?: ""
        val bannerUrl = bannerImageUrl.value ?: ""

        if (date == 0L || start.isEmpty() || end.isEmpty()) return
        val normalizedDate = normalizeDate(date)

        val batch = db.batch()
        val bookingRef = db.collection("bookings").document(bookingId)

        val updates = mapOf(
            "castingTitle" to title,
            "name" to tName,
            "role" to cat,
            "date" to normalizedDate,
            "startTime" to start,
            "endTime" to end,
            "location" to loc,
            "notes" to nts,
            "projectImageUrl" to bannerUrl,
            "status" to "PENDING"
        )
        batch.update(bookingRef, updates)

        db.collection("bookings").document(bookingId).get().addOnSuccessListener { doc ->
            val b = doc.toObject(Booking::class.java) ?: return@addOnSuccessListener
            
            val notifId = db.collection("notifications").document().id
            val notification = Notification(
                notificationId = notifId,
                receiverId = b.talentId,
                senderId = recruiterId,
                senderName = b.recruiterName,
                title = "Booking Updated",
                message = "${b.recruiterName} updated the schedule for $title. Please review.",
                type = "BOOKING_UPDATED",
                referenceId = bookingId,
                createdAt = System.currentTimeMillis()
            )
            batch.set(db.collection("notifications").document(notifId), notification)

            batch.commit()
                .addOnSuccessListener { _updateStatus.value = Result.success(bookingId) }
                .addOnFailureListener { _updateStatus.value = Result.failure(it) }
        }
    }
}
