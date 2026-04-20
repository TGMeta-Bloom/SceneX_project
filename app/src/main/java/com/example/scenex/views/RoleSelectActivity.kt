package com.example.scenex.views

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.scenex.R

class RoleSelectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_role_select)

        val talentButton = findViewById<Button>(R.id.talentButton)
        val recruiterButton = findViewById<Button>(R.id.recruiterButton)

        talentButton.setOnClickListener {
            Log.d("RoleSelect", "Talent button clicked")
            navigateToSignup()
        }

        recruiterButton.setOnClickListener {
            Log.d("RoleSelect", "Recruiter button clicked")
            navigateToSignup()
        }
    }

    private fun navigateToSignup() {
        Log.d("RoleSelect", "Navigating to SignupActivity")
        val intent = Intent(this, SignupActivity::class.java)
        startActivity(intent)
        finish()
    }
}
