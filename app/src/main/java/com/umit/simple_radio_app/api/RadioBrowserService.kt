package com.umit.simple_radio_app.api

import com.umit.simple_radio_app.model.Station
import retrofit2.http.GET
import retrofit2.http.Query

interface RadioService {
    @GET("json/stations/search")
    suspend fun getStations(
        @Query("language") language: String,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int = 20
    ): List<Station>
}
