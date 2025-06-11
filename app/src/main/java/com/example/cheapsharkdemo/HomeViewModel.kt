package com.example.cheapsharkdemo

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cheapsharkdemo.model.Deal
import com.example.cheapsharkdemo.repository.DealsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.UUID

// La grandezza della pagina viene definita con una costante per seguire la richiesta dell'esercizio
const val PAGE_SIZE = 20

class HomeViewModel(private val dealsRepository: DealsRepository = DealsRepository()) : ViewModel() {

    // Serve per garantire in fase di testing che il ViewModel sia lo stesso tra le varie schermate
    private val viewModelId = UUID.randomUUID().toString()

    // Lista degli elementi Deals
    private val _deals = MutableStateFlow<List<Deal>>(emptyList())
    val deals: StateFlow<List<Deal>> = _deals.asStateFlow()

    // Elemento Deal selezionato (su cui l'utente ha toccato), da mostrare in DealDetailScreen
    private val _selectedDeal = MutableStateFlow<Deal?>(null)
    val selectedDeal: StateFlow<Deal?> = _selectedDeal.asStateFlow()

    // Per controllare gli stati di caricamento, messaggi di errore, query di ricerca
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _isLoadingSelectedDeal = MutableStateFlow(false) // Loading state for the selected deal
    val isLoadingSelectedDeal: StateFlow<Boolean> = _isLoadingSelectedDeal.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var searchJob: Job? = null
    private var currentPage = 0
    var canLoadMore = true

    // Per il filtro tramite Store digitale
    private val _selectedStoreId = MutableStateFlow<String?>(null)
    val selectedStoreId: StateFlow<String?> = _selectedStoreId.asStateFlow()

    // Visibilita del Dialog per il filtro tramite Store digitale
    var showStoreFilterDialog by mutableStateOf(false)
        private set

    // Store digitali disponibili per il filtro
    val availableStores: Map<String, String> = storeNameMap.filterKeys { it.isNotBlank() } // Filter out any blank keys if necessary

    // Salvo tutte le Deal gia' caricate
    private val allLoadedDealsCache = mutableMapOf<String, Deal>()

    // Salvo tutte le Deal gia' animate
    private val _playedEntryAnimationDealIds = mutableSetOf<String>()

    // I due Log sono stati usati per controllare che il ViewModel usato nelle varie sezioni dell'app fosse lo stesso
    // Inizializzo il ViewModel e vado a caricare gli elementi Deal
    init {
        Log.d("HomeViewModelLifecycle", "ViewModel instance $viewModelId CREATED. Cache size BEFORE init fetch: ${allLoadedDealsCache.size}")
        fetchDeals(isNewSearch = true)
        Log.d("HomeViewModelLifecycle", "ViewModel instance $viewModelId init fetch COMPLETE. Cache size AFTER init fetch: ${allLoadedDealsCache.size}")
    }

