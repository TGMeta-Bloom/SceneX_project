package com.example.scenex.views

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.scenex.R

class SchedulingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scheduling)

        val role = intent.getStringExtra("SELECTED_ROLE") ?: "TALENT"

        if (savedInstanceState == null) {
            val fragment = if (role == "TALENT") {
                TalentSchedulingFragment()
            } else {
                RecruiterBookingFragment()
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
        }
    }
}
