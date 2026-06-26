package com.example.scenex.views

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.scenex.R
import com.example.scenex.viewmodels.HireRequestEvent
import com.example.scenex.viewmodels.HireRequestViewModel
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.collectLatest
import java.util.*

class HireRequestDetailsActivity : AppCompatActivity() {

    private val viewModel: HireRequestViewModel by viewModels()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var requestId: String? = null

    private lateinit var tvRecruiterName: TextView
    private lateinit var tvRecruiterCompany: TextView
    private lateinit var tvProjectTitle: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvPayment: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvStartDate: TextView
    private lateinit var tvEndDate: TextView
    private lateinit var tvMessage: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvHeaderTitle: TextView
    private lateinit var tvProposedDateTime: TextView
    private lateinit var ivRecruiterProfile: ShapeableImageView
    private lateinit var cvProposedSlot: View
    private lateinit var layoutActions: View
    private lateinit var layoutRecruiterActions: View
    private lateinit var btnAccept: View
    private lateinit var btnReject: View
    private lateinit var btnReschedule: View
    private lateinit var btnConfirmBooking: View
    private lateinit var btnCancelRequest: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hire_request_details)

        requestId = intent.getStringExtra("HIRE_REQUEST_ID")
        if (requestId == null) {
            Toast.makeText(this, "Request context missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        applyTextGradients()
        observeViewModel()
        viewModel.fetchHireRequest(requestId!!)
    }

    private fun initViews() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }

        tvRecruiterName = findViewById(R.id.tvRecruiterName)
        tvRecruiterCompany = findViewById(R.id.tvRecruiterCompany)
        tvProjectTitle = findViewById(R.id.tvProjectTitle)
        tvDescription = findViewById(R.id.tvDescription)
        tvPayment = findViewById(R.id.tvPayment)
        tvLocation = findViewById(R.id.tvLocation)
        tvStartDate = findViewById(R.id.tvStartDate)
        tvEndDate = findViewById(R.id.tvEndDate)
        tvMessage = findViewById(R.id.tvMessage)
        tvStatus = findViewById(R.id.tvStatus)
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)
        tvProposedDateTime = findViewById(R.id.tvProposedDateTime)
        ivRecruiterProfile = findViewById(R.id.ivRecruiterProfile)
        cvProposedSlot = findViewById(R.id.cvProposedSlot)
        layoutActions = findViewById(R.id.layoutActions)
        layoutRecruiterActions = findViewById(R.id.layoutRecruiterActions)
        
        btnAccept = findViewById(R.id.btnAccept)
        btnReject = findViewById(R.id.btnReject)
        btnReschedule = findViewById(R.id.btnReschedule)
        btnConfirmBooking = findViewById(R.id.btnConfirmBooking)
        btnCancelRequest = findViewById(R.id.btnCancelRequest)

        btnAccept.setOnClickListener { viewModel.updateRequestStatus(requestId!!, "ACCEPTED") }
        btnReject.setOnClickListener { viewModel.updateRequestStatus(requestId!!, "REJECTED") }
        btnReschedule.setOnClickListener { showRescheduleDialog() }
        btnConfirmBooking.setOnClickListener { viewModel.confirmAndBook(requestId!!) }
        btnCancelRequest.setOnClickListener { viewModel.updateRequestStatus(requestId!!, "REJECTED") }
    }

    private fun showRescheduleDialog() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            val date = String.format("%d-%02d-%02d", year, month + 1, day)
            TimePickerDialog(this, { _, hour, minute ->
                val time = String.format("%02d:%02d", hour, minute)
                viewModel.proposeNewTime(requestId!!, date, time)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun observeViewModel() {
        viewModel.hireRequest.observe(this) { request ->
            request?.let {
                tvProjectTitle.text = it.projectTitle
                tvDescription.text = it.description
                tvPayment.text = "LKR ${it.payment}"
                tvLocation.text = it.location
                tvStartDate.text = it.startDate
                tvEndDate.text = if (it.endDate.isNullOrEmpty()) "Flexible" else it.endDate
                tvMessage.text = it.message

                val currentUserId = auth.currentUser?.uid
                val isRecruiter = currentUserId == it.recruiterId
                val isTalent = currentUserId == it.talentId

                // 1. Status UI Update
                if (it.status != "PENDING") {
                    tvStatus.visibility = View.VISIBLE
                    tvStatus.text = "STATUS: ${it.status}"
                    when(it.status) {
                        "BOOKED", "ACCEPTED" -> {
                            tvStatus.setBackgroundResource(R.drawable.bg_status_accepted)
                            tvStatus.setTextColor(Color.parseColor("#2E7D32"))
                        }
                        "REJECTED" -> {
                            tvStatus.setBackgroundResource(R.drawable.bg_status_rejected)
                            tvStatus.setTextColor(Color.parseColor("#C62828"))
                        }
                        "RESCHEDULED" -> {
                            tvStatus.setBackgroundResource(R.drawable.bg_tag_outline)
                            tvStatus.setTextColor(Color.BLACK)
                        }
                    }
                } else {
                    tvStatus.visibility = View.GONE
                }

                // 2. Proposed Slot UI
                if (!it.proposedDate.isNullOrEmpty()) {
                    cvProposedSlot.visibility = View.VISIBLE
                    tvProposedDateTime.text = "${it.proposedDate} at ${it.proposedTime}"
                } else {
                    cvProposedSlot.visibility = View.GONE
                }

                // 3. Conditional Action Bars
                when (it.status) {
                    "RESCHEDULED" -> {
                        if (isRecruiter) {
                            layoutRecruiterActions.visibility = View.VISIBLE
                            layoutActions.visibility = View.GONE
                        } else {
                            layoutRecruiterActions.visibility = View.GONE
                            layoutActions.visibility = View.GONE 
                        }
                    }
                    "PENDING" -> {
                        if (isTalent) {
                            layoutActions.visibility = View.VISIBLE
                            layoutRecruiterActions.visibility = View.GONE
                        } else {
                            layoutActions.visibility = View.GONE
                            layoutRecruiterActions.visibility = View.GONE
                        }
                    }
                    else -> {
                        layoutActions.visibility = View.GONE
                        layoutRecruiterActions.visibility = View.GONE
                    }
                }

                // 4. Role-based labels
                if (isTalent) {
                    tvHeaderTitle.text = "Hire Invitation"
                    tvRecruiterCompany.text = "Production House"
                    fetchPartnerDetails(it.recruiterId)
                } else if (isRecruiter) {
                    tvHeaderTitle.text = "Hire Request Status"
                    tvRecruiterCompany.text = "Prospective Talent"
                    fetchPartnerDetails(it.talentId)
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.events.collectLatest { event ->
                when (event) {
                    is HireRequestEvent.StatusUpdated -> {
                        Toast.makeText(this@HireRequestDetailsActivity, "Request ${event.status}", Toast.LENGTH_SHORT).show()
                    }
                    is HireRequestEvent.Error -> Toast.makeText(this@HireRequestDetailsActivity, event.message, Toast.LENGTH_SHORT).show()
                    else -> {}
                }
            }
        }
    }

    private fun fetchPartnerDetails(userId: String) {
        db.collection("profiles").document(userId).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                tvRecruiterName.text = snapshot.getString("fullName") ?: "SceneX User"
                val imageUrl = snapshot.getString("profileImageUrl") ?: snapshot.getString("profileImage") ?: ""
                if (imageUrl.isNotEmpty()) {
                    Glide.with(this).load(imageUrl).placeholder(R.drawable.ic_profile_placeholder).circleCrop().into(ivRecruiterProfile)
                }
            }
        }
    }

    private fun applyTextGradients() {
        tvHeaderTitle.post {
            val width = tvHeaderTitle.paint.measureText(tvHeaderTitle.text.toString())
            if (width > 0) {
                val startColor = ContextCompat.getColor(this, R.color.gradient_start)
                val endColor = ContextCompat.getColor(this, R.color.gradient_end)
                tvHeaderTitle.paint.shader = LinearGradient(0f, 0f, width, 0f, intArrayOf(startColor, endColor), null, Shader.TileMode.CLAMP)
                tvHeaderTitle.invalidate()
            }
        }
    }
}
