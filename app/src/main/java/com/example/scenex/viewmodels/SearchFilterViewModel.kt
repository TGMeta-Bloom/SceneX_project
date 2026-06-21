package com.example.scenex.viewmodels

import androidx.lifecycle.ViewModel
import com.example.scenex.models.SearchCriteria
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SearchFilterViewModel : ViewModel() {

    private val _criteria = MutableStateFlow(SearchCriteria())
    val criteria: StateFlow<SearchCriteria> = _criteria.asStateFlow()

    fun updateQuery(query: String) {
        _criteria.update { it.copy(query = query) }
    }

    /**
     * Unified Figma Search: Updates the raw location string.
     */
    fun updateLocation(location: String) {
        _criteria.update { it.copy(location = location) }
    }

    /**
     * Structured Geographic Filter: Updates province, city, and generates a unified string.
     */
    fun updateRegion(province: String, city: String) {
        _criteria.update {
            it.copy(
                province = province,
                city = city,
                location = if (city.isNotEmpty() && city != "All Cities") "$city, $province" else province
            )
        }
    }

    fun updateGender(gender: String) {
        _criteria.update { it.copy(gender = gender) }
    }

    fun updateAgeRange(min: Int, max: Int) {
        _criteria.update { it.copy(minAge = min, maxAge = max) }
    }

    fun updateHeightRange(min: Int, max: Int) {
        _criteria.update { it.copy(minHeight = min, maxHeight = max) }
    }

    fun updateLanguages(languages: List<String>) {
        _criteria.update { it.copy(languages = languages) }
    }

    fun updateRating(rating: Float) {
        _criteria.update { it.copy(minRating = rating) }
    }

    fun resetFilters() {
        _criteria.value = SearchCriteria()
    }
}
