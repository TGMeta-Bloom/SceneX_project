package com.example.scenex.views

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
    private val TAG = "SceneX_Auth"

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

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val userId = result.user?.uid ?: ""
                    
                    repository.getUserRoutingData(userId) { role, status, error ->
                        progressBar.visibility = View.GONE
                        btnLogin.isEnabled = true
                        
                        if (error == null) {
                            Log.d(TAG, "Login Handshake: Role=$role | Status=$status")
                            SessionManager.establishSession(this, userId, role, status)
                            routeUser(role, status)
                        } else {
                            Log.e(TAG, "Database Sync Failed", error)
                            Toast.makeText(this, "Sync Error: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true
                    Log.e(TAG, "Auth Failed", e)
                    Toast.makeText(this, "Auth Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        tvSignupLink.setOnClickListener {
            startActivity(Intent(this, RoleSelectActivity::class.java))
        }
    }

    private fun routeUser(role: String?, status: String?) {
        val normalizedRole = role?.uppercase()?.trim()
        val normalizedStatus = status?.lowercase()?.trim()

        val intent = when {
            // TALENT FLOW
            normalizedRole == "TALENT" -> {
                when (normalizedStatus) {
                    "verified", "active" -> Intent(this, MainActivity::class.java)
                    "pending_review" -> Intent(this, WaitingRoomActivity::class.java)
                    else -> Intent(this, SignupActivity::class.java).apply { putExtra("USER_ROLE", "TALENT") }
                }
            }
            
            // RECRUITER FLOW
            normalizedRole == "RECRUITER" -> {
                if (normalizedStatus == "pending_review") {
                    Intent(this, WaitingRoomActivity::class.java)
                } else {
                    Intent(this, MainActivity::class.java)
                }
            }
            
            // NO ROLE FOUND
            else -> {
                Log.w(TAG, "Identity not recognized. Falling back to Role Selection.")
                Intent(this, RoleSelectActivity::class.java)
            }
        }
        
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
