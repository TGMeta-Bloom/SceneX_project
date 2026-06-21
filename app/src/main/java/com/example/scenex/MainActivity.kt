package com.example.scenex

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.scenex.views.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Senior Architect Implementation: MainActivity as a Dynamic Fragment Router.
 * Uses a "Bulletproof" Firestore Handshake to resolve the user experience.
 * Enhanced: Enforces strict Admin approval access-control.
 */
class MainActivity : AppCompatActivity() {

    // Default fallback to talent, updated dynamically from the database
    private var userRole: String = "talent"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // 1. The Bulletproof Fetch: Ask Firestore directly who this is!
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            FirebaseFirestore.getInstance().collection("profiles").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        
                        // 🛡️ SECURITY GATE: Verify Approval status before allowing feature access
                        val verificationStatus = document.getString("verificationStatus") ?: "pending"
                        val status = document.getString("status") ?: "pending_review"
                        
                        if (verificationStatus == "pending" || status == "pending_review") {
                            Log.w("SceneX_Security", "Access Blocked: Pending Approval. Redirecting to Waiting Room.")
                            redirectToWaitingRoom()
                            return@addOnSuccessListener
                        }

                        // Check for verified state
                        if (verificationStatus != "verified" || status != "verified") {
                            Log.w("SceneX_Security", "Access Blocked: Unverified Account.")
                            redirectToWaitingRoom()
                            return@addOnSuccessListener
                        }

                        // Grab the role exactly as it is spelled in the database
                        val dbRole = document.getString("userRole") ?: document.getString("role")
                        userRole = dbRole?.lowercase() ?: "talent"
                        
                        Log.d("SceneX_Main", "User Role Confirmed: $userRole")
                        
                        // 2. NOW load the home fragment, because we know their real role
                        loadAppropriateHomeFragment()
                    } else {
                        // No profile found, force login/splash
                        startSplash()
                    }
                }
                .addOnFailureListener {
                    // Fallback to talent if there is a sync issue, but strictly restricted
                    Log.e("SceneX_Main", "Firestore sync failed")
                    startSplash()
                }
        } else {
            // No user session, kick out
            startSplash()
        }

        // 3. The Smart Switchboard for Tab Navigation
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadAppropriateHomeFragment()
                    true
                }
                R.id.nav_search -> {
                    if (userRole == "recruiter") {
                        loadFragment(SearchFragment()) 
                    } else {
                        loadFragment(SearchFragment()) 
                    }
                    true
                }
                R.id.nav_calendar -> {
                    loadFragment(CalendarFragment())
                    true
                }
                R.id.nav_inbox -> {
                    loadFragment(InboxFragment())
                    true
                }
                R.id.nav_profile -> {
                    loadAppropriateProfileFragment()
                    true
                }
                else -> false
            }
        }
    }

    private fun redirectToWaitingRoom() {
        val intent = Intent(this, WaitingRoomActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun startSplash() {
        val intent = Intent(this, SplashActivity::class.java)
        startActivity(intent)
        finish()
    }

    /**
     * Resolves the Home dashboard context based on the confirmed role.
     */
    private fun loadAppropriateHomeFragment() {
        if (userRole == "recruiter") {
            loadFragment(RecruiterHomeFragment()) 
        } else {
            loadFragment(TalentHomeFragment())    
        }
    }

    /**
     * Resolves the Profile interface context based on the confirmed role.
     */
    private fun loadAppropriateProfileFragment() {
        if (userRole == "recruiter") {
            loadFragment(RecruiterProfileFragment())
        } else {
            loadFragment(TalentProfileFragment())
        }
    }

    /**
     * Standard Swapper to replace the navigation host container.
     */
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }
}
