package co.luka.ui.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectTapGestures
import co.luka.domain.money.CopMoney
import co.luka.ui.theme.LocalLukaColors
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

private val EaseOut = CubicBezierEasing(0f, 0f, 0.2f, 1f)

@Composable
fun animationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
}

@Composable
fun CountingBalance(
    amount: Long,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    val animate = animationsEnabled()
    val progress = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(amount, animate) {
        if (!animate) {
            progress.snapTo(1f)
        } else {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(durationMillis = 600, easing = EaseOut))
        }
    }
    val shown = (amount.toDouble() * progress.value).toLong()
    Text(
        text = CopMoney.format(shown),
        color = color,
        style = style.copy(
            fontFeatureSettings = "tnum",
            fontFamily = LocalLukaColors.current.displayFont,
            fontWeight = FontWeight.Bold,
        ),
        modifier = modifier,
    )
}

@Composable
fun MoneyText(amount: Long, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = CopMoney.format(amount),
        color = color,
        style = MaterialTheme.typography.titleMedium.copy(
            fontFeatureSettings = "tnum",
            fontFamily = LocalLukaColors.current.displayFont,
        ),
        modifier = modifier,
    )
}

@Composable
fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(vertical = 12.dp),
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text = text, style = MaterialTheme.typography.headlineMedium, modifier = modifier.padding(bottom = 8.dp))
}

@Composable
fun TrendLine(values: List<Long>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(36.dp)) {
        if (values.size < 2) return@Canvas
        val max = values.max().coerceAtLeast(1L).toFloat()
        val minValue = values.min().toFloat()
        val span = (max - minValue).takeIf { it > 0f } ?: 1f
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = index / values.lastIndex.toFloat() * size.width
            val y = size.height - ((value - minValue) / span) * (size.height - 4f)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    }
}

data class Slice(val id: Long?, val name: String, val amount: Long, val color: Color)

@Composable
fun ExpenseDonut(slices: List<Slice>, onSlice: (Long?) -> Unit, modifier: Modifier = Modifier) {
    val total = slices.sumOf { it.amount }
    if (total <= 0L) {
        EmptyHint("Registra un gasto para ver esta gráfica")
        return
    }
    val animate = animationsEnabled()
    val progress = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(slices, animate) {
        if (!animate) progress.snapTo(1f) else {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(700, easing = EaseOut))
        }
    }
    var touched by remember(slices) { mutableStateOf<Slice?>(null) }
    Canvas(
        modifier
            .fillMaxWidth()
            .height(220.dp)
            .pointerInput(slices) {
                detectTapGestures { offset ->
                    val hit = hitSlice(slices, offset, size)
                    touched = hit
                    if (hit != null) onSlice(hit.id)
                }
            },
    ) {
        val diameter = min(size.width, size.height) * 0.78f
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val stroke = Stroke(width = 28.dp.toPx(), cap = StrokeCap.Butt)
        var start = -90f
        slices.forEach { slice ->
            val sweep = slice.amount.toFloat() / total.toFloat() * 360f
            drawArc(
                color = slice.color,
                startAngle = start,
                sweepAngle = sweep * progress.value,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = stroke,
            )
            start += sweep
        }
    }
    touched?.let { slice ->
        Text("${slice.name} ${CopMoney.format(slice.amount)}", style = MaterialTheme.typography.bodyMedium)
    }
    slices.forEach { slice ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(10.dp).background(slice.color, CircleShape))
            Text(slice.name, modifier = Modifier.padding(start = 8.dp).weight(1f), style = MaterialTheme.typography.bodyMedium)
            MoneyText(slice.amount, MaterialTheme.colorScheme.onSurface)
        }
    }
}

private fun hitSlice(slices: List<Slice>, offset: Offset, size: androidx.compose.ui.unit.IntSize): Slice? {
    val center = Offset(size.width / 2f, size.height / 2f)
    val dx = offset.x - center.x
    val dy = offset.y - center.y
    val distance = hypot(dx.toDouble(), dy.toDouble()).toFloat()
    val radius = min(size.width, size.height) * 0.39f
    val inner = radius - 36f
    if (distance < inner || distance > radius + 20f) return null
    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    angle = (angle + 90f + 360f) % 360f
    val total = slices.sumOf { it.amount }.toFloat()
    var cursor = 0f
    slices.forEach { slice ->
        val sweep = slice.amount / total * 360f
        if (angle >= cursor && angle < cursor + sweep) return slice
        cursor += sweep
    }
    return null
}

@Composable
fun HorizontalBars(items: List<Pair<String, Long>>, color: Color, modifier: Modifier = Modifier) {
    if (items.isEmpty() || items.all { it.second == 0L }) {
        EmptyHint("Registra un gasto para ver esta gráfica")
        return
    }
    val max = items.maxOf { it.second }.coerceAtLeast(1L)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (name, amount) ->
            Text(name, style = MaterialTheme.typography.bodyMedium)
            Canvas(Modifier.fillMaxWidth().height(10.dp)) {
                drawRoundRect(
                    color = color.copy(alpha = 0.18f),
                    cornerRadius = CornerRadius(8f, 8f),
                    size = Size(size.width, size.height),
                )
                drawRoundRect(
                    color = color,
                    cornerRadius = CornerRadius(8f, 8f),
                    size = Size(size.width * (amount.toFloat() / max.toFloat()), size.height),
                )
            }
            Text(CopMoney.format(amount), style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum", fontWeight = FontWeight.Medium))
        }
    }
}

@Composable
fun Heatmap(days: List<Pair<Int, Long>>, lead: Int, modifier: Modifier = Modifier) {
    if (days.all { it.second == 0L }) {
        EmptyHint("Registra un gasto para ver esta gráfica")
        return
    }
    val max = days.maxOf { it.second }.coerceAtLeast(1L)
    val color = LocalLukaColors.current.expense
    var selected by remember { mutableStateOf<Pair<Int, Long>?>(null) }
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom").forEach {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val cells = List(lead) { null } + days.map { it }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                week.forEach { cell ->
                    val amount = cell?.second ?: 0L
                    val alpha = if (cell == null) 0f else 0.12f + 0.88f * (amount.toFloat() / max.toFloat())
                    Box(
                        Modifier
                            .weight(1f)
                            .height(28.dp)
                            .background(if (cell == null) Color.Transparent else color.copy(alpha = alpha.coerceIn(0f, 1f)))
                            .pointerInput(cell) {
                                detectTapGestures { if (cell != null) selected = cell }
                            },
                    )
                }
                repeat(7 - week.size) { Box(Modifier.weight(1f).height(28.dp)) }
            }
        }
        selected?.let { (day, amount) ->
            Text("Día $day, ${CopMoney.format(amount)}", modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun ProgressLine(label: String, amount: Long, target: Long, color: Color) {
    val fraction = if (target <= 0L) 0f else (amount.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text("${CopMoney.format(amount)} de ${CopMoney.format(target)}", style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"))
        }
        Canvas(Modifier.fillMaxWidth().height(8.dp).padding(top = 4.dp)) {
            drawRoundRect(color.copy(alpha = 0.2f), cornerRadius = CornerRadius(8f, 8f), size = size)
            drawRoundRect(color, cornerRadius = CornerRadius(8f, 8f), size = Size(size.width * fraction, size.height))
        }
    }
}
