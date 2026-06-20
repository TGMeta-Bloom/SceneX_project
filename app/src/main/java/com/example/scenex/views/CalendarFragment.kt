package com.example.scenex.views

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.scenex.databinding.FragmentCalendarBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            // Robust check for recruiter vs talent
            identifyUserRoleAndLoad(userId)
        } else {
            loadSubFragment(TalentSchedulingFragment()) 
        }
    }

    private fun identifyUserRoleAndLoad(userId: String) {
        val db = FirebaseFirestore.getInstance()
        
        // 1. Check 'users' collection first - primary source of role data
        db.collection("users").document(userId).get()
            .addOnSuccessListener { userDoc ->
                val role = userDoc.getString("role") ?: ""
                if (role.isNotEmpty()) {
                    routeByRole(role)
                } else {
                    // 2. Fallback to 'profiles' collection
                    db.collection("profiles").document(userId).get()
                        .addOnSuccessListener { profileDoc ->
                            val pRole = profileDoc.getString("userRole") ?: profileDoc.getString("role") ?: "talent"
                            routeByRole(pRole)
                        }
                        .addOnFailureListener { routeByRole("talent") }
                }
            }
            .addOnFailureListener { routeByRole("talent") }
    }

    private fun routeByRole(role: String) {
        if (!isAdded) return
        
        Log.d("SceneX_Router", "Calendar tab successfully identified role: $role")
        
        // Ensure case-insensitive matching
        if (role.equals("recruiter", ignoreCase = true)) {
            loadSubFragment(RecruiterBookingFragment())
        } else {
            loadSubFragment(TalentSchedulingFragment())
        }
    }

    private fun loadSubFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction()
            .replace(binding.calendarContainer.id, fragment)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
