package com.example.scenex

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
                        // Grab the role exactly as it is spelled in the database
                        // Checking both 'role' and 'userRole' for bulletproof compatibility
                        val dbRole = document.getString("userRole") ?: document.getString("role")
                        userRole = dbRole?.lowercase() ?: "talent"
                        
                        Log.d("SceneX_Main", "User Role Confirmed: $userRole")
                        
                        // 2. NOW load the home fragment, because we know their real role
                        loadAppropriateHomeFragment()
                    }
                }
                .addOnFailureListener {
                    // Fallback to talent if there is a sync issue
                    Log.e("SceneX_Main", "Firestore sync failed, defaulting to Talent view")
                    loadAppropriateHomeFragment()
                }
        } else {
            // No user session, load default
            loadAppropriateHomeFragment()
        }

        // 3. The Smart Switchboard for Tab Navigation
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadAppropriateHomeFragment()
                    true
                }
                R.id.nav_search -> {
                    // Switchboard ready for separate Talent/Job search modules
                    if (userRole == "recruiter") {
                        loadFragment(SearchFragment()) // Destination: Search Talent
                    } else {
                        loadFragment(SearchFragment()) // Destination: Search Opportunities
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
                    // UPDATED: Now points to the Dynamic Router
                    loadAppropriateProfileFragment()
                    true
                }
                else -> false
            }
        }
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
     * Recruiter -> RecruiterProfileFragment (Compose)
     * Talent -> TalentProfileFragment (XML)
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
