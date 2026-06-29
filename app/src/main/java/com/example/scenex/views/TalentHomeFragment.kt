package com.example.scenex.views

import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.scenex.R
import com.example.scenex.adapters.CastingCallAdapter
import com.example.scenex.viewmodels.AvailabilityViewModel
import com.example.scenex.viewmodels.TalentHomeViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson

class TalentHomeFragment : Fragment() {

    private val viewModel: TalentHomeViewModel by viewModels()
    private val availabilityViewModel: AvailabilityViewModel by viewModels()
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
        val tvAvailabilityStatus = view.findViewById<TextView>(R.id.tvAvailabilityStatus)
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
            data?.let { profileMap ->
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

                val imageUrl = (profileMap["profileImage"] as? String)?.takeIf { it.isNotEmpty() }
                    ?: (profileMap["profileImageUrl"] as? String)?.takeIf { it.isNotEmpty() }

                Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .circleCrop()
                    .into(ivProfileHeader)

                tvUserName.text = profileMap["fullName"] as? String ?: "Talent"

                val yield = (profileMap["completenessScore"] as? Number)?.toInt() ?: 0
                pbProfileStrength.progress = yield
                tvStrengthPercent.text = "$yield%"
                view.findViewById<TextView>(R.id.tvStrengthPercentLabel)?.text = "$yield% complete"

                val ranking = (profileMap["rankingScore"] as? Number)?.toInt() ?: 0
                tvRankingStatus?.text = "⚡ System Ranking: $ranking Points"

                // Trigger Intelligence Engine Calculation
                availabilityViewModel.resolveCombinedStatus(userId)

                // PROFILE COMPLETENESS CHECKLIST CLICK
                view.findViewById<View>(R.id.clProfileStrength).setOnClickListener {
                    showCompletenessChecklist(profileMap)
                }
            }
        }

        // Availability Intelligence Integration
        availabilityViewModel.calculatedStatus.observe(viewLifecycleOwner) { status ->
            tvAvailabilityStatus.text = status
            when {
                status.contains("Available") -> tvAvailabilityStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.calendar_green))
                status.contains("Busy") -> tvAvailabilityStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.calendar_orange))
                else -> tvAvailabilityStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
            }
        }

        // Observe Casting Calls
        viewModel.castingCalls.observe(viewLifecycleOwner) { calls ->
            Log.d("SceneX_UI", "Received ${calls.size} casting calls in UI")
            castingAdapter.updateData(calls)
        }

        viewModel.fetchProfileData()
        viewModel.fetchCastingCalls()
    }

    private fun showCompletenessChecklist(profile: Map<String, Any>) {
        val dialog = BottomSheetDialog(requireContext(), R.style.SceneX_Dialog_Rounded)
        val dialogView = layoutInflater.inflate(R.layout.bottom_sheet_completeness, null)
        dialog.setContentView(dialogView)

        val container = dialogView.findViewById<LinearLayout>(R.id.checklistContainer)
        val btnGoEdit = dialogView.findViewById<Button>(R.id.btnGoToEdit)

        // Logic to define checklist items
        val items = mutableListOf<Pair<String, Boolean>>()

        // 1. Identity
        val hasIdentity = !(profile["fullName"] as? String).isNullOrBlank() &&
                !(profile["email"] as? String).isNullOrBlank() &&
                !(profile["phoneNumber"] as? String).isNullOrBlank()
        items.add("Basic Identity & Contact Info" to hasIdentity)

        // 2. Physical Specs
        val hasPhysical = !(profile["height"] as? String).isNullOrBlank() &&
                (profile["age"] as? Number)?.toInt() ?: 0 > 0 &&
                !(profile["gender"] as? String).isNullOrBlank()
        items.add("Physical Appearance Specs" to hasPhysical)

        // 3. Professional
        val hasPro = !(profile["spotlightCategory"] as? String).isNullOrBlank() &&
                !(profile["languages"] as? String).isNullOrBlank() &&
                !(profile["experience_level"] as? String).isNullOrBlank()
        items.add("Professional Category & Experience" to hasPro)

        // 4. Assets
        val hasAssets = !(profile["headshotUrl"] as? String).isNullOrBlank() &&
                !(profile["fullBodyUrl"] as? String).isNullOrBlank()
        items.add("High-Quality Portfolio Headshots" to hasAssets)

        // 5. Bio
        val hasBio = !(profile["bio"] as? String).isNullOrBlank()
        items.add("Professional Biography" to hasBio)

        // 6. Portfolio
        val hasWorks = (profile["portfolioWorks"] as? List<*>)?.isNotEmpty() ?: false
        items.add("Work Credits & Projects" to hasWorks)

        // Render Checklist (Show both Complete and Pending items)
        container.removeAllViews()
        items.forEach { (text, isComplete) ->
            val itemView = layoutInflater.inflate(R.layout.item_completeness_check, container, false)
            val tv = itemView.findViewById<TextView>(R.id.tvCheckLabel)
            val iv = itemView.findViewById<ImageView>(R.id.ivCheckIcon)

            tv.text = text
            if (isComplete) {
                iv.setImageResource(R.drawable.ic_check_circle_green)
                iv.setColorFilter(ContextCompat.getColor(requireContext(), R.color.calendar_green))
                tv.setTextColor(Color.parseColor("#888888"))
            } else {
                iv.setImageResource(android.R.drawable.checkbox_off_background)
                iv.setColorFilter(ContextCompat.getColor(requireContext(), R.color.red))
                tv.setTextColor(Color.BLACK)
            }
            container.addView(itemView)
        }

        btnGoEdit.visibility = if (items.all { it.second }) View.GONE else View.VISIBLE

        btnGoEdit.setOnClickListener {
            dialog.dismiss()
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, EditTalentProfileFragment())
                .addToBackStack(null)
                .commit()
        }

        dialog.show()
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
