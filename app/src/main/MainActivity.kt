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

class MainActivity : AppCompatActivity() {

    private var userRole: String = "talent"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            FirebaseFirestore.getInstance().collection("profiles").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        
                        val vStatus = document.getString("verificationStatus")?.lowercase() ?: "pending"
                        val status = document.getString("status")?.lowercase() ?: "pending_review"
                        
                        // 🛡️ UNIFIED SECURITY GATE: Allows both 'verified' and 'active'
                        val isApproved = (status == "verified" || status == "active") && 
                                         (vStatus == "verified" || vStatus == "active")

                        if (!isApproved) {
                            Log.w("SceneX_Security", "Access Blocked: Redirecting to Waiting Room.")
                            redirectToWaitingRoom()
                            return@addOnSuccessListener
                        }

                        val dbRole = document.getString("userRole") ?: document.getString("role")
                        userRole = dbRole?.lowercase() ?: "talent"
                        
                        loadAppropriateHomeFragment()
                    } else {
                        startSplash()
                    }
                }
                .addOnFailureListener {
                    startSplash()
                }
        } else {
            startSplash()
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { loadAppropriateHomeFragment(); true }
                R.id.nav_search -> { loadFragment(SearchFragment()); true }
                R.id.nav_calendar -> { loadFragment(CalendarFragment()); true }
                R.id.nav_inbox -> { loadFragment(InboxFragment()); true }
                R.id.nav_profile -> { loadAppropriateProfileFragment(); true }
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

    private fun loadAppropriateHomeFragment() {
        if (userRole == "recruiter") loadFragment(RecruiterHomeFragment()) else loadFragment(TalentHomeFragment())
    }

    private fun loadAppropriateProfileFragment() {
        if (userRole == "recruiter") loadFragment(RecruiterProfileFragment()) else loadFragment(TalentProfileFragment())
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.nav_host_fragment, fragment).commit()
    }
}
