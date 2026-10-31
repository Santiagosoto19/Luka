package co.luka.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

fun Modifier.lukaSoftShadow(shape: Shape): Modifier = shadow(
    elevation = 10.dp,
    shape = shape,
    clip = false,
    ambientColor = Ink.copy(alpha = 0.05f),
    spotColor = Ink.copy(alpha = 0.08f),
)

fun Modifier.lukaIndigoShadow(shape: Shape): Modifier = shadow(
    elevation = 22.dp,
    shape = shape,
    clip = false,
    ambientColor = PrimaryLight.copy(alpha = 0.22f),
    spotColor = PrimaryLight.copy(alpha = 0.36f),
)

@Composable
fun Modifier.lukaWhiteCard(shape: Shape): Modifier {
    val border = LocalLukaColors.current.border
    return this
        .lukaSoftShadow(shape)
        .clip(shape)
        .background(MaterialTheme.colorScheme.background)
        .border(1.dp, border, shape)
}
