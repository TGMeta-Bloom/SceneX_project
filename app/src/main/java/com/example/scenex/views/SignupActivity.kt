package com.example.scenex.views

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.scenex.R
import com.example.scenex.fragments.SignupStep1Fragment

class SignupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.signupFragmentContainer, SignupStep1Fragment())
                .commit()
        }
    }
}
