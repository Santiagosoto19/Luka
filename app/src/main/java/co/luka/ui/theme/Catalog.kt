package co.luka.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.ui.graphics.vector.ImageVector

val iconChoices = listOf(
    "restaurant", "bus", "home", "bolt", "health", "bag", "movie", "repeat", "payments", "store", "wallet", "spark", "category",
)

val swatches = listOf(
    "#5B5BF0", "#12B886", "#F0526B", "#F5A524", "#38BDF8", "#8B7CF6", "#FF8A5B", "#2DD4BF", "#EC4899", "#84CC16",
)

fun iconFor(key: String): ImageVector = when (key) {
    "restaurant" -> Icons.Filled.Restaurant
    "bus" -> Icons.Filled.DirectionsBus
    "home" -> Icons.Filled.Home
    "bolt" -> Icons.Filled.Bolt
    "health" -> Icons.Filled.Favorite
    "bag" -> Icons.Filled.ShoppingBag
    "movie" -> Icons.Filled.Movie
    "repeat" -> Icons.Filled.Subscriptions
    "payments" -> Icons.Filled.Payments
    "store" -> Icons.Filled.Storefront
    "wallet" -> Icons.Filled.AccountBalanceWallet
    "spark" -> Icons.Filled.AutoAwesome
    else -> Icons.Filled.Category
}
