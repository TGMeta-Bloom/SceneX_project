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
import com.bumptech.glide.load.engine.DiskCacheStrategy
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

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                Glide.with(this).load(uri).placeholder(R.drawable.ic_profile_placeholder).centerCrop().into(ivProfileImage)
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_signup_step1, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ivProfileImage = view.findViewById(R.id.ivProfileImage)
        pbImageUpload = view.findViewById(R.id.pbImageUpload)
        val etFullName = view.findViewById<EditText>(R.id.etFullName)
        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etUserName = view.findViewById<EditText>(R.id.etUserName)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = view.findViewById<EditText>(R.id.etConfirmPassword)
        val passwordContainer = view.findViewById<View>(R.id.passwordContainer)
        val confirmPasswordContainer = view.findViewById<View>(R.id.confirmPasswordContainer)
        val labelPassword = view.findViewById<View>(R.id.labelPassword)
        val labelConfirmPassword = view.findViewById<View>(R.id.labelConfirmPassword)
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
        val tvLogin = view.findViewById<TextView>(R.id.tvLogin)
        val btnCreateAccount = view.findViewById<Button>(R.id.btnCreateAccount)

        // FIX: Hide password fields if Social Auth (Google/FB) is used
        if (viewModel.isSocialAuth) {
            passwordContainer.visibility = View.GONE
            confirmPasswordContainer.visibility = View.GONE
            labelPassword.visibility = View.GONE
            labelConfirmPassword.visibility = View.GONE
            // Pre-fill social data
            if (viewModel.fullName.isNotEmpty()) etFullName.setText(viewModel.fullName)
            if (viewModel.email.isNotEmpty()) etEmail.setText(viewModel.email)
            etEmail.isEnabled = false // Manage email via provider
        }

        view.findViewById<View>(R.id.btnUploadImage).setOnClickListener { showImagePickerDialog() }

        // FIX: Wire up the Login link
        tvLogin.setOnClickListener {
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }

        viewModel.isUploading.observe(viewLifecycleOwner) { isUploading ->
            pbImageUpload.visibility = if (isUploading == true) View.VISIBLE else View.GONE
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

        viewModel.navigateToNextStep.observe(viewLifecycleOwner) { destination ->
            if (destination == "STEP2") {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.signupFragmentContainer, SignupStep2Fragment())
                    .addToBackStack(null).commit()
            }
        }

        btnCreateAccount.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val userName = etUserName.text.toString().trim()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            if (fullName.isEmpty() || email.isEmpty() || userName.isEmpty()) {
                Toast.makeText(context, "Required fields missing", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!viewModel.isSocialAuth) {
                if (password.isEmpty() || password != confirmPassword) {
                    Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                viewModel.password = password
            }

            viewModel.fullName = fullName
            viewModel.email = email
            viewModel.userName = userName
            viewModel.phoneNumber = etPhone.text.toString()
            viewModel.age = etAge.text.toString().toIntOrNull() ?: 0
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
        android.app.AlertDialog.Builder(requireContext()).setTitle("Upload Profile Picture")
            .setItems(options) { dialog, item ->
                when (options[item]) {
                    "Take Photo" -> cameraLauncher.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
                    "Choose from Gallery" -> galleryLauncher.launch(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI))
                    else -> dialog.dismiss()
                }
            }.show()
    }

    private fun uriToFile(uri: Uri): File {
        val inputStream = requireContext().contentResolver.openInputStream(uri)
        val tempFile = File(requireContext().cacheDir, "temp_profile_${System.currentTimeMillis()}.jpg")
        inputStream?.use { input -> tempFile.outputStream().use { output -> input.copyTo(output) } }
        return tempFile
    }

    private fun bitmapToFile(bitmap: Bitmap): File {
        val tempFile = File(requireContext().cacheDir, "temp_cam_${System.currentTimeMillis()}.jpg")
        tempFile.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        return tempFile
    }

    private fun setupLocationSpinners(provinceSpinner: Spinner, citySpinner: Spinner) {
        val provincesList: List<String> = viewModel.provinces
        val provinceAdapter = ArrayAdapter<String>(requireContext(), R.layout.custom_spinner_item, provincesList)
        provinceAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item)
        provinceSpinner.adapter = provinceAdapter
        viewModel.availableCities.observe(viewLifecycleOwner) { cities ->
            citySpinner.adapter = ArrayAdapter<String>(requireContext(), R.layout.custom_spinner_item, cities ?: emptyList()).apply { setDropDownViewResource(R.layout.custom_spinner_dropdown_item) }
        }
        provinceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { viewModel.onProvinceSelected(provincesList[position]) }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
}
