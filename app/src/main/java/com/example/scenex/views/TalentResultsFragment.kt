package com.example.scenex.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scenex.R
import com.example.scenex.adapters.TalentMatchAdapter
import com.example.scenex.models.UserProfile
import com.example.scenex.viewmodels.SearchFilterViewModel
import com.example.scenex.viewmodels.TalentResultsState
import com.example.scenex.viewmodels.TalentResultsViewModel
import kotlinx.coroutines.launch

/**
 * Senior Architect Implementation: Talent Results Display.
 * Consumes shared criteria, executes the search, and handles all UI states (Loading, Success, Empty, Error).
 */
class TalentResultsFragment : Fragment() {

    private val sharedViewModel: SearchFilterViewModel by activityViewModels()
    private val resultsViewModel: TalentResultsViewModel by viewModels()
    private lateinit var adapter: TalentMatchAdapter

    private lateinit var rvResults: RecyclerView
    private lateinit var pbLoading: View
    private lateinit var llEmptyState: View
    private lateinit var tvStateMessage: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_talent_results, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupRecyclerView()
        observeState()

        // 🎯 DISCOVERY EXECUTION: Consume shared criteria and execute query
        val criteria = sharedViewModel.criteria.value
        resultsViewModel.performSearch(criteria)
    }

    private fun initViews(view: View) {
        rvResults = view.findViewById(R.id.rvTalentResults)
        pbLoading = view.findViewById(R.id.pbLoading)
        llEmptyState = view.findViewById(R.id.llEmptyState)
        tvStateMessage = view.findViewById(R.id.tvStateMessage)
        
        view.findViewById<View>(R.id.ivBack)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        adapter = TalentMatchAdapter(
            talents = emptyList(),
            onProfileClick = { talent ->
                navigateToDetail(talent)
            },
            onHireClick = { talent ->
                // 🎯 FLOW FIX: Hire button on card now also goes to Detail page
                navigateToDetail(talent)
            }
        )
        rvResults.layoutManager = LinearLayoutManager(requireContext())
        rvResults.adapter = adapter
    }

    private fun navigateToDetail(talent: UserProfile) {
        val detailFragment = TalentDetailFragment.newInstance(talent)
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.nav_host_fragment, detailFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                resultsViewModel.uiState.collect { state ->
                    when (state) {
                        is TalentResultsState.Loading -> handleLoading()
                        is TalentResultsState.Success -> handleSuccess(state)
                        is TalentResultsState.Empty -> handleEmpty()
                        is TalentResultsState.Error -> handleError(state.message)
                    }
                }
            }
        }
    }

    private fun handleLoading() {
        pbLoading.visibility = View.VISIBLE
        rvResults.visibility = View.GONE
        llEmptyState.visibility = View.GONE
    }

    private fun handleSuccess(state: TalentResultsState.Success) {
        pbLoading.visibility = View.GONE
        llEmptyState.visibility = View.GONE
        rvResults.visibility = View.VISIBLE
        adapter.updateData(state.talents)
    }

    private fun handleEmpty() {
        pbLoading.visibility = View.GONE
        rvResults.visibility = View.GONE
        llEmptyState.visibility = View.VISIBLE
        tvStateMessage.text = "No talents match your criteria. Try adjusting your filters."
    }

    private fun handleError(message: String) {
        pbLoading.visibility = View.GONE
        rvResults.visibility = View.GONE
        llEmptyState.visibility = View.VISIBLE
        tvStateMessage.text = "Error: $message"
    }
}
