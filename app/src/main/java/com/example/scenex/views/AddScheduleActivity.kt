package com.example.scenex.views

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
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
    private var scheduleId: String? = null // For Edit mode

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Clear seconds/ms for consistent comparison
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        setupTypeDropdown()
        setupPickers()

        // Check for Edit Mode
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
            val schedule = doc.toObject(Schedule::class.java)
            if (schedule != null) {
                binding.etTitle.setText(schedule.title)
                
                // Set dropdown value: "SHOOT" -> "Shoot"
                val displayType = schedule.type.lowercase().replace("_", " ")
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                binding.spinnerEventType.setText(displayType, false)
                
                binding.etLocation.setText(schedule.location)
                binding.etDescription.setText(schedule.description)
                binding.etNotes.setText(schedule.notes)
                binding.switchAllDay.isChecked = schedule.isAllDay
                
                // Sync calendar and update UI
                calendar.timeInMillis = schedule.date
                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                binding.txtSelectedDate.text = dateFormat.format(calendar.time)
                binding.txtStartTime.text = schedule.startTime
                binding.txtEndTime.text = schedule.endTime
            }
        }
    }

    private fun setupTypeDropdown() {
        val types = arrayOf("Personal Event", "Shoot", "Meeting", "Travel", "Rehearsal", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, types)
        binding.spinnerEventType.setAdapter(adapter)
    }

    private fun setupPickers() {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        
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
                calendar.set(Calendar.HOUR_OF_DAY, h)
                calendar.set(Calendar.MINUTE, min)
                binding.txtStartTime.text = timeFormat.format(calendar.time)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
        }

        binding.btnPickEndTime.setOnClickListener {
            TimePickerDialog(this, { _, h, min ->
                val tempCal = Calendar.getInstance()
                tempCal.set(Calendar.HOUR_OF_DAY, h)
                tempCal.set(Calendar.MINUTE, min)
                binding.txtEndTime.text = timeFormat.format(tempCal.time)
            }, calendar.get(Calendar.HOUR_OF_DAY) + 1, calendar.get(Calendar.MINUTE), false).show()
        }
    }

    private fun saveSchedule() {
        val title = binding.etTitle.text.toString().trim()
        val typeStr = binding.spinnerEventType.text.toString()
        
        if (title.isEmpty()) {
            Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show()
            return
        }

        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // Reuse ID if updating, otherwise generate new
        val docRef = if (scheduleId != null) {
            db.collection("schedules").document(scheduleId!!)
        } else {
            db.collection("schedules").document()
        }

        val schedule = Schedule(
            id = docRef.id,
            userId = FirebaseAuth.getInstance().currentUser?.uid ?: "test_user_123",
            title = title,
            type = typeStr.uppercase().replace(" ", "_"),
            location = binding.etLocation.text.toString(),
            date = calendar.timeInMillis, 
            startTime = binding.txtStartTime.text.toString(),
            endTime = binding.txtEndTime.text.toString(),
            isAllDay = binding.switchAllDay.isChecked,
            description = binding.etDescription.text.toString(),
            notes = binding.etNotes.text.toString()
        )

        docRef.set(schedule).addOnSuccessListener {
            Toast.makeText(this, if (scheduleId != null) "Schedule updated" else "Schedule saved", Toast.LENGTH_SHORT).show()
            finish()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
