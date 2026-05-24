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
import com.example.scenex.R
import com.example.scenex.viewmodels.SignupViewModel
import com.google.android.material.imageview.ShapeableImageView
import java.io.File
import java.io.FileOutputStream

class RecruiterVerificationFragment : Fragment() {

    private val viewModel: SignupViewModel by activityViewModels()
    private lateinit var ivProfileImage: ShapeableImageView
    private lateinit var pbImageUpload: ProgressBar
    private lateinit var tvUploadPrompt: TextView
    private lateinit var ivUploadIcon: ImageView

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) launchCamera() else Toast.makeText(requireContext(), "Camera permission required", Toast.LENGTH_SHORT).show()
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                uploadDocument(uri)
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            (result.data?.extras?.get("data") as? Bitmap)?.let { bitmap ->
                uploadDocument(bitmap)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_recruiter_verification, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ivProfileImage = view.findViewById(R.id.ivProfileImage)
        pbImageUpload = view.findViewById(R.id.pbImageUpload)
        val uploadBox = view.findViewById<View>(R.id.uploadBox)
        ivUploadIcon = view.findViewById(R.id.ivUploadIcon)
        tvUploadPrompt = view.findViewById(R.id.tvUploadPrompt)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmit)
        val tvSupport = view.findViewById<TextView>(R.id.tvSupport)

        // Observe verification status to update UI
        viewModel.verificationDocStatus.observe(viewLifecycleOwner) { status ->
            tvUploadPrompt.text = status
            if (status.contains("✅")) {
                ivUploadIcon.setImageResource(R.drawable.ic_projects) // Or a success icon
                ivUploadIcon.alpha = 1.0f
            }
        }

        uploadBox.setOnClickListener {
            showImagePickerDialog()
        }

        btnSubmit.setOnClickListener {
            viewModel.finalizeRecruiterSignup()
        }

        // Navigation Observer for finalization
        viewModel.navigateToNextStep.observe(viewLifecycleOwner) { destination ->
            if (destination == "FINISH") {
                startActivity(Intent(requireContext(), WaitingRoomActivity::class.java))
                requireActivity().finish()
            }
        }

        tvSupport.setOnClickListener {
            Toast.makeText(context, "Redirecting to support...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadDocument(uri: Uri) {
        viewModel.uploadVerificationDoc(uriToFile(uri))
    }

    private fun uploadDocument(bitmap: Bitmap) {
        viewModel.uploadVerificationDoc(bitmapToFile(bitmap))
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Upload Verification Document")
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
        val tempFile = File(requireContext().cacheDir, "temp_recruiter_verify_${System.currentTimeMillis()}.jpg")
        inputStream?.use { input -> tempFile.outputStream().use { output -> input.copyTo(output) } }
        return tempFile
    }

    private fun bitmapToFile(bitmap: Bitmap): File {
        val tempFile = File(requireContext().cacheDir, "temp_recruiter_verify_cam_${System.currentTimeMillis()}.jpg")
        val out = FileOutputStream(tempFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        out.flush()
        out.close()
        return tempFile
    }
}
