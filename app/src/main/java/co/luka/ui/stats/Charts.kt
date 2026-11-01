package co.luka.ui.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.luka.domain.money.CopMoney
import co.luka.ui.components.EmptyHint
import co.luka.ui.components.animationsEnabled
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.compose.cartesian.layer.dashed
import com.patrykandpatrick.vico.compose.cartesian.layer.grouped
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer

@Composable
fun ColumnCompareChart(
    first: List<Long>,
    second: List<Long>,
    labels: List<String>,
    firstColor: Color,
    secondColor: Color,
) {
    if (first.isEmpty() || (first.all { it == 0L } && second.all { it == 0L })) {
        EmptyHint("Registra un gasto para ver esta gráfica")
        return
    }
    val producer = remember { CartesianChartModelProducer() }
    LaunchedEffect(first, second) {
        producer.runTransaction {
            columnSeries {
                series(first.map { it.toDouble() })
                series(second.map { it.toDouble() })
            }
        }
    }
    ChartHost(
        producer = producer,
        labels = labels,
        columnColors = listOf(firstColor, secondColor),
        lineColors = listOf(firstColor, secondColor),
        columns = true,
        area = false,
    )
}

@Composable
fun SingleColumnChart(values: List<Long>, labels: List<String>, color: Color) {
    if (values.isEmpty() || values.all { it == 0L }) {
        EmptyHint("Registra un gasto para ver esta gráfica")
        return
    }
    val producer = remember { CartesianChartModelProducer() }
    LaunchedEffect(values) {
        producer.runTransaction { columnSeries { series(values.map { it.toDouble() }) } }
    }
    ChartHost(producer, labels, listOf(color), listOf(color), columns = true, area = false)
}

@Composable
fun ExpenseLineChart(
    actual: List<Long>,
    labels: List<String>,
    color: Color,
    muted: Color,
    periodKey: String,
    revealed: MutableSet<String>,
) {
    val reveal = rememberReveal(periodKey, revealed)
    if (actual.isEmpty()) {
        EmptyHint("Registra un gasto para ver esta gráfica")
        return
    }
    val average = if (actual.isEmpty()) 0.0 else actual.sum().toDouble() / actual.size
    val producer = remember { CartesianChartModelProducer() }
    LaunchedEffect(actual) {
        producer.runTransaction {
            lineSeries {
                series(actual.map { it.toDouble() })
                series(List(actual.size) { average })
            }
        }
    }
    ProvideVicoTheme(
        rememberM3VicoTheme(
            columnCartesianLayerColors = listOf(color, muted),
            lineCartesianLayerColors = listOf(color, muted),
            lineColor = muted,
            textColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(
                    lineProvider = LineCartesianLayer.LineProvider.series(
                        LineCartesianLayer.rememberLine(fill = LineCartesianLayer.LineFill.single(fill(color))),
                        LineCartesianLayer.rememberLine(
                            fill = LineCartesianLayer.LineFill.single(fill(muted)),
                            stroke = LineCartesianLayer.LineStroke.dashed(),
                        ),
                    ),
                ),
                startAxis = VerticalAxis.rememberStart(valueFormatter = moneyFormatter()),
                bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = indexFormatter(labels)),
                marker = marker(),
            ),
            modelProducer = producer,
            modifier = Modifier.fillMaxWidth().height(220.dp).revealClip(reveal),
            scrollState = rememberVicoScrollState(scrollEnabled = labels.size > 8),
            animateIn = false,
        )
    }
}

@Composable
fun BalanceAreaChart(
    values: List<Long>,
    labels: List<String>,
    color: Color,
    periodKey: String,
    revealed: MutableSet<String>,
) {
    val reveal = rememberReveal(periodKey, revealed)
    if (values.isEmpty()) {
        EmptyHint("Registra un gasto para ver esta gráfica")
        return
    }
    val producer = remember { CartesianChartModelProducer() }
    LaunchedEffect(values) {
        producer.runTransaction { lineSeries { series(values.map { it.toDouble() }) } }
    }
    ProvideVicoTheme(
        rememberM3VicoTheme(
            lineCartesianLayerColors = listOf(color),
            lineColor = color.copy(alpha = 0.4f),
            textColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(
                    lineProvider = LineCartesianLayer.LineProvider.series(
                        LineCartesianLayer.rememberLine(
                            fill = LineCartesianLayer.LineFill.single(fill(color)),
                            areaFill = LineCartesianLayer.AreaFill.single(fill(color.copy(alpha = 0.28f))),
                        ),
                    ),
                ),
                startAxis = VerticalAxis.rememberStart(valueFormatter = moneyFormatter()),
                bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = indexFormatter(labels)),
                marker = marker(),
            ),
            modelProducer = producer,
            modifier = Modifier.fillMaxWidth().height(220.dp).revealClip(reveal),
            scrollState = rememberVicoScrollState(scrollEnabled = labels.size > 10),
            animateIn = false,
        )
    }
}

@Composable
private fun rememberReveal(periodKey: String, revealed: MutableSet<String>): Float {
    val animate = animationsEnabled()
    val play = animate && periodKey !in revealed
    val progress = remember(periodKey) { Animatable(if (play) 0f else 1f) }
    LaunchedEffect(periodKey, animate) {
        if (!animate || periodKey in revealed) {
            progress.snapTo(1f)
            revealed += periodKey
            return@LaunchedEffect
        }
        progress.snapTo(0f)
        progress.animateTo(1f, tween(700))
        revealed += periodKey
    }
    return progress.value
}

private fun Modifier.revealClip(fraction: Float): Modifier = drawWithContent {
    clipRect(right = size.width * fraction.coerceIn(0f, 1f)) {
        this@drawWithContent.drawContent()
    }
}

@Composable
private fun ChartHost(
    producer: CartesianChartModelProducer,
    labels: List<String>,
    columnColors: List<Color>,
    lineColors: List<Color>,
    columns: Boolean,
    area: Boolean,
) {
    val animate = animationsEnabled()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    ProvideVicoTheme(
        rememberM3VicoTheme(
            columnCartesianLayerColors = columnColors,
            lineCartesianLayerColors = lineColors,
            lineColor = muted,
            textColor = muted,
        ),
    ) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                if (columns) {
                    rememberColumnCartesianLayer(mergeMode = { ColumnCartesianLayer.MergeMode.grouped() })
                } else {
                    rememberLineCartesianLayer()
                },
                startAxis = VerticalAxis.rememberStart(valueFormatter = moneyFormatter()),
                bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = indexFormatter(labels)),
                marker = marker(),
            ),
            modelProducer = producer,
            modifier = Modifier.fillMaxWidth().height(220.dp),
            scrollState = rememberVicoScrollState(scrollEnabled = labels.size > 8),
            animateIn = animate,
        )
    }
}

@Composable
private fun marker() = rememberDefaultCartesianMarker(
    label = rememberTextComponent(
        color = MaterialTheme.colorScheme.onSurface,
        textSize = 12.sp,
    ),
)

@Composable
private fun moneyFormatter(): CartesianValueFormatter = remember {
    CartesianValueFormatter { _, value, _ -> CopMoney.format(value.toLong()) }
}

@Composable
private fun indexFormatter(labels: List<String>): CartesianValueFormatter {
    val current = rememberUpdatedState(labels)
    return remember {
        CartesianValueFormatter { _, value, _ -> current.value.getOrNull(value.toInt()).orEmpty() }
    }
}
