package com.umit.simple_radio_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umit.simple_radio_app.model.Station
import com.umit.simple_radio_app.repository.RadioRepository
import com.umit.simple_radio_app.util.FavoritesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

class RadioViewModel(
    private val repository: RadioRepository,
    private val favoritesManager: FavoritesManager
) : ViewModel() {

    private val _allStations = MutableStateFlow<List<Station>>(emptyList())
    val allStations: StateFlow<List<Station>> get() = _allStations

    private val _currentSearchQuery = MutableStateFlow("")
    private var lastApiQuery = ""

    private val _displayedStationsApiResult = MutableStateFlow<List<Station>>(emptyList())
    val displayedStations: StateFlow<List<Station>> = _displayedStationsApiResult
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _favorites = MutableStateFlow(favoritesManager.getFavorites())
    val favorites: StateFlow<Set<Station>> get() = _favorites

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> get() = _errorMessage

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> get() = _loading

    private var currentOffset = 0
    private val limit = 20
    private val language = "turkish"

    private var isLoading = false
    private var canLoadMore = true

    init {
        fetchStationsInternal(query = "", reset = true)
    }

    private fun fetchStationsInternal(query: String, reset: Boolean) {
        if (isLoading && !reset) return

        isLoading = true
        _loading.value = true

        if (reset) {
            currentOffset = 0
            _displayedStationsApiResult.value = emptyList()
            canLoadMore = true
            lastApiQuery = query
        } else if (query != lastApiQuery) {
            fetchStationsInternal(query, true)
            return
        }

        viewModelScope.launch {
            try {
                val newStations = repository.getStations(
                    name = query,
                    language = language,
                    offset = currentOffset,
                    limit = limit
                )

                if (newStations?.isEmpty() == true) {
                    canLoadMore = false
                }

                _displayedStationsApiResult.value += newStations.orEmpty()
                _allStations.value = mergeStations(_allStations.value, newStations.orEmpty())
                currentOffset = _displayedStationsApiResult.value.size
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Hata: ${e.localizedMessage}"
                canLoadMore = false
            } finally {
                isLoading = false
                _loading.value = false
            }
        }
    }

    fun loadMore() {
        if (canLoadMore) {
            fetchStationsInternal(query = lastApiQuery, reset = false)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _currentSearchQuery.value = query.trim()

        if (_currentSearchQuery.value != lastApiQuery) {
            fetchStationsInternal(query = _currentSearchQuery.value, reset = true)
        }
    }

    fun toggleFavorite(station: Station) {
        if (favoritesManager.isFavorite(station.stationuuid)) {
            favoritesManager.removeFavorite(station)
        } else {
            favoritesManager.addFavorite(station)
        }
        _favorites.value = favoritesManager.getFavorites()
    }

    fun getLastPlayedUrl() = favoritesManager.getLastPlayedStation()

    fun saveLastPlayedUrl(station: Station) = favoritesManager.saveLastPlayedStation(station)

    fun clearError() {
        _errorMessage.value = null
    }

    private fun mergeStations(oldList: List<Station>, newList: List<Station>): List<Station> {
        val map = oldList.associateBy { it.stationuuid }.toMutableMap()
        for (station in newList) {
            map[station.stationuuid] = station
        }
        return map.values.toList()
    }
}