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
            navigateToSignup("TALENT")
        }

        recruiterButton.setOnClickListener {
            navigateToSignup("RECRUITER")
        }
    }

    private fun navigateToSignup(role: String) {
        val intent = Intent(this, SignupActivity::class.java)
        intent.putExtra("USER_ROLE", role)
        startActivity(intent)
        finish()
    }
}
