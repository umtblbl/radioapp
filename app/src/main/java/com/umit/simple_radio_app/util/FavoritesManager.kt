package com.umit.simple_radio_app.util

import android.content.Context
import android.content.SharedPreferences

class FavoritesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("favorites_prefs", Context.MODE_PRIVATE)

    fun getFavorites(): Set<String> {
        return prefs.getStringSet("favorites", emptySet()) ?: emptySet()
    }

    fun addFavorite(url: String) {
        val current = getFavorites().toMutableSet()
        current.add(url)
        prefs.edit().putStringSet("favorites", current).apply()
    }

    fun removeFavorite(url: String) {
        val current = getFavorites().toMutableSet()
        current.remove(url)
        prefs.edit().putStringSet("favorites", current).apply()
    }

    fun isFavorite(url: String): Boolean {
        return getFavorites().contains(url)
    }
}
