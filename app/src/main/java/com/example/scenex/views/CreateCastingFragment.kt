package com.example.scenex.views

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.scenex.R
import com.example.scenex.models.AvailabilityResult
import com.example.scenex.models.CastingCall
import com.example.scenex.models.ImgBBResponse
import com.example.scenex.network.ImgBBService
import com.example.scenex.repository.AvailabilityRepository
import com.example.scenex.utils.AvailabilityEngine
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class CreateCastingFragment : Fragment() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val calendar = Calendar.getInstance()

    // Engine and Repository for Conflict Checking
    private val repository = AvailabilityRepository()
    private val engine = AvailabilityEngine()

    private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    private var selectedImageUri: Uri? = null
    private var selectedCategory: String = "Actor"
    private val IMGBB_API_KEY = "113d28dab202d082d441249d7debf1f7"

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.imgbb.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val imgBBService = retrofit.create(ImgBBService::class.java)

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            view?.findViewById<ImageView>(R.id.ivPoster)?.setImageURI(it)
            view?.findViewById<View>(R.id.layoutPickImage)?.visibility = View.GONE
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_create_casting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvToolbarTitle = view.findViewById<TextView>(R.id.tvToolbarTitle)
        val toolbar = view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)

        applyTextGradient(tvToolbarTitle)
        toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }

        setupCategoryToggle(view)
        setupDropdowns(view)
        setupPickers(view)

        view.findViewById<View>(R.id.cardPoster)?.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        view.findViewById<View>(R.id.btnPublish)?.setOnClickListener { prepareAndSubmit(view) }
    }

    private fun setupCategoryToggle(view: View) {
        val tvActor = view.findViewById<TextView>(R.id.tvToggleActor)
        val tvDancer = view.findViewById<TextView>(R.id.tvToggleDancer)

        val tvRoleTitleLabel = view.findViewById<TextView>(R.id.tvRoleTitleLabel)
        val etRoleTitle = view.findViewById<EditText>(R.id.etRoleTitle)
        val tvSkillsLabel = view.findViewById<TextView>(R.id.tvSkillsLabel)
        val etSkills = view.findViewById<EditText>(R.id.etSkills)
        val dropdownRoleType = view.findViewById<AutoCompleteTextView>(R.id.dropdownRoleType)

        fun updateUI(category: String) {
            selectedCategory = category
            tvActor.isSelected = category == "Actor"
            tvDancer.isSelected = category == "Dancer"

            if (category == "Actor") {
                tvRoleTitleLabel.text = "Role Title / Character Name"
                etRoleTitle.hint = "e.g. Lead Male"
                tvSkillsLabel.text = "Required Skills"
                etSkills.hint = "e.g. Singing, Dialects"
                setupAdapter(dropdownRoleType, R.array.role_types)
                dropdownRoleType.setText("", false)
                dropdownRoleType.hint = "Select Role"
            } else {
                tvRoleTitleLabel.text = "Dance Style / Role"
                etRoleTitle.hint = "e.g. Ballet Soloist"
                tvSkillsLabel.text = "Required Dance Techniques"
                etSkills.hint = "e.g. Contemporary, Jazz, Acrobatics"
                setupAdapter(dropdownRoleType, R.array.dance_role_types)
                dropdownRoleType.setText("", false)
                dropdownRoleType.hint = "Select Dance Role"
            }
        }

        tvActor.setOnClickListener { updateUI("Actor") }
        tvDancer.setOnClickListener { updateUI("Dancer") }

        updateUI("Actor")
    }

    private fun setupDropdowns(view: View) {
        setupAdapter(view.findViewById(R.id.dropdownProductionType), R.array.production_types)
        setupAdapter(view.findViewById(R.id.dropdownLanguage), R.array.production_languages)
        setupAdapter(view.findViewById(R.id.dropdownAuditionType), R.array.audition_types)
        setupAdapter(view.findViewById(R.id.dropdownGender), R.array.gender_requirements)
        setupAdapter(view.findViewById(R.id.dropdownExperience), R.array.experience_levels)
        setupAdapter(view.findViewById(R.id.dropdownCompensation), R.array.compensation_options)
    }

    private fun setupAdapter(autoCompleteTextView: AutoCompleteTextView?, arrayResId: Int) {
        autoCompleteTextView?.let {
            val items = resources.getStringArray(arrayResId)
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, items)
            it.setAdapter(adapter)
            it.setOnClickListener { _ -> it.showDropDown() }
            it.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) it.showDropDown() }
        }
    }

    private fun setupPickers(view: View) {
        val etAuditionDate = view.findViewById<EditText>(R.id.etAuditionDate)
        val etDeadline = view.findViewById<EditText>(R.id.etDeadline)
        val etStartTime = view.findViewById<EditText>(R.id.etStartTime)
        val etEndTime = view.findViewById<EditText>(R.id.etEndTime)
        val etShootDate = view.findViewById<EditText>(R.id.etShootDate)

        etAuditionDate?.setOnClickListener { showDatePicker { etAuditionDate.setText(it) } }
        etDeadline?.setOnClickListener { showDatePicker { etDeadline.setText(it) } }
        etShootDate?.setOnClickListener { showDatePicker { etShootDate.setText(it) } }

        etStartTime?.setOnClickListener { showTimePicker { etStartTime.setText(it) } }
        etEndTime?.setOnClickListener { showTimePicker { etEndTime.setText(it) } }
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        DatePickerDialog(requireContext(), { _, y, m, d ->
            val sel = Calendar.getInstance().apply { set(y, m, d) }
            onDateSelected(dateFormatter.format(sel.time))
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showTimePicker(onTimeSelected: (String) -> Unit) {
        TimePickerDialog(requireContext(), { _, h, m ->
            val sel = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m) }
            onTimeSelected(timeFormatter.format(sel.time))
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
    }

    // ---  Added Conflict Checking Logic (Thamasha) ---
    private fun prepareAndSubmit(view: View) {
        val title = view.findViewById<EditText>(R.id.etProjectTitle).text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(context, "Project Title is required", Toast.LENGTH_SHORT).show()
            return
        }

        val btnPublish = view.findViewById<Button>(R.id.btnPublish)
        btnPublish.isEnabled = false
        btnPublish.text = "Checking Conflicts..."

        val auditionDateStr = view.findViewById<EditText>(R.id.etAuditionDate)?.text.toString().trim()
        val startTimeStr = view.findViewById<EditText>(R.id.etStartTime)?.text.toString().trim()
        val endTimeStr = view.findViewById<EditText>(R.id.etEndTime)?.text.toString().trim()

        val recruiterId = auth.currentUser?.uid ?: return

        // Check if user provided time details to perform conflict check
        if (auditionDateStr.isNotEmpty() && startTimeStr.isNotEmpty() && endTimeStr.isNotEmpty()) {
            lifecycleScope.launch {
                try {
                    val timestamp = parseToUtcMidnight(auditionDateStr)
                    val (bookings, schedules, castingCalls) = repository.getDailyEvents(recruiterId, timestamp)

                    // Aggregate all events assigned to this recruiter
                    val allEvents = bookings + schedules + castingCalls.filter {
                        val rId = it.getString("recruiterId") ?: it.getString("recruiterid") ?: it.getString("userId") ?: ""
                        rId == recruiterId
                    }

                    // Run the Engine
                    val result = engine.checkSlotAvailability(allEvents, startTimeStr, endTimeStr)

                    if (result == AvailabilityResult.BUSY) {
                        Toast.makeText(context, "Conflict! You already have an event scheduled during this time.", Toast.LENGTH_LONG).show()
                        btnPublish.isEnabled = true
                        btnPublish.text = "Publish Casting Call"
                    } else {
                        proceedWithUpload(view)
                    }
                } catch (e: Exception) {
                    Log.e("CreateCasting", "Conflict check failed", e)
                    proceedWithUpload(view) // Fallback to publish if check fails
                }
            }
        } else {
            proceedWithUpload(view)
        }
    }

    // Helper to safely convert the DatePicker string to a UTC Timestamp
    private fun parseToUtcMidnight(dateStr: String): Long {
        return try {
            val parsedDate = dateFormatter.parse(dateStr)
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                time = parsedDate!!
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun proceedWithUpload(view: View) {
        view.findViewById<Button>(R.id.btnPublish).text = "Publishing..."
        if (selectedImageUri != null) {
            uploadPosterAndSubmit(view)
        } else {
            validateAndSubmit(view, "")
        }
    }
    // -------------------------------------------

    private fun uploadPosterAndSubmit(view: View) {
        val file = uriToFile(selectedImageUri!!) ?: run {
            Toast.makeText(context, "Failed to process image", Toast.LENGTH_SHORT).show()
            view.findViewById<Button>(R.id.btnPublish).isEnabled = true
            view.findViewById<Button>(R.id.btnPublish).text = "Publish Casting Call"
            return
        }

        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("image", file.name, requestFile)

        imgBBService.uploadImage(IMGBB_API_KEY, body).enqueue(object : Callback<ImgBBResponse> {
            override fun onResponse(call: Call<ImgBBResponse>, response: Response<ImgBBResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val url = response.body()?.data?.url ?: ""
                    validateAndSubmit(view, url)
                } else {
                    Toast.makeText(context, "Image upload failed", Toast.LENGTH_SHORT).show()
                    view.findViewById<Button>(R.id.btnPublish).isEnabled = true
                    view.findViewById<Button>(R.id.btnPublish).text = "Publish Casting Call"
                }
            }

            override fun onFailure(call: Call<ImgBBResponse>, t: Throwable) {
                Toast.makeText(context, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                view.findViewById<Button>(R.id.btnPublish).isEnabled = true
                view.findViewById<Button>(R.id.btnPublish).text = "Publish Casting Call"
            }
        })
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val file = File(requireContext().cacheDir, "temp_poster_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            file
        } catch (e: Exception) {
            Log.e("CreateCasting", "uriToFile error", e)
            null
        }
    }

    private fun validateAndSubmit(view: View, posterUrl: String) {
        val title = view.findViewById<EditText>(R.id.etProjectTitle).text.toString().trim()
        val minAge = view.findViewById<EditText>(R.id.etMinAge)?.text.toString().toIntOrNull() ?: 15
        val maxAge = view.findViewById<EditText>(R.id.etMaxAge)?.text.toString().toIntOrNull() ?: 100

        val recruiterId = auth.currentUser?.uid ?: return
        val castingId = firestore.collection("CastingCalls").document().id

        val castingCall = CastingCall(
            id = castingId,
            recruiterId = recruiterId,
            posterUrl = posterUrl,
            category = selectedCategory,
            projectTitle = title,
            productionType = view.findViewById<AutoCompleteTextView>(R.id.dropdownProductionType)?.text.toString(),
            productionCompany = view.findViewById<EditText>(R.id.etProductionCompany)?.text.toString(),
            directorName = view.findViewById<EditText>(R.id.etDirectorName)?.text.toString(),
            projectSynopsis = view.findViewById<EditText>(R.id.etSynopsis)?.text.toString(),
            productionLanguage = view.findViewById<AutoCompleteTextView>(R.id.dropdownLanguage)?.text.toString(),

            auditionType = view.findViewById<AutoCompleteTextView>(R.id.dropdownAuditionType)?.text.toString(),
            auditionDate = view.findViewById<EditText>(R.id.etAuditionDate)?.text.toString(),
            startTime = view.findViewById<EditText>(R.id.etStartTime)?.text.toString(),
            endTime = view.findViewById<EditText>(R.id.etEndTime)?.text.toString(),
            auditionLocation = view.findViewById<EditText>(R.id.etAuditionLocation)?.text.toString(),
            submissionDeadline = view.findViewById<EditText>(R.id.etDeadline)?.text.toString(),

            characterName = view.findViewById<EditText>(R.id.etRoleTitle)?.text.toString(),
            minAge = minAge,
            maxAge = maxAge,
            genderRequirement = view.findViewById<AutoCompleteTextView>(R.id.dropdownGender)?.text.toString(),
            roleType = view.findViewById<AutoCompleteTextView>(R.id.dropdownRoleType)?.text.toString(),
            requiredSkills = view.findViewById<EditText>(R.id.etSkills)?.text.toString(),
            experienceLevel = view.findViewById<AutoCompleteTextView>(R.id.dropdownExperience)?.text.toString(),
            characterBreakdown = view.findViewById<EditText>(R.id.etRoleDescription)?.text.toString(),

            compensation = view.findViewById<AutoCompleteTextView>(R.id.dropdownCompensation)?.text.toString(),
            shootLocation = view.findViewById<EditText>(R.id.etShootLocation)?.text.toString(),
            firstDayOfShoot = view.findViewById<EditText>(R.id.etShootDate)?.text.toString(),

            contactEmail = view.findViewById<EditText>(R.id.etContactEmail)?.text.toString(),
            phoneNumber = view.findViewById<EditText>(R.id.etContactPhone)?.text.toString()
        )

        firestore.collection("CastingCalls").document(castingId).set(castingCall)
            .addOnSuccessListener {
                Toast.makeText(context, "Casting Call Published!", Toast.LENGTH_LONG).show()
                parentFragmentManager.popBackStack()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                view.findViewById<View>(R.id.btnPublish)?.isEnabled = true
                view.findViewById<Button>(R.id.btnPublish).text = "Publish Casting Call"
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