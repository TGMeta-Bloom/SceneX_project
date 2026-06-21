package com.example.scenex.views

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.scenex.R
import com.example.scenex.viewmodels.HireRequestEvent
import com.example.scenex.viewmodels.HireRequestViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.collectLatest

class HireRequestDetailsActivity : AppCompatActivity() {

    private val viewModel: HireRequestViewModel by viewModels()
    private val db = FirebaseFirestore.getInstance()
    private var requestId: String? = null

    private lateinit var tvRecruiterName: TextView
    private lateinit var tvProjectTitle: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvPayment: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvStartDate: TextView
    private lateinit var tvEndDate: TextView
    private lateinit var tvMessage: TextView
    private lateinit var tvStatus: TextView
    private lateinit var ivRecruiterProfile: ShapeableImageView
    private lateinit var layoutActions: View
    private lateinit var btnAccept: View
    private lateinit var btnReject: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hire_request_details)

        requestId = intent.getStringExtra("HIRE_REQUEST_ID")
        if (requestId == null) {
            Toast.makeText(this, "Error: Request ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        observeViewModel()
        viewModel.fetchHireRequest(requestId!!)
    }

    private fun initViews() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }

        tvRecruiterName = findViewById(R.id.tvRecruiterName)
        tvProjectTitle = findViewById(R.id.tvProjectTitle)
        tvDescription = findViewById(R.id.tvDescription)
        tvPayment = findViewById(R.id.tvPayment)
        tvLocation = findViewById(R.id.tvLocation)
        tvStartDate = findViewById(R.id.tvStartDate)
        tvEndDate = findViewById(R.id.tvEndDate)
        tvMessage = findViewById(R.id.tvMessage)
        tvStatus = findViewById(R.id.tvStatus)
        ivRecruiterProfile = findViewById(R.id.ivRecruiterProfile)
        layoutActions = findViewById(R.id.layoutActions)
        btnAccept = findViewById(R.id.btnAccept)
        btnReject = findViewById(R.id.btnReject)

        btnAccept.setOnClickListener {
            viewModel.updateRequestStatus(requestId!!, "ACCEPTED")
        }

        btnReject.setOnClickListener {
            viewModel.updateRequestStatus(requestId!!, "REJECTED")
        }
    }

    private fun observeViewModel() {
        viewModel.hireRequest.observe(this) { request ->
            request?.let {
                tvProjectTitle.text = it.projectTitle
                tvDescription.text = it.description
                tvPayment.text = it.payment
                tvLocation.text = it.location
                tvStartDate.text = it.startDate
                tvEndDate.text = if (it.endDate.isEmpty()) "Not specified" else it.endDate
                tvMessage.text = it.message

                if (it.status == "PENDING") {
                    layoutActions.visibility = View.VISIBLE
                    tvStatus.visibility = View.GONE
                } else {
                    layoutActions.visibility = View.GONE
                    tvStatus.visibility = View.VISIBLE
                    tvStatus.text = "Status: ${it.status}"
                }

                // Fetch recruiter details
                fetchRecruiterDetails(it.recruiterId)
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.events.collectLatest { event ->
                when (event) {
                    is HireRequestEvent.StatusUpdated -> {
                        Toast.makeText(this@HireRequestDetailsActivity, "Request ${event.status}", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    is HireRequestEvent.Error -> {
                        Toast.makeText(this@HireRequestDetailsActivity, event.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun fetchRecruiterDetails(recruiterId: String) {
        db.collection("profiles").document(recruiterId).get().addOnSuccessListener { snapshot ->
            val name = snapshot.getString("fullName") ?: "Recruiter"
            val imageUrl = snapshot.getString("profileImageUrl") ?: snapshot.getString("profileImage") ?: ""

            tvRecruiterName.text = name
            if (imageUrl.isNotEmpty()) {
                Glide.with(this).load(imageUrl).circleCrop().into(ivRecruiterProfile)
            }
        }
    }
}
