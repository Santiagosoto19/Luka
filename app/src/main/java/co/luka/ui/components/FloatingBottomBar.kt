package co.luka.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import co.luka.ui.theme.LocalLukaColors
import co.luka.ui.theme.LukaTheme
import co.luka.ui.theme.Space
import co.luka.ui.theme.lukaSoftShadow
import kotlin.math.roundToInt

data class LukaDestination(
    val key: String,
    val label: String,
    val filled: ImageVector,
    val outlined: ImageVector,
)

@Composable
fun FloatingBottomBar(
    destinations: List<LukaDestination>,
    selectedKey: String,
    onSelect: (LukaDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(28.dp)
    val density = LocalDensity.current
    var leftPx by remember { mutableFloatStateOf(0f) }
    var widthPx by remember { mutableFloatStateOf(0f) }
    var seen by remember { mutableStateOf(false) }
    val left = remember { Animatable(0f) }
    val width = remember { Animatable(0f) }
    LaunchedEffect(leftPx, widthPx) {
        if (widthPx <= 0f) return@LaunchedEffect
        if (!seen) {
            left.snapTo(leftPx)
            width.snapTo(widthPx)
            seen = true
        } else {
            left.animateTo(leftPx, tween(280))
            width.animateTo(widthPx, tween(280))
        }
    }
    Surface(
        modifier = modifier
            .padding(horizontal = Space.s16)
            .padding(bottom = Space.s8)
            .navigationBarsPadding()
            .fillMaxWidth()
            .height(68.dp)
            .lukaSoftShadow(shape),
        shape = shape,
        color = MaterialTheme.colorScheme.background,
        border = androidx.compose.foundation.BorderStroke(1.dp, LocalLukaColors.current.border),
        shadowElevation = 0.dp,
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = Space.s8, vertical = Space.s8)) {
            if (width.value > 0f) {
                Box(
                    Modifier
                        .offset { IntOffset(left.value.roundToInt(), 0) }
                        .fillMaxHeight()
                        .widthFromPx(width.value, density)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(22.dp)),
                )
            }
            Row(
                Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                destinations.forEach { destination ->
                    val active = destination.key == selectedKey
                    Column(
                        Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .onGloballyPositioned { coords ->
                                if (destination.key == selectedKey) {
                                    val bounds = coords.boundsInParent()
                                    leftPx = bounds.left
                                    widthPx = bounds.width
                                }
                            }
                            .clickable(role = Role.Tab, onClick = { onSelect(destination) })
                            .semantics { selected = active }
                            .padding(horizontal = Space.s8),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = if (active) destination.filled else destination.outlined,
                            contentDescription = destination.label,
                            tint = if (active) MaterialTheme.colorScheme.primary else LocalLukaColors.current.muted,
                        )
                        if (active) {
                            Text(
                                destination.label,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.widthFromPx(px: Float, density: androidx.compose.ui.unit.Density): Modifier {
    val dp = with(density) { px.toDp() }
    return this.width(dp)
}

fun lukaDestinations() = listOf(
    LukaDestination("home", "Inicio", Icons.Filled.Home, Icons.Outlined.Home),
    LukaDestination("movements", "Movimientos", Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong),
    LukaDestination("stats", "Estadísticas", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    LukaDestination("wallets", "Carteras", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    LukaDestination("settings", "Ajustes", Icons.Filled.Settings, Icons.Outlined.Settings),
)

@Preview(name = "Barra inicio", showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 360)
@Composable
private fun BarHomePreview() {
    LukaTheme("light") {
        Box(Modifier.padding(vertical = Space.s16)) {
            FloatingBottomBar(lukaDestinations(), "home", onSelect = {})
        }
    }
}

@Preview(name = "Barra sin selección", showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 360)
@Composable
private fun BarEmptyPreview() {
    LukaTheme("light") {
        Box(Modifier.padding(vertical = Space.s16)) {
            FloatingBottomBar(lukaDestinations(), "", onSelect = {})
        }
    }
}
