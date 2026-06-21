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
 * MainActivity as a Dynamic Fragment Router.
 * Senior Architect Implementation: MainActivity as a Dynamic Fragment Router.
 * Uses a "Bulletproof" Firestore Handshake to resolve the user experience.
 * Enhanced: Enforces strict Admin approval access-control.
 */
class MainActivity : AppCompatActivity() {

    private var userRole: String = "talent"
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigation = findViewById(R.id.bottomNavigation)

        if (savedInstanceState != null) {
            userRole = savedInstanceState.getString("SAVED_USER_ROLE", "talent")
        }

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
                        
                        if (savedInstanceState == null && supportFragmentManager.findFragmentById(R.id.nav_host_fragment) == null) {
                            handleNavigation(intent)
                        }
                        Log.d("SceneX_Main", "User Role Confirmed: $userRole")

                        // 2. NOW load the home fragment, because we know their real role
                        loadAppropriateHomeFragment()
                    } else {
                        // No profile found, force login/splash
                        startSplash()
                    }
                }
                .addOnFailureListener {
                    if (savedInstanceState == null && supportFragmentManager.findFragmentById(R.id.nav_host_fragment) == null) {
                        handleNavigation(intent)
                    }
                    // Fallback to talent if there is a sync issue, but strictly restricted
                    Log.e("SceneX_Main", "Firestore sync failed")
                    startSplash()
                }
        } else {
            if (savedInstanceState == null && supportFragmentManager.findFragmentById(R.id.nav_host_fragment) == null) {
                handleNavigation(intent)
            }
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
                    // FIXED: Now both roles redirect to the Filter screen as requested
                    loadFragment(SearchFilterFragment())
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
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("SAVED_USER_ROLE", userRole)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNavigation(intent)
    }

    private fun handleNavigation(intent: Intent?) {
        val openTab = intent?.getStringExtra("OPEN_TAB")
        when (openTab) {
            "INBOX" -> {
                bottomNavigation.selectedItemId = R.id.nav_inbox
            }
            "TIMELINE" -> {
                bottomNavigation.selectedItemId = R.id.nav_calendar
            }
            "SEARCH" -> {
                bottomNavigation.selectedItemId = R.id.nav_search
            }
            else -> {
                loadAppropriateHomeFragment()
            }
        }
    }

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

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }
}
