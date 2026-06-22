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
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigation = findViewById(R.id.bottomNavigation)

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            FirebaseFirestore.getInstance().collection("profiles").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val dbRole = document.getString("userRole") ?: document.getString("role")
                        userRole = dbRole?.lowercase() ?: "talent"
                        
                        if (savedInstanceState == null) {
                            handleNavigation(intent)
                        }
                    }
                }
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadAppropriateHomeFragment()
                    true
                }
                R.id.nav_search -> {
                    // 🎯 RECRUITER FLOW FIX: Recruiters go to their specialized dashboard
                    if (userRole == "recruiter") {
                        loadFragment(RecruiterSearchFragment())
                    } else {
                        loadFragment(SearchFragment())
                    }
                    true
                }
                R.id.nav_calendar -> { loadFragment(CalendarFragment()); true }
                R.id.nav_inbox -> { loadFragment(InboxFragment()); true }
                R.id.nav_profile -> { loadAppropriateProfileFragment(); true }
                else -> false
            }
        }
    }

    private fun handleNavigation(intent: Intent?) {
        loadAppropriateHomeFragment()
    }

    private fun loadAppropriateHomeFragment() {
        if (userRole == "recruiter") loadFragment(RecruiterHomeFragment())
        else loadFragment(TalentHomeFragment())
    }

    private fun loadAppropriateProfileFragment() {
        if (userRole == "recruiter") loadFragment(RecruiterProfileFragment())
        else loadFragment(TalentProfileFragment())
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }
}
