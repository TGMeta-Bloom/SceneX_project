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
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private val repository = UserRepository()
    private val TAG = "SceneX_Splash"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // ==========================================
        //  DEBUG TESTING BLOCK: UNCOMMENT TO CLEAR GHOST SESSIONS
        // If you are stuck in the Waiting Room on a fresh install during testing,
        // uncomment the following two lines, run the app once, then comment them out again.
        //
        // FirebaseAuth.getInstance().signOut()
        // SessionManager.clearSession(this)
        // ==========================================

        Handler(Looper.getMainLooper()).postDelayed({
            // 1. Fresh Install check
            if (!SessionManager.hasSeenOnboarding(this)) {
                startActivity(Intent(this, OnboardingActivity::class.java))
                finish()
                return@postDelayed
            }

            // 2. Auth Session Handshake
            val userId = repository.getCurrentUserId()
            if (userId != null) {
                repository.getUserRoutingData(userId) { role, status, vStatus, error ->
                    if (error == null) {
                        Log.d(TAG, "Auto-Login: Role=$role | Status=$status | VStatus=$vStatus")
                        SessionManager.establishSession(this, userId, role, status)
                        routeUser(role, status, vStatus)
                    } else {
                        Log.e(TAG, "Sync Error - Falling back to Role Selection")
                        startActivity(Intent(this, RoleSelectActivity::class.java))
                        finish()
                    }
                }
            } else {
                startActivity(Intent(this, RoleSelectActivity::class.java))
                finish()
            }
        }, 2000)
    }

    /**
     * UNIFIED ROUTING ENGINE:
     * Correctly handles "Draft" vs "Pending" vs "Approved" states.
     * Prevents incorrect WaitingRoom redirects for new or incomplete users.
     */
    private fun routeUser(role: String?, status: String?, vStatus: String?) {
        val normalizedRole = role?.uppercase()?.trim()
        val normalizedStatus = status?.lowercase()?.trim() ?: "draft"
        val normalizedVStatus = vStatus?.lowercase()?.trim() ?: "unverified"

        // STRICT APPROVAL GATE: Access is granted ONLY if BOTH profile and identity are verified/active.
        val isApproved = (normalizedStatus == "verified" || normalizedStatus == "active" ||
                normalizedStatus == "available" || normalizedStatus == "unavailable") &&
                (normalizedVStatus == "verified" || normalizedVStatus == "active")

        // EXPLICIT PENDING CHECK: Only show Waiting Room if status is explicitly awaiting review.
        val isPending = normalizedStatus == "pending_review" || normalizedVStatus == "pending"

        val intent = when {
            normalizedRole == "TALENT" || normalizedRole == "RECRUITER" -> {
                when {
                    isApproved -> Intent(this, MainActivity::class.java)
                    isPending -> Intent(this, WaitingRoomActivity::class.java)
                    // Else: New user or Draft profile -> Go to Signup Flow
                    else -> Intent(this, SignupActivity::class.java).apply { putExtra("USER_ROLE", normalizedRole) }
                }
            }
            else -> {
                Log.w(TAG, "Role unknown. Resetting to Role Selection.")
                Intent(this, RoleSelectActivity::class.java)
            }
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}