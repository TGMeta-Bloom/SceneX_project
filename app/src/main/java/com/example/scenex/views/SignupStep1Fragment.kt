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
import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.example.scenex.R
import com.example.scenex.viewmodels.SignupViewModel
import com.google.android.material.imageview.ShapeableImageView
import java.io.File
import java.io.FileOutputStream

class SignupStep1Fragment : Fragment() {

    private val viewModel: SignupViewModel by activityViewModels()
    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    private lateinit var ivProfileImage: ShapeableImageView
    private lateinit var pbImageUpload: ProgressBar

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) launchCamera() else Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_SHORT).show()
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> viewModel.uploadProfilePicture(uriToFile(uri)) }
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            (result.data?.extras?.get("data") as? Bitmap)?.let { viewModel.uploadProfilePicture(bitmapToFile(it)) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_signup_step1, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ivProfileImage = view.findViewById(R.id.ivProfileImage)
        pbImageUpload = view.findViewById(R.id.pbImageUpload)
        val btnUploadImage = view.findViewById<View>(R.id.btnUploadImage)
        val btnCreateAccount = view.findViewById<Button>(R.id.btnCreateAccount)
        val etFullName = view.findViewById<EditText>(R.id.etFullName)
        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etUserName = view.findViewById<EditText>(R.id.etUserName)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = view.findViewById<EditText>(R.id.etConfirmPassword)
        val ivTogglePassword = view.findViewById<ImageView>(R.id.ivTogglePassword)
        val ivToggleConfirmPassword = view.findViewById<ImageView>(R.id.ivToggleConfirmPassword)
        val etPhone = view.findViewById<EditText>(R.id.etPhone)
        val etAge = view.findViewById<EditText>(R.id.etAge)
        val rgGender = view.findViewById<RadioGroup>(R.id.rgGender)
        val spinnerProvince = view.findViewById<Spinner>(R.id.spinnerProvince)
        val spinnerCity = view.findViewById<Spinner>(R.id.spinnerCity)
        val rgRelationship = view.findViewById<RadioGroup>(R.id.rgRelationship)
        val etHobbies = view.findViewById<EditText>(R.id.etHobbies)
        val etBio = view.findViewById<EditText>(R.id.etBio)

        btnUploadImage.setOnClickListener { showImagePickerDialog() }

        viewModel.isUploading.observe(viewLifecycleOwner) { isUploading ->
            pbImageUpload.visibility = if (isUploading == true) View.VISIBLE else View.GONE
            btnUploadImage.isEnabled = isUploading != true
        }

        viewModel.profileImageUrl.observe(viewLifecycleOwner) { url ->
            url?.let { Glide.with(this).load(it).into(ivProfileImage) }
        }

        ivTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            etPassword.transformationMethod = if (isPasswordVisible) HideReturnsTransformationMethod.getInstance() else PasswordTransformationMethod.getInstance()
            etPassword.setSelection(etPassword.text.length)
        }

        ivToggleConfirmPassword.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            etConfirmPassword.transformationMethod = if (isConfirmPasswordVisible) HideReturnsTransformationMethod.getInstance() else PasswordTransformationMethod.getInstance()
            etConfirmPassword.setSelection(etConfirmPassword.text.length)
        }

        setupLocationSpinners(spinnerProvince, spinnerCity)

        // Observe navigation to Step 2
        viewModel.navigateToNextStep.observe(viewLifecycleOwner) { destination ->
            if (destination == "STEP2") {
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.signupFragmentContainer, SignupStep2Fragment())
                    .addToBackStack(null)
                    .commit()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
        }

        btnCreateAccount.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val userName = etUserName.text.toString().trim()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            if (fullName.isEmpty()) { etFullName.error = "Required"; return@setOnClickListener }
            if (email.isEmpty()) { etEmail.error = "Required"; return@setOnClickListener }
            if (userName.isEmpty()) { etUserName.error = "Required"; return@setOnClickListener }
            if (password.isEmpty()) { etPassword.error = "Required"; return@setOnClickListener }
            if (password != confirmPassword) { etConfirmPassword.error = "Passwords do not match"; return@setOnClickListener }

            viewModel.fullName = fullName
            viewModel.email = email
            viewModel.userName = userName
            viewModel.password = password
            viewModel.phoneNumber = etPhone.text.toString()
            viewModel.age = etAge.text.toString()
            
            val selectedGenderId = rgGender.checkedRadioButtonId
            viewModel.gender = if (selectedGenderId != -1) view.findViewById<RadioButton>(selectedGenderId).text.toString() else ""
            
            viewModel.province = spinnerProvince.selectedItem?.toString() ?: ""
            viewModel.city = spinnerCity.selectedItem?.toString() ?: ""
            
            val selectedRelId = rgRelationship.checkedRadioButtonId
            viewModel.relationshipStatus = if (selectedRelId != -1) view.findViewById<RadioButton>(selectedRelId).text.toString() else ""
                
            viewModel.hobbies = etHobbies.text.toString()
            viewModel.shortBio = etBio.text.toString()
            
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
        val tempFile = File(requireContext().cacheDir, "temp_profile_image.jpg")
        inputStream?.use { input -> tempFile.outputStream().use { output -> input.copyTo(output) } }
        return tempFile
    }

    private fun bitmapToFile(bitmap: Bitmap): File {
        val tempFile = File(requireContext().cacheDir, "temp_camera_image.jpg")
        tempFile.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        return tempFile
    }

    private fun setupLocationSpinners(provinceSpinner: Spinner, citySpinner: Spinner) {
        // Explicitly define type to prevent compilation inference errors
        val provincesList: List<String> = viewModel.provinces
        val provinceAdapter = ArrayAdapter<String>(requireContext(), R.layout.custom_spinner_item, provincesList)
        provinceAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item)
        provinceSpinner.adapter = provinceAdapter

        viewModel.availableCities.observe(viewLifecycleOwner, Observer { cities ->
            val cityAdapter = ArrayAdapter<String>(requireContext(), R.layout.custom_spinner_item, cities ?: emptyList())
            cityAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item)
            citySpinner.adapter = cityAdapter
        })

        provinceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position in provincesList.indices) {
                    viewModel.onProvinceSelected(provincesList[position])
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
}
