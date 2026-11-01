package co.luka.ui.stats

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import co.luka.domain.model.Direction
import co.luka.ui.components.StatCard
import co.luka.ui.theme.Space
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import co.luka.data.db.BudgetProgress
import co.luka.data.db.GoalProgress
import co.luka.data.repository.StatsRepository
import co.luka.domain.money.CopMoney
import co.luka.domain.stats.Delta
import co.luka.domain.stats.DirectionTotals
import co.luka.domain.stats.NamedAmount
import co.luka.domain.stats.Stats
import co.luka.domain.time.BOGOTA
import co.luka.domain.time.PeriodKind
import co.luka.domain.time.Periods
import co.luka.ui.components.EmptyHint
import co.luka.ui.components.ExpenseDonut
import co.luka.ui.components.LukaCard
import co.luka.ui.components.MoneyText
import co.luka.ui.components.animationsEnabled
import co.luka.ui.components.Heatmap
import co.luka.ui.components.HorizontalBars
import co.luka.ui.components.ProgressLine
import co.luka.ui.components.SectionTitle
import co.luka.ui.components.Slice
import co.luka.ui.theme.FallbackCategoryHex
import co.luka.ui.theme.LocalLukaColors
import co.luka.ui.theme.colorFromHex
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted

data class StatsUi(
    val label: String,
    val income: Long,
    val expense: Long,
    val balance: Long,
    val savings: Double?,
    val dailyAverage: Long,
    val projection: Long?,
    val expenseDelta: Delta,
    val incomeDelta: Delta,
    val topExpenseLabel: String?,
    val topMerchant: String?,
    val slices: List<Slice>,
    val monthLabels: List<String>,
    val monthIncome: List<Long>,
    val monthExpense: List<Long>,
    val span: Int,
    val lineLabels: List<String>,
    val lineValues: List<Long>,
    val cumulativeLabels: List<String>,
    val cumulative: List<Long>,
    val merchants: List<Pair<String, Long>>,
    val sources: List<Pair<String, Long>>,
    val sourceLabels: List<String>,
    val sourceValues: List<Long>,
    val weekdays: List<Pair<String, Long>>,
    val hours: List<Pair<String, Long>>,
    val heatmapLead: Int,
    val heatmapDays: List<Pair<Int, Long>>,
    val fixedIncome: Long,
    val variableIncome: Long,
    val budgets: List<BudgetProgress>,
    val goals: List<GoalProgress>,
    val subscriptionMonth: Long,
    val subscriptionYear: Long,
    val subscriptionBars: List<Pair<String, Long>>,
    val insights: List<String>,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val stats: StatsRepository,
) : ViewModel() {
    var kind by mutableStateOf(PeriodKind.MONTH)
    var steps by mutableStateOf(0)
    var span by mutableStateOf(6)
    private val tick = MutableStateFlow(0)
    val ui = combine(stats.revision(), tick) { _, token -> token }
        .mapLatest { build() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun previous() { steps -= 1; tick.value++ }
    fun next() { steps += 1; tick.value++ }
    fun select(value: PeriodKind) { kind = value; steps = 0; tick.value++ }
    fun toggleSpan() { span = if (span == 6) 12 else 6; tick.value++ }

    private suspend fun build(): StatsUi {
        val today = LocalDate.now(BOGOTA)
        val window = Periods.shift(Periods.resolve(kind, today), steps)
        val previous = Periods.previous(window)
        val monthAnchor = window.start.withDayOfMonth(1)
        val from = monthAnchor.minusMonths((span - 1).toLong())
        val fromKey = "%04d-%02d".format(from.year, from.monthValue)
        val budgetMonth = "%04d-%02d".format(window.start.year, window.start.monthValue)
        val current = stats.load(window.start.toString(), window.endExclusive.toString(), fromKey, budgetMonth)
        val prior = stats.load(previous.start.toString(), previous.endExclusive.toString(), fromKey, budgetMonth)
        val totals = totalsOf(current.totals)
        val priorTotals = totalsOf(prior.totals)
        val elapsed = Periods.elapsedDays(window, today)
        val average = Stats.dailyAverage(totals.expense, elapsed)
        val projection = Stats.monthProjection(average, Periods.daysInMonth(window), Periods.isCurrentMonth(window, today))
        val months = (0 until span).map { offset -> from.plusMonths(offset.toLong()) }
        val monthLabels = months.map { date ->
            val short = Periods.shortMonth(date)
            if (span > 6 && date.monthValue == 1) "$short ${date.year % 100}" else short
        }
        val monthIncome = months.map { date ->
            val key = "%04d-%02d".format(date.year, date.monthValue)
            current.months.firstOrNull { it.month == key && it.direction == "income" }?.total ?: 0L
        }
        val monthExpense = months.map { date ->
            val key = "%04d-%02d".format(date.year, date.monthValue)
            current.months.firstOrNull { it.month == key && it.direction == "expense" }?.total ?: 0L
        }
        val dayCount = ChronoUnit.DAYS.between(window.start, window.endExclusive).toInt().coerceAtLeast(1)
        val expenseByDay = current.days.filter { it.direction == "expense" }.associate { it.day to it.total }
        val incomeByDay = current.days.filter { it.direction == "income" }.associate { it.day to it.total }
        val dates = (0 until dayCount).map { window.start.plusDays(it.toLong()) }
        val line = if (dayCount > 62) {
            dates.chunked(7).map { chunk ->
                chunk.sumOf { expenseByDay[it.toString()] ?: 0L } to chunk.first().dayOfMonth.toString()
            }
        } else {
            dates.map { (expenseByDay[it.toString()] ?: 0L) to it.dayOfMonth.toString() }
        }
        val nets = dates.map { (incomeByDay[it.toString()] ?: 0L) - (expenseByDay[it.toString()] ?: 0L) }
        val cumulative = if (kind == PeriodKind.MONTH) Stats.cumulative(nets) else emptyList()
        val heat = stats.expenseBetween(window.start.toString(), window.endExclusive.toString()).associate { it.day to it.expense }
        val heatmap = Periods.heatmap(window, heat)
        val weekdayNames = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
        val weekday = (0..6).map { index -> weekdayNames[index] to (current.weekdays.firstOrNull { it.weekday == index }?.total ?: 0L) }
        val bands = longArrayOf(0, 0, 0, 0, 0, 0)
        current.hours.forEach { bands[(it.hour.coerceIn(0, 23)) / 4] += it.total }
        val bandLabels = listOf("0–4", "4–8", "8–12", "12–16", "16–20", "20–24")
        val fixed = current.incomeKinds.firstOrNull { it.incomeKind == "fixed" }?.total ?: 0L
        val variable = current.incomeKinds.filter { it.incomeKind != "fixed" }.sumOf { it.total }
        val activeSubs = current.subscriptions.filter { it.status == "active" }
        val subMonth = activeSubs.sumOf { Stats.monthlyCost(it.typicalAmount, it.cadence) }
        val subYear = activeSubs.sumOf { Stats.yearlyCost(it.typicalAmount, it.cadence) }
        val topMerchant = current.merchants.firstOrNull()
        val topExpense = current.topExpense
        return StatsUi(
            label = Periods.label(window),
            income = totals.income,
            expense = totals.expense,
            balance = totals.balance,
            savings = Stats.savingsRate(totals),
            dailyAverage = average,
            projection = projection,
            expenseDelta = Stats.deltaPercent(totals.expense, priorTotals.expense),
            incomeDelta = Stats.deltaPercent(totals.income, priorTotals.income),
            topExpenseLabel = topExpense?.let { "${it.counterparty ?: "Gasto"} ${CopMoney.format(it.amount)}" },
            topMerchant = topMerchant?.let { "${it.name} ${CopMoney.format(it.total)}" },
            slices = current.categories.filter { it.total > 0 }.map {
                Slice(it.categoryId, it.name ?: "Sin categoría", it.total, colorFromHex(it.color ?: FallbackCategoryHex))
            },
            monthLabels = monthLabels,
            monthIncome = monthIncome,
            monthExpense = monthExpense,
            span = span,
            lineLabels = line.map { it.second },
            lineValues = line.map { it.first },
            cumulativeLabels = if (cumulative.isEmpty()) emptyList() else dates.map { it.dayOfMonth.toString() },
            cumulative = cumulative,
            merchants = current.merchants.map { it.name to it.total },
            sources = current.sources.map { (it.name ?: "Sin entidad") to it.total },
            sourceLabels = current.sources.map { it.name ?: "Sin entidad" },
            sourceValues = current.sources.map { it.total },
            weekdays = weekday,
            hours = bandLabels.mapIndexed { index, label -> label to bands[index] },
            heatmapLead = heatmap.lead,
            heatmapDays = heatmap.days,
            fixedIncome = fixed,
            variableIncome = variable,
            budgets = current.budgets,
            goals = current.goals,
            subscriptionMonth = subMonth,
            subscriptionYear = subYear,
            subscriptionBars = activeSubs.map { it.displayName to Stats.monthlyCost(it.typicalAmount, it.cadence) },
            insights = Stats.insights(
                totals,
                current.categories.map { NamedAmount(it.name ?: "Sin categoría", it.total, it.categoryId) },
                prior.categories.map { NamedAmount(it.name ?: "Sin categoría", it.total, it.categoryId) },
                subMonth,
                projection,
            ),
        )
    }

    private fun totalsOf(rows: List<co.luka.data.db.DirectionSum>) = DirectionTotals(
        income = rows.firstOrNull { it.direction == "income" }?.total ?: 0L,
        expense = rows.firstOrNull { it.direction == "expense" }?.total ?: 0L,
    )
}

@Composable
fun StatsScreen(onCategory: (Long) -> Unit, viewModel: StatsViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    var forward by remember { mutableStateOf(true) }
    val revealedPeriods = remember { mutableSetOf<String>() }
    val data = ui
    val periods = listOf(
        PeriodKind.TODAY to "Hoy",
        PeriodKind.WEEK to "Semana",
        PeriodKind.MONTH to "Mes",
        PeriodKind.QUARTER to "Trimestre",
        PeriodKind.YEAR to "Año",
    )
    val tabs = listOf("Resumen", "Categorías", "Ingresos", "Tendencias", "Comercios")
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        PeriodBar(
            label = data?.label ?: "Este mes",
            forward = forward,
            onPrevious = {
                forward = false
                viewModel.previous()
            },
            onNext = {
                forward = true
                viewModel.next()
            },
        )
        PeriodRail(periods, viewModel.kind, viewModel::select)
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Space.s20)
                .padding(bottom = Space.s8),
            horizontalArrangement = Arrangement.spacedBy(Space.s8),
        ) {
            tabs.forEachIndexed { index, label ->
                PeriodChip(label, tab == index) { tab = index }
            }
        }
        if (data == null) return@Column
        val motion = animationsEnabled()
        LazyColumn(
            contentPadding = PaddingValues(start = Space.s20, end = Space.s20, top = Space.s8, bottom = Space.s32),
            verticalArrangement = Arrangement.spacedBy(Space.s16),
        ) {
            item {
                AnimatedContent(
                    targetState = data,
                    contentKey = { it.label },
                    transitionSpec = { periodMove(forward, motion) },
                    label = "periodBody",
                ) { snapshot ->
                    val frame = if (snapshot.label == data.label) data else snapshot
                    when (tab) {
                        0 -> SummaryTab(frame, onCategory, viewModel::toggleSpan)
                        1 -> Column(verticalArrangement = Arrangement.spacedBy(Space.s12)) {
                            SectionTitle("Gastos por categoría")
                            if (frame.slices.isEmpty()) {
                                EmptyHint("Registra un gasto para ver esta gráfica.")
                            } else {
                                CategoryBlock(frame.slices, frame.label, onCategory)
                            }
                        }
                        2 -> IncomeTab(frame)
                        3 -> TrendTab(frame, revealedPeriods)
                        else -> MerchantTab(frame)
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodBar(label: String, forward: Boolean, onPrevious: () -> Unit, onNext: () -> Unit) {
    val motion = animationsEnabled()
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Space.s8, vertical = Space.s4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = "Periodo anterior")
        }
        AnimatedContent(
            targetState = label,
            modifier = Modifier.weight(1f),
            transitionSpec = { periodMove(forward, motion) },
            contentAlignment = Alignment.Center,
            label = "periodTitle",
        ) { title ->
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            )
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Filled.ChevronRight, contentDescription = "Periodo siguiente")
        }
    }
}

