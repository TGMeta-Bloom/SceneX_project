package com.example.scenex.views

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.scenex.R
import com.example.scenex.viewmodels.SignupViewModel

class SignupActivity : AppCompatActivity() {

    private val viewModel: SignupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        //  CAPTURE PRE-FILL DATA (Convenience Feature)
        val role = intent.getStringExtra("USER_ROLE") ?: "TALENT"
        val prefillName = intent.getStringExtra("PREFILL_NAME")
        val prefillEmail = intent.getStringExtra("PREFILL_EMAIL")
        val isSocialAuth = intent.getBooleanExtra("IS_SOCIAL_AUTH", false)

        viewModel.userRole = role
        viewModel.isSocialAuth = isSocialAuth
        prefillName?.let { viewModel.fullName = it }
        prefillEmail?.let { viewModel.email = it }

        if (savedInstanceState == null) {
            val startFragment = if (role == "RECRUITER") {
                RecruiterSignupStep1Fragment()
            } else {
                SignupStep1Fragment()
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.signupFragmentContainer, startFragment)
                .commit()
        }
    }
}
