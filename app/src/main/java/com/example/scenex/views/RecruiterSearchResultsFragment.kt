package com.example.scenex.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RatingBar
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
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
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

/**
 * ELITE DISCOVERY HUB: Integrated Side-Rail Filter System.
 * Optimized with high-precision structured dropdowns for Sri Lankan parameters.
 */
class RecruiterSearchResultsFragment : Fragment() {

    private val sharedViewModel: SearchFilterViewModel by activityViewModels()
    private val resultsViewModel: TalentResultsViewModel by viewModels()
    private lateinit var adapter: TalentMatchAdapter

    private lateinit var cvFilterPanel: View
    private lateinit var btnFilterToggle: View
    private lateinit var rvResults: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var pbLoading: View
    
    private lateinit var filterPanes: Map<Int, View>
    private lateinit var railItems: List<TextView>

    // Dropdown Data
    private val provinces = listOf("All", "Western Province", "Central Province", "Southern Province", "Northern Province", "Eastern Province", "North Western Province", "North Central Province", "Uva Province", "Sabaragamuwa Province")
    private val citiesMap = mapOf(
        "Western Province" to listOf("All Cities", "Colombo", "Dehiwala", "Moratuwa", "Negombo", "Panadura", "Kalutara"),
        "Central Province" to listOf("All Cities", "Kandy", "Matale", "Nuwara Eliya", "Gampola"),
        "Southern Province" to listOf("All Cities", "Galle", "Matara", "Hambantota", "Tangalle"),
        "Northern Province" to listOf("All Cities", "Jaffna", "Kilinochchi", "Mullaitivu", "Vavuniya", "Mannar"),
        "Eastern Province" to listOf("All Cities", "Trincomalee", "Batticaloa", "Kalmunai", "Ampara"),
        "North Western Province" to listOf("All Cities", "Kurunegala", "Puttalam", "Chilaw"),
        "North Central Province" to listOf("All Cities", "Anuradhapura", "Polonnaruwa"),
        "Uva Province" to listOf("All Cities", "Badulla", "Bandarawela", "Monaragala"),
        "Sabaragamuwa Province" to listOf("All Cities", "Ratnapura", "Kegalle", "Balangoda")
    )
    private val ageList = (18..65).map { it.toString() }
    private val heightList = (140..210).map { it.toString() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_recruiter_search_results, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupSideRail(view)
        setupFilterListeners(view)
        setupRecyclerView()
        observeData()

        etSearch.setText(sharedViewModel.criteria.value.query)
        refreshDiscovery()
    }

    private fun initViews(view: View) {
        cvFilterPanel = view.findViewById(R.id.cvFilterPanel)
        btnFilterToggle = view.findViewById(R.id.btnFilterToggle)
        rvResults = view.findViewById(R.id.rvResults)
        etSearch = view.findViewById(R.id.etSearch)
        pbLoading = view.findViewById(R.id.pbLoading)

        btnFilterToggle.setOnClickListener {
            cvFilterPanel.visibility = if (cvFilterPanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        view.findViewById<View>(R.id.btnSearch).setOnClickListener {
            sharedViewModel.updateQuery(etSearch.text.toString())
            refreshDiscovery()
            cvFilterPanel.visibility = View.GONE
        }

        view.findViewById<View>(R.id.ivBack).setOnClickListener { parentFragmentManager.popBackStack() }
    }

    private fun setupSideRail(view: View) {
        railItems = listOf(
            view.findViewById(R.id.tvRailLocation),
            view.findViewById(R.id.tvRailGender),
            view.findViewById(R.id.tvRailAge),
            view.findViewById(R.id.tvRailHeight),
            view.findViewById(R.id.tvRailLanguage),
            view.findViewById(R.id.tvRailRating)
        )

        filterPanes = mapOf(
            0 to view.findViewById(R.id.layoutFilterLocation),
            1 to view.findViewById(R.id.layoutFilterGender),
            2 to view.findViewById(R.id.layoutFilterAge),
            3 to view.findViewById(R.id.layoutFilterHeight),
            4 to view.findViewById(R.id.layoutFilterLanguage),
            5 to view.findViewById(R.id.layoutFilterRating)
        )

        railItems.forEachIndexed { index, textView ->
            textView.setOnClickListener {
                railItems.forEach { 
                    it.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
                    it.setTypeface(null, android.graphics.Typeface.NORMAL)
                }
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_magenta))
                textView.setTypeface(null, android.graphics.Typeface.BOLD)

                filterPanes.values.forEach { it.visibility = View.GONE }
                filterPanes[index]?.visibility = View.VISIBLE
            }
        }
    }