private fun periodMove(forward: Boolean, motion: Boolean): ContentTransform {
    if (!motion) return fadeIn(tween(180)) togetherWith fadeOut(tween(180))
    val spec = tween<IntOffset>(220)
    return if (forward) {
        slideInHorizontally(spec) { it } togetherWith slideOutHorizontally(spec) { -it }
    } else {
        slideInHorizontally(spec) { -it } togetherWith slideOutHorizontally(spec) { it }
    }
}

private data class PillSlot(val x: Int, val width: Int, val height: Int)

@Composable
private fun PeriodRail(
    periods: List<Pair<PeriodKind, String>>,
    selected: PeriodKind,
    onSelect: (PeriodKind) -> Unit,
) {
    val animate = animationsEnabled()
    var slots by remember { mutableStateOf(mapOf<PeriodKind, PillSlot>()) }
    val slot = slots[selected]
    val x = remember { Animatable(0f) }
    val width = remember { Animatable(0f) }
    var placed by remember { mutableStateOf(false) }
    LaunchedEffect(slot?.x, slot?.width, animate) {
        val current = slot ?: return@LaunchedEffect
        if (!animate || !placed) {
            x.snapTo(current.x.toFloat())
            width.snapTo(current.width.toFloat())
            placed = true
        } else {
            val spec = spring<Float>(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
            launch { x.animateTo(current.x.toFloat(), spec) }
            width.animateTo(current.width.toFloat(), spec)
        }
    }
    Box(
        Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Space.s20)
            .padding(bottom = Space.s8),
    ) {
        if (slot != null && width.value > 0f) {
            Box(
                Modifier
                    .offset { IntOffset(x.value.roundToInt(), 0) }
                    .width(with(LocalDensity.current) { width.value.toDp() })
                    .height(with(LocalDensity.current) { slot.height.toDp() })
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp)),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s8)) {
            periods.forEach { (kind, label) ->
                val active = kind == selected
                Box(
                    Modifier
                        .onGloballyPositioned { coords ->
                            val parent = coords.parentLayoutCoordinates?.parentLayoutCoordinates ?: return@onGloballyPositioned
                            val pos = parent.localPositionOf(coords, Offset.Zero)
                            val next = PillSlot(pos.x.roundToInt(), coords.size.width, coords.size.height)
                            if (slots[kind] != next) slots = slots + (kind to next)
                        }
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onSelect(kind) }
                        .padding(horizontal = Space.s16, vertical = Space.s8),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        color = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = Color.White,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            labelColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outline,
            selectedBorderColor = Color.Transparent,
        ),
    )
}

