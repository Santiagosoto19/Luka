package co.luka.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.luka.ui.theme.LocalLukaColors
import co.luka.ui.theme.LukaTheme
import co.luka.ui.theme.Space
import kotlinx.coroutines.isActive

@Composable
fun EmptyState(
    onAddExpense: () -> Unit,
    showNotificationAction: Boolean,
    onEnableNotifications: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().padding(vertical = Space.s16),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.s12),
    ) {
        EmptyIllustration(Modifier.fillMaxWidth().height(140.dp))
        Text("Aún no hay movimientos", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            "Registra un gasto y aparecerá en este mes.",
            color = LocalLukaColors.current.muted,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        PrimaryButton("Agregar mi primer gasto", onClick = onAddExpense, modifier = Modifier.fillMaxWidth())
        if (showNotificationAction) {
            SecondaryButton("Activar captura automática", onClick = onEnableNotifications, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun EmptyIllustration(modifier: Modifier = Modifier) {
    val colors = LocalLukaColors.current
    val primary = MaterialTheme.colorScheme.primary
    val violet = MaterialTheme.colorScheme.secondary
    val paper = MaterialTheme.colorScheme.onPrimary
    val animate = animationsEnabled()
    val phase = remember { Animatable(0f) }
    LaunchedEffect(animate) {
        if (!animate) {
            phase.snapTo(0f)
            return@LaunchedEffect
        }
        while (isActive) {
            phase.snapTo(0f)
            phase.animateTo(1f, tween(3200, easing = LinearEasing))
        }
    }
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        if (animate) {
            val shift = (phase.value - 0.5f) * 24f
            val ribbon = Path().apply {
                moveTo(w * 0.08f, h * 0.72f + shift)
                quadraticTo(w * 0.32f, h * 0.18f, w * 0.55f, h * 0.46f)
                quadraticTo(w * 0.78f, h * 0.74f - shift, w * 0.96f, h * 0.28f)
            }
            drawPath(
                ribbon,
                violet.copy(alpha = 0.55f),
                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        drawRoundRect(
            color = primary,
            topLeft = Offset(w * 0.28f, h * 0.42f),
            size = Size(w * 0.44f, h * 0.34f),
            cornerRadius = CornerRadius(28f, 28f),
        )
        drawRoundRect(
            color = violet,
            topLeft = Offset(w * 0.28f, h * 0.34f),
            size = Size(w * 0.44f, h * 0.16f),
            cornerRadius = CornerRadius(24f, 24f),
        )
        drawCircle(colors.income, radius = h * 0.09f, center = Offset(w * 0.66f, h * 0.36f))
        drawRoundRect(
            color = paper.copy(alpha = 0.85f),
            topLeft = Offset(w * 0.36f, h * 0.56f),
            size = Size(w * 0.22f, h * 0.035f),
            cornerRadius = CornerRadius(8f, 8f),
        )
    }
}

@Preview(name = "Vacío con captura", showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 360)
@Composable
private fun EmptyWithCapturePreview() {
    LukaTheme("light") {
        EmptyState(onAddExpense = {}, showNotificationAction = true, onEnableNotifications = {}, modifier = Modifier.padding(Space.s16))
    }
}

@Preview(name = "Vacío sin captura", showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 360)
@Composable
private fun EmptyQuietPreview() {
    LukaTheme("light") {
        EmptyState(onAddExpense = {}, showNotificationAction = false, onEnableNotifications = {}, modifier = Modifier.padding(Space.s16))
    }
}
