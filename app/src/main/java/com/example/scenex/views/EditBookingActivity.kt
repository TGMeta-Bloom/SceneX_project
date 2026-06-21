package com.example.scenex.views

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.scenex.databinding.ActivityEditBookingBinding
import com.example.scenex.databinding.DialogConflictFoundBinding
import com.example.scenex.viewmodels.EditBookingViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class EditBookingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditBookingBinding
    private lateinit var viewModel: EditBookingViewModel
    private var bookingId: String? = null
    private var photoFile: File? = null

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) openCamera() else Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { handleImageSelection(it) }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) photoFile?.let { viewModel.uploadBannerImage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditBookingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[EditBookingViewModel::class.java]
        bookingId = intent.getStringExtra("BOOKING_ID")

        if (bookingId == null) {
            Toast.makeText(this, "Error: Booking ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        setupPickers()
        setupImageUpload()
        setupTextWatchers()
        observeViewModel()
        
        viewModel.loadBookingDetails(bookingId!!)
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }
        
        binding.btnCheckAvailability.setOnClickListener {
            if (validateTimeSelection()) {
                val talentId = viewModel.existingBooking.value?.talentId ?: ""
                viewModel.checkAvailability(talentId, bookingId!!)
            }
        }

        binding.btnUpdateBooking.setOnClickListener {
            if (validateForm()) {
                viewModel.updateBooking(bookingId!!)
            }
        }
    }

    private fun setupTextWatchers() {
        binding.etProjectTitle.addTextChangedListener(createTextWatcher { viewModel.projectTitle.value = it })
        binding.etTalentName.addTextChangedListener(createTextWatcher { viewModel.talentName.value = it })
        binding.etCategory.addTextChangedListener(createTextWatcher { viewModel.category.value = it })
        binding.etLocation.addTextChangedListener(createTextWatcher { viewModel.location.value = it })
        binding.etNotes.addTextChangedListener(createTextWatcher { viewModel.notes.value = it })
    }

    private fun createTextWatcher(onChanged: (String) -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { onChanged(s.toString()) }
        override fun afterTextChanged(s: Editable?) {}
    }

    private fun setupPickers() {
        binding.layoutDateSelector.setOnClickListener {
            val calendar = Calendar.getInstance()
            val currentVal = viewModel.selectedDate.value ?: 0L
            if (currentVal != 0L) calendar.timeInMillis = currentVal

            DatePickerDialog(this, { _, year, month, day ->
                calendar.set(year, month, day)
                viewModel.selectedDate.value = calendar.timeInMillis
                resetAvailabilityUI()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.layoutStartTimeSelector.setOnClickListener {
            showTimePicker { display ->
                viewModel.startTime.value = display
                resetAvailabilityUI()
            }
        }

        binding.layoutEndTimeSelector.setOnClickListener {
            showTimePicker { display ->
                viewModel.endTime.value = display
                resetAvailabilityUI()
            }
        }
    }

    private fun resetAvailabilityUI() {
        binding.btnCheckAvailability.visibility = View.VISIBLE
        binding.btnUpdateBooking.visibility = View.GONE
    }

    private fun showTimePicker(onTimeSelected: (String) -> Unit) {
        TimePickerDialog(this, { _, h, m ->
            val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m) }
            onTimeSelected(SimpleDateFormat("hh:mm a", Locale.US).format(cal.time))
        }, 12, 0, false).show()
    }

    private fun setupImageUpload() {
        binding.btnSelectBannerImage.setOnClickListener { showImagePickerDialog() }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
        AlertDialog.Builder(this)
            .setTitle("Change Project Banner")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> checkCameraPermissionAndOpen()
                    1 -> galleryLauncher.launch("image/*")
                    2 -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        try {
            val file = File.createTempFile("BANNER_EDIT_", ".jpg", cacheDir)
            photoFile = file
            val uri = FileProvider.getUriForFile(this, "com.example.scenex.fileprovider", file)
            cameraLauncher.launch(uri)
        } catch (exception: Exception) {
            Toast.makeText(this, "Error starting camera", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleImageSelection(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val file = File(cacheDir, "banner_edit_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.use { input -> outputStream.use { output -> input.copyTo(output) } }
            viewModel.uploadBannerImage(file)
        } catch (exception: Exception) {
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewModel.projectTitle.observe(this) { if (binding.etProjectTitle.text.toString() != it) binding.etProjectTitle.setText(it) }
        viewModel.talentName.observe(this) { if (binding.etTalentName.text.toString() != it) binding.etTalentName.setText(it) }
        viewModel.category.observe(this) { if (binding.etCategory.text.toString() != it) binding.etCategory.setText(it) }
        viewModel.location.observe(this) { if (binding.etLocation.text.toString() != it) binding.etLocation.setText(it) }
        viewModel.notes.observe(this) { if (binding.etNotes.text.toString() != it) binding.etNotes.setText(it) }

        viewModel.selectedDate.observe(this) { date ->
            if (date != 0L) binding.tvDisplayDate.text = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(date))
        }
        viewModel.startTime.observe(this) { if (it.isNotEmpty()) binding.tvDisplayStartTime.text = it }
        viewModel.endTime.observe(this) { if (it.isNotEmpty()) binding.tvDisplayEndTime.text = it }

        viewModel.bannerImageUrl.observe(this) { url ->
            if (!url.isNullOrEmpty()) {
                binding.ivBannerPreview.visibility = View.VISIBLE
                binding.layoutUploadPlaceholder.visibility = View.GONE
                Glide.with(this).load(url).into(binding.ivBannerPreview)
            }
        }

        viewModel.uploadStatus.observe(this) { status ->
            binding.pbImageUpload.visibility = if (status == "UPLOADING") View.VISIBLE else View.GONE
        }

        viewModel.conflictResult.observe(this) { result ->
            if (result.hasConflict) {
                showConflictDialog(result.suggestedSlots)
            } else {
                if ((viewModel.selectedDate.value ?: 0L) != 0L && !viewModel.startTime.value.isNullOrEmpty()) {
                    binding.btnCheckAvailability.visibility = View.GONE
                    binding.btnUpdateBooking.visibility = View.VISIBLE
                    Toast.makeText(this, "Time slot is available!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.updateStatus.observe(this) { result ->
            if (result.isSuccess) {
                Toast.makeText(this, "Schedule Updated & Talent Notified!", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun showConflictDialog(slots: List<String>) {
        val bottomSheet = BottomSheetDialog(this)
        val dialogBinding = DialogConflictFoundBinding.inflate(layoutInflater)
        bottomSheet.setContentView(dialogBinding.root)

        val radioGroup = dialogBinding.rgSuggestedSlots
        radioGroup.removeAllViews()

        if (slots.isEmpty()) {
            val rb = RadioButton(this).apply { text = "No other slots available"; isEnabled = false }
            radioGroup.addView(rb)
        } else {
            slots.forEach { slot ->
                val rb = RadioButton(this).apply { text = slot; setPadding(24, 32, 24, 32) }
                radioGroup.addView(rb)
            }
        }

        dialogBinding.btnSelectSlot.setOnClickListener {
            val checkedId = radioGroup.checkedRadioButtonId
            if (checkedId != -1) {
                val selectedRb = radioGroup.findViewById<RadioButton>(checkedId)
                val slotText = selectedRb.text.toString()
                val times = slotText.split(" - ")
                viewModel.startTime.value = times[0]
                viewModel.endTime.value = times[1]
                bottomSheet.dismiss()
                binding.btnCheckAvailability.visibility = View.GONE
                binding.btnUpdateBooking.visibility = View.VISIBLE
            }
        }
        bottomSheet.show()
    }

    private fun validateTimeSelection(): Boolean {
        if ((viewModel.selectedDate.value ?: 0L) == 0L) { Toast.makeText(this, "Select a date", Toast.LENGTH_SHORT).show(); return false }
        if (viewModel.startTime.value.isNullOrEmpty()) { Toast.makeText(this, "Select start time", Toast.LENGTH_SHORT).show(); return false }
        if (viewModel.endTime.value.isNullOrEmpty()) { Toast.makeText(this, "Select end time", Toast.LENGTH_SHORT).show(); return false }
        return true
    }

    private fun validateForm(): Boolean {
        if (binding.etProjectTitle.text.isNullOrEmpty()) { binding.etProjectTitle.error = "Required"; return false }
        if (binding.etLocation.text.isNullOrEmpty()) { binding.etLocation.error = "Required"; return false }
        return true
    }
}
