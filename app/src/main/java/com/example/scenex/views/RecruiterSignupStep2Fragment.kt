package com.example.scenex.views

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.example.scenex.R
import com.example.scenex.viewmodels.SignupViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.imageview.ShapeableImageView
import java.io.File
import java.io.FileOutputStream

class RecruiterSignupStep2Fragment : Fragment() {

    private val viewModel: SignupViewModel by activityViewModels()
    private lateinit var ivProfileImage: ShapeableImageView
    private lateinit var pbImageUpload: ProgressBar

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) launchCamera() else Toast.makeText(requireContext(), "Camera permission required", Toast.LENGTH_SHORT).show()
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                ivProfileImage.setImageURI(uri)
                viewModel.uploadProfilePicture(uriToFile(uri))
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            (result.data?.extras?.get("data") as? Bitmap)?.let { bitmap ->
                ivProfileImage.setImageBitmap(bitmap)
                viewModel.uploadProfilePicture(bitmapToFile(bitmap))
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_recruiter_signup_step2, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ivProfileImage = view.findViewById(R.id.ivProfileImage)
        pbImageUpload = view.findViewById(R.id.pbImageUpload)
        val btnUploadImage = view.findViewById<View>(R.id.btnUploadImage)
        val chipGroup = view.findViewById<ChipGroup>(R.id.chipGroupSpotlight)
        val btnNext = view.findViewById<Button>(R.id.btnNext)
        val tvLogin = view.findViewById<TextView>(R.id.tvLogin)

        btnUploadImage.setOnClickListener { showImagePickerDialog() }

        viewModel.isUploading.observe(viewLifecycleOwner) { isUploading ->
            pbImageUpload.visibility = if (isUploading) View.VISIBLE else View.GONE
        }

        viewModel.profileImageUrl.observe(viewLifecycleOwner) { url ->
            if (!url.isNullOrEmpty()) {
                Glide.with(this).load(url).circleCrop().into(ivProfileImage)
            }
        }

        // Navigation Observer
        viewModel.navigateToNextStep.observe(viewLifecycleOwner) { destination ->
            if (destination == "RECRUITER_STEP3") {
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.signupFragmentContainer, RecruiterSignupStep3Fragment())
                    .addToBackStack(null)
                    .commit()
                viewModel.clearNavigation()
            }
        }

        btnNext.setOnClickListener {
            val selectedChipId = chipGroup.checkedChipId
            if (selectedChipId == -1) {
                Toast.makeText(context, "Please select your spotlight category", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedChip = view.findViewById<Chip>(selectedChipId)
            viewModel.spotlightCategory = selectedChip.text.toString()
            
            viewModel.updateSpotlightAndNavigate()
        }

        tvLogin.setOnClickListener {
            activity?.finish()
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Upload Profile Picture")
            .setItems(options) { dialog, item ->
                when (options[item]) {
                    "Take Photo" -> checkCameraPermissionAndLaunch()
                    "Choose from Gallery" -> galleryLauncher.launch(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI))
                    else -> dialog.dismiss()
                }
            }.show()
    }

    private fun checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) launchCamera()
        else requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun launchCamera() { cameraLauncher.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE)) }

    private fun uriToFile(uri: Uri): File {
        val inputStream = requireContext().contentResolver.openInputStream(uri)
        val tempFile = File(requireContext().cacheDir, "temp_recruiter_profile_${System.currentTimeMillis()}.jpg")
        inputStream?.use { input -> tempFile.outputStream().use { output -> input.copyTo(output) } }
        return tempFile
    }

    private fun bitmapToFile(bitmap: Bitmap): File {
        val tempFile = File(requireContext().cacheDir, "temp_recruiter_camera_${System.currentTimeMillis()}.jpg")
        val out = FileOutputStream(tempFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        out.flush()
        out.close()
        return tempFile
    }
}
