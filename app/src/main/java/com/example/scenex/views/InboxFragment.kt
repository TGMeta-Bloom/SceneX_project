package com.example.scenex.views

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.google.firebase.firestore.Filter

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

        // DISPLAY ALL CHATS: No status filter, so Rejected/Cancelled/Old chats show up.
        // NEW CHATS FIRST: Order by lastActivityTimestamp descending.
        // Filter by current user ID (either as Recruiter or Talent).
        db.collection("bookings")
            .where(Filter.or(
                Filter.equalTo("recruiterId", userId),
                Filter.equalTo("talentId", userId)
            ))
            .orderBy("lastActivityTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (_binding == null) return@addSnapshotListener
                binding.progressBar.visibility = View.GONE
                
                if (e != null) {
                    Log.e("InboxFragment", "Firestore error: ${e.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val userThreads = snapshot.toObjects(Booking::class.java)
                    if (userThreads.isEmpty()) {
                        binding.tvNoMessages.visibility = View.VISIBLE
                        adapter.submitList(emptyList())
                    } else {
                        binding.tvNoMessages.visibility = View.GONE
                        adapter.submitList(userThreads)
                    }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
