package com.example.cheapsharkdemo

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.cheapsharkdemo.model.Deal
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.UUID

// La schermata principale della demo
// Contiene:
// - Una barra di ricerca in cui gli utenti possono scremare i giochi nella lista via nome
// - Un pulsante per filtrare gli elementi della lista seguendo lo Store digitale di appartenenza
// - Una lista di elementi ognuno rappresentante un'offerta su un gioco.
// Ogni elemento mostra:
// - Un'immagine del gioco (scaricata, spesso e volentieri un po' sgranata in questa demo)
// - Il nome del gioco
// - Lo Store digitale di appartenenza (dove l'offerta si trova)
// - Il prezzo originale (sbarrato)
// - Il prezzo scontato
// Ogni elemento puo' essere cliccato e apre una schermata con piu' dettagli al riguardo
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    navController: NavController
) {
    val deals by homeViewModel.deals.collectAsState()
    val isLoading by homeViewModel.isLoading.collectAsState()
    val isLoadingMore by homeViewModel.isLoadingMore.collectAsState()
    val errorMessage by homeViewModel.errorMessage.collectAsState()
    val searchQuery by homeViewModel.searchQuery.collectAsState()
    val selectedStoreId by homeViewModel.selectedStoreId.collectAsState()
    val canLoadMore by remember {
        derivedStateOf { homeViewModel.canLoadMore }
    }

    // Theming per il colore del pulsante del filtro tramite Store digitale
    val isFilterActive = selectedStoreId != null
    val iconColor = if (isFilterActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
    val containerColor = if(isFilterActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant


    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Barra di ricerca dei giochi in offerta tramite nome
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { homeViewModel.onSearchQueryChanged(it) },
                label = { Text("Search Games") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search Icon") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { homeViewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear Search")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(8.dp))

            // Bottone per filtrare via Store digitale
            FilledIconButton(
                onClick = { homeViewModel.onStoreFilterClicked() },
                modifier = Modifier
                    .height(TextFieldDefaults.MinHeight)
                    .aspectRatio(1f),
                shape = RoundedCornerShape(16.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = containerColor,
                    contentColor = iconColor
                )
            ) {
                Icon(
                    Icons.Filled.FilterList,
                    contentDescription = "Filter by Store"
                )
            }
        }

        // Se ho un filtro attivo, lo mostro
        // Da questo elemento posso anche rimuovere il filtro attivo
        // e ritornare alla lista di partenza
        selectedStoreId?.let { storeId ->
            storeNameMap[storeId]?.let { name ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.shapes.small
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Filtered by: $name",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    IconButton(onClick = { homeViewModel.clearStoreFilter() }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear filter", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }


        // Dialog per il filtro tramite Store digitale
        if (homeViewModel.showStoreFilterDialog) {
            StoreFilterDialog(
                availableStores = homeViewModel.availableStores,
                onDismiss = { homeViewModel.onDismissStoreFilterDialog() },
                onStoreSelected = { storeId ->
                    homeViewModel.onStoreSelected(storeId)
                },
                currentFilter = selectedStoreId
            )
        }

        // Box principale che incapsula il contenuto della schermata
        Box(modifier = Modifier
            .weight(1f)
            .fillMaxWidth()) {
            when {
                // Mentre carichiamo gli elementi della lista mostriamo un'animazione
                // di caricamento in mezzo allo schermo
                isLoading && deals.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                // Mostro errore se la lista risulta vuota
                errorMessage != null && deals.isEmpty() -> {
                    Text(
                        text = "Error: $errorMessage",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
                // Se non ci sono offerte per il gioco inserito nella barra di ricerca,
                // Lo comunico all'utente con una stringa
                deals.isEmpty() && searchQuery.isNotEmpty() -> {
                    Text(
                        text = "No deals found for \"$searchQuery\".",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
                // Non ci sono offerte nella maniera piu' assoluta per nessun gioco,
                // Sostanzialmente Nintendo
                deals.isEmpty() -> {
                    Text(
                        text = "No deals found.",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
                else -> {
                    // Se ho gia' una lista e ho fatto una ricerca (o ho rimosso un filtro)
                    // Triggero una ricomposizione della lista e lo mostro a schermo
                    // all'utente mostrandogli una barra di caricamento orizzontale
                    if (isLoading || isLoadingMore) {
                        LinearProgressIndicator(modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter))
                    }
                    // L'effettiva lista di elementi visualizzata a schermo
                    DealsList(
                        deals = deals,
                        isLoadingMore = isLoadingMore,
                        canLoadMore = canLoadMore,
                        homeViewModel = homeViewModel,
                        navController = navController,
                        // Se premo il pulsante devo caricare altri 20 elementi
                        onLoadMore = { homeViewModel.loadMoreDeals() }
                    )
                }
            }
            // Se c'e' errore anche se la lista non e' vuota, lo mostro in basso
            if (errorMessage != null && deals.isNotEmpty()) {
                Text(
                    text = "Error: $errorMessage",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(8.dp)
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(8.dp)
                )
            }
        }
    }
}

// Definisco il filtro tramite Store digitale
@Composable
fun StoreFilterDialog(
    availableStores: Map<String, String>,
    onDismiss: () -> Unit,
    onStoreSelected: (String?) -> Unit,
    currentFilter: String?
) {
    // La finestra che viene effettivamente aperta al premere del pulsante del filtro
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter by Store") },
        text = {
            LazyColumn {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStoreSelected(null) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentFilter == null,
                            onClick = { onStoreSelected(null) }
                        )
                        Text("All Stores (No Filter)")
                    }
                }

                // La lista degli Store digitali disponibili per il filtro
                availableStores.toList().sortedBy { it.second }.forEach { (storeId, storeName) -> // Sort by name
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onStoreSelected(storeId) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentFilter == storeId,
                                onClick = { onStoreSelected(storeId) }
                            )
                            Text(storeName)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

// Definisco la lista di elementi Deal
// Oltre alla lista, e' presente un pulsante
// che permette di caricare altri 20 elementi
// Le pagine sono da 20 elementi secondo la richiesta dell'esercizio
@Composable
fun DealsList(
    deals: List<Deal>,
    isLoadingMore: Boolean,
    canLoadMore: Boolean,
    homeViewModel: HomeViewModel,
    navController: NavController,
    onLoadMore: () -> Unit
) {
    // La lista ha due elementi per riga per seguire con piu' precisione l'esempio
    // fornito su Figma
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = if (canLoadMore || isLoadingMore) 0.dp else 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = deals,
            key = { deal -> deal.dealID ?: deal.internalName ?: deal.title ?: UUID.randomUUID().toString() }
        ) { dealData ->
            DealItem(
                deal = dealData,
                onDealClick = { originalClickedDealId ->
                    if (originalClickedDealId != null) {
                        try {
                            val decodedIdForViewModel = URLDecoder.decode(
                                originalClickedDealId,
                                StandardCharsets.UTF_8.name()
                            )
                            homeViewModel.selectDealById(decodedIdForViewModel)
                            navController.navigate(
                                Screen.DealDetail.createRoute(
                                    originalClickedDealId
                                )
                            )
                        } catch (e: Exception) {
                            Log.e(
                                "DealItemClick",
                                "Error in onDealClick with ID $originalClickedDealId",
                                e
                            )
                        }
                    }
                },
                // Animazione per quando gli elementi vengono aggiunti alla lista
                // (solo la prima volta, non ogni volta che appaiono a schermo)
                modifier = Modifier.animateItem(
                    fadeInSpec = null,
                    fadeOutSpec = null,
                    placementSpec = tween(durationMillis = 300)
                ),
                homeViewModel = homeViewModel
            )
        }

        // In questo spazio mettiamo o il pulsante per caricare altri 20 elementi
        // oppure una piccola circular progress bar in attesa che si carichino
        if (canLoadMore || isLoadingMore) {
            item(
                span = { GridItemSpan(2) },
                key = "load_more_footer"
            ) {
                Modifier
                    .fillMaxWidth()
                Row(
                    modifier = Modifier
                        .animateItem(
                            fadeInSpec = null,
                            fadeOutSpec = null,
                            placementSpec = tween(durationMillis = 300)
                        )
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLoadingMore) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    } else {
                        Button(onClick = onLoadMore) {
                            Text("Load More Deals")
                        }
                    }
                }
            }
        }
    }
}

// Definisco gli effettivi elementi della lista,
// Ognuno rappresenta un'offerta valida (prezzo originale != prezzo scontato, arrivano gia' filtrati)
// Ogni DealItem mostra:
// - Immagine del gioco
// - Nome del gioco
// - Store digitale di appartenenza
// - Prezzo originale (sbarrato)
// - Prezzo scontato
// Ogni elemento e' cliccabile e porta ad una schermata dedicata con maggiori informazioni sull'offerta
@Composable
fun DealItem(
    deal: Deal,
    onDealClick: (String?) -> Unit,
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel
) {
    val hasAnimated = homeViewModel.hasPlayedEntryAnimation(deal.dealID)
    val animationProgress = remember { Animatable(if (hasAnimated) 1f else 0f) } // Initial state

    // Per gestire l'animazione dell'elemento quando viene aggiunto alla lista
    LaunchedEffect(key1 = deal.dealID, key2 = hasAnimated) {
        if (!hasAnimated) {
            animationProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 400,
                    delayMillis = 50
                )
            )
            // Mi salvo che ho eseguito l'animazione, cosi da non ripeterla di continuo ogni volta
            // che LazyList vuole farlo vedere di nuovo a schermo
            homeViewModel.markAsPlayedEntryAnimation(deal.dealID)
        } else {
            // Mi assicuro anche in caso di recomposizione che l'elemento sia subito al suo posto
            animationProgress.snapTo(1f)
        }
    }

    Card(
        modifier = modifier
            .graphicsLayer(
                alpha = animationProgress.value,
                translationY = (1f - animationProgress.value) * 50f
            )
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable { onDealClick(deal.dealID) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            AsyncImage(
                model = deal.thumb,
                contentDescription = deal.title ?: "Game thumbnail",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                contentScale = ContentScale.Crop
            )

            Text(
                text = deal.title ?: "Unknown Game",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .padding(top = 8.dp)
            )

            val storeName = storeNameMap[deal.storeID] ?: "Unknown Store"
            Text(
                text = storeName,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .padding(top = 2.dp, bottom = 4.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$${deal.normalPrice}",
                    fontSize = 12.sp,
                    textDecoration = TextDecoration.LineThrough,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    text = "$${deal.salePrice}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}