package com.example.scenex.views

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.example.scenex.R
import com.example.scenex.viewmodels.SignupViewModel
import com.google.android.material.imageview.ShapeableImageView
import java.io.File
import java.io.FileOutputStream

class RecruiterSignupStep1Fragment : Fragment() {

    private val viewModel: SignupViewModel by activityViewModels()
    private var isPasswordVisible = false

    private lateinit var ivProfileImage: ShapeableImageView
    private lateinit var pbImageUpload: ProgressBar
    private lateinit var btnCreateAccount: AppCompatButton

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) launchCamera() else Toast.makeText(requireContext(), "Camera permission required", Toast.LENGTH_SHORT).show()
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                Glide.with(this).load(uri).circleCrop().into(ivProfileImage)
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
        return inflater.inflate(R.layout.fragment_recruiter_signup_step1, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ivProfileImage = view.findViewById(R.id.ivProfileImage)
        pbImageUpload = view.findViewById(R.id.pbImageUpload)
        val btnUploadImage = view.findViewById<View>(R.id.btnUploadImage)
        val etFullName = view.findViewById<EditText>(R.id.etFullName)
        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etPhone = view.findViewById<EditText>(R.id.etPhone)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val ivTogglePassword = view.findViewById<ImageView>(R.id.ivTogglePassword)
        btnCreateAccount = view.findViewById(R.id.btnCreateAccount)
        val tvLogin = view.findViewById<TextView>(R.id.tvLogin)

        btnUploadImage.setOnClickListener { showImagePickerDialog() }

        // Observe Uploading State
        viewModel.isUploading.observe(viewLifecycleOwner) { isUploading ->
            pbImageUpload.visibility = if (isUploading == true) View.VISIBLE else View.GONE
            btnCreateAccount.isEnabled = isUploading != true
        }

        // Observe Profile Image URL
        viewModel.profileImageUrl.observe(viewLifecycleOwner) { url ->
            if (!url.isNullOrEmpty()) {
                Glide.with(this).load(url).circleCrop().into(ivProfileImage)
            }
        }

        // Password Visibility Toggle
        ivTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            etPassword.transformationMethod = if (isPasswordVisible) HideReturnsTransformationMethod.getInstance() else PasswordTransformationMethod.getInstance()
            etPassword.setSelection(etPassword.text.length)
        }

        // Login Link Handshake
        tvLogin.setOnClickListener {
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }

        // Senior Fix: Observe Errors to provide feedback and reset button state
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                btnCreateAccount.isEnabled = true
            }
        }

        // Navigation Observer for Recruiter Flow
        viewModel.navigateToNextStep.observe(viewLifecycleOwner) { destination ->
            if (destination == "RECRUITER_STEP2") {
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.signupFragmentContainer, RecruiterSignupStep2Fragment())
                    .addToBackStack(null)
                    .commit()
                viewModel.clearNavigation()
            }
        }

        // Account Creation Handshake
        btnCreateAccount.setOnClickListener {
            val name = etFullName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val pass = etPassword.text.toString()

            if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || pass.isEmpty()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass.length < 6) {
                etPassword.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }

            // Disable button to prevent double-click
            btnCreateAccount.isEnabled = false
            
            // Sync Data with ViewModel
            viewModel.userRole = "RECRUITER"
            viewModel.fullName = name
            viewModel.email = email
            viewModel.phoneNumber = phone
            viewModel.password = pass
            viewModel.userName = name 
            
            // Trigger Authentication Handshake
            viewModel.createAccount()
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
        val tempFile = File(requireContext().cacheDir, "temp_rec_signup_${System.currentTimeMillis()}.jpg")
        inputStream?.use { input -> tempFile.outputStream().use { output -> input.copyTo(output) } }
        return tempFile
    }

    private fun bitmapToFile(bitmap: Bitmap): File {
        val tempFile = File(requireContext().cacheDir, "temp_rec_cam_${System.currentTimeMillis()}.jpg")
        val out = FileOutputStream(tempFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        out.flush()
        out.close()
        return tempFile
    }
}
