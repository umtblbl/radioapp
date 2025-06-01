package com.umit.simple_radio_app.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    val api: RadioService by lazy {
        Retrofit.Builder()
            .baseUrl("https://de1.api.radio-browser.info/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RadioService::class.java)
    }
}
