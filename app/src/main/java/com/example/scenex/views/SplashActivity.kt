package com.example.scenex.views

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.scenex.MainActivity
import com.example.scenex.R
import com.example.scenex.repository.UserRepository

class SplashActivity : AppCompatActivity() {
    
    private val repository = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            val userId = repository.getCurrentUserId()
            if (userId != null) {
                // User is logged in, perform Smart Routing
                repository.getUserRoutingData(userId) { role, status, _ ->
                    routeUser(role, status)
                }
            } else {
                // No session, go to Onboarding
                startActivity(Intent(this, OnboardingActivity::class.java))
                finish()
            }
        }, 2000)
    }

    private fun routeUser(role: String?, status: String?) {
        val intent = when {
            role == "TALENT" && status == "pending_review" -> Intent(this, WaitingRoomActivity::class.java)
            role == "TALENT" && status == "verified" -> Intent(this, MainActivity::class.java)
            else -> Intent(this, RoleSelectActivity::class.java) // Resync or default
        }
        
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
