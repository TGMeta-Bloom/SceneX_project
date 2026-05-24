package com.example.scenex.views

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.scenex.MainActivity
import com.example.scenex.R
import com.example.scenex.repository.UserRepository
import com.example.scenex.utils.SessionManager

class SplashActivity : AppCompatActivity() {
    
    private val repository = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            // Senior Fix: Prioritize Onboarding Handshake for new installs/updates
            if (!SessionManager.hasSeenOnboarding(this)) {
                startActivity(Intent(this, OnboardingActivity::class.java))
                finish()
                return@postDelayed
            }

            val userId = repository.getCurrentUserId()
            if (userId != null) {
                // User is logged in, perform Smart Routing with Session Sync
                repository.getUserRoutingData(userId) { role, status, _ ->
                    SessionManager.establishSession(this, userId, role, status)
                    routeUser(role, status)
                }
            } else {
                // Already onboarded but not logged in -> Skip slides and go to entry screen
                startActivity(Intent(this, RoleSelectActivity::class.java))
                finish()
            }
        }, 2000)
    }

    private fun routeUser(role: String?, status: String?) {
        val intent = when {
            // TALENT ROUTING
            role == "TALENT" && status == "pending_review" -> Intent(this, WaitingRoomActivity::class.java)
            role == "TALENT" && (status == "verified" || status == "active") -> Intent(this, MainActivity::class.java)
            role == "TALENT" && status == "draft" -> Intent(this, SignupActivity::class.java).apply { putExtra("USER_ROLE", "TALENT") }
            
            // RECRUITER ROUTING
            role == "RECRUITER" && (status == "verified" || status == "active") -> Intent(this, MainActivity::class.java)
            role == "RECRUITER" && (status == "pending_review" || status == "draft") -> Intent(this, WaitingRoomActivity::class.java)
            
            else -> Intent(this, RoleSelectActivity::class.java)
        }
        
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
