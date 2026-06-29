package com.example.scenex.views

import android.content.Intent
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.scenex.R

class RoleSelectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_role_select)

        val talentButton = findViewById<Button>(R.id.talentButton)
        val recruiterButton = findViewById<Button>(R.id.recruiterButton)
        val talentLabel = findViewById<TextView>(R.id.talentLabel)
        val recruiterLabel = findViewById<TextView>(R.id.recruiterLabel)

        applyTextGradient(talentButton)
        applyTextGradient(recruiterButton)
        applyTextGradient(talentLabel)
        applyTextGradient(recruiterLabel)

        talentButton.setOnClickListener { navigateToSignup("TALENT") }
        recruiterButton.setOnClickListener { navigateToSignup("RECRUITER") }
    }

    private fun applyTextGradient(textView: TextView) {
        textView.post {
            val paint = textView.paint
            val width = paint.measureText(textView.text.toString())
            val textShader: Shader = LinearGradient(
                0f, 0f, width, 0f,
                intArrayOf(
                    ContextCompat.getColor(this, R.color.primary_magenta),
                    ContextCompat.getColor(this, R.color.primary_dark)
                ),
                null, Shader.TileMode.CLAMP
            )
            textView.paint.shader = textShader
            textView.invalidate()
        }
    }

    private fun navigateToSignup(role: String) {
        val intent = Intent(this, SignupActivity::class.java)
        intent.putExtra("USER_ROLE", role)

        // Pass through pre-fill and social flag convenience data
        intent.putExtra("PREFILL_NAME", getIntent().getStringExtra("PREFILL_NAME"))
        intent.putExtra("PREFILL_EMAIL", getIntent().getStringExtra("PREFILL_EMAIL"))
        intent.putExtra("IS_SOCIAL_AUTH", getIntent().getBooleanExtra("IS_SOCIAL_AUTH", false))

        startActivity(intent)
    }
}
