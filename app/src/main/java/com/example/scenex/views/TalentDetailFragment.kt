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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.scenex.R
import com.example.scenex.models.UserProfile
import com.example.scenex.viewmodels.AvailabilityViewModel
import com.google.android.material.imageview.ShapeableImageView
import com.google.gson.Gson

/**
 * Senior Technical Implementation: Talent Analytical Profile.
 */
class TalentDetailFragment : Fragment() {

    private lateinit var talent: UserProfile
    private val availabilityViewModel: AvailabilityViewModel by viewModels()

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_talent_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. UI Binding
        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        val ivDetailAvatar = view.findViewById<ShapeableImageView>(R.id.ivDetailAvatar)
        val tvDetailName = view.findViewById<TextView>(R.id.tvDetailName)
        val tvDetailRole = view.findViewById<TextView>(R.id.tvDetailRole)
        val tvDetailLocation = view.findViewById<TextView>(R.id.tvDetailLocation)
        val tvDetailRanking = view.findViewById<TextView>(R.id.tvDetailRanking)
        val tvDetailYield = view.findViewById<TextView>(R.id.tvDetailYield)
        val tvDetailBio = view.findViewById<TextView>(R.id.tvDetailBio)
        val tvDetailSpecs = view.findViewById<TextView>(R.id.tvDetailSpecs)
        val tvDetailPro = view.findViewById<TextView>(R.id.tvDetailPro)

        // 🎯 Manual Availability display area
        val tvDetailTitle = view.findViewById<TextView>(R.id.tvDetailTitle)

        val ivDetailHeadshot = view.findViewById<ImageView>(R.id.ivDetailHeadshot)
        val ivDetailFullBody = view.findViewById<ImageView>(R.id.ivDetailFullBody)

        val tvDetailPortfolio = view.findViewById<TextView>(R.id.tvDetailPortfolio)
        val tvDetailShowreel = view.findViewById<TextView>(R.id.tvDetailShowreel)
        val tvDetailSocial = view.findViewById<TextView>(R.id.tvDetailSocial)
        val btnHireTalent = view.findViewById<Button>(R.id.btnHireTalent)

        // 2. Data Binding
        tvDetailName.text = talent.fullName
        tvDetailRole.text = (talent.spotlightCategory.ifEmpty { "Performer" }).uppercase()
        tvDetailLocation.text = "📍 ${talent.city}, ${talent.province}"
        tvDetailRanking.text = "⚡ ${talent.rankingScore.toInt()}"
        tvDetailYield.text = "${talent.completenessScore.toInt()}%"
        tvDetailBio.text = talent.bio.ifEmpty { "Professional artist with verified portfolio assets." }
        tvDetailSpecs.text = talent.physicalSpecs.ifEmpty { "Physical specifications verified." }

        val matrix = "🎓 Qualification: ${talent.qualification.ifEmpty { "Not Disclosed" }}\n" +
                "⏱️ Experience: ${talent.experience.ifEmpty { "1-3 Years" }}\n" +
                "🗣️ Languages: ${talent.languages.ifEmpty { "Sinhala / English" }}"
        tvDetailPro.text = matrix

        // 🎯 3. Availability Intelligence Integration
        availabilityViewModel.calculatedStatus.observe(viewLifecycleOwner) { status ->
            tvDetailTitle.text = status
            when {
                status.contains("Available") -> tvDetailTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.calendar_green))
                status.contains("Busy") -> tvDetailTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.calendar_orange))
                else -> tvDetailTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_dark))
            }
        }
        availabilityViewModel.resolveCombinedStatus(talent.userId)

        // 4. Asset Rendering
        loadProfessionalImage(talent.effectiveAvatarUrl, ivDetailAvatar, isCircle = true)
        loadProfessionalImage(talent.headshotUrl, ivDetailHeadshot, isCircle = false)
        loadProfessionalImage(talent.fullBodyUrl, ivDetailFullBody, isCircle = false)

        // 5. Matrix Link Routing
        setupLink(tvDetailPortfolio, talent.portfolioLink)
        setupLink(tvDetailShowreel, talent.showreelUrl)
        setupLink(tvDetailSocial, talent.socialMediaLinks)

        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        btnHireTalent.setOnClickListener {
            Toast.makeText(requireContext(), "Initiating hire protocol...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadProfessionalImage(url: String, imageView: ImageView, isCircle: Boolean) {
        val request = Glide.with(this)
            .load(url)
            .thumbnail(0.1f)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .transition(DrawableTransitionOptions.withCrossFade())
            .priority(Priority.HIGH)

        if (isCircle) request.circleCrop().into(imageView)
        else request.into(imageView)
    }

    private fun setupLink(textView: TextView, url: String) {
        if (url.isNotBlank() && url.startsWith("http")) {
            textView.visibility = View.VISIBLE
            textView.setOnClickListener {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Link unreachable", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            textView.visibility = View.GONE
        }
    }
}