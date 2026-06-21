package com.example.scenex.views

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.scenex.R
import com.example.scenex.models.HireRequest
import com.example.scenex.models.UserProfile
import com.example.scenex.utils.SessionManager
import com.example.scenex.viewmodels.HireRequestEvent
import com.example.scenex.viewmodels.HireRequestViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.flow.collectLatest
import java.util.*

class HireRequestFragment : Fragment() {

    private val viewModel: HireRequestViewModel by viewModels()
    private lateinit var talent: UserProfile
    private val db = FirebaseFirestore.getInstance()

    private lateinit var etProjectTitle: EditText
    private lateinit var etCastingId: EditText
    private lateinit var etProjectDescription: EditText
    private lateinit var etPayment: EditText
    private lateinit var etStartDate: EditText
    private lateinit var etEndDate: EditText
    private lateinit var etLocation: EditText
    private lateinit var etMessage: EditText
    private lateinit var btnSubmitRequest: Button
    private lateinit var toolbar: Toolbar

    companion object {
        private const val ARG_TALENT_JSON = "arg_talent_json"
        fun newInstance(talent: UserProfile): HireRequestFragment {
            val fragment = HireRequestFragment()
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
        return inflater.inflate(R.layout.fragment_hire_request, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupListeners()
        observeViewModel()
    }

    private fun initViews(view: View) {
        etProjectTitle = view.findViewById(R.id.etProjectTitle)
        etCastingId = view.findViewById(R.id.etCastingId)
        etProjectDescription = view.findViewById(R.id.etProjectDescription)
        etPayment = view.findViewById(R.id.etPayment)
        etStartDate = view.findViewById(R.id.etStartDate)
        etEndDate = view.findViewById(R.id.etEndDate)
        etLocation = view.findViewById(R.id.etLocation)
        etMessage = view.findViewById(R.id.etMessage)
        btnSubmitRequest = view.findViewById(R.id.btnSubmitRequest)
        toolbar = view.findViewById(R.id.toolbar)
    }

    private fun setupListeners() {
        toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }
        etStartDate.setOnClickListener { showDatePicker { date -> etStartDate.setText(date) } }
        etEndDate.setOnClickListener { showDatePicker { date -> etEndDate.setText(date) } }
        btnSubmitRequest.setOnClickListener { validateAndSubmit() }
    }

    private fun validateAndSubmit() {
        val title = etProjectTitle.text.toString().trim()
        val castingId = etCastingId.text.toString().trim()
        val desc = etProjectDescription.text.toString().trim()
        val payment = etPayment.text.toString().trim()
        val start = etStartDate.text.toString().trim()
        val end = etEndDate.text.toString().trim()
        val loc = etLocation.text.toString().trim()
        val msg = etMessage.text.toString().trim()

        if (title.isEmpty() || desc.isEmpty() || payment.isEmpty() || start.isEmpty() || loc.isEmpty()) {
            Toast.makeText(requireContext(), "Fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val recruiterId = SessionManager.getUserId(requireContext()) ?: "anon"
        
        // Fetch recruiter name first for the notification
        db.collection("profiles").document(recruiterId).get().addOnSuccessListener { snapshot ->
            val recruiterName = snapshot.getString("fullName") ?: "Recruiter"
            
            val request = HireRequest(
                recruiterId = recruiterId,
                talentId = talent.userId,
                castingId = castingId,
                projectTitle = title,
                description = desc,
                payment = payment,
                startDate = start,
                endDate = end,
                location = loc,
                message = msg
            )

            viewModel.submitHireRequest(request, recruiterName)
        }.addOnFailureListener {
            val request = HireRequest(
                recruiterId = recruiterId,
                talentId = talent.userId,
                castingId = castingId,
                projectTitle = title,
                description = desc,
                payment = payment,
                startDate = start,
                endDate = end,
                location = loc,
                message = msg
            )
            viewModel.submitHireRequest(request, "Recruiter")
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.events.collectLatest { event ->
                when (event) {
                    is HireRequestEvent.Success -> {
                        Toast.makeText(requireContext(), "Hire request sent!", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    }
                    is HireRequestEvent.Error -> {
                        Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, day ->
            onDateSelected("$day/${month + 1}/$year")
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }
}
