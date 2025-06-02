package com.umit.simple_radio_app.util

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.umit.simple_radio_app.model.Station

class FavoritesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    private val GSON = Gson()

    fun getFavorites(): Set<Station> {
        val json = prefs.getString("favorites_stations", null)
        return if (json == null) {
            emptySet()
        } else {
            val type = object : TypeToken<Set<Station>>() {}.type
            GSON.fromJson(json, type) ?: emptySet()
        }
    }

    fun isFavorite(id: String?): Boolean {
        return getFavorites().any { it.stationuuid == id }
    }

    fun addFavorite(station: Station) {
        val currentFavorites = getFavorites().toMutableSet()
        if (!currentFavorites.any { it.stationuuid == station.stationuuid }) {
            currentFavorites.add(station)
            saveFavorites(currentFavorites)
        }
    }

    fun removeFavorite(station: Station) {
        val currentFavorites = getFavorites().toMutableSet()
        if (currentFavorites.remove(station)) {
            saveFavorites(currentFavorites)
        }
    }

    private fun saveFavorites(favorites: Set<Station>) {
        val json = GSON.toJson(favorites)
        prefs.edit().putString("favorites_stations", json).apply()
    }

    fun saveLastPlayedStation(station: Station) {
        val json = GSON.toJson(station)
        prefs.edit().putString("last_played_station", json).apply()
    }

    fun getLastPlayedStation(): Station? {
        val json = prefs.getString("last_played_station", null) ?: return null
        return GSON.fromJson(json, Station::class.java)
    }
}