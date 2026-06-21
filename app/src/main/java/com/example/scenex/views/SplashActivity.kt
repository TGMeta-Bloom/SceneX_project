package com.example.scenex.views

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.scenex.MainActivity
import com.example.scenex.R
import com.example.scenex.repository.UserRepository
import com.example.scenex.utils.SessionManager

class SplashActivity : AppCompatActivity() {
    
    private val repository = UserRepository()
    private val TAG = "SceneX_Splash"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            if (!SessionManager.hasSeenOnboarding(this)) {
                startActivity(Intent(this, OnboardingActivity::class.java))
                finish()
                return@postDelayed
            }

            val userId = repository.getCurrentUserId()
            if (userId != null) {
                repository.getUserRoutingData(userId) { role, status, _ ->
                    Log.d(TAG, "Auto-Login: Role=$role, Status=$status")
                    SessionManager.establishSession(this, userId, role, status)
                    routeUser(role, status)
                }
            } else {
                startActivity(Intent(this, RoleSelectActivity::class.java))
                finish()
            }
        }, 2000)
    }

    private fun routeUser(role: String?, status: String?) {
        val normalizedRole = role?.uppercase()?.trim()
        val normalizedStatus = status?.lowercase()?.trim()

        val intent = when {
            // TALENT ROUTING
            normalizedRole == "TALENT" -> {
                when (normalizedStatus) {
                    "verified", "active" -> Intent(this, MainActivity::class.java)
                    "pending_review" -> Intent(this, WaitingRoomActivity::class.java)
                    else -> Intent(this, SignupActivity::class.java).apply { putExtra("USER_ROLE", "TALENT") }
                }
            }
            
            // RECRUITER ROUTING (More permissive as status might be null)
            normalizedRole == "RECRUITER" -> {
                if (normalizedStatus == "pending_review") {
                    Intent(this, WaitingRoomActivity::class.java)
                } else {
                    Intent(this, MainActivity::class.java)
                }
            }
            
            else -> {
                Log.w(TAG, "Unrecognized identity. Redirecting to Role Selection.")
                Intent(this, RoleSelectActivity::class.java)
            }
        }
        
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
