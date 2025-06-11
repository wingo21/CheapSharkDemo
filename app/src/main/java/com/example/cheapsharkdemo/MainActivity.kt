package com.example.cheapsharkdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.cheapsharkdemo.ui.theme.CheapSharkDemoTheme

// Definisco le varie schermate presenti nell'applicazione
// Ogni schermata ha un label (un nome) e un'icona
// (che la rappresenta nella barra di navigazione)
sealed class Screen(val route: String, val label: String? = null, val icon: ImageVector? = null) {
    data object Home : Screen("home", "Home", Icons.Filled.Home)
    data object Wishlist : Screen("wishlist", "Wishlist", Icons.Filled.Favorite)
    data object Cart : Screen("cart", "Cart", Icons.Filled.ShoppingCart)
    data object Profile : Screen("profile", "Profile", Icons.Filled.Person)
    data object DealDetail : Screen("deal_detail/{dealId}", "Deal Details") {
        fun createRoute(dealId: String) = "deal_detail/$dealId"
    }
}

// Definisco gli elementi della barra di navigazione
val bottomNavItems = listOf(
    Screen.Home,
    Screen.Wishlist,
    Screen.Cart,
    Screen.Profile
)

// Queste tre schermate sono qui solo perche' in questa demo sono
// dei semplici placeholder, in un'app vera avrebbero ognuna il loro file come HomeScreen
@Composable
fun WishlistScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Wishlist Screen")
    }
}

@Composable
fun CartScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Cart Screen")
    }
}

@Composable
fun ProfileScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Profile Screen")
    }
}


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CheapSharkDemoTheme {
                AppNavigation()
            }
        }
    }
}

// Definisco la navigazione tra le schermate
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    // Dichiaro una singola istanza del ViewModel che poi passero' alle
    // schermate che ne fanno utilizzo, in questo modo la lista di elementi
    // viene condivisa correttamente e passata alla schermata di dettaglio della Deal
    val activityScopedHomeViewModel: HomeViewModel = viewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity
    )
    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val showBottomBar = bottomNavItems.any { it.route == currentDestination?.route }

            // Mostro la bottom bar solo nelle schermate top-level.
            // Quella dei dettagli della Deal non la necessita
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon!!, contentDescription = screen.label) },
                            label = { Text(screen.label!!) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Alla Home passo l'istanza del ViewModel precedentemente definita
            composable(Screen.Home.route) {
                HomeScreen(
                    homeViewModel = activityScopedHomeViewModel,
                    navController = navController
                )
            }

            // Schermate PlaceHolder
            composable(Screen.Wishlist.route) { WishlistScreen() }
            composable(Screen.Cart.route) { CartScreen() }
            composable(Screen.Profile.route) { ProfileScreen() }

            // Schermata aperta quando clicco su una Deal nella HomeScreen
            // Anche qui il ViewModel di riferimento rimane il medesimo
            composable(
                route = Screen.DealDetail.route,
                arguments = listOf(navArgument("dealId") { type = NavType.StringType })
            ) { backStackEntry ->
                val dealId = backStackEntry.arguments?.getString("dealId")
                DealDetailScreen(
                    navController = navController,
                    dealIdFromNav = dealId,
                    homeViewModel = activityScopedHomeViewModel
                )
            }
        }
    }
}