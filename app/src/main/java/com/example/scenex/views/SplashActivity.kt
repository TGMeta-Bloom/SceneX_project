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
            // 1. Check Onboarding (First-time user path)
            if (!SessionManager.hasSeenOnboarding(this)) {
                startActivity(Intent(this, OnboardingActivity::class.java))
                finish()
                return@postDelayed
            }

            // 2. Check Session (Existing user path)
            val userId = repository.getCurrentUserId()
            if (userId != null) {
                // Handshake fetches both status and verificationStatus for secure approval checking
                repository.getUserRoutingData(userId) { role, status, vStatus, error ->
                    if (error == null) {
                        Log.d(TAG, "Auto-Login: Role=$role | Status=$status | VStatus=$vStatus")
                        SessionManager.establishSession(this, userId, role, status)
                        routeUser(role, status, vStatus)
                    } else {
                        Log.e(TAG, "Database Sync Failed", error)
                        startActivity(Intent(this, RoleSelectActivity::class.java))
                        finish()
                    }
                }
            } else {
                // No session - Go to start of app flow (Role Selection)
                startActivity(Intent(this, RoleSelectActivity::class.java))
                finish()
            }
        }, 2000)
    }

    /**
     * 🛡️ SECURE ROUTING ENGINE:
     * Enforces Admin Approval status before allowing access to app features.
     * Prevents session persistence from bypassing the Waiting Room.
     */
    private fun routeUser(role: String?, status: String?, vStatus: String?) {
        val normalizedRole = role?.uppercase()?.trim()
        val normalizedStatus = status?.lowercase()?.trim() ?: "draft"
        val normalizedVStatus = vStatus?.lowercase()?.trim()

        Log.d(TAG, "Routing Logic: Status=$normalizedStatus | VStatus=$normalizedVStatus")

        // 🛡️ PERMISSIVE APPROVAL LOGIC:
        // Access is granted if either the primary status or the verificationStatus is 'verified' or 'active'.
        val isApproved = normalizedStatus == "verified" || normalizedStatus == "active" || normalizedVStatus == "verified"
        
        val isPending = !isApproved && (normalizedStatus == "pending_review" || normalizedVStatus == "pending")

        val intent = when {
            normalizedRole == "TALENT" || normalizedRole == "RECRUITER" -> {
                when {
                    isApproved -> Intent(this, MainActivity::class.java)
                    isPending -> Intent(this, WaitingRoomActivity::class.java)
                    else -> Intent(this, SignupActivity::class.java).apply { putExtra("USER_ROLE", normalizedRole) }
                }
            }
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