    // Quando l'utente cerca qualcosa, in una coroutine cerco applicando il nome desiderato
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            fetchDeals(searchTerm = query.trim().ifEmpty { null }, isNewSearch = true)
        }
    }

    // Apro il Dialog del filtro
    fun onStoreFilterClicked() {
        showStoreFilterDialog = true
    }

    // Chiudo il Dialog del filtro
    fun onDismissStoreFilterDialog() {
        showStoreFilterDialog = false
    }

    // Quando seleziono uno Store digitale nel filtro, chiudo il Dialog e lo applico, creando una nuova lista di oggetti Deal
    fun onStoreSelected(storeId: String?) {
        _selectedStoreId.value = storeId
        showStoreFilterDialog = false
        fetchDeals(
            searchTerm = _searchQuery.value.trim().ifEmpty { null },
            storeId = storeId,
            isNewSearch = true
        )
    }

    // Rimuovo il filtro
    fun clearStoreFilter() {
        onStoreSelected(null)
    }

    // Controllo se ho eseguito l'animazione di un'elemento che entra nella lista
    fun hasPlayedEntryAnimation(dealId: String?): Boolean {
        return dealId != null && _playedEntryAnimationDealIds.contains(dealId)
    }

    // Salvo che ho eseguito l'animazione di un'elemento che entra nella lista
    fun markAsPlayedEntryAnimation(dealId: String?) {
        dealId?.let { _playedEntryAnimationDealIds.add(it) }
    }

    // Se devo mostrare una nuova lista reimposto le animazioni in modo che sia animata come la lista iniziale
    private fun clearPlayedAnimations() {
        _playedEntryAnimationDealIds.clear()
    }

    // La funzione principale che recupera i Deals tramite l'API e crea la lista mostrata a schermo
    private fun fetchDeals(
        searchTerm: String? = _searchQuery.value.trim().ifEmpty { null },
        storeId: String? = _selectedStoreId.value,
        sortBy: String = "Deal Rating",
        isNewSearch: Boolean = false
    ) {
        // Nuova ricerca, reimposto animazioni e elementi caricati
        if (isNewSearch) {
            currentPage = 0
            canLoadMore = true
            clearPlayedAnimations()
        }

        // Se ho già una ricerca in corso, esco
        if (_isLoading.value || (!isNewSearch && _isLoadingMore.value) || !canLoadMore && !isNewSearch) {
            return
        }

        // Effettuo la ricerca, applicando eventuali filtri tramite Store digitale o nome del gioco
        viewModelScope.launch {
            if (isNewSearch) _isLoading.value = true else _isLoadingMore.value = true
            _errorMessage.value = null
            try {
                Log.d("HomeViewModel", "Fetching deals with: term='$searchTerm', storeID='$storeId', page='$currentPage'")
                val fetchedDealsFromRepo = dealsRepository.getDeals(
                    title = searchTerm,
                    storeID = storeId,
                    sortBy = sortBy,
                    pageNumber = currentPage,
                    pageSize = PAGE_SIZE
                )

                val newDeals = fetchedDealsFromRepo.filter { deal ->
                    // Mi assicuro che siano entrambi numeri validi prima di compararli
                    val normalPrice = deal.normalPrice?.toFloatOrNull()
                    val salePrice = deal.salePrice?.toFloatOrNull()

                    if (normalPrice != null && salePrice != null) {
                        // Mostro il gioco se il prezzo scontato risulta strettamente minore del prezzo originale
                        // O se il gioco e' gratuito (anche se pure originariamente era gratuito, FREE IS FREE)
                        salePrice < normalPrice || (normalPrice == 0f && salePrice == 0f)
                    } else {
                        // In qualsiasi altro caso non li vogliamo
                        false
                    }
                }
                Log.d("HomeViewModel", "Fetched from repo: ${fetchedDealsFromRepo.size}, Filtered to: ${newDeals.size} deals (after discount check)")


                // Salvo nella cache
                newDeals.forEach { deal ->
                    deal.dealID?.let { originalId ->
                        try {
                            val decodedId = URLDecoder.decode(originalId, StandardCharsets.UTF_8.name())
                            allLoadedDealsCache[decodedId] = deal
                        } catch (e: Exception) {
                            Log.e("HomeViewModelCache", "Error decoding dealID for caching: $originalId", e)
                            allLoadedDealsCache[originalId] = deal
                        }
                    }
                }

                // Aggiungo le nuove offerte alla lista attuale
                _deals.update { currentDeals ->
                    if (isNewSearch) newDeals else currentDeals + newDeals
                }

                // Il risultato dell'API senza la paginazione (limite 20 elementi per caricamento)
                if (fetchedDealsFromRepo.isNotEmpty()) {
                    currentPage++
                }

                // Vediamo se ci sono altri risultati che potremmo caricare
                canLoadMore = fetchedDealsFromRepo.size == PAGE_SIZE

                Log.d("HomeViewModel", "Current _deals count: ${_deals.value.size}, Total cached: ${allLoadedDealsCache.size}")

            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error fetching deals: ${e.message}", e)
                _errorMessage.value = "Failed to fetch deals: ${e.message}"
            } finally {
                if (isNewSearch) _isLoading.value = false else _isLoadingMore.value = false
            }
        }
    }

    // Se l'utente lo richiede, carico altri 20 elementi Deal
    fun loadMoreDeals() {
        if (!isLoading.value && !isLoadingMore.value && canLoadMore) {
            fetchDeals(isNewSearch = false)
        }
    }

    // Seleziono la Deal per mostrarne i dettagli in DealDetailScreen
    fun selectDealById(decodedDealId: String?) {
        Log.d("HomeViewModelLifecycle", "selectDealById called on VM instance $viewModelId. Current cache size: ${allLoadedDealsCache.size}")
        if (decodedDealId == null) {
            if (_selectedDeal.value != null) _selectedDeal.value = null
            _isLoadingSelectedDeal.value = false
            return
        }

        // Se gia selezionato non faccio niente, altrimenti decodo l'ID e cerco nella cache
        val currentSelectedDecodedId = _selectedDeal.value?.dealID?.let {
            try { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) } catch (e: Exception) { null }
        }
        if (currentSelectedDecodedId == decodedDealId && _selectedDeal.value != null) {
            Log.d("HomeViewModelSelect", "Deal $decodedDealId already selected and matches. No change.")
            _isLoadingSelectedDeal.value = false
            return
        }

        Log.d("HomeViewModelSelect", "Attempting to select deal with DECODED key: $decodedDealId")
        _isLoadingSelectedDeal.value = true

        val cachedDeal = allLoadedDealsCache[decodedDealId]

        if (cachedDeal != null) {
            Log.d("HomeViewModelSelect", "Deal $decodedDealId found in cache. Setting as selected.")
            _selectedDeal.value = cachedDeal
        } else {
            Log.w("HomeViewModelSelect", "Deal $decodedDealId NOT found in cache. Cached keys (first 5): ${allLoadedDealsCache.keys.take(5)}")
            _selectedDeal.value = null
        }
        _isLoadingSelectedDeal.value = false
    }

    // Resetto Deal selezionata
    fun clearSelectedDeal() {
        _selectedDeal.value = null
        _isLoadingSelectedDeal.value = false
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("HomeViewModelLifecycle", "ViewModel instance $viewModelId cleared.")
    }
}