@Composable
private fun SummaryTab(data: StatsUi, onCategory: (Long) -> Unit, onToggleSpan: () -> Unit) {
    val extra = LocalLukaColors.current
    Column(verticalArrangement = Arrangement.spacedBy(Space.s12)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s12)) {
            StatCard(
                amount = data.income,
                direction = Direction.INCOME,
                variation = deltaText(data.incomeDelta).ifBlank { null },
                variationColor = extra.income,
                hidden = false,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                amount = data.expense,
                direction = Direction.EXPENSE,
                variation = deltaText(data.expenseDelta).ifBlank { null },
                variationColor = extra.expense,
                hidden = false,
                modifier = Modifier.weight(1f),
            )
        }
        LukaCard(Modifier.fillMaxWidth()) {
            Text("Balance", color = extra.muted, style = MaterialTheme.typography.bodyMedium)
            Text(
                CopMoney.format(data.balance),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s12)) {
            MetricTile("Ahorro", data.savings?.let { "${(it * 100).roundToInt()} %" } ?: "Sin ingresos", Modifier.weight(1f))
            MetricTile("Al día", CopMoney.format(data.dailyAverage), Modifier.weight(1f))
        }
        MetricTile("Proyección", data.projection?.let { CopMoney.format(it) } ?: "Solo en el mes en curso")
        MetricTile("Mayor gasto", data.topExpenseLabel ?: "Todavía no hay un gasto")
        MetricTile("Donde más gastas", data.topMerchant ?: "Todavía no hay un comercio")
        if (data.insights.isEmpty()) {
            EmptyHint("Cuando haya movimientos, Luka resumirá el periodo aquí.")
        } else {
            LukaCard(Modifier.fillMaxWidth()) {
                data.insights.forEach { line ->
                    Text(line, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = Space.s8))
                }
            }
        }
        ChartSurface("Gastos por categoría") {
            CategoryBlock(data.slices, data.label, onCategory)
        }
        ChartSurface("Ingresos y gastos") {
            ColumnCompareChart(data.monthIncome, data.monthExpense, data.monthLabels, extra.income, extra.expense)
            TextButton(onClick = onToggleSpan) { Text(if (data.span == 6) "Ver 12 meses" else "Ver 6 meses") }
        }
        data.budgets.forEach { ProgressLine(it.categoryName, it.spent, it.limitAmount, colorFromHex(it.color)) }
        data.goals.forEach { ProgressLine(it.name, it.saved, it.targetAmount, colorFromHex(it.color)) }
    }
}

