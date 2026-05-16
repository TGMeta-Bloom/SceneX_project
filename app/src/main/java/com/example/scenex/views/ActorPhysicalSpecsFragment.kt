package com.example.scenex.views

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.scenex.R
import com.example.scenex.viewmodels.SignupViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.io.File
import java.io.FileOutputStream

class ActorPhysicalSpecsFragment : Fragment() {

    private val viewModel: SignupViewModel by activityViewModels()
    private var currentImageType: String = ""

    // Media Pickers
    private val pickImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            val file = uriToFile(it)
            file?.let { f -> viewModel.uploadMedia(f, currentImageType) }
        }
    }

    private val pickVideo = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            val file = uriToFile(it)
            file?.let { f -> viewModel.uploadMedia(f, "VIDEO") }
        }
    }

    private val pickAudio = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val file = uriToFile(it)
            file?.let { f -> viewModel.uploadMedia(f, "AUDIO") }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_actor_specs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Physical Specs Views
        val spinnerHeight = view.findViewById<Spinner>(R.id.spinnerHeight)
        val spinnerBuild = view.findViewById<Spinner>(R.id.spinnerBuild)
        val spinnerHair = view.findViewById<Spinner>(R.id.spinnerHair)
        val spinnerEye = view.findViewById<Spinner>(R.id.spinnerEye)
        
        // Skills Views
        val chipGroupAccents = view.findViewById<ChipGroup>(R.id.chipGroupAccents)
        val chipGroupOtherSkills = view.findViewById<ChipGroup>(R.id.chipGroupOtherSkills)
        val btnAddSkill = view.findViewById<ImageView>(R.id.btnAddSkill)
        
        // Media Buttons
        val btnHeadshot = view.findViewById<Button>(R.id.btnHeadshot)
        val btnFullBody = view.findViewById<Button>(R.id.btnFullBody)
        val btnUploadVideo = view.findViewById<Button>(R.id.btnUploadVideo)
        val btnUploadAudio = view.findViewById<Button>(R.id.btnUploadAudio)
        
        val btnGeneratePortfolio = view.findViewById<Button>(R.id.btnGeneratePortfolio)

        // Initialize Spinner Data
        setupSpinners(spinnerHeight, spinnerBuild, spinnerHair, spinnerEye)

        // Observe Media Statuses
        viewModel.headshotStatus.observe(viewLifecycleOwner) { btnHeadshot?.text = it }
        viewModel.fullBodyStatus.observe(viewLifecycleOwner) { btnFullBody?.text = it }
        viewModel.videoStatus.observe(viewLifecycleOwner) { btnUploadVideo?.text = it }
        viewModel.audioStatus.observe(viewLifecycleOwner) { btnUploadAudio?.text = it }

        // Add Skill Logic
        btnAddSkill?.setOnClickListener {
            showAddSkillDialog(chipGroupOtherSkills)
        }

        // Media Button Listeners
        btnHeadshot?.setOnClickListener {
            currentImageType = "HEADSHOT"
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        btnFullBody?.setOnClickListener {
            currentImageType = "FULLBODY"
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        btnUploadVideo?.setOnClickListener {
            pickVideo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
        }
        btnUploadAudio?.setOnClickListener {
            pickAudio.launch("audio/*")
        }

        btnGeneratePortfolio.setOnClickListener {
            // Collect Physical Specs
            viewModel.height = spinnerHeight.selectedItem?.toString() ?: ""
            viewModel.bodyType = spinnerBuild.selectedItem?.toString() ?: ""
            viewModel.hairColor = spinnerHair.selectedItem?.toString() ?: ""
            viewModel.eyeColor = spinnerEye.selectedItem?.toString() ?: ""

            // Collect Selected Skills
            viewModel.accents = getCheckedChipsText(chipGroupAccents)
            viewModel.otherSkills = getCheckedChipsText(chipGroupOtherSkills)

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

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
        }
    }

    private fun showAddSkillDialog(chipGroup: ChipGroup?) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Add New Skill")
        val input = EditText(requireContext())
        builder.setView(input)
        builder.setPositiveButton("Add") { _, _ ->
            val skill = input.text.toString().trim()
            if (skill.isNotEmpty()) {
                val chip = Chip(requireContext(), null, com.google.android.material.R.style.Widget_MaterialComponents_Chip_Choice)
                chip.text = skill
                chip.isCheckable = true
                chip.isChecked = true
                chipGroup?.addView(chip)
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun getCheckedChipsText(chipGroup: ChipGroup?): List<String> {
        val texts = mutableListOf<String>()
        chipGroup?.let {
            for (i in 0 until it.childCount) {
                val chip = it.getChildAt(i) as? Chip
                if (chip != null && chip.isChecked) {
                    texts.add(chip.text.toString())
                }
            }
        }
        return texts
    }

    private fun setupSpinners(height: Spinner, build: Spinner, hair: Spinner, eye: Spinner) {
        val heights = (140..210).map { "$it cm" }
        val builds = listOf("Slim", "Athletic", "Average", "Heavyset", "Muscular")
        val hairColors = listOf("Black", "Brown", "Blonde", "Auburn", "Grey", "Other")
        val eyeColors = listOf("Black", "Brown", "Blue", "Green", "Hazel", "Other")

        val context = requireContext()
        height.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, heights)
        build.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, builds)
        hair.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, hairColors)
        eye.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, eyeColors)
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val file = File(requireContext().cacheDir, "temp_media_${System.currentTimeMillis()}")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file
        } catch (e: Exception) {
            null
        }
    }
}
