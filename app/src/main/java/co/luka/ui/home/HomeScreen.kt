package co.luka.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import co.luka.R
import co.luka.data.db.GoalProgress
import co.luka.data.db.MovementListRow
import co.luka.data.db.SourceEntity
import co.luka.data.db.SubscriptionEntity
import co.luka.data.repository.CaptureRepository
import co.luka.data.repository.CatalogRepository
import co.luka.data.repository.ManualEntry
import co.luka.data.repository.MovementRepository
import co.luka.data.repository.PlanningRepository
import co.luka.data.repository.StatsRepository
import co.luka.domain.model.Direction
import co.luka.domain.money.CopMoney
import co.luka.domain.stats.DirectionTotals
import co.luka.domain.stats.Stats
import co.luka.domain.time.BOGOTA
import co.luka.domain.time.PeriodKind
import co.luka.domain.time.Periods
import co.luka.ui.access.notificationAccessEnabled
import co.luka.ui.access.openNotificationAccess
import co.luka.ui.components.BalanceHeroCard
import co.luka.ui.components.EmptyState
import co.luka.ui.components.ExpandableFab
import co.luka.ui.components.MontoText
import co.luka.ui.components.QuickAccessCard
import co.luka.ui.components.StatCard
import co.luka.ui.components.TransactionItem
import co.luka.ui.components.WalletChip
import co.luka.ui.components.animationsEnabled
import co.luka.ui.components.percentLabel
import co.luka.ui.movements.MovementForm
import co.luka.ui.movements.localDateFromPicker
import co.luka.ui.theme.LocalLukaColors
import co.luka.ui.theme.LukaSheetShape
import co.luka.ui.theme.Space
import co.luka.ui.theme.colorFromHex
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WalletBalance(
    val id: Long,
    val name: String,
    val colorHex: String,
    val balance: Long,
)

data class HomeUi(
    val label: String,
    val previousMonth: String,
    val balance: Long,
    val totalBalance: Long,
    val income: Long,
    val expense: Long,
    val balanceDelta: Int?,
    val incomeDelta: Int?,
    val expenseDelta: Int?,
    val trend: List<Long>,
    val recent: List<MovementListRow>,
    val wallets: List<WalletBalance>,
    val reviewCount: Int,
    val budgetsLabel: String,
    val goalsLabel: String,
    val recurringLabel: String,
)

private data class HomeTrigger(
    val monthOffset: Int = 0,
    val sourceId: Long? = null,
    val generation: Int = 0,
)

