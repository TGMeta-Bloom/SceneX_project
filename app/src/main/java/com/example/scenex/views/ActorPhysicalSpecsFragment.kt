package com.example.scenex.views

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.LinearGradient
import android.graphics.Shader
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.scenex.R
import com.example.scenex.viewmodels.SignupViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.io.FileOutputStream

class ActorPhysicalSpecsFragment : Fragment() {

    private val viewModel: SignupViewModel by activityViewModels()
    
    private var currentImageType: String = ""
    private var tempImageUri: Uri? = null

    // --- Image Permission and Media Launchers ---

    private val requestCameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) launchCamera() 
        else Toast.makeText(context, "Camera permission required for photos", Toast.LENGTH_SHORT).show()
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { processMediaUpload(it) }
    }

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            tempImageUri?.let { processMediaUpload(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            currentImageType = savedInstanceState.getString("img_type", "")
            tempImageUri = savedInstanceState.getParcelable("temp_uri")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("img_type", currentImageType)
        outState.putParcelable("temp_uri", tempImageUri)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_actor_specs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Senior Fix: Apply Cinematic Brand Gradient to Title Text
        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        applyTextGradient(tvTitle)

        // 1. Setup Standardized Dropdowns with Auto-Save
        setupDropdowns(view)

        // 2. Setup Skills and Accents (Auto-save logic)
        setupSkillsAndAccents(view)

        // 3. Media Controls
        val btnHeadshot = view.findViewById<Button>(R.id.btnHeadshot)
        val btnFullBody = view.findViewById<Button>(R.id.btnFullBody)
        
        // Reel Link Input Fields
        val etVideoLink = view.findViewById<TextInputEditText>(R.id.etVideoLink)
        val etAudioLink = view.findViewById<TextInputEditText>(R.id.etAudioLink)

        // Observe progress from ViewModel (Images only)
        viewModel.headshotStatus.observe(viewLifecycleOwner) { btnHeadshot.text = it }
        viewModel.fullBodyStatus.observe(viewLifecycleOwner) { btnFullBody.text = it }

        btnHeadshot.setOnClickListener { showImageChoiceDialog("HEADSHOT") }
        btnFullBody.setOnClickListener { showImageChoiceDialog("FULLBODY") }

        // 4. Submission with Validation
        view.findViewById<Button>(R.id.btnGeneratePortfolio).setOnClickListener {
            // Validate Required Photos
            if (viewModel.headshotUrl.isEmpty() || viewModel.fullBodyUrl.isEmpty()) {
                Toast.makeText(context, "Required: Both Head-shot and Full Body photo.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Capture and Validate Reel Links
            val videoLink = etVideoLink?.text.toString().trim()
            val audioLink = etAudioLink?.text.toString().trim()

            // Video link is mandatory for portfolio evidence scoring
            if (videoLink.isEmpty()) {
                etVideoLink?.error = "Video reel link is required"
                Toast.makeText(context, "Please provide a video reel link (YouTube/Drive).", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Save reel links to ViewModel
            viewModel.videoUrl = videoLink
            viewModel.audioUrl = audioLink

            viewModel.saveActorSpecsAndNavigate()
        }

        // Navigation Observer
        viewModel.navigateToNextStep.observe(viewLifecycleOwner) { destination ->
            if (destination == "STEP5") {
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                    )
                    .replace(R.id.signupFragmentContainer, SignupStep5Fragment())
                    .addToBackStack(null)
                    .commit()
                viewModel.clearNavigation()
            }
        }
    }

    /**
     * Senior Fix: Programmatically applies the SceneX Brand Gradient to the text.
     */
    private fun applyTextGradient(textView: TextView) {
        textView.post {
            val paint = textView.paint
            val width = paint.measureText(textView.text.toString())
            if (width > 0) {
                val textShader: Shader = LinearGradient(
                    0f, 0f, width, 0f,
                    intArrayOf(
                        ContextCompat.getColor(requireContext(), R.color.primary_magenta),
                        ContextCompat.getColor(requireContext(), R.color.primary_dark)
                    ),
                    null, Shader.TileMode.CLAMP
                )
                textView.paint.shader = textShader
                textView.invalidate()
            }
        }
    }

    private fun setupDropdowns(v: View) {
        val heights = (140..215).map { "$it cm" }.toTypedArray()
        val builds = arrayOf("Slim", "Athletic", "Average", "Muscular", "Heavyset", "Petite", "Plus-sized")
        val hairColors = arrayOf("Black", "Brown", "Blonde", "Red", "Auburn", "Grey", "White", "Bald")
        val eyeColors = arrayOf("Black", "Brown", "Blue", "Green", "Hazel", "Grey")

        bindDropdown(v.findViewById(R.id.etHeight), heights) { viewModel.height = it }
        bindDropdown(v.findViewById(R.id.etBuild), builds) { viewModel.bodyType = it }
        bindDropdown(v.findViewById(R.id.etHair), hairColors) { viewModel.hairColor = it }
        bindDropdown(v.findViewById(R.id.etEye), eyeColors) { viewModel.eyeColor = it }
    }

    private fun bindDropdown(view: AutoCompleteTextView?, items: Array<String>, onSelect: (String) -> Unit) {
        view?.let {
            it.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, items))
            it.setOnClickListener { view.showDropDown() }
            it.setOnItemClickListener { parent, _, pos, _ ->
                val selection = parent.getItemAtPosition(pos).toString()
                onSelect(selection)
            }
        }
    }

    private fun setupSkillsAndAccents(v: View) {
        val accentGroup = v.findViewById<ChipGroup>(R.id.chipGroupAccents)
        val otherSkillsGroup = v.findViewById<ChipGroup>(R.id.chipGroupOtherSkills)
        val etAddSkill = v.findViewById<AutoCompleteTextView>(R.id.etAddSkill)

        // Listen for checked changes in Accents
        for (i in 0 until accentGroup.childCount) {
            (accentGroup.getChildAt(i) as? Chip)?.setOnCheckedChangeListener { _, _ -> saveSkills(accentGroup, otherSkillsGroup) }
        }

        // Search and Add Skill Logic
        val skills = arrayOf("Martial Arts", "Stunts", "Swimming", "Horse Riding", "Dubbing", "Singing", "Dance", "Dialect Coaching")
        etAddSkill.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, skills))
        etAddSkill.setOnClickListener { etAddSkill.showDropDown() }
        etAddSkill.setOnItemClickListener { parent, _, pos, _ ->
            val skill = parent.getItemAtPosition(pos).toString()
            addSkillChip(skill, otherSkillsGroup, accentGroup)
            etAddSkill.setText("")
        }
    }

    private fun addSkillChip(text: String, group: ChipGroup, accentGroup: ChipGroup) {
        for (i in 0 until group.childCount) if ((group.getChildAt(i) as Chip).text == text) return
        val chip = Chip(requireContext(), null, com.google.android.material.R.style.Widget_MaterialComponents_Chip_Entry)
        chip.text = text
        chip.isCloseIconVisible = true
        chip.setOnCloseIconClickListener { 
            group.removeView(chip)
            saveSkills(accentGroup, group)
        }
        group.addView(chip)
        saveSkills(accentGroup, group)
    }

    private fun saveSkills(accents: ChipGroup, others: ChipGroup) {
        val accentList = mutableListOf<String>()
        for (i in 0 until accents.childCount) {
            val chip = accents.getChildAt(i) as Chip
            if (chip.isChecked) accentList.add(chip.text.toString())
        }
        viewModel.accents = accentList

        val otherList = mutableListOf<String>()
        for (i in 0 until others.childCount) {
            otherList.add((others.getChildAt(i) as Chip).text.toString())
        }
        viewModel.otherSkills = otherList
    }

    private fun showImageChoiceDialog(type: String) {
        currentImageType = type
        val options = arrayOf("Take Photo with Camera", "Choose from Gallery")
        AlertDialog.Builder(requireContext())
            .setTitle("Upload Photo")
            .setItems(options) { _, which ->
                if (which == 0) checkCameraPermission() 
                else pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
            .show()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        try {
            val file = File(requireContext().cacheDir, "camera_capture.jpg")
            if (file.exists()) file.delete()
            file.createNewFile()
            tempImageUri = FileProvider.getUriForFile(requireContext(), "com.example.scenex.fileprovider", file)
            takePhoto.launch(tempImageUri!!)
        } catch (e: Exception) {
            Toast.makeText(context, "Error starting camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processMediaUpload(uri: Uri) {
        uriToFile(uri)?.let { viewModel.uploadMedia(it, currentImageType) }
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val file = File(requireContext().cacheDir, "upload_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.use { input -> outputStream.use { output -> input.copyTo(output) } }
            file
        } catch (e: Exception) { null }
    }
}
