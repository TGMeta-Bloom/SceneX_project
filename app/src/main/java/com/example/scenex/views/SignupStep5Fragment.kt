package com.example.scenex.views

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.example.scenex.MainActivity
import com.example.scenex.R
import com.example.scenex.viewmodels.SignupViewModel
import com.google.android.material.imageview.ShapeableImageView
import java.io.File
import java.io.FileOutputStream

class SignupStep5Fragment : Fragment() {

    private val viewModel: SignupViewModel by activityViewModels()

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val file = uriToFile(uri)
            file?.let { viewModel.addPortfolioImage(it) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_signup_step5, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ivProfileImage = view.findViewById<ShapeableImageView>(R.id.ivProfileImage)
        val uploadPlaceholder = view.findViewById<CardView>(R.id.uploadPlaceholder)
        val btnFinish = view.findViewById<Button>(R.id.btnFinish)

        // Load profile picture from Step 1
        viewModel.profileImageUrl.observe(viewLifecycleOwner) { url ->
            Glide.with(this)
                .load(url)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .into(ivProfileImage)
        }

        uploadPlaceholder.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        viewModel.portfolioImages.observe(viewLifecycleOwner) { images ->
            if (images.isNotEmpty()) {
                Toast.makeText(requireContext(), "${images.size} images uploaded to portfolio", Toast.LENGTH_SHORT).show()
            }
        }

        btnFinish.setOnClickListener {
            viewModel.finalizeRegistration()
        }

        // Navigation Observer
        viewModel.navigateToNextStep.observe(viewLifecycleOwner) { destination ->
            if (destination == "FINISH") {
                Toast.makeText(requireContext(), "Welcome to SceneX!", Toast.LENGTH_LONG).show()
                val intent = Intent(requireContext(), MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
        }
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val file = File(requireContext().cacheDir, "portfolio_${System.currentTimeMillis()}.jpg")
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
