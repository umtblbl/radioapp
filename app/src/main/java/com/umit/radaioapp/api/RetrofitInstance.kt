package com.umit.radaioapp.api

import kotlinx.coroutines.runBlocking
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private val baseUrls = listOf(
        "https://de1.api.radio-browser.info/",
        "https://de2.api.radio-browser.info/",
        "https://at1.api.radio-browser.info/",
        "https://nl1.api.radio-browser.info/",
        "https://fr1.api.radio-browser.info/",
        "https://ru1.api.radio-browser.info/",
        "https://us1.api.radio-browser.info/",
        "https://gb1.api.radio-browser.info/"
    )

    val api: RadioService? by lazy {
        var service: RadioService? = null

        runBlocking {
            for (baseUrl in baseUrls) {
                try {
                    val testService = Retrofit.Builder()
                        .baseUrl(baseUrl)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                        .create(RadioService::class.java)

                    val result = testService.getStations(limit = 1)
                    if (result.isNotEmpty()) {
                        service = testService
                        break
                    }
                } catch (_: Exception) {
                }
            }
        }

        service
    }
}