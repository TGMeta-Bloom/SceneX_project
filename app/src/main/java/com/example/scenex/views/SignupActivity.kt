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

        // Retrieve role from Intent and sync with ViewModel
        val role = intent.getStringExtra("USER_ROLE") ?: "TALENT"
        viewModel.userRole = role

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
