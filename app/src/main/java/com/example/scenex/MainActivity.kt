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
 */
class MainActivity : AppCompatActivity() {

    private var userRole: String = "talent"
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigation = findViewById(R.id.bottomNavigation)

        // Restore user role from state
        if (savedInstanceState != null) {
            userRole = savedInstanceState.getString("SAVED_USER_ROLE", "talent")
        }

        // 1. The Bulletproof Fetch
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            FirebaseFirestore.getInstance().collection("profiles").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val dbRole = document.getString("userRole") ?: document.getString("role")
                        userRole = dbRole?.lowercase() ?: "talent"
                        
                        Log.d("SceneX_Main", "User Role Confirmed: $userRole")
                        
                        // Only navigate if this is NOT a recreation AND no fragment is loaded
                        if (savedInstanceState == null && supportFragmentManager.findFragmentById(R.id.nav_host_fragment) == null) {
                            handleNavigation(intent)
                        }
                    }
                }
                .addOnFailureListener {
                    Log.e("SceneX_Main", "Firestore sync failed, defaulting to Talent view")
                    if (savedInstanceState == null && supportFragmentManager.findFragmentById(R.id.nav_host_fragment) == null) {
                        handleNavigation(intent)
                    }
                }
        } else {
            if (savedInstanceState == null && supportFragmentManager.findFragmentById(R.id.nav_host_fragment) == null) {
                handleNavigation(intent)
            }
        }

        // 3. Bottom Navigation Setup
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadAppropriateHomeFragment()
                    true
                }
                R.id.nav_search -> {
                    loadFragment(SearchFragment())
                    true
                }
                R.id.nav_calendar -> {
                    if (userRole == "recruiter") {
                        loadFragment(RecruiterBookingFragment())
                    } else {
                        loadFragment(TalentSchedulingFragment())
                    }
                    true
                }
                R.id.nav_inbox -> {
                    loadFragment(InboxFragment())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

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

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }
}
