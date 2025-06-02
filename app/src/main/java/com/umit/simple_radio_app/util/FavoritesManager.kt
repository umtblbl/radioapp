package com.umit.simple_radio_app.util

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.umit.simple_radio_app.model.Station

class FavoritesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getFavorites(): Set<String> {
        return prefs.getStringSet("favorites", emptySet()) ?: emptySet()
    }

    fun isFavorite(url: String): Boolean {
        return getFavorites().contains(url)
    }

    fun addFavorite(url: String) {
        val favs = getFavorites().toMutableSet()
        favs.add(url)
        prefs.edit().putStringSet("favorites", favs).apply()
    }

    fun removeFavorite(url: String) {
        val favs = getFavorites().toMutableSet()
        favs.remove(url)
        prefs.edit().putStringSet("favorites", favs).apply()
    }

    // Yeni: Localde station listesi tutmak için

    fun getLocalStations(): List<Station> {
        val json = prefs.getString("local_stations", null)
        if (json.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<Station>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveLocalStations(stations: List<Station>) {
        val json = gson.toJson(stations)
        prefs.edit().putString("local_stations", json).apply()
    }

    fun saveLastPlayedStation(station: Station) {
        val json = Gson().toJson(station)
        prefs.edit().putString("last_played_station", json).apply()
    }

    fun getLastPlayedStation(): Station? {
        val json = prefs.getString("last_played_station", null) ?: return null
        return Gson().fromJson(json, Station::class.java)
    }
}
