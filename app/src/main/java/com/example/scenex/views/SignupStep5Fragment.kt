package com.example.scenex.views

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.scenex.R
import com.example.scenex.adapters.PortfolioWorkAdapter
import com.example.scenex.models.PortfolioWork
import com.example.scenex.viewmodels.SignupViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import java.io.File
import java.io.FileOutputStream

class SignupStep5Fragment : Fragment() {

    private val viewModel: SignupViewModel by activityViewModels()
    private lateinit var adapter: PortfolioWorkAdapter

    private var ivDialogPreview: ShapeableImageView? = null
    private var selectedWorkImageUrl: String? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            uriToFile(it)?.let { file ->
                ivDialogPreview?.alpha = 0.5f
                viewModel.uploadWorkImage(file) { url ->
                    ivDialogPreview?.alpha = 1.0f
                    if (url != null) {
                        selectedWorkImageUrl = url
                        ivDialogPreview?.let { preview ->
                            Glide.with(this).load(url).into(preview)
                        }
                    } else {
                        Toast.makeText(requireContext(), "Image upload failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
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
        val rvPortfolioWorks = view.findViewById<RecyclerView>(R.id.rvPortfolioWorks)
        val tvEmptyState = view.findViewById<TextView>(R.id.tvEmptyState)
        val btnAddWork = view.findViewById<MaterialButton>(R.id.btnAddWork)
        val btnFinish = view.findViewById<Button>(R.id.btnFinish)

        // Initialize RecyclerView
        adapter = PortfolioWorkAdapter(works = emptyList(), showOptions = false) { _, _ ->
            /* Signup version doesn't need actions */
        }
        rvPortfolioWorks.layoutManager = LinearLayoutManager(requireContext())
        rvPortfolioWorks.adapter = adapter

        viewModel.profileImageUrl.observe(viewLifecycleOwner) { url ->
            Glide.with(this)
                .load(url)
                .placeholder(R.drawable.ic_profile_placeholder)
                .into(ivProfileImage)
        }

        // Observe Portfolio Works
        viewModel.portfolioWorks.observe(viewLifecycleOwner) { works ->
            adapter.updateWorks(works)
            tvEmptyState.visibility = if (works.isEmpty()) View.VISIBLE else View.GONE
        }

        btnAddWork.setOnClickListener {
            showAddWorkDialog()
        }

        btnFinish.setOnClickListener {
            viewModel.finalizeRegistration()
        }

        // Navigation Observer
        viewModel.navigateToNextStep.observe(viewLifecycleOwner) { destination ->
            if (destination == "FINISH") {
                Toast.makeText(requireContext(), "Profile Ready for Review!", Toast.LENGTH_LONG).show()
                val intent = Intent(requireContext(), WaitingRoomActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
        }
    }

    private fun showAddWorkDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_portfolio_work, null)
        val dialog = AlertDialog.Builder(requireContext(), R.style.SceneX_Dialog_Rounded)
            .setView(dialogView)
            .create()

        ivDialogPreview = dialogView.findViewById(R.id.ivWorkImagePreview)
        val tvAddImage = dialogView.findViewById<TextView>(R.id.tvAddImageLabel)
        val etTitle = dialogView.findViewById<EditText>(R.id.etWorkTitle)
        val etProjectType = dialogView.findViewById<EditText>(R.id.etProjectType)
        val etRole = dialogView.findViewById<EditText>(R.id.etRolePlayed)
        val etYear = dialogView.findViewById<EditText>(R.id.etYear)
        val etDesc = dialogView.findViewById<EditText>(R.id.etDescription)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveWork)

        selectedWorkImageUrl = null

        val pickAction = View.OnClickListener {
            pickImageLauncher.launch("image/*")
        }
        ivDialogPreview?.setOnClickListener(pickAction)
        tvAddImage.setOnClickListener(pickAction)

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val type = etProjectType.text.toString().trim()
            val role = etRole.text.toString().trim()
            val year = etYear.text.toString().trim()
            val desc = etDesc.text.toString().trim()

            if (title.isEmpty() || type.isEmpty() || role.isEmpty() || year.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill mandatory fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val work = PortfolioWork(
                title = title,
                projectType = type,
                rolePlayed = role,
                year = year,
                description = desc,
                imageUrl = selectedWorkImageUrl
            )

            viewModel.addPortfolioWork(work)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun uriToFile(uri: android.net.Uri): File? {
        return try {
            val contentResolver = requireContext().contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return null

            // Load bitmap for compression
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            val file = File(requireContext().cacheDir, "temp_work_image_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)

            //  Compress to 70% quality to reduce upload size
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
            outputStream.close()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
