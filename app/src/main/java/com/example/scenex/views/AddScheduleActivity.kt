package com.example.scenex.views

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.scenex.databinding.ActivityAddScheduleBinding
import com.example.scenex.models.Schedule
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AddScheduleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddScheduleBinding
    private val calendar = Calendar.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var scheduleId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        setupPickers()

        scheduleId = intent.getStringExtra("SCHEDULE_ID")
        if (scheduleId != null) {
            loadExistingSchedule(scheduleId!!)
            binding.headerTitle.text = "Update Schedule"
            binding.btnSave.text = "Update Schedule"
        }

        binding.btnSave.setOnClickListener {
            saveSchedule()
        }

        binding.btnCancel.setOnClickListener { finish() }
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun loadExistingSchedule(id: String) {
        db.collection("schedules").document(id).get().addOnSuccessListener { doc ->
            try {
                val schedule = doc.toObject(Schedule::class.java)
                if (schedule != null) {
                    binding.etTitle.setText(schedule.castingTitle)
                    binding.etRole.setText(schedule.role)
                    binding.etLocation.setText(schedule.location)
                    binding.etNotes.setText(schedule.notes)
                    
                    calendar.timeInMillis = schedule.date
                    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
                    binding.txtSelectedDate.text = dateFormat.format(calendar.time)
                    binding.txtStartTime.text = schedule.startTime
                    binding.txtEndTime.text = schedule.endTime
                }
            } catch (e: Exception) {
                Log.e("SceneX_Debug", "Error loading data: ${e.message}")
            }
        }
    }

    private fun setupPickers() {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
        
        binding.txtSelectedDate.text = dateFormat.format(calendar.time)
        binding.txtStartTime.text = timeFormat.format(calendar.time)
        
        binding.btnPickDate.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d ->
                calendar.set(Calendar.YEAR, y)
                calendar.set(Calendar.MONTH, m)
                calendar.set(Calendar.DAY_OF_MONTH, d)
                binding.txtSelectedDate.text = dateFormat.format(calendar.time)
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.btnPickStartTime.setOnClickListener {
            TimePickerDialog(this, { _, h, min ->
                val tempCal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, min) }
                binding.txtStartTime.text = timeFormat.format(tempCal.time)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
        }

        binding.btnPickEndTime.setOnClickListener {
            TimePickerDialog(this, { _, h, min ->
                val tempCal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, min) }
                binding.txtEndTime.text = timeFormat.format(tempCal.time)
            }, calendar.get(Calendar.HOUR_OF_DAY) + 1, calendar.get(Calendar.MINUTE), false).show()
        }
    }

    private fun saveSchedule() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to save schedules", Toast.LENGTH_LONG).show()
            return
        }

        val userId = currentUser.uid
        val title = binding.etTitle.text.toString().trim()
        val role = binding.etRole.text.toString().trim()
        
        if (title.isEmpty()) {
            Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show()
            return
        }

        val saveCalendar = Calendar.getInstance()
        saveCalendar.timeInMillis = calendar.timeInMillis
        saveCalendar.set(Calendar.HOUR_OF_DAY, 0); saveCalendar.set(Calendar.MINUTE, 0)
        saveCalendar.set(Calendar.SECOND, 0); saveCalendar.set(Calendar.MILLISECOND, 0)

        val docRef = if (scheduleId != null) db.collection("schedules").document(scheduleId!!) else db.collection("schedules").document()

        val schedule = Schedule(
            id = docRef.id,
            userId = userId,
            castingTitle = title,
            role = role,
            location = binding.etLocation.text.toString(),
            date = saveCalendar.timeInMillis, 
            startTime = binding.txtStartTime.text.toString(),
            endTime = binding.txtEndTime.text.toString(),
            notes = binding.etNotes.text.toString(),
            status = "CONFIRMED"
        )

        docRef.set(schedule).addOnSuccessListener {
            Toast.makeText(this, "Schedule Saved", Toast.LENGTH_SHORT).show()
            finish()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
