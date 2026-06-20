package com.example.scenex.views

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.scenex.R
import com.example.scenex.databinding.DialogConflictFoundBinding
import com.example.scenex.databinding.FragmentRecruiterCreateBookingBinding
import com.example.scenex.viewmodels.RecruiterCreateBookingViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class RecruiterCreateBookingFragment : Fragment() {

    private var _binding: FragmentRecruiterCreateBookingBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: RecruiterCreateBookingViewModel
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var talentId: String = ""
    private var talentName: String = ""
    private var spotlightCategory: String = ""
    private var castingCallId: String = ""
    private var castingTitle: String = ""
    private var recruiterName: String = ""
    private var editBookingId: String? = null

    private var photoFile: File? = null

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) openCamera() else Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { handleImageSelection(it) }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) photoFile?.let { viewModel.uploadBannerImage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            talentId = it.getString(ARG_TALENT_ID) ?: ""
            talentName = it.getString(ARG_TALENT_NAME) ?: ""
            spotlightCategory = it.getString(ARG_SPOTLIGHT_CATEGORY) ?: ""
            castingCallId = it.getString(ARG_CASTING_ID) ?: ""
            castingTitle = it.getString(ARG_CASTING_TITLE) ?: ""
            editBookingId = it.getString(ARG_EDIT_BOOKING_ID)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        _binding = FragmentRecruiterCreateBookingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[RecruiterCreateBookingViewModel::class.java]

        // RESTORE STATE: Recover Date/Time if returning from Gallery
        savedInstanceState?.let { bundle ->
            val savedDate = bundle.getLong("saved_date_ms", 0L)
            if (savedDate != 0L) {
                viewModel.selectedDate.value = savedDate
            }
            viewModel.startTime.value = bundle.getString("saved_start_time", "")
            viewModel.endTime.value = bundle.getString("saved_end_time", "")
            viewModel.location.value = bundle.getString("saved_location", "")
            viewModel.notes.value = bundle.getString("saved_notes", "")
        }

        setupUI()
        setupPickers()
        setupImageUpload()
        setupTextWatchers()
        observeViewModel()
        setupListeners()
        setupNotificationBadge()

        if (editBookingId != null) {
            binding.headerTitle.text = "Edit Booking"
            binding.btnSubmitBooking.text = "Update Booking"
            viewModel.loadBookingDetails(editBookingId!!)
        } else {
            fetchTalentDetails()
            fetchCastingDetails()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("saved_date_ms", viewModel.selectedDate.value ?: 0L)
        outState.putString("saved_start_time", viewModel.startTime.value)
        outState.putString("saved_end_time", viewModel.endTime.value)
        outState.putString("saved_location", viewModel.location.value)
        outState.putString("saved_notes", viewModel.notes.value)
    }

    private fun setupNotificationBadge() {
        val currentUserId = auth.currentUser?.uid ?: return
        db.collection("notifications")
            .whereEqualTo("receiverId", currentUserId)
            .whereEqualTo("read", false)
            .addSnapshotListener { snapshot, _ ->
                if (_binding == null) return@addSnapshotListener
                val count = snapshot?.size() ?: 0
                if (count > 0) {
                    binding.txtNotifCount.visibility = View.VISIBLE
                    binding.txtNotifCount.text = if (count > 9) "9+" else count.toString()
                } else {
                    binding.txtNotifCount.visibility = View.GONE
                }
            }
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnNotification.setOnClickListener {
            startActivity(Intent(requireContext(), NotificationsActivity::class.java))
        }
        binding.etProjectTitle.setText(castingTitle)
        binding.etCategory.setText(spotlightCategory)
        binding.etTalentName.setText(talentName)
    }

    private fun setupTextWatchers() {
        binding.etLocation.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.location.value = s.toString()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etNotes.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.notes.value = s.toString()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun fetchTalentDetails() {
        if (talentId.isEmpty()) return
        db.collection("profiles").document(talentId).get()
            .addOnSuccessListener { document ->
                if (_binding != null && document != null && document.exists()) {
                    val fullName = document.getString("fullName") ?: document.getString("name") ?: talentName
                    val category = document.getString("spotlightCategory") ?: spotlightCategory
                    binding.etTalentName.setText(fullName)
                    binding.etCategory.setText(category)
                    talentName = fullName
                    spotlightCategory = category
                }
            }
    }

    private fun fetchCastingDetails() {
        if (castingCallId.isEmpty()) return
        db.collection("CastingCalls").document(castingCallId).get()
            .addOnSuccessListener { document ->
                if (_binding != null && document != null && document.exists()) {
                    val title = document.getString("projectTitle") ?: document.getString("title") ?: castingTitle
                    val loc = document.getString("shootLocation") ?: "Not specified"
                    val recId = document.getString("recruiterId") ?: ""
                    binding.etProjectTitle.setText(title)
                    
                    if (viewModel.location.value.isNullOrEmpty()) {
                        binding.etLocation.setText(loc)
                        viewModel.location.value = loc
                    }
                    
                    castingTitle = title
                    if (recId.isNotEmpty()) fetchRecruiterProfile(recId)
                }
            }
    }

    private fun fetchRecruiterProfile(recId: String) {
        db.collection("profiles").document(recId).get()
            .addOnSuccessListener { doc ->
                if (_binding != null && doc != null && doc.exists()) {
                    recruiterName = doc.getString("fullName") ?: "Official Recruiter"
                }
            }
    }

    private fun setupPickers() {
        binding.layoutDateSelector.setOnClickListener {
            val context = context ?: return@setOnClickListener
            val calendar = Calendar.getInstance()
            val currentVal = viewModel.selectedDate.value ?: 0L
            if (currentVal != 0L) calendar.timeInMillis = currentVal

            DatePickerDialog(context, { _, year, month, day ->
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
        binding.btnSubmitBooking.visibility = View.GONE
        binding.layoutNotes.visibility = View.GONE
    }

    private fun showTimePicker(onTimeSelected: (String) -> Unit) {
        val context = context ?: return
        TimePickerDialog(context, { _, h, m ->
            val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m) }
            onTimeSelected(SimpleDateFormat("hh:mm a", Locale.US).format(cal.time))
        }, 12, 0, false).show()
    }

    private fun setupImageUpload() {
        binding.btnSelectBannerImage.setOnClickListener { showImagePickerDialog() }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
        AlertDialog.Builder(requireContext())
            .setTitle("Add Project Banner")
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
        val context = context ?: return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        val context = context ?: return
        try {
            val file = File.createTempFile("BANNER_", ".jpg", context.cacheDir)
            photoFile = file
            val uri = FileProvider.getUriForFile(context, "com.example.scenex.fileprovider", file)
            cameraLauncher.launch(uri)
        } catch (exception: Exception) {
            Toast.makeText(context, "Error starting camera", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleImageSelection(uri: Uri) {
        val ctx = context ?: return
        try {
            val inputStream = ctx.contentResolver.openInputStream(uri)
            val file = File(ctx.cacheDir, "banner_temp_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            viewModel.uploadBannerImage(file)
        } catch (exception: Exception) {
            Log.e("ImageUpload", "Selection Error: ${exception.message}")
            Toast.makeText(ctx, "Failed to process image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        // PERMANENT DATE/TIME SYNC
        viewModel.selectedDate.observe(viewLifecycleOwner) { date ->
            if (date != 0L) {
                val formatted = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(date))
                binding.tvDisplayDate.text = formatted
                binding.etDate.setText(formatted)
            }
        }

        viewModel.startTime.observe(viewLifecycleOwner) { time ->
            if (time.isNotEmpty()) {
                binding.tvDisplayStartTime.text = time
                binding.etStartTime.setText(time)
            }
        }

        viewModel.endTime.observe(viewLifecycleOwner) { time ->
            if (time.isNotEmpty()) {
                binding.tvDisplayEndTime.text = time
                binding.etEndTime.setText(time)
            }
        }

        // PERMANENT LOCATION/NOTES SYNC
        viewModel.location.observe(viewLifecycleOwner) { loc ->
            if (binding.etLocation.text.toString() != loc) {
                binding.etLocation.setText(loc)
            }
        }

        viewModel.notes.observe(viewLifecycleOwner) { nts ->
            if (binding.etNotes.text.toString() != nts) {
                binding.etNotes.setText(nts)
            }
        }

        viewModel.bannerImageUrl.observe(viewLifecycleOwner) { url ->
            if (_binding != null && !url.isNullOrEmpty()) {
                binding.ivBannerPreview.visibility = View.VISIBLE
                binding.layoutUploadPlaceholder.visibility = View.GONE
                Glide.with(this).load(url).into(binding.ivBannerPreview)
            }
        }

        viewModel.uploadStatus.observe(viewLifecycleOwner) { status ->
            if (_binding != null) {
                binding.pbImageUpload.visibility = if (status == "UPLOADING") View.VISIBLE else View.GONE
            }
        }

        viewModel.conflictResult.observe(viewLifecycleOwner) { result ->
            if (_binding != null) {
                if (result.hasConflict) {
                    showConflictDialog(result.suggestedSlots)
                } else {
                    if ((viewModel.selectedDate.value ?: 0L) != 0L && 
                        !viewModel.startTime.value.isNullOrEmpty() && 
                        !viewModel.endTime.value.isNullOrEmpty()) {
                        showSuccessFlow()
                    }
                }
            }
        }

        viewModel.bookingStatus.observe(viewLifecycleOwner) { result ->
            if (_binding != null && result.isSuccess) {
                val msg = if (editBookingId != null) "Booking Updated Successfully!" else "Booking Sent Successfully!"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                if (activity is EditBookingActivity) {
                    activity?.finish()
                } else {
                    parentFragmentManager.popBackStack()
                }
            }
        }

        viewModel.existingBooking.observe(viewLifecycleOwner) { booking ->
            if (booking != null) {
                binding.etProjectTitle.setText(booking.castingTitle)
                binding.etCategory.setText(booking.role)
                binding.etTalentName.setText(booking.name)
                binding.etLocation.setText(booking.location)
                binding.etNotes.setText(booking.notes)
                
                talentId = booking.talentId
                castingCallId = booking.castingCallId
                castingTitle = booking.castingTitle
                spotlightCategory = booking.role
                recruiterName = booking.recruiterName
            }
        }
    }

    private fun showSuccessFlow() {
        binding.btnCheckAvailability.visibility = View.GONE
        binding.btnSubmitBooking.visibility = View.VISIBLE
        binding.layoutNotes.visibility = View.VISIBLE
        Toast.makeText(context, "Talent is available!", Toast.LENGTH_SHORT).show()
    }

    private fun showConflictDialog(slots: List<String>) {
        val ctx = context ?: return
        val bottomSheet = BottomSheetDialog(ctx)
        val dialogBinding = DialogConflictFoundBinding.inflate(layoutInflater)
        bottomSheet.setContentView(dialogBinding.root)

        val radioGroup = dialogBinding.rgSuggestedSlots
        radioGroup.removeAllViews()

        if (slots.isEmpty()) {
            val rb = RadioButton(ctx)
            rb.text = "No other slots available for this day"
            rb.isEnabled = false
            radioGroup.addView(rb)
        } else {
            slots.forEach { slot ->
                val rb = RadioButton(ctx)
                rb.text = slot
                rb.setPadding(24, 32, 24, 32)
                radioGroup.addView(rb)
            }
        }

        dialogBinding.btnSelectSlot.setOnClickListener {
            val checkedId = radioGroup.checkedRadioButtonId
            if (checkedId != -1) {
                val selectedRb = radioGroup.findViewById<RadioButton>(checkedId)
                if (selectedRb.isEnabled) {
                    val slotText = selectedRb.text.toString()
                    val times = slotText.split(" - ")
                    viewModel.startTime.value = times[0]
                    viewModel.endTime.value = times[1]
                    bottomSheet.dismiss()
                    showSuccessFlow()
                }
            } else {
                Toast.makeText(ctx, "Please select a slot", Toast.LENGTH_SHORT).show()
            }
        }
        bottomSheet.show()
    }

    private fun setupListeners() {
        binding.btnCheckAvailability.setOnClickListener {
            if (validateTimeSelection()) {
                viewModel.checkAvailability(talentId, editBookingId)
            }
        }

        binding.btnSubmitBooking.setOnClickListener {
            if (validateForm()) {
                if (editBookingId != null) {
                    viewModel.updateBooking(editBookingId!!)
                } else {
                    viewModel.createBooking(
                        castingCallId = castingCallId,
                        castingTitle = binding.etProjectTitle.text.toString(),
                        talentId = talentId,
                        talentName = binding.etTalentName.text.toString(),
                        spotlightCategory = binding.etCategory.text.toString(),
                        recruiterName = recruiterName
                    )
                }
            }
        }
    }

    private fun validateTimeSelection(): Boolean {
        if ((viewModel.selectedDate.value ?: 0L) == 0L) { Toast.makeText(context, "Select a date", Toast.LENGTH_SHORT).show(); return false }
        if (viewModel.startTime.value.isNullOrEmpty()) { Toast.makeText(context, "Select start time", Toast.LENGTH_SHORT).show(); return false }
        if (viewModel.endTime.value.isNullOrEmpty()) { Toast.makeText(context, "Select end time", Toast.LENGTH_SHORT).show(); return false }
        return true
    }

    private fun validateForm(): Boolean {
        if (binding.etProjectTitle.text.isNullOrEmpty()) { binding.etProjectTitle.error = "Required"; return false }
        if (binding.etLocation.text.isNullOrEmpty()) { binding.etLocation.error = "Required"; return false }
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TALENT_ID = "talent_id"
        private const val ARG_TALENT_NAME = "talent_name"
        private const val ARG_SPOTLIGHT_CATEGORY = "spotlight_category"
        private const val ARG_CASTING_ID = "casting_id"
        private const val ARG_CASTING_TITLE = "casting_title"
        private const val ARG_EDIT_BOOKING_ID = "edit_booking_id"

        fun newInstance(talentId: String, talentName: String, spotlightCategory: String, castingCallId: String, castingTitle: String) =
            RecruiterCreateBookingFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TALENT_ID, talentId)
                    putString(ARG_TALENT_NAME, talentName)
                    putString(ARG_SPOTLIGHT_CATEGORY, spotlightCategory)
                    putString(ARG_CASTING_ID, castingCallId)
                    putString(ARG_CASTING_TITLE, castingTitle)
                }
            }

        fun newEditInstance(bookingId: String) =
            RecruiterCreateBookingFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_EDIT_BOOKING_ID, bookingId)
                }
            }
    }
}
