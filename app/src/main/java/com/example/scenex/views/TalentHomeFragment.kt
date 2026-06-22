package com.example.scenex.views

import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.scenex.R
import com.example.scenex.adapters.CastingCallAdapter
import com.example.scenex.viewmodels.TalentHomeViewModel
import com.google.android.material.imageview.ShapeableImageView
import com.google.gson.Gson

class TalentHomeFragment : Fragment() {

    private val viewModel: TalentHomeViewModel by viewModels()
    private lateinit var castingAdapter: CastingCallAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_talent_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ivNotification = view.findViewById<ImageView>(R.id.ivNotification)
        val ivProfileHeader = view.findViewById<ShapeableImageView>(R.id.ivProfileHeader)
        val tvAppName = view.findViewById<TextView>(R.id.tvAppName)
        val tvUserName = view.findViewById<TextView>(R.id.tvUserName)
        val pbProfileStrength = view.findViewById<ProgressBar>(R.id.pbProfileStrength)
        val tvStrengthPercent = view.findViewById<TextView>(R.id.tvStrengthPercent)
        val tvRankingStatus = view.findViewById<TextView>(R.id.tvRankingStatus)
        val rvCastingFeed = view.findViewById<RecyclerView>(R.id.rvCastingFeed)

        applyTextGradient(tvAppName)

        // Notification Click Listener
        ivNotification.setOnClickListener {
            val intent = Intent(requireContext(), NotificationsActivity::class.java)
            startActivity(intent)
        }

        // Setup RecyclerView with Click Listener
        castingAdapter = CastingCallAdapter(emptyList()) { castingCall ->
            val intent = Intent(requireContext(), CastingCallDetailsActivity::class.java)
            val json = Gson().toJson(castingCall)
            intent.putExtra("CASTING_CALL_JSON", json)
            startActivity(intent)
        }
        rvCastingFeed.layoutManager = LinearLayoutManager(requireContext())
        rvCastingFeed.adapter = castingAdapter

        // REAL-TIME IDENTITY OBSERVATION
        viewModel.profileData.observe(viewLifecycleOwner) { data ->
            data?.let {
                val imageUrl = (it["profileImage"] as? String)?.takeIf { it.isNotEmpty() }
                    ?: (it["profileImageUrl"] as? String)?.takeIf { it.isNotEmpty() }

                Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .circleCrop()
                    .into(ivProfileHeader)

                tvUserName.text = it["fullName"] as? String ?: "Talent"

                val yield = (it["completenessScore"] as? Number)?.toInt() ?: 0
                pbProfileStrength.progress = yield
                tvStrengthPercent.text = "$yield%"

                val ranking = (it["rankingScore"] as? Number)?.toInt() ?: 0
                tvRankingStatus?.text = "⚡ System Ranking: $ranking Points"
            }
        }

        // Observe Casting Calls
        viewModel.castingCalls.observe(viewLifecycleOwner) { calls ->
            android.util.Log.d("SceneX_UI", "Received ${calls.size} casting calls in UI")
            castingAdapter.updateData(calls)
        }


        viewModel.fetchProfileData()
        viewModel.fetchCastingCalls()
    }

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
