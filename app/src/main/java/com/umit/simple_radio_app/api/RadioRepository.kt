package com.umit.simple_radio_app.repository

import com.umit.simple_radio_app.api.RadioService
import com.umit.simple_radio_app.model.Station

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
