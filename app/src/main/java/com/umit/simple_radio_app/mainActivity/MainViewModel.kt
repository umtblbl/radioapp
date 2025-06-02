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

    private val _favorites = MutableStateFlow(favoritesManager.getFavorites())
    val favorites: StateFlow<Set<String>> get() = _favorites

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> get() = _errorMessage

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> get() = _loading

    private var currentOffset = 0
    private val limit = 20
    private val language = "turkish"

    private var isLoading = false

    init {
        // İlk önce localden yükle
        val localStations = favoritesManager.getLocalStations()
        if (localStations.isNotEmpty()) {
            _stations.value = localStations
            currentOffset = localStations.size
        }
        // Sonra servisten çek
        fetchStations()
    }

    fun fetchStations() {
        if (isLoading) return
        isLoading = true
        _loading.value = true
        viewModelScope.launch {
            try {
                val newStations = repository.getStations(language, currentOffset, limit)
                // Yeni gelenler localdekilerle merge edilip güncelleniyor
                val mergedStations = mergeStations(_stations.value, newStations)
                _stations.value = mergedStations
                currentOffset = mergedStations.size
                _errorMessage.value = null
                // Local prefs'i güncelle
                favoritesManager.saveLocalStations(mergedStations)
            } catch (e: Exception) {
                _errorMessage.value =
                    "Hata: ${e.localizedMessage}"
            } finally {
                isLoading = false
                _loading.value = false
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

    fun getLastPlayedUrl() = favoritesManager.getLastPlayedStation()
    fun saveLastPlayedUrl(
        station: Station
    ) = favoritesManager.saveLastPlayedStation(station)

    fun clearError() {
        _errorMessage.value = null
    }

    // İki listeyi stationuuid bazlı merge et
    private fun mergeStations(
        oldList: List<Station>,
        newList: List<Station>
    ): List<Station> {
        val map = oldList.associateBy { it.stationuuid }.toMutableMap()
        for (station in newList) {
            val oldStation = map[station.stationuuid]
            if (oldStation == null) {
                map[station.stationuuid] = station
            } else {
                // Eğer URL değiştiyse güncelle
                if (oldStation.url != station.url) {
                    map[station.stationuuid] = station
                }
                // Diğer alanlar için istersen başka güncellemeler yapılabilir
            }
        }
        return map.values.toList()
    }
}
