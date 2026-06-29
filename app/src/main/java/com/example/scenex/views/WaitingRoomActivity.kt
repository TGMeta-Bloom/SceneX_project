package com.example.scenex.views

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.scenex.MainActivity
import com.example.scenex.R
import com.example.scenex.repository.UserRepository
import com.example.scenex.utils.SessionManager
import com.google.firebase.firestore.ListenerRegistration

class WaitingRoomActivity : AppCompatActivity() {

    private val repository = UserRepository()
    private var statusListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waiting_room)

        setupSignOut()
        startRealTimeVerificationSync()
    }

    /**
     * Senior Engineer Implementation: Real-Time Handshake with Web Admin Dashboard.
     * Listens for both "status" and "verificationStatus" for secure approval.
     */
    private fun startRealTimeVerificationSync() {
        val userId = repository.getCurrentUserId() ?: return

        statusListener = repository.listenToProfileStatus(userId) { status, vStatus ->
            val normalizedStatus = status?.lowercase()?.trim() ?: ""
            val normalizedVStatus = vStatus?.lowercase()?.trim() ?: ""

            android.util.Log.d("WaitingRoom", "Verification Update: Status=$normalizedStatus | VStatus=$normalizedVStatus")

            // UNIFIED SECURITY GATE: Synchronized with SplashActivity strictness
            // Access is granted ONLY if BOTH profile status and verification are in approved states.
            val isApprovedStrict = (normalizedStatus == "verified" || normalizedStatus == "active" ||
                    normalizedStatus == "available" || normalizedStatus == "unavailable") &&
                    (normalizedVStatus == "verified" || normalizedVStatus == "active")

            if (isApprovedStrict) {
                Toast.makeText(this, "Verification Successful! Welcome back to SceneX.", Toast.LENGTH_LONG).show()
                navigateToMain()
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setupSignOut() {
        findViewById<Button>(R.id.btnSignOut).setOnClickListener {
            // SECURITY PURGE: Clear local cache AND Firebase session to prevent state loops
            SessionManager.clearSession(this)
            repository.signOut()

            val intent = Intent(this, RoleSelectActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Essential: Stop listening to prevent memory leaks and redundant Firestore usage
        statusListener?.remove()
    }
}