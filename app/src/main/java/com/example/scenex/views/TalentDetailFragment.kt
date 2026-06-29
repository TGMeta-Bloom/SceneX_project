package com.example.scenex.views

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.scenex.R
import com.example.scenex.adapters.PortfolioWorkAdapter
import com.example.scenex.models.PortfolioWork
import com.example.scenex.models.UserProfile
import com.example.scenex.viewmodels.AvailabilityViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
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

        // Manual Availability display area
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

        // 3. Skills & Accents
        val cgDetailSkills = view.findViewById<ChipGroup>(R.id.cgDetailSkills)
        cgDetailSkills.removeAllViews()
        val allSkills = (talent.accents + talent.otherSkills).distinct()
        if (allSkills.isEmpty()) {
            val emptyChip = Chip(requireContext()).apply { text = "No specific skills listed" }
            cgDetailSkills.addView(emptyChip)
        } else {
            allSkills.forEach { skill ->
                val chip = Chip(requireContext(), null, com.google.android.material.R.style.Widget_Material3_Chip_Suggestion).apply {
                    text = skill
                    isClickable = false
                    setChipBackgroundColorResource(android.R.color.transparent)
                    setChipStrokeColorResource(R.color.primary_magenta)
                    chipStrokeWidth = 1f
                }
                cgDetailSkills.addView(chip)
            }
        }

        // 4. Best Works & Credits
        val rvDetailCredits = view.findViewById<RecyclerView>(R.id.rvDetailCredits)
        val tvNoCredits = view.findViewById<TextView>(R.id.tvNoCredits)

        if (talent.portfolioWorks.isEmpty()) {
            tvNoCredits.visibility = View.VISIBLE
            rvDetailCredits.visibility = View.GONE
        } else {
            tvNoCredits.visibility = View.GONE
            rvDetailCredits.visibility = View.VISIBLE
            rvDetailCredits.layoutManager = LinearLayoutManager(requireContext())
            // Read-only adapter (hiding options for recruiter)
            rvDetailCredits.adapter = PortfolioWorkAdapter(
                works = talent.portfolioWorks,
                showOptions = false
            ) { work, action ->
                if (action == "View Details") {
                    showWorkDetailsDialog(work)
                }
            }
        }

        // 5. Availability Intelligence Integration
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

        // 5. Image Expansion Intelligence
        val imageClickListener = View.OnClickListener { v ->
            val url = when(v.id) {
                R.id.ivDetailAvatar -> talent.effectiveAvatarUrl
                R.id.ivDetailHeadshot -> talent.headshotUrl
                R.id.ivDetailFullBody -> talent.fullBodyUrl
                else -> ""
            }
            if (url.isNotBlank()) {
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.nav_host_fragment, FullImageViewerFragment.newInstance(url))
                    .addToBackStack(null)
                    .commit()
            }
        }
        ivDetailAvatar.setOnClickListener(imageClickListener)
        ivDetailHeadshot.setOnClickListener(imageClickListener)
        ivDetailFullBody.setOnClickListener(imageClickListener)

        // 6. Matrix Link Routing
        setupLink(tvDetailPortfolio, talent.portfolioLink)
        setupLink(tvDetailShowreel, talent.showreelUrl)
        setupLink(tvDetailSocial, talent.socialMediaLinks)

        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        // REDIRECTION FIX: Navigate to Hire Request Form
        btnHireTalent.setOnClickListener {
            val hireFragment = HireRequestFragment.newInstance(talent)
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.fade_out)
                .replace(R.id.nav_host_fragment, hireFragment)
                .addToBackStack(null)
                .commit()
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

    private fun showWorkDetailsDialog(work: PortfolioWork) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_portfolio_work, null)
        val dialog = AlertDialog.Builder(requireContext(), R.style.SceneX_Dialog_Rounded)
            .setView(dialogView)
            .create()

        dialogView.findViewById<TextView>(R.id.dialogTitle).text = "Credit Details"
        val ivPreview = dialogView.findViewById<ShapeableImageView>(R.id.ivWorkImagePreview)
        val etTitle = dialogView.findViewById<EditText>(R.id.etWorkTitle)
        val etProjectType = dialogView.findViewById<EditText>(R.id.etProjectType)
        val etRole = dialogView.findViewById<EditText>(R.id.etRolePlayed)
        val etYear = dialogView.findViewById<EditText>(R.id.etYear)
        val etDesc = dialogView.findViewById<EditText>(R.id.etDescription)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveWork)

        // Fill data
        etTitle.setText(work.title)
        etProjectType.setText(work.projectType)
        etRole.setText(work.rolePlayed)
        etYear.setText(work.year)
        etDesc.setText(work.description)

        Glide.with(this).load(work.imageUrl).placeholder(R.drawable.ic_profile_placeholder).into(ivPreview)

        // Make read-only
        etTitle.isEnabled = false
        etProjectType.isEnabled = false
        etRole.isEnabled = false
        etYear.isEnabled = false
        etDesc.isEnabled = false
        dialogView.findViewById<TextView>(R.id.tvAddImageLabel).visibility = View.GONE

        // Expansion in Dialog
        ivPreview.setOnClickListener {
            if (!work.imageUrl.isNullOrBlank()) {
                dialog.dismiss()
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.nav_host_fragment, FullImageViewerFragment.newInstance(work.imageUrl))
                    .addToBackStack(null)
                    .commit()
            }
        }

        btnSave.text = "Close"
        btnSave.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }
}
