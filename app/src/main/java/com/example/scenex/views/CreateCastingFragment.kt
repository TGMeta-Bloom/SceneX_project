package com.example.scenex.views

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.scenex.R
import com.example.scenex.models.CastingCall
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class CreateCastingFragment : Fragment() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val calendar = Calendar.getInstance()
    private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    private var auditionDateTs: Timestamp? = null
    private var deadlineTs: Timestamp? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_create_casting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvToolbarTitle = view.findViewById<TextView>(R.id.tvToolbarTitle)
        val toolbar = view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)

        applyTextGradient(tvToolbarTitle)
        toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }

        setupDropdowns(view)
        setupPickers(view)
        
        // Matches the ID in your XML
        view.findViewById<View>(R.id.btnPublish)?.setOnClickListener { validateAndSubmit(view) }
    }

    private fun setupDropdowns(view: View) {
        setupAdapter(view.findViewById(R.id.dropdownProductionType), R.array.production_types)
        setupAdapter(view.findViewById(R.id.dropdownLanguage), R.array.production_languages)
        setupAdapter(view.findViewById(R.id.dropdownAuditionType), R.array.audition_types)
        setupAdapter(view.findViewById(R.id.dropdownGender), R.array.gender_requirements)
    }

    private fun setupAdapter(autoCompleteTextView: AutoCompleteTextView?, arrayResId: Int) {
        autoCompleteTextView?.let {
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, resources.getStringArray(arrayResId))
            it.setAdapter(adapter)
        }
    }

    private fun setupPickers(view: View) {
        val etAuditionDate = view.findViewById<EditText>(R.id.etAuditionDate)
        val etDeadline = view.findViewById<EditText>(R.id.etDeadline)
        val etStartTime = view.findViewById<EditText>(R.id.etStartTime)
        val etEndTime = view.findViewById<EditText>(R.id.etEndTime)

        etAuditionDate?.setOnClickListener { showDatePicker { d, ts -> etAuditionDate.setText(d); auditionDateTs = ts } }
        etDeadline?.setOnClickListener { showDatePicker { d, ts -> etDeadline.setText(d); deadlineTs = ts } }
        etStartTime?.setOnClickListener { showTimePicker { etStartTime.setText(it) } }
        etEndTime?.setOnClickListener { showTimePicker { etEndTime.setText(it) } }
    }

    private fun showDatePicker(onDateSelected: (String, Timestamp) -> Unit) {
        DatePickerDialog(requireContext(), { _, y, m, d ->
            val sel = Calendar.getInstance().apply { set(y, m, d) }
            onDateSelected(dateFormatter.format(sel.time), Timestamp(sel.time))
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showTimePicker(onTimeSelected: (String) -> Unit) {
        TimePickerDialog(requireContext(), { _, h, m ->
            val sel = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m) }
            onTimeSelected(timeFormatter.format(sel.time))
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
    }

    private fun validateAndSubmit(view: View) {
        val title = view.findViewById<EditText>(R.id.etProjectTitle).text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(context, "Project Title is required", Toast.LENGTH_SHORT).show()
            return
        }

        // Parsing age range string (e.g. "20-30") into model integers
        val ageText = view.findViewById<EditText>(R.id.etAgeRange)?.text.toString()
        var minAge = 15
        var maxAge = 100
        if (ageText.contains("-")) {
            val parts = ageText.split("-")
            minAge = parts[0].trim().toIntOrNull() ?: 15
            maxAge = parts[1].trim().toIntOrNull() ?: 100
        }

        val recruiterId = auth.currentUser?.uid ?: return
        val castingId = firestore.collection("CastingCalls").document().id

        val castingCall = CastingCall(
            id = castingId,
            recruiterId = recruiterId,
            projectTitle = title,
            productionType = view.findViewById<AutoCompleteTextView>(R.id.dropdownProductionType)?.text.toString(),
            productionCompany = view.findViewById<EditText>(R.id.etProductionCompany)?.text.toString(),
            directorName = view.findViewById<EditText>(R.id.etDirectorName)?.text.toString(),
            projectSynopsis = view.findViewById<EditText>(R.id.etSynopsis)?.text.toString(),
            productionLanguage = view.findViewById<AutoCompleteTextView>(R.id.dropdownLanguage)?.text.toString(),
            auditionType = view.findViewById<AutoCompleteTextView>(R.id.dropdownAuditionType)?.text.toString(),
            auditionDate = auditionDateTs,
            startTime = view.findViewById<EditText>(R.id.etStartTime)?.text.toString(),
            endTime = view.findViewById<EditText>(R.id.etEndTime)?.text.toString(),
            auditionLocation = view.findViewById<EditText>(R.id.etAuditionLocation)?.text.toString(),
            submissionDeadline = deadlineTs,
            characterName = view.findViewById<EditText>(R.id.etRoleTitle)?.text.toString(),
            minAge = minAge,
            maxAge = maxAge,
            genderRequirement = view.findViewById<AutoCompleteTextView>(R.id.dropdownGender)?.text.toString(),
            characterBreakdown = view.findViewById<EditText>(R.id.etRoleDescription)?.text.toString()
        )

        view.findViewById<View>(R.id.btnPublish)?.isEnabled = false
        firestore.collection("CastingCalls").document(castingId).set(castingCall)
            .addOnSuccessListener {
                Toast.makeText(context, "Casting Call Published!", Toast.LENGTH_LONG).show()
                parentFragmentManager.popBackStack()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                view.findViewById<View>(R.id.btnPublish)?.isEnabled = true
            }
    }

    private fun applyTextGradient(textView: TextView?) {
        textView?.post {
            val width = textView.paint.measureText(textView.text.toString())
            if (width > 0) {
                val textShader: Shader = LinearGradient(0f, 0f, width, 0f,
                    intArrayOf(Color.parseColor("#720056"), Color.parseColor("#4A0038")),
                    null, Shader.TileMode.CLAMP)
                textView.paint.shader = textShader
                textView.invalidate()
            }
        }
    }
}
