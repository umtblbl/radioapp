package com.umit.radaioapp.repository

import com.umit.radaioapp.api.RadioService
import com.umit.radaioapp.model.Station

class RadioRepository(
    private val service: RadioService?
) {
    suspend fun getStations(
        name: String,
        language: String,
        offset: Int,
        limit: Int = 20
    ): List<Station>? {
        return service?.getStations(
            language = language,
            offset = offset,
            limit = limit,
            name = name
        )
    }
}
