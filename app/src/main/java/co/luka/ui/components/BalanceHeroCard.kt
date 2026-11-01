package co.luka.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.luka.ui.theme.LukaTheme
import co.luka.ui.theme.Space
import co.luka.ui.theme.lukaIndigoShadow
import kotlin.math.abs

fun percentLabel(percent: Int): String {
    val arrow = if (percent < 0) "↓" else "↑"
    return "$arrow ${abs(percent)} %"
}

@Composable
fun BalanceHeroCard(
    label: String,
    balance: Long,
    deltaPercent: Int?,
    previousMonth: String,
    trend: List<Long>,
    hidden: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleHidden: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val onHero = MaterialTheme.colorScheme.onPrimary
    val shape = RoundedCornerShape(28.dp)
    val brush = Brush.linearGradient(
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
    )
    Box(
        modifier
            .fillMaxWidth()
            .lukaIndigoShadow(shape)
            .clip(shape)
            .background(brush),
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(
                color = onHero.copy(alpha = 0.16f),
                radius = size.width * 0.42f,
                center = Offset(size.width * 0.92f, size.height * 0.02f),
            )
            drawCircle(
                color = onHero.copy(alpha = 0.10f),
                radius = size.width * 0.28f,
                center = Offset(size.width * 0.02f, size.height * 0.98f),
            )
        }
        ShineOverlay(Modifier.matchParentSize())
        Column(Modifier.padding(Space.s20)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPrevious) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Mes anterior", tint = onHero)
                    }
                    Text(
                        text = label,
                        color = onHero,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    IconButton(onClick = onNext) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = "Mes siguiente", tint = onHero)
                    }
                }
                IconButton(onClick = onToggleHidden) {
                    Icon(
                        imageVector = if (hidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (hidden) "Mostrar montos" else "Ocultar montos",
                        tint = onHero,
                    )
                }
            }
            Crossfade(
                targetState = hidden,
                animationSpec = if (animationsEnabled()) tween(180) else snap(),
                label = "balanceHidden",
            ) { isHidden ->
                if (isHidden) {
                    Text(
                        text = "••••",
                        color = onHero,
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                        modifier = Modifier.semantics { contentDescription = "Saldo oculto" },
                    )
                } else {
                    CountingBalance(
                        amount = balance,
                        style = MaterialTheme.typography.displayMedium,
                        color = onHero,
                    )
                }
            }
            if (deltaPercent != null && previousMonth.isNotBlank()) {
                Text(
                    text = "${percentLabel(deltaPercent)} vs $previousMonth",
                    color = onHero,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .padding(top = Space.s12)
                        .background(onHero.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
                        .padding(horizontal = Space.s12, vertical = Space.s8),
                )
            }
            HeroSparkline(trend, Modifier.padding(top = Space.s16), onHero)
        }
    }
}

@Composable
private fun HeroSparkline(values: List<Long>, modifier: Modifier, color: androidx.compose.ui.graphics.Color) {
    val points = when (values.size) {
        0 -> emptyList()
        1 -> listOf(values[0], values[0])
        else -> values
    }
    Canvas(modifier.fillMaxWidth().height(52.dp)) {
        if (points.size < 2) {
            val y = size.height / 2f
            drawLine(
                color = color.copy(alpha = 0.8f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
                cap = StrokeCap.Round,
            )
            return@Canvas
        }
        val max = points.max().toFloat()
        val min = points.min().toFloat()
        val span = (max - min).takeIf { it != 0f } ?: 1f
        val path = Path()
        points.forEachIndexed { index, value ->
            val x = index / points.lastIndex.toFloat() * size.width
            val y = size.height - ((value - min) / span) * (size.height - 8f) - 4f
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Preview(name = "Hero con datos", showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 360)
@Composable
private fun HeroDataPreview() {
    LukaTheme("light") {
        Box(Modifier.padding(Space.s16)) {
            BalanceHeroCard(
                label = "octubre 2026",
                balance = 1_280_000,
                deltaPercent = 12,
                previousMonth = "septiembre",
                trend = listOf(0, 40_000, -10_000, 80_000, 30_000, 120_000, 90_000),
                hidden = false,
                onPrevious = {},
                onNext = {},
                onToggleHidden = {},
            )
        }
    }
}

@Preview(name = "Hero vacío", showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 360)
@Composable
private fun HeroEmptyPreview() {
    LukaTheme("light") {
        Box(Modifier.padding(Space.s16)) {
            BalanceHeroCard(
                label = "octubre 2026",
                balance = 0,
                deltaPercent = null,
                previousMonth = "septiembre",
                trend = emptyList(),
                hidden = false,
                onPrevious = {},
                onNext = {},
                onToggleHidden = {},
            )
        }
    }
}
