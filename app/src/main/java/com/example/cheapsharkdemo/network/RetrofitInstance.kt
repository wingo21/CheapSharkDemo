package com.example.cheapsharkdemo.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

// Oggetto Singleton per creare e configurare un'istanza di Retrofit
// per le richieste di rete verso l'API di CheapShark.
// Utilizza Moshi per la
// serializzazione/deserializzazione JSON.
object RetrofitInstance {
    private const val BASE_URL = "https://www.cheapshark.com/api/1.0/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val api: CheapSharkApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(CheapSharkApiService::class.java)
    }
}
