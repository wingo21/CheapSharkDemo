package com.example.cheapsharkdemo.network

import com.example.cheapsharkdemo.model.Deal
import retrofit2.http.GET
import retrofit2.http.Query

interface CheapSharkApiService {

    // Funzione suspend per ottenere le offerte dall'API di CheapShark
    @GET("deals")
    suspend fun getDeals(
        @Query("storeID") storeID: String? = null,
        @Query("pageNumber") pageNumber: Int? = null,
        @Query("pageSize") pageSize: Int? = null,
        @Query("sortBy") sortBy: String? = "Deal Rating", // Di default consigliano di sortare secondo il "Deal Rating", quanto conveniente sia l'offerta ovvero
        @Query("desc") descending: Boolean? = null,
        @Query("lowerPrice") lowerPrice: Int? = null,
        @Query("upperPrice") upperPrice: Int? = null,
        @Query("metacritic") metacritic: Int? = null,
        @Query("steamRating") steamRating: Int? = null,
        @Query("title") title: String? = null,
        @Query("exact") exact: Boolean? = null,
        @Query("AAA") AAA: Boolean? = null,
        @Query("steamworks") steamworks: Boolean? = null,
        @Query("onSale") onSale: Boolean? = null
    ): List<Deal> // Ritorno un JSON con una lista di Deal
}