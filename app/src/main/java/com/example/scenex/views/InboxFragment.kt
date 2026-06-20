package com.example.scenex.views

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.scenex.databinding.FragmentInboxBinding
import com.example.scenex.models.Booking
import com.example.scenex.views.adapter.ChatThreadAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class InboxFragment : Fragment() {
    private var _binding: FragmentInboxBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: ChatThreadAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInboxBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        fetchChatThreads()
    }

    private fun setupRecyclerView() {
        adapter = ChatThreadAdapter { booking ->
            val intent = Intent(requireContext(), ChatActivity::class.java).apply {
                putExtra("BOOKING_ID", booking.id)
                putExtra("PROJECT_TITLE", booking.castingTitle)
                val isRecruiter = auth.currentUser?.uid == booking.recruiterId
                putExtra("OTHER_PARTY_NAME", if (isRecruiter) booking.name else booking.recruiterName)
                putExtra("RECRUITER_ID", booking.recruiterId)
                putExtra("TALENT_ID", booking.talentId)
            }
            startActivity(intent)
        }
        binding.rvChatThreads.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChatThreads.adapter = adapter
    }

    private fun fetchChatThreads() {
        val userId = auth.currentUser?.uid ?: return
        binding.progressBar.visibility = View.VISIBLE

        // Member 3 Logic: A chat thread is created for every booking.
        // We fetch bookings where the user is either the recruiter or the talent.
        
        // Strategy: Query both fields or use a logical OR (if possible) 
        // For simplicity and speed in Firestore, we'll listen to both.
        
        db.collection("bookings")
            .whereIn("status", listOf("PENDING", "CONFIRMED", "RESCHEDULE_REQUESTED", "COMPLETED"))
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                binding.progressBar.visibility = View.GONE
                if (e != null || snapshot == null) return@addSnapshotListener

                val allBookings = snapshot.toObjects(Booking::class.java)
                val userThreads = allBookings.filter { it.talentId == userId || it.recruiterId == userId }

                if (userThreads.isEmpty()) {
                    binding.tvNoMessages.visibility = View.VISIBLE
                } else {
                    binding.tvNoMessages.visibility = View.GONE
                    adapter.submitList(userThreads)
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
