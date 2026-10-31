package co.luka.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.luka.ui.theme.LocalLukaColors
import co.luka.ui.theme.LukaTheme
import co.luka.ui.theme.Space
import co.luka.ui.theme.lukaIndigoShadow

private val ButtonShape = RoundedCornerShape(16.dp)

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val haptic = LocalHapticFeedback.current
    val canClick = enabled && !loading
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && canClick) 0.97f else 1f, tween(90), label = "primaryPress")
    val shape = ButtonShape
    val brush = Brush.horizontalGradient(
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
    )
    val face = if (canClick) {
        Modifier.lukaIndigoShadow(shape).clip(shape).background(brush)
    } else {
        Modifier.clip(shape).background(MaterialTheme.colorScheme.surface)
    }
    Box(
        modifier
            .scale(scale)
            .height(52.dp)
            .then(face)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = Color.White),
                enabled = canClick,
                role = Role.Button,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
            )
            .semantics { if (loading) contentDescription = "Cargando" },
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                text = text,
                color = if (enabled) MaterialTheme.colorScheme.onPrimary else LocalLukaColors.current.muted,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) 0.97f else 1f, tween(90), label = "secondaryPress")
    Box(
        modifier
            .scale(scale)
            .height(52.dp)
            .clip(ButtonShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = MaterialTheme.colorScheme.primary),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) MaterialTheme.colorScheme.primary else LocalLukaColors.current.muted,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Preview(name = "Botones con datos", showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 360)
@Composable
private fun ButtonsDataPreview() {
    LukaTheme("light") {
        Column(Modifier.padding(Space.s16), verticalArrangement = Arrangement.spacedBy(Space.s12)) {
            PrimaryButton("Guardar", onClick = {}, modifier = Modifier.fillMaxWidth())
            SecondaryButton("Activar captura automática", onClick = {}, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Preview(name = "Botones vacíos", showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 360)
@Composable
private fun ButtonsEmptyPreview() {
    LukaTheme("light") {
        Column(Modifier.padding(Space.s16), verticalArrangement = Arrangement.spacedBy(Space.s12)) {
            PrimaryButton("Guardar", onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth())
            PrimaryButton("Guardar", onClick = {}, loading = true, modifier = Modifier.fillMaxWidth())
            SecondaryButton("Activar captura automática", onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth())
        }
    }
}