@Composable
private fun IncomeTab(data: StatsUi) {
    val extra = LocalLukaColors.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ChartSurface("Fijos y variables") {
            SingleColumnChart(listOf(data.fixedIncome, data.variableIncome), listOf("Fijo", "Variable"), extra.income)
        }
        Indicator("Suscripciones al mes", CopMoney.format(data.subscriptionMonth), null)
        Indicator("Suscripciones al año", CopMoney.format(data.subscriptionYear), null)
        HorizontalBars(data.subscriptionBars, extra.income)
    }
}

@Composable
private fun TrendTab(data: StatsUi, revealed: MutableSet<String>) {
    val extra = LocalLukaColors.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ChartSurface("Gasto del periodo") {
            ExpenseLineChart(data.lineValues, data.lineLabels, extra.expense, extra.muted, data.label, revealed)
        }
        ChartSurface("Balance acumulado") {
            BalanceAreaChart(data.cumulative, data.cumulativeLabels, extra.income, data.label, revealed)
        }
        ChartSurface("Calendario") { Heatmap(data.heatmapDays, data.heatmapLead) }
        ChartSurface("Día de la semana") { HorizontalBars(data.weekdays, extra.expense) }
        ChartSurface("Franja horaria") { HorizontalBars(data.hours, extra.warning) }
    }
}

