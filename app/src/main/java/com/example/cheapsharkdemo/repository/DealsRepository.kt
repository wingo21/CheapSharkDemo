package com.example.cheapsharkdemo.repository

import com.example.cheapsharkdemo.model.Deal
import com.example.cheapsharkdemo.network.CheapSharkApiService
import com.example.cheapsharkdemo.network.RetrofitInstance

// Recupero le offerte utilizzando l'API di CheapShark (CheapSharkApiService)
// Nel caso di errori di network o di composizione della lista, ritorno una lista vuota
// (nel composable mostro un messaggio all'utente)
class DealsRepository(private val apiService: CheapSharkApiService = RetrofitInstance.api) {

    // Funzione per ottenere le offerte e convertirle in una lista di oggetti Deal
    // Funzione suspend in modo che possiamo usarla dentro coroutines
    suspend fun getDeals(
        storeID: String? = null,
        pageNumber: Int? = null,
        pageSize: Int? = null,
        sortBy: String? = "Deal Rating",
        title: String? = null
    ): List<Deal> {
        return try {
            apiService.getDeals(
                storeID = storeID,
                pageNumber = pageNumber,
                pageSize = pageSize,
                sortBy = sortBy,
                title = title
            )
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
