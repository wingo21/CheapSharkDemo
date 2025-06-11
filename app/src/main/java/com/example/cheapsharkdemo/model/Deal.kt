package com.example.cheapsharkdemo.model

import com.squareup.moshi.JsonClass

// Definisco l'oggetto Deal che poi usero' per creare le liste nei composable
@JsonClass(generateAdapter = true)
data class Deal(
    val internalName: String?,
    val title: String?,
    val dealID: String?,
    val storeID: String?,
    val gameID: String?,
    val salePrice: String?,
    val normalPrice: String?,
    val isOnSale: String?, // L'API torna 0 (non in sconto) o 1 (in sconto)
    val savings: String?,
    val metaCriticScore: String?,
    val steamRatingText: String?,
    val steamRatingPercent: String?,
    val steamRatingCount: String?,
    val steamAppID: String?,
    val releaseDate: Long?, // Timestamp
    val lastChange: Long?, // Timestamp
    val dealRating: String?,
    val thumb: String?
)