@Composable
private fun MerchantTab(data: StatsUi) {
    val extra = LocalLukaColors.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ChartSurface("Top comercios") { HorizontalBars(data.merchants, extra.expense) }
        ChartSurface("Por entidad") { SingleColumnChart(data.sourceValues, data.sourceLabels, extra.income) }
    }
}

@Composable
private fun CategoryBlock(slices: List<Slice>, periodLabel: String, onCategory: (Long) -> Unit) {
    var marked by remember(periodLabel) { mutableStateOf<Long?>(null) }
    ExpenseDonut(
        slices = slices,
        onSlice = { id ->
            if (id != null) {
                marked = id
                onCategory(id)
            }
        },
        showRows = false,
    )
    if (slices.isNotEmpty()) {
        CategoryBars(slices, periodLabel, marked) { id ->
            marked = id
            onCategory(id)
        }
    }
}

@Composable
private fun CategoryBars(
    slices: List<Slice>,
    periodLabel: String,
    markedId: Long?,
    onSelect: (Long) -> Unit,
) {
    val animate = animationsEnabled()
    val total = slices.sumOf { it.amount }.coerceAtLeast(1L)
    val growth = remember(periodLabel) { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(periodLabel, animate) {
        if (!animate) {
            growth.snapTo(1f)
        } else {
            growth.snapTo(0f)
            growth.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow))
        }
    }
    val track = LocalLukaColors.current.border
    Column(verticalArrangement = Arrangement.spacedBy(Space.s8)) {
        slices.forEach { slice ->
            val fraction = (slice.amount.toFloat() / total.toFloat() * growth.value).coerceIn(0f, 1f)
            val marked = slice.id != null && slice.id == markedId
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (marked) slice.color.copy(alpha = 0.16f) else Color.Transparent)
                    .clickable(enabled = slice.id != null) {
                        val id = slice.id ?: return@clickable
                        onSelect(id)
                    }
                    .padding(horizontal = Space.s8, vertical = Space.s4),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(slice.color, CircleShape))
                    Text(
                        slice.name,
                        modifier = Modifier.padding(start = Space.s8).weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    MoneyText(slice.amount, MaterialTheme.colorScheme.onSurface)
                }
                Box(
                    Modifier
                        .padding(top = Space.s4)
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(track),
                ) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(fraction).background(slice.color))
                }
            }
        }
    }
}

@Composable
private fun ChartSurface(title: String, content: @Composable () -> Unit) {
    LukaCard(Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        content()
    }
}

@Composable
private fun MetricTile(label: String, value: String, modifier: Modifier = Modifier) {
    LukaCard(modifier.fillMaxWidth()) {
        Text(label, color = LocalLukaColors.current.muted, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
        )
    }
}

@Composable
private fun Indicator(label: String, value: String, detail: String?) {
    MetricTile(label, if (detail.isNullOrBlank()) value else "$value · $detail")
}

private fun deltaText(delta: Delta): String = when {
    delta.increasedFromZero -> "Subió y el periodo anterior estaba en cero"
    delta.percent == null -> ""
    delta.percent > 0.5 -> "Subió ${delta.percent.roundToInt()}%"
    delta.percent < -0.5 -> "Bajó ${abs(delta.percent).roundToInt()}%"
    else -> "Sin cambio frente al periodo anterior"
}