private data class HomeRequest(
    val monthOffset: Int,
    val sourceId: Long?,
    val sources: List<SourceEntity>,
    val reviewCount: Int,
    val goals: List<GoalProgress>,
    val subscriptions: List<SubscriptionEntity>,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val movements: MovementRepository,
    private val stats: StatsRepository,
    private val catalog: CatalogRepository,
    private val planning: PlanningRepository,
    captures: CaptureRepository,
) : ViewModel() {
    val categories = catalog.observeCategories(false).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val sources = catalog.observeSources().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val savedEvents = MutableSharedFlow<Unit>()
    val saved = savedEvents
    var error by mutableStateOf<String?>(null)
        private set
    var refreshing by mutableStateOf(false)
        private set

    private val trigger = MutableStateFlow(HomeTrigger())
    val selectedSourceId = trigger.map { it.sourceId }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val ui = combine(
        combine(stats.revision(), planning.budgetRevision(), trigger) { _, _, current -> current },
        catalog.observeSources(),
        captures.observeReview(),
        planning.observeGoals(),
        planning.observeSubscriptions(),
    ) { current, sources, review, goals, subscriptions ->
        HomeRequest(current.monthOffset, current.sourceId, sources, review.size, goals, subscriptions)
    }.mapLatest { request ->
        try {
            load(request)
        } finally {
            if (coroutineContext.isActive) refreshing = false
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun shiftMonth(steps: Int) {
        trigger.update { it.copy(monthOffset = it.monthOffset + steps) }
    }

    fun selectSource(id: Long?) {
        trigger.update { it.copy(sourceId = id) }
    }

    // Sin red: el gesto solo vuelve a leer la base local.
    fun refresh() {
        refreshing = true
        trigger.update { it.copy(generation = it.generation + 1) }
    }

    fun save(entry: ManualEntry, onSaved: () -> Unit) {
        viewModelScope.launch {
            try {
                movements.insertManual(entry)
                error = null
                savedEvents.emit(Unit)
                onSaved()
            } catch (exception: IllegalArgumentException) {
                error = exception.message
            }
        }
    }

    private suspend fun load(request: HomeRequest): HomeUi {
        val today = LocalDate.now(BOGOTA)
        val month = Periods.shift(Periods.resolve(PeriodKind.MONTH, today), request.monthOffset)
        val previous = Periods.previous(month)
        val start = month.start.toString()
        val end = month.endExclusive.toString()
        val sourceId = request.sourceId
        val allTotals = totalsOf(stats.totalsFor(start, end, null))
        val shown = if (sourceId == null) allTotals else totalsOf(stats.totalsFor(start, end, sourceId))
        val previousShown = totalsOf(stats.totalsFor(previous.start.toString(), previous.endExclusive.toString(), sourceId))
        val dayRows = stats.daysFor(start, end, sourceId)
        val real = movements.balances()
        val recent = movements.recentInRange(start, end, sourceId, limit = 12)
        val budgets = planning.budgets(month.start.toString().take(7))
        val trend = monthTrend(month.start, month.endExclusive, dayRows)
        val wallets = request.sources.filter { !it.archived }.map { source ->
            WalletBalance(source.id, source.name, source.color, real[source.id] ?: 0L)
        }
        return HomeUi(
            label = Periods.label(month),
            previousMonth = Periods.label(previous).substringBefore(" ").lowercase(Locale("es", "CO")),
            balance = shown.balance,
            totalBalance = wallets.sumOf { it.balance },
            income = shown.income,
            expense = shown.expense,
            balanceDelta = percentOrNull(shown.balance, previousShown.balance),
            incomeDelta = percentOrNull(shown.income, previousShown.income),
            expenseDelta = percentOrNull(shown.expense, previousShown.expense),
            trend = trend,
            recent = recent,
            wallets = wallets,
            reviewCount = request.reviewCount,
            budgetsLabel = budgetsLabel(budgets.size),
            goalsLabel = goalsLabel(request.goals.firstOrNull()),
            recurringLabel = recurringLabel(request.subscriptions),
        )
    }
}

private fun totalsOf(rows: List<co.luka.data.db.DirectionSum>) = DirectionTotals(
    income = rows.firstOrNull { it.direction == "income" }?.total ?: 0L,
    expense = rows.firstOrNull { it.direction == "expense" }?.total ?: 0L,
)

private fun monthTrend(start: LocalDate, endExclusive: LocalDate, rows: List<co.luka.data.db.DaySum>): List<Long> {
    val nets = ArrayList<Long>()
    var any = false
    var cursor = start
    while (cursor.isBefore(endExclusive)) {
        val key = cursor.toString()
        var income = 0L
        var expense = 0L
        rows.forEach { row ->
            if (row.day != key) return@forEach
            if (row.direction == "income") income += row.total else if (row.direction == "expense") expense += row.total
        }
        val net = income - expense
        if (net != 0L) any = true
        nets += net
        cursor = cursor.plusDays(1)
    }
    return if (any) nets else emptyList()
}

// El mes anterior en cero no es una base: no se inventa un porcentaje.
private fun percentOrNull(current: Long, previous: Long): Int? {
    if (previous == 0L) return null
    val percent = Stats.deltaPercent(current, previous).percent ?: return null
    return percent.roundToInt()
}

private fun budgetsLabel(count: Int) = when (count) {
    0 -> "Sin presupuestos"
    1 -> "1 activo"
    else -> "$count activos"
}

private fun goalsLabel(goal: GoalProgress?): String {
    if (goal == null) return "Sin meta"
    if (goal.targetAmount <= 0L) return "0 %"
    return "${(goal.saved * 100 / goal.targetAmount).toInt()} %"
}

private fun recurringLabel(subscriptions: List<SubscriptionEntity>): String {
    val next = subscriptions
        .asSequence()
        .filter { it.status == "active" || it.status == "suggested" }
        .mapNotNull { it.nextExpectedAt }
        .minOrNull() ?: return "Sin pagos próximos"
    val day = Instant.ofEpochMilli(next).atZone(BOGOTA).toLocalDate()
    return day.format(DateTimeFormatter.ofPattern("d MMM", Locale("es", "CO")))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onReview: () -> Unit,
    onBudgets: () -> Unit,
    onGoals: () -> Unit,
    onRecurring: () -> Unit,
    onMovements: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val selectedSourceId by viewModel.selectedSourceId.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val sources by viewModel.sources.collectAsStateWithLifecycle()
    var hidden by rememberSaveable { mutableStateOf(false) }
    var sheet by remember { mutableStateOf(false) }
    var sheetDirection by remember { mutableStateOf(Direction.EXPENSE) }
    var fabExpanded by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    var pendingDate by remember { mutableStateOf<((LocalDate) -> Unit)?>(null) }
    val context = LocalContext.current
    var listening by remember { mutableStateOf(notificationAccessEnabled(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) listening = notificationAccessEnabled(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val colors = LocalLukaColors.current
    val data = ui
    val greeting = remember {
        when (LocalTime.now(BOGOTA).hour) {
            in 5..11 -> "Buenos días"
            in 12..18 -> "Buenas tardes"
            else -> "Buenas noches"
        }
    }

    fun openForm(direction: Direction) {
        sheetDirection = direction
        fabExpanded = false
        sheet = true
    }

    if (sheet) {
        ModalBottomSheet(
            onDismissRequest = { sheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = LukaSheetShape,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            MovementForm(
                categories = categories,
                sources = sources,
                error = viewModel.error,
                initialDirection = sheetDirection,
                onPickDate = { _, onPicked ->
                    pendingDate = onPicked
                    showDate = true
                },
                onSave = { entry -> viewModel.save(entry) { sheet = false } },
            )
        }
    }
    if (showDate) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    localDateFromPicker(state.selectedDateMillis)?.let { pendingDate?.invoke(it) }
                    showDate = false
                }) { Text("Listo") }
            },
        ) { DatePicker(state = state) }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        PullToRefreshBox(
            isRefreshing = viewModel.refreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = Space.s20, end = Space.s20, top = Space.s8, bottom = 108.dp),
            ) {
                item {
                    HomeEnter(0) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(greeting, style = MaterialTheme.typography.headlineMedium)
                            Image(
                                painter = painterResource(R.drawable.logo_luka),
                                contentDescription = "Luka",
                                modifier = Modifier.size(48.dp),
                                contentScale = ContentScale.Fit,
                            )
                        }
                    }
                }
                item {
                    HomeEnter(1) {
                        BalanceHeroCard(
                            label = data?.label ?: "Este mes",
                            balance = data?.balance ?: 0L,
                            deltaPercent = data?.balanceDelta,
                            previousMonth = data?.previousMonth.orEmpty(),
                            trend = data?.trend.orEmpty(),
                            hidden = hidden,
                            onPrevious = { viewModel.shiftMonth(-1) },
                            onNext = { viewModel.shiftMonth(1) },
                            onToggleHidden = { hidden = !hidden },
                            modifier = Modifier.padding(top = Space.s20),
                        )
                    }
                }
                item {
                    HomeEnter(2) {
                        Row(
                            Modifier.fillMaxWidth().padding(top = Space.s16),
                            horizontalArrangement = Arrangement.spacedBy(Space.s12),
                        ) {
                            StatCard(
                                amount = data?.income ?: 0L,
                                direction = Direction.INCOME,
                                variation = data?.incomeDelta?.let(::percentLabel),
                                variationColor = tone(data?.incomeDelta, income = true),
                                hidden = hidden,
                                modifier = Modifier.weight(1f),
                            )
                            StatCard(
                                amount = data?.expense ?: 0L,
                                direction = Direction.EXPENSE,
                                variation = data?.expenseDelta?.let(::percentLabel),
                                variationColor = tone(data?.expenseDelta, income = false),
                                hidden = hidden,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                item {
                    HomeEnter(3) {
                        LazyRow(
                            Modifier.fillMaxWidth().padding(top = Space.s16),
                            horizontalArrangement = Arrangement.spacedBy(Space.s8),
                            contentPadding = PaddingValues(vertical = Space.s4),
                        ) {
                            item {
                                WalletChip(
                                    label = "Todas",
                                    balance = data?.totalBalance ?: 0L,
                                    color = MaterialTheme.colorScheme.primary,
                                    selected = selectedSourceId == null,
                                    hidden = hidden,
                                    onClick = { viewModel.selectSource(null) },
                                )
                            }
                            items(data?.wallets.orEmpty(), key = { it.id }) { wallet ->
                                WalletChip(
                                    label = wallet.name,
                                    balance = wallet.balance,
                                    color = colorFromHex(wallet.colorHex),
                                    selected = selectedSourceId == wallet.id,
                                    hidden = hidden,
                                    onClick = { viewModel.selectSource(wallet.id) },
                                )
                            }
                        }
                    }
                }
                item {
                    HomeEnter(4) {
                        val review = data?.reviewCount ?: 0
                        Column(Modifier.padding(top = Space.s16), verticalArrangement = Arrangement.spacedBy(Space.s12)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(Space.s12)) {
                                QuickAccessCard(
                                    icon = Icons.Filled.Inbox,
                                    iconTint = colors.info,
                                    iconBackground = colors.info.copy(alpha = 0.16f),
                                    title = "Por revisar",
                                    detail = when {
                                        data == null -> ""
                                        review <= 0 -> "Al día"
                                        review == 1 -> "1 por revisar"
                                        else -> "$review por revisar"
                                    },
                                    badge = review,
                                    onClick = onReview,
                                    modifier = Modifier.weight(1f),
                                )
                                QuickAccessCard(
                                    icon = Icons.Filled.PieChart,
                                    iconTint = MaterialTheme.colorScheme.primary,
                                    iconBackground = MaterialTheme.colorScheme.secondaryContainer,
                                    title = "Presupuestos",
                                    detail = data?.budgetsLabel.orEmpty(),
                                    badge = 0,
                                    onClick = onBudgets,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(Space.s12)) {
                                QuickAccessCard(
                                    icon = Icons.Filled.Flag,
                                    iconTint = colors.warning,
                                    iconBackground = colors.warning.copy(alpha = 0.16f),
                                    title = "Metas",
                                    detail = data?.goalsLabel ?: "Sin meta",
                                    badge = 0,
                                    onClick = onGoals,
                                    modifier = Modifier.weight(1f),
                                )
                                QuickAccessCard(
                                    icon = Icons.Filled.EventRepeat,
                                    iconTint = MaterialTheme.colorScheme.secondary,
                                    iconBackground = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                                    title = "Recurrentes",
                                    detail = data?.recurringLabel ?: "Sin pagos próximos",
                                    badge = 0,
                                    onClick = onRecurring,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
                item {
                    HomeEnter(5) {
                        Row(
                            Modifier.fillMaxWidth().padding(top = Space.s24, bottom = Space.s8),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Últimos movimientos", style = MaterialTheme.typography.headlineMedium)
                            TextButton(onClick = onMovements) { Text("Ver todos") }
                        }
                    }
                }
                if (data != null && data.recent.isEmpty()) {
                    item {
                        EmptyState(
                            onAddExpense = { openForm(Direction.EXPENSE) },
                            showNotificationAction = !listening,
                            onEnableNotifications = {
                                openNotificationAccess(context)
                                listening = notificationAccessEnabled(context)
                            },
                        )
                    }
                } else if (data != null) {
                    val today = LocalDate.now(BOGOTA)
                    data.recent.groupBy { it.occurredDay }.forEach { (day, rows) ->
                        item(key = "day-$day") {
                            Text(
                                dayHeading(day, today),
                                color = colors.muted,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(top = Space.s12, bottom = Space.s4),
                            )
                        }
                        items(rows, key = { it.id }) { row ->
                            val icon = categories.firstOrNull { it.id == row.categoryId }?.icon ?: "category"
                            TransactionItem(row, icon, whenLabel(row, today), hidden)
                        }
                    }
                }
            }
        }
        ExpandableFab(
            expanded = fabExpanded,
            onExpandedChange = { fabExpanded = it },
            onExpense = { openForm(Direction.EXPENSE) },
            onIncome = { openForm(Direction.INCOME) },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun tone(percent: Int?, income: Boolean): androidx.compose.ui.graphics.Color? {
    if (percent == null) return null
    val colors = LocalLukaColors.current
    val favorable = if (income) percent >= 0 else percent < 0
    return if (favorable) colors.income else colors.expense
}

@Composable
private fun HomeEnter(index: Int, content: @Composable () -> Unit) {
    val animate = animationsEnabled()
    var visible by remember { mutableStateOf(!animate) }
    LaunchedEffect(animate) { if (animate) visible = true }
    val offset = with(LocalDensity.current) { 16.dp.roundToPx() }
    AnimatedVisibility(
        visible = visible,
        enter = if (animate) {
            fadeIn(tween(280, delayMillis = index * 60)) +
                slideInVertically(tween(280, delayMillis = index * 60)) { offset }
        } else {
            EnterTransition.None
        },
    ) { content() }
}

private fun dayHeading(day: String, today: LocalDate): String {
    val date = runCatching { LocalDate.parse(day) }.getOrNull() ?: return day
    return when (date) {
        today -> "Hoy"
        today.minusDays(1) -> "Ayer"
        else -> date.format(DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("es", "CO")))
    }
}

private fun whenLabel(row: MovementListRow, today: LocalDate): String {
    val date = runCatching { LocalDate.parse(row.occurredDay) }.getOrNull()
    val time = Instant.ofEpochMilli(row.occurredAt).atZone(BOGOTA).toLocalTime()
        .format(DateTimeFormatter.ofPattern("HH:mm"))
    return when (date) {
        null -> row.occurredDay
        today, today.minusDays(1) -> time
        else -> date.format(DateTimeFormatter.ofPattern("d MMM", Locale("es", "CO")))
    }
}

@Composable
fun MovementLine(row: MovementListRow) {
    val transfer = row.direction == "transfer"
    val adjustment = row.origin == "adjustment"
    val title = when {
        transfer -> "Traslado"
        adjustment -> "Ajuste"
        else -> row.counterparty?.takeIf { it.isNotBlank() } ?: row.categoryName ?: "Sin comercio"
    }
    val detail = if (transfer) {
        listOfNotNull(row.sourceName?.let { "Desde $it" }, row.counterSourceName?.let { "Hacia $it" }, row.occurredDay)
    } else {
        listOfNotNull(row.categoryName, row.sourceName, row.occurredDay)
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                detail.joinToString("  "),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (transfer) {
            Text(
                CopMoney.format(row.amount),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
            )
        } else {
            MontoText(row.amount, if (row.direction == "income") Direction.INCOME else Direction.EXPENSE)
        }
    }
}