    private fun setupFilterListeners(view: View) {
        // --- 1. Structured Location ---
        val spProvince = view.findViewById<Spinner>(R.id.spFilterProvince)
        val spCity = view.findViewById<Spinner>(R.id.spFilterCity)
        
        spProvince.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, provinces)
        spProvince.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val province = provinces[pos]
                val cities = if (province == "All") listOf("All Cities") else citiesMap[province] ?: listOf("All Cities")
                spCity.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, cities)
                sharedViewModel.updateRegion(if(province == "All") "" else province, "")
                refreshDiscovery()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        spCity.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val province = if(spProvince.selectedItem.toString() == "All") "" else spProvince.selectedItem.toString()
                val city = if(p?.getItemAtPosition(pos).toString() == "All Cities") "" else p?.getItemAtPosition(pos).toString()
                sharedViewModel.updateRegion(province, city)
                refreshDiscovery()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        // --- 2. Gender Chips ---
        view.findViewById<ChipGroup>(R.id.cgGender).setOnCheckedStateChangeListener { _, checkedIds ->
            val gender = when (checkedIds.firstOrNull()) {
                R.id.chipMale -> "Male"
                R.id.chipFemale -> "Female"
                else -> "Any"
            }
            sharedViewModel.updateGender(gender)
            refreshDiscovery()
        }

        // --- 3. Structured Age (Min/Max Spinners) ---
        val spMinAge = view.findViewById<Spinner>(R.id.spMinAge)
        val spMaxAge = view.findViewById<Spinner>(R.id.spMaxAge)
        spMinAge.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, ageList)
        spMaxAge.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, ageList)
        spMaxAge.setSelection(ageList.size - 1)

        val ageListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                sharedViewModel.updateAgeRange(spMinAge.selectedItem.toString().toInt(), spMaxAge.selectedItem.toString().toInt())
                refreshDiscovery()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        spMinAge.onItemSelectedListener = ageListener
        spMaxAge.onItemSelectedListener = ageListener

        // --- 4. Structured Height (Min/Max Spinners) ---
        val spMinHeight = view.findViewById<Spinner>(R.id.spMinHeight)
        val spMaxHeight = view.findViewById<Spinner>(R.id.spMaxHeight)
        spMinHeight.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, heightList)
        spMaxHeight.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, heightList)
        spMaxHeight.setSelection(heightList.size - 1)

        val heightListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                sharedViewModel.updateHeightRange(spMinHeight.selectedItem.toString().toInt(), spMaxHeight.selectedItem.toString().toInt())
                refreshDiscovery()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        spMinHeight.onItemSelectedListener = heightListener
        spMaxHeight.onItemSelectedListener = heightListener

        // --- 5. Language Checklist ---
        val langListener = View.OnClickListener {
            val selected = mutableListOf<String>()
            if (view.findViewById<CheckBox>(R.id.cbSinhala).isChecked) selected.add("Sinhala")
            if (view.findViewById<CheckBox>(R.id.cbEnglish).isChecked) selected.add("English")
            if (view.findViewById<CheckBox>(R.id.cbTamil).isChecked) selected.add("Tamil")
            sharedViewModel.updateLanguages(selected)
            refreshDiscovery()
        }
        view.findViewById<CheckBox>(R.id.cbSinhala).setOnClickListener(langListener)
        view.findViewById<CheckBox>(R.id.cbEnglish).setOnClickListener(langListener)
        view.findViewById<CheckBox>(R.id.cbTamil).setOnClickListener(langListener)

        // --- 6. Rating ---
        view.findViewById<RatingBar>(R.id.rbMinRating).setOnRatingBarChangeListener { _, rating, _ ->
            sharedViewModel.updateRating(rating)
            refreshDiscovery()
        }
    }

    private fun refreshDiscovery() {
        resultsViewModel.performSearch(sharedViewModel.criteria.value)
    }

    private fun setupRecyclerView() {
        adapter = TalentMatchAdapter(emptyList()) { talent ->
            val detailFragment = TalentDetailFragment.newInstance(talent)
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.nav_host_fragment, detailFragment)
                .addToBackStack(null)
                .commit()
        }
        rvResults.layoutManager = LinearLayoutManager(requireContext())
        rvResults.adapter = adapter
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                resultsViewModel.uiState.collect { state ->
                    when (state) {
                        is TalentResultsState.Success -> {
                            pbLoading.visibility = View.GONE
                            adapter.updateData(state.talents)
                        }
                        is TalentResultsState.Loading -> pbLoading.visibility = View.VISIBLE
                        else -> {
                            pbLoading.visibility = View.GONE
                            adapter.updateData(emptyList())
                        }
                    }
                }
            }
        }
    }
}
