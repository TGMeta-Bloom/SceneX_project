package com.example.scenex.views

import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.example.scenex.R
import com.example.scenex.viewmodels.TalentHomeViewModel
import com.google.android.material.imageview.ShapeableImageView

class TalentHomeFragment : Fragment() {

    private val viewModel: TalentHomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_talent_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ivProfileHeader = view.findViewById<ShapeableImageView>(R.id.ivProfileHeader)
        val tvAppName = view.findViewById<TextView>(R.id.tvAppName)
        val tvUserName = view.findViewById<TextView>(R.id.tvUserName)
        val pbProfileStrength = view.findViewById<ProgressBar>(R.id.pbProfileStrength)
        val tvStrengthPercent = view.findViewById<TextView>(R.id.tvStrengthPercent)

        // Apply Primary Gradient to branding elements matching button_rounded_magenta
        applyTextGradient(tvAppName)

        viewModel.profileData.observe(viewLifecycleOwner) { data ->
            data?.let {
                val imageUrl = it["profileImage"] as? String ?: it["profileImageUrl"] as? String
                Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .circleCrop()
                    .into(ivProfileHeader)

                val name = it["fullName"] as? String ?: it["name"] as? String ?: "Talent"
                tvUserName.text = name

                val score = (it["completenessScore"] as? Long)?.toInt() ?: 0
                pbProfileStrength.progress = score
                tvStrengthPercent.text = "$score%"
            }
        }

        viewModel.fetchProfileData()
    }

    /**
     * Programmatically applies the branding gradient (#B0006D to #4A0038) to match button styles.
     */
    private fun applyTextGradient(textView: TextView) {
        textView.post {
            val width = textView.paint.measureText(textView.text.toString())
            if (width > 0) {
                val textShader: Shader = LinearGradient(
                    0f, 0f, width, 0f,
                    intArrayOf(Color.parseColor("#B0006D"), Color.parseColor("#4A0038")),
                    null, Shader.TileMode.CLAMP
                )
                textView.paint.shader = textShader
                textView.invalidate()
            }
        }
    }
}
