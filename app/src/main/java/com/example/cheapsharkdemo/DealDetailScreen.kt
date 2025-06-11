package com.example.cheapsharkdemo

import kotlin.text.isNotEmpty
import kotlin.text.replace
import kotlin.text.toDoubleOrNull
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import androidx.core.net.toUri

// Ho Dichiarato manualmente gli store digitali solo per la demo usando gli storeID, potremmo prenderli anche con l'API penso
val storeNameMap = mapOf(
    "1" to "Steam", "2" to "GamersGate", "3" to "GreenManGaming", "7" to "GOG",
    "8" to "Origin", "11" to "Humble Store", "13" to "Uplay", "15" to "Fanatical",
    "21" to "WinGameStore", "23" to "GameBillet", "24" to "Voidu", "25" to "Epic Games Store",
    "27" to "Gamesplanet", "28" to "Gamesload", "29" to "2Game", "30" to "IndieGala",
    "31" to "Blizzard Shop", "32" to "AllYouPlay", "33" to "DLGamer", "34" to "Noctremedia",
    "35" to "DreamGame"
)

// Definizione della schermata di dettaglio dell'oggetto Deal scelto
// La schermata contiene:
// - Un Pulsante "Back" che permette di tornare alla schermata precedente (Sempre HomeScreen)
// - Un'immagine del gioco recuperata tramite API (spesso sgranata rip) che copre la parte superiore dello schermo
// - Il titolo del gioco
// - La percentuale dello sconto sul prezzo originale
// - Lo store di provenienza dello sconto e il rating del gioco su Steam
// - Un lungo testo PlaceHolder che mostrerebbe la descrizione del gioco (principalmente per dimostrare lo scrolling della schermata)
// - Un tasto che reindirizza al link del sito con l'offerta (esce dall'app) e affianco il prezzo originale e scontato, uno sopra l'altro
@Composable
fun DealDetailScreen(
    navController: NavController,
    dealIdFromNav: String?,
    homeViewModel: HomeViewModel
) {
    val deal by homeViewModel.selectedDeal.collectAsState()
    val isLoadingDeal by homeViewModel.isLoadingSelectedDeal.collectAsState()
    val context = LocalContext.current

    // Recupero l'ID del Deal selezionato in modo da caricare correttamente il resto delle info della schermata
    LaunchedEffect(dealIdFromNav) {
        if (dealIdFromNav != null) {
            val currentSelectedDecodedId = homeViewModel.selectedDeal.value?.dealID?.let {
                try { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) } catch (e: Exception) { null }
            }
            if (homeViewModel.selectedDeal.value == null || currentSelectedDecodedId != dealIdFromNav) {
                Log.d("DealDetailScreen", "LaunchedEffect: Selecting deal with ID from Nav: $dealIdFromNav")
                homeViewModel.selectDealById(URLDecoder.decode(dealIdFromNav, StandardCharsets.UTF_8.name()))
            }
        }
    }

    // Quando chiudo la schermata resetto l'oggetto Deal selezionato
    DisposableEffect(Unit) {
        onDispose {
            Log.d("DealDetailScreen", "onDispose: Clearing selected deal.")
            homeViewModel.clearSelectedDeal()
        }
    }

    Scaffold { scaffoldPadding ->
        when {
            // Mentre carica mostro icona con messaggio
            isLoadingDeal -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(scaffoldPadding),
                        contentAlignment = Alignment.TopCenter
                ) {
                    CircularProgressIndicator()
                    Text("Loading deal details...", modifier = Modifier.padding(top = 8.dp))
                }
            }
            // Se non trova la Deal (errore nel passaggio da una schermata all'altro) mostra codice di errore
            deal == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(WindowInsets.systemBars.asPaddingValues()),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Deal not found (ID: ${dealIdFromNav ?: "Unknown"}).")
                }
            }
            // Mostriamo la schermata correttamente con tutte le informazioni inserite
            else -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Metto l'immagine nella parte alta dello schermo, anche sotto il bottone Back
                        AsyncImage(
                            model = deal!!.thumb?.replace("capsule_184x69", "header"),
                            contentDescription = deal!!.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .consumeWindowInsets(WindowInsets.statusBars),
                            contentScale = ContentScale.Crop
                        )

                        // Colonna con tutte le informazioni testuali della schermata
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .padding(horizontal = 16.dp)
                                // Questo padding finale semplicemente permette che i bottoni abbiano
                                // il loro spazio giusto sotto tutto il testo,
                                // se la schermata viene scrollata completamente
                                .padding(bottom = 95.dp)
                        ) {
                            // Titolo del gioco
                            Text(
                                text = deal!!.title ?: "Unknown Game",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // Sconto espresso in percentuale
                            deal!!.savings?.toDoubleOrNull()?.let { savingsPercentage ->
                                if (savingsPercentage > 0) {
                                    Text(
                                        text = "${savingsPercentage.toInt()}% OFF!!",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                }
                            }

                            // Store digitale in cui si trova lo sconto e valutazione su Steam del gioco
                            val storeName = storeNameMap[deal!!.storeID] ?: "Unknown Store (ID: ${deal!!.storeID})"
                            InfoRow(label = "Store:", value = storeName)

                            deal!!.metaCriticScore?.let {
                                if (it.isNotEmpty() && it != "0") {
                                    InfoRow(label = "Metacritic Score:", value = it)
                                }
                            }
                            deal!!.steamRatingText?.let {
                                if (it.isNotEmpty()) {
                                    InfoRow(label = "Steam Rating:", value = "$it (${deal!!.steamRatingPercent}%)")
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // Testo PlaceHolder lunghissimo
                            Text(
                                text = "About This Game:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                Text(
                                    text = """
                                    Lorem ipsum dolor sit amet, consectetur adipiscing elit. 
                                    Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. 
                                    Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. 
                                    Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. 
                                    Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.

                                    Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. 
                                    Vestibulum tortor quam, feugiat vitae, ultricies eget, tempor sit amet, ante. 
                                    Donec eu libero sit amet quam egestas semper. Aenean ultricies mi vitae est. 
                                    Mauris placerat eleifend leo. Quisque sit amet est et sapien ullamcorper pharetra. 
                                    Vestibulum erat wisi, condimentum sed, commodo vitae, ornare sit amet, wisi. 
                                    Aenean fermentum, elit eget tincidunt condimentum, eros ipsum rutrum orci, sagittis tempus lacus enim ac dui. 
                                    Donec non enim in turpis pulvinar facilisis. Ut felis. Praesent dapibus, neque id cursus faucibus, tortor neque egestas augue, eu vulputate magna eros eu erat. 
                                    Aliquam erat volutpat. Nam dui mi, tincidunt quis, accumsan porttitor, facilisis luctus, metus.

                                    Phasellus ultrices nulla quis nibh. Quisque a lectus. Donec consectetuer ligula vulputate sem tristique cursus. 
                                    Nam nec ante. Sed lacinia, urna non tincidunt mattis, tortor neque adipiscing diam, a cursus ipsum ante quis turpis. 
                                    Nulla facilisi. Ut fringilla. Suspendisse potenti. Nunc feugiat mi a tellus consequat imperdiet. 
                                    Vestibulum sapien. Proin quam. Etiam ultrices. Suspendisse in justo eu magna luctus suscipit. 
                                    Sed lectus. Integer euismod lacus luctus magna. Quisque cursus, metus vitae pharetra auctor, sem massa mattis sem, at interdum magna augue eget diam. 
                                    Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Morbi lacinia molestie dui. 
                                    Praesent blandit dolor. Sed non quam. In vel mi sit amet augue congue elementum. Morbi in ipsum sit amet pede facilisis laoreet. 
                                    Donec lacus nunc, viverra nec, blandit vel, egestas et, augue. Vestibulum tincidunt malesuada tellus. 
                                    Ut ultrices ultrices enim. Curabitur sit amet mauris. Morbi in dui quis est pulvinar ullamcorper. 
                                    Nulla facilisi. Integer lacinia sollicitudin massa. Cras metus. Sed aliquet risus a tortor. 
                                    Integer id quam. Morbi mi. Quisque nisl felis, venenatis tristique, dignissim in, ultrices sit amet, augue. 
                                    Proin sodales libero eget ante. Nulla quam. Aenean laoreet. Vestibulum nisi lectus, commodo ac, facilisis ac, ultricies eu, pede. 
                                    Ut orci risus, accumsan porttitor, cursus quis, aliquet eget, justo. 
                                    Sed pretium blandit orci. Ut eu diam at pede suscipit sodales. Aenean lectus elit, fermentum non, convallis id, sagittis at, neque. 
                                    Nullam mauris orci, aliquet et, iaculis et, viverra vitae, ligula. Nulla ut felis in purus aliquam imperdiet. 
                                    Maecenas aliquet mollis lectus. Vivamus consectetuer risus et tortor. Lorem ipsum dolor sit amet, consectetur adipiscing elit. 
                                    Integer nec odio. Praesent libero. Sed cursus ante dapibus diam. Sed nisi. 
                                    Nulla quis sem at nibh elementum imperdiet. Duis sagittis ipsum. Praesent mauris. Fusce nec tellus sed augue semper porta. 
                                    Mauris massa. Vestibulum lacinia arcu eget nulla. 
                                    Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos.
                                    """.trimIndent(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    // Bottone per tornare alla schermata precedente (HomeScreen), visualizzato sopra l'immagine del gioco
                    Button(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 16.dp, top = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 1f),
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                            Text("Back")
                        }
                    }

                    // Riga che contiene il bottone "Buy Now" e i prezzi (originale sbarrato e scontato)
                    // Il bottone occupa circa 2/3 dello spazio, i prezzi il restante 1/3
                    // Ho leggermente modificato il design rispetto all'esempio di Figma per aumentarne la visibilita
                    // Nel caso in cui sosti sopra il testo
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Url per portare l'utente al sito con l'offerta
                        val dealUrl = "https://www.cheapshark.com/redirect?dealID=${deal!!.dealID}"
                        // Bottone che porta all'offerta
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, dealUrl.toUri())
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Log.e("DealDetailScreen", "Could not launch URL $dealUrl", e)
                                }
                            },
                            modifier = Modifier
                                .weight(2f)
                                .padding(end = 2.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "Buy Now",
                                style = MaterialTheme.typography.displaySmall
                            )
                        }

                        // Prezzo originale e scontato
                        DealPricesDisplay(
                            normalPrice = deal?.normalPrice,
                            salePrice = deal?.salePrice,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

// Semplice elemento grafico che mostra il prezzo originale, sbarrato e leggermente piu' piccolo
// ed il prezzo scontato, piu grande e di un colore che lo evidenzi
// Il tutto racchiuso in un box che garantisce la leggibilita'
@Composable
fun DealPricesDisplay(
    normalPrice: String?,
    salePrice: String?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 1f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Prezzo originale
            if (normalPrice != null && normalPrice != salePrice) {
                Text(
                    text = "$$normalPrice",
                    style = MaterialTheme.typography.bodySmall,
                    textDecoration = TextDecoration.LineThrough,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            // Prezzo scontato
            if (salePrice != null) {
                Text(
                    text = "$$salePrice",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// Semplice riga di testo con label e valore
// Usato come struttura per alcune informazioni testuali dello schermo
@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(140.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}