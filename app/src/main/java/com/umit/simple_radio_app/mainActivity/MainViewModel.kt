package com.umit.simple_radio_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umit.simple_radio_app.model.Station
import com.umit.simple_radio_app.repository.RadioRepository
import com.umit.simple_radio_app.util.FavoritesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RadioViewModel(
    private val repository: RadioRepository,
    private val favoritesManager: FavoritesManager
) : ViewModel() {

    private val _stations = MutableStateFlow<List<Station>>(emptyList())
    val stations: StateFlow<List<Station>> get() = _stations

    private val _favorites = MutableStateFlow<Set<String>>(favoritesManager.getFavorites())
    val favorites: StateFlow<Set<String>> get() = _favorites

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> get() = _errorMessage

    private var currentOffset = 0
    private val limit = 20
    private val language = "turkish" // Dil desteği burada API çağrısına eklendi

    private var isLoading = false

    init {
        fetchStations()
    }

    fun fetchStations() {
        if (isLoading) return
        isLoading = true
        viewModelScope.launch {
            try {
                val newStations = repository.getStations(language, currentOffset, limit)
                _stations.value = _stations.value + newStations
                currentOffset += limit
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Radyo verileri yüklenirken hata oluştu: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    fun loadMore() {
        fetchStations()
    }

    fun toggleFavorite(url: String) {
        if (favoritesManager.isFavorite(url)) {
            favoritesManager.removeFavorite(url)
        } else {
            favoritesManager.addFavorite(url)
        }
        _favorites.value = favoritesManager.getFavorites()
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
