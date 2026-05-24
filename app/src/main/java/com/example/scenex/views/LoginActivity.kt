package com.example.scenex.views

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.scenex.MainActivity
import com.example.scenex.R
import com.example.scenex.repository.UserRepository
import com.example.scenex.utils.SessionManager
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private val repository = UserRepository()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvSignupLink = findViewById<TextView>(R.id.tvSignupLink)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Credentials required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnLogin.isEnabled = false

            // 1. Verifies identity via Firebase Authentication
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val userId = result.user?.uid ?: ""
                    
                    // 2. Load Permissions: Check Role and Profile Status via Firestore
                    repository.getUserRoutingData(userId) { role, status, error ->
                        progressBar.visibility = View.GONE
                        btnLogin.isEnabled = true
                        
                        if (error == null) {
                            // 3. Establish Local Session
                            SessionManager.establishSession(this, userId, role, status)
                            
                            // 4. Smart Routing
                            routeUser(role, status)
                        } else {
                            Toast.makeText(this, "Sync Error: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true
                    Toast.makeText(this, "Auth Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        tvSignupLink.setOnClickListener {
            startActivity(Intent(this, RoleSelectActivity::class.java))
            finish()
        }
    }

    private fun routeUser(role: String?, status: String?) {
        val intent = when {
            role == "TALENT" && status == "pending_review" -> Intent(this, WaitingRoomActivity::class.java)
            role == "TALENT" && status == "verified" -> Intent(this, MainActivity::class.java)
            role == "TALENT" && status == "draft" -> Intent(this, SignupActivity::class.java)
            role == "RECRUITER" -> Intent(this, MainActivity::class.java) // Recruiter Dashboard
            else -> Intent(this, RoleSelectActivity::class.java)
        }
        
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
