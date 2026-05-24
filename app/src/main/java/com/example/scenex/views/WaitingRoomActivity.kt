package com.example.scenex.views

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.scenex.MainActivity
import com.example.scenex.R
import com.example.scenex.repository.UserRepository
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
     * Listens for the "status" change from 'pending_review' to 'verified'.
     */
    private fun startRealTimeVerificationSync() {
        val userId = repository.getCurrentUserId() ?: return

        statusListener = repository.listenToProfileStatus(userId) { status ->
            if (status == "verified") {
                Toast.makeText(this, "Profile Verified! Welcome to SceneX.", Toast.LENGTH_LONG).show()
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
            repository.signOut()
            val intent = Intent(this, RoleSelectActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop listening to prevent memory leaks and unnecessary data usage
        statusListener?.remove()
    }
}
