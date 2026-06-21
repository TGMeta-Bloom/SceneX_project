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
import com.example.scenex.R
import com.example.scenex.viewmodels.SearchFilterViewModel

/**
 * Senior Architect Implementation: Search Refinement Step.
 * Allows recruiters to adjust requirements before hitting the database.
 */
class SearchFilterFragment : Fragment() {

    private val viewModel: SearchFilterViewModel by activityViewModels()

    private val provinces = listOf("Select Province", "Western Province", "Central Province", "Southern Province", "Northern Province", "Eastern Province", "North Western Province", "North Central Province", "Uva Province", "Sabaragamuwa Province")
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
        return inflater.inflate(R.layout.fragment_search_filter, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvQuery = view.findViewById<TextView>(R.id.tvCurrentQuery)
        val spProvince = view.findViewById<Spinner>(R.id.spProvince)
        val spCity = view.findViewById<Spinner>(R.id.spCity)
        val cgGender = view.findViewById<com.google.android.material.chip.ChipGroup>(R.id.cgGender)
        
        val spMinAge = view.findViewById<Spinner>(R.id.spMinAge)
        val spMaxAge = view.findViewById<Spinner>(R.id.spMaxAge)
        val spMinHeight = view.findViewById<Spinner>(R.id.spMinHeight)
        val spMaxHeight = view.findViewById<Spinner>(R.id.spMaxHeight)
        
        val btnFind = view.findViewById<View>(R.id.btnApplyFilters)

        tvQuery.text = "Finding: ${viewModel.criteria.value.query}"

        // Setup Province/City
        val provinceAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, provinces)
        spProvince.adapter = provinceAdapter

        spProvince.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val province = provinces[position]
                if (province != "Select Province") {
                    val cities = citiesMap[province] ?: emptyList()
                    val cityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, cities)
                    spCity.adapter = cityAdapter
                    spCity.isEnabled = true
                } else {
                    spCity.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listOf("Select City"))
                    spCity.isEnabled = false
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Setup Age/Height Spinners
        val ageAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, ageList)
        spMinAge.adapter = ageAdapter
        spMaxAge.adapter = ageAdapter
        spMaxAge.setSelection(ageList.size - 1)

        val heightAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, heightList)
        spMinHeight.adapter = heightAdapter
        spMaxHeight.adapter = heightAdapter
        spMaxHeight.setSelection(heightList.size - 1)

        btnFind.setOnClickListener {
            val selectedProvince = if (spProvince.selectedItemPosition > 0) spProvince.selectedItem.toString() else ""
            val selectedCity = if (spCity.isEnabled && spCity.selectedItemPosition > 0 && spCity.selectedItem.toString() != "All Cities") {
                spCity.selectedItem.toString()
            } else ""
            
            val gender = when (cgGender.checkedChipId) {
                R.id.chipMale -> "Male"
                R.id.chipFemale -> "Female"
                else -> "Any"
            }

            viewModel.updateRegion(selectedProvince, selectedCity)
            viewModel.updateGender(gender)
            viewModel.updateAgeRange(spMinAge.selectedItem.toString().toInt(), spMaxAge.selectedItem.toString().toInt())
            viewModel.updateHeightRange(spMinHeight.selectedItem.toString().toInt(), spMaxHeight.selectedItem.toString().toInt())

            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, TalentResultsFragment())
                .addToBackStack(null)
                .commit()
        }
    }
}
