package com.example.scenex.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.scenex.MainActivity
import com.example.scenex.R
import com.example.scenex.viewmodels.SignupViewModel

class SignupStep1Fragment : Fragment() {

    private val viewModel: SignupViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_signup_step1, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnCreateAccount = view.findViewById<Button>(R.id.btnCreateAccount)
        val etFullName = view.findViewById<EditText>(R.id.etFullName)
        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etStageName = view.findViewById<EditText>(R.id.etStageName)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)

        viewModel.signupStatus.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Account Created!", Toast.LENGTH_SHORT).show()
                // Navigating to MainActivity (which is in the root package)
                startActivity(Intent(requireContext(), MainActivity::class.java))
                requireActivity().finish()
            } else {
                Toast.makeText(requireContext(), "Failed to create account. Please check your data.", Toast.LENGTH_SHORT).show()
            }
        }

        btnCreateAccount.setOnClickListener {
            viewModel.fullName = etFullName.text.toString()
            viewModel.email = etEmail.text.toString()
            viewModel.stageName = etStageName.text.toString()
            viewModel.password = etPassword.text.toString()
            viewModel.createAccount()
        }
    }
}
