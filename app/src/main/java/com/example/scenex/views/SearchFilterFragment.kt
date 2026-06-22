package com.example.scenex.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
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
import com.example.scenex.viewmodels.SearchFilterViewModel
import com.example.scenex.viewmodels.TalentResultsState
import com.example.scenex.viewmodels.TalentResultsViewModel
import kotlinx.coroutines.launch

/**
 * LIVE SEARCH HUB: Merges refinement with instant results.
 * Fixed: Redirection to Hire Form and high-visibility list integration.
 */
class SearchFilterFragment : Fragment() {

    private val filterViewModel: SearchFilterViewModel by activityViewModels()
    private val resultsViewModel: TalentResultsViewModel by viewModels()
    private lateinit var adapter: TalentMatchAdapter

    private val provinces = listOf("All Provinces", "Western Province", "Central Province", "Southern Province", "Northern Province", "Eastern Province", "North Western Province", "North Central Province", "Uva Province", "Sabaragamuwa Province")
    private val ageList = (18..65).map { it.toString() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_search_filter, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvResults = view.findViewById<RecyclerView>(R.id.rvResults)
        val spProvince = view.findViewById<Spinner>(R.id.spProvince)
        val cgGender = view.findViewById<com.google.android.material.chip.ChipGroup>(R.id.cgGender)
        val spMinAge = view.findViewById<Spinner>(R.id.spMinAge)
        val spMaxAge = view.findViewById<Spinner>(R.id.spMaxAge)

        // 🎯 REDIRECTION FIX: Wiring up the Hire Button for the results list
        adapter = TalentMatchAdapter(
            talents = emptyList(),
            onProfileClick = { talent ->
                val detailFragment = TalentDetailFragment.newInstance(talent)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, detailFragment)
                    .addToBackStack(null)
                    .commit()
            },
            onHireClick = { talent ->
                val hireFragment = HireRequestFragment.newInstance(talent)
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.fade_out)
                    .replace(R.id.nav_host_fragment, hireFragment)
                    .addToBackStack(null)
                    .commit()
            }
        )
        rvResults.layoutManager = LinearLayoutManager(requireContext())
        rvResults.adapter = adapter

        // Setup UI logic
        spProvince.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, provinces)
        spMinAge.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, ageList)
        spMaxAge.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, ageList)
        spMaxAge.setSelection(ageList.size - 1)

        val changeListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                updateFiltersAndSearch(spProvince, cgGender, spMinAge, spMaxAge)
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        spProvince.onItemSelectedListener = changeListener
        spMinAge.onItemSelectedListener = changeListener
        spMaxAge.onItemSelectedListener = changeListener
        
        cgGender.setOnCheckedStateChangeListener { _, _ -> 
            updateFiltersAndSearch(spProvince, cgGender, spMinAge, spMaxAge)
        }

        view.findViewById<View>(R.id.btnApplyFilters).setOnClickListener {
            updateFiltersAndSearch(spProvince, cgGender, spMinAge, spMaxAge)
        }

        observeData(view)
        resultsViewModel.loadInitialData()
    }

    private fun updateFiltersAndSearch(spProv: Spinner, cgGen: com.google.android.material.chip.ChipGroup, spMin: Spinner, spMax: Spinner) {
        val province = if (spProv.selectedItemPosition > 0) spProv.selectedItem.toString() else ""
        val gender = when (cgGen.checkedChipId) {
            R.id.chipMale -> "Male"
            R.id.chipFemale -> "Female"
            else -> "Any"
        }
        
        filterViewModel.updateRegion(province, "")
        filterViewModel.updateGender(gender)
        filterViewModel.updateAgeRange(spMin.selectedItem.toString().toInt(), spMax.selectedItem.toString().toInt())
        
        resultsViewModel.performSearch(filterViewModel.criteria.value)
    }

    private fun observeData(view: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                resultsViewModel.uiState.collect { state ->
                    when (state) {
                        is TalentResultsState.Success -> {
                            adapter.updateData(state.talents)
                            view.findViewById<View>(R.id.rvResults).visibility = View.VISIBLE
                        }
                        else -> adapter.updateData(emptyList())
                    }
                }
            }
        }
    }
}
