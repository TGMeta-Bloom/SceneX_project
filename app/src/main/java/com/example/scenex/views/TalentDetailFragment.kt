package com.example.scenex.views

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.scenex.R
import com.example.scenex.models.UserProfile
import com.example.scenex.utils.SessionManager
import com.example.scenex.viewmodels.TalentNavEvent
import com.example.scenex.viewmodels.TalentResultsViewModel
import com.google.gson.Gson
import kotlinx.coroutines.flow.collectLatest

class TalentDetailFragment : Fragment() {

    private val viewModel: TalentResultsViewModel by viewModels()
    private lateinit var talent: UserProfile

    companion object {
        private const val ARG_TALENT_JSON = "arg_talent_json"
        fun newInstance(talent: UserProfile): TalentDetailFragment {
            val fragment = TalentDetailFragment()
            val args = Bundle()
            args.putString(ARG_TALENT_JSON, Gson().toJson(talent))
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val json = it.getString(ARG_TALENT_JSON)
            talent = Gson().fromJson(json, UserProfile::class.java)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_talent_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Bind Cinematic UI
        val ivHeaderImage = view.findViewById<ImageView>(R.id.ivHeaderImage)
        val tvName = view.findViewById<TextView>(R.id.tvDetailName)
        val tvRole = view.findViewById<TextView>(R.id.tvDetailRole)
        val chipLocation = view.findViewById<TextView>(R.id.chipLocation)
        val chipAge = view.findViewById<TextView>(R.id.chipAge)
        val tvBio = view.findViewById<TextView>(R.id.tvDetailBio)
        
        val tvStatHeight = view.findViewById<TextView>(R.id.tvStatHeight)
        val tvStatWeight = view.findViewById<TextView>(R.id.tvStatWeight)
        
        val ivPortfolio1 = view.findViewById<ImageView>(R.id.ivPortfolio1)
        val ivPortfolio2 = view.findViewById<ImageView>(R.id.ivPortfolio2)
        val btnHire = view.findViewById<Button>(R.id.btnHireTalent)
        val btnBack = view.findViewById<ImageView>(R.id.btnBack)

        // 2. Set Data
        tvName.text = talent.fullName
        tvRole.text = talent.spotlightCategory.uppercase()
        chipLocation.text = "📍 ${talent.city}"
        chipAge.text = "🎂 ${talent.age} Years"
        tvBio.text = talent.bio.ifBlank { "Professional SceneX Elite Talent." }
        
        // Split physical specs if possible, or show raw
        tvStatHeight.text = if (talent.physicalSpecs.contains("|")) talent.physicalSpecs.split("|")[0] else talent.physicalSpecs
        tvStatWeight.text = talent.gender.uppercase()

        Glide.with(this).load(talent.effectiveAvatarUrl).centerCrop().into(ivHeaderImage)
        Glide.with(this).load(talent.headshotUrl).centerCrop().into(ivPortfolio1)
        Glide.with(this).load(talent.fullBodyUrl).centerCrop().into(ivPortfolio2)

        // 3. Navigation & Actions
        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        
        btnHire.setOnClickListener {
            val recruiterId = SessionManager.getUserId(requireContext()) ?: "anon"
            viewModel.initiateHire(talent, recruiterId)
        }

        // Observe WhatsApp Bridge
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.navEvent.collectLatest { event ->
                if (event is TalentNavEvent.OpenWhatsApp) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(event.url)))
                }
            }
        }
    }
}
