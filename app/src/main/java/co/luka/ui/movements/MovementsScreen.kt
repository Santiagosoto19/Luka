package co.luka.ui.movements

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import co.luka.data.db.CategoryEntity
import co.luka.data.db.MovementEntity
import co.luka.data.db.SourceEntity
import co.luka.data.repository.CatalogRepository
import co.luka.data.repository.ManualEntry
import co.luka.data.repository.MovementRepository
import co.luka.domain.model.Direction
import co.luka.domain.time.BOGOTA
import co.luka.domain.time.PeriodKind
import co.luka.domain.time.Periods
import co.luka.ui.components.LukaCard
import co.luka.ui.components.MontoText
import co.luka.ui.components.PrimaryButton
import co.luka.ui.components.TransactionItem
import co.luka.ui.theme.LocalLukaColors
import co.luka.ui.theme.LukaSheetShape
import co.luka.ui.theme.Space
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MovementFilter(
    val fromDay: String? = null,
    val toDay: String? = null,
    val categoryId: Long? = null,
    val sourceId: Long? = null,
    val direction: String? = null,
    val minAmount: Long? = null,
    val maxAmount: Long? = null,
)

@HiltViewModel
class MovementsViewModel @Inject constructor(
    private val movements: MovementRepository,
    catalog: CatalogRepository,
) : ViewModel() {
    val categories = catalog.observeCategories(false).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val sources = catalog.observeSources().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val filter = MutableStateFlow(MovementFilter())
    val rows = filter.flatMapLatest {
        movements.observeFiltered(it.fromDay, it.toDay, it.categoryId, it.sourceId, it.direction, it.minAmount, it.maxAmount)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    var editing by mutableStateOf<MovementEntity?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var undoId by mutableStateOf<Long?>(null)
        private set

    fun setInitialCategory(id: Long?) {
        if (id != null && filter.value.categoryId == null) filter.update { it.copy(categoryId = id) }
    }

    fun updateFilter(block: (MovementFilter) -> MovementFilter) = filter.update(block)

    fun edit(id: Long) {
        viewModelScope.launch { editing = movements.find(id) }
    }

    fun dismissEdit() { editing = null }

    fun save(entry: ManualEntry) {
        val current = editing ?: return
        viewModelScope.launch {
            try {
                movements.update(current.id, entry)
                error = null
                editing = null
            } catch (exception: IllegalArgumentException) {
                error = exception.message
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            movements.softDelete(id)
            undoId = id
        }
    }

    fun undo() {
        val id = undoId ?: return
        viewModelScope.launch {
            movements.restore(id)
            undoId = null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MovementsScreen(initialCategoryId: Long?, viewModel: MovementsViewModel = hiltViewModel()) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val sources by viewModel.sources.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    var filtersOpen by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    var pendingDate by remember { mutableStateOf<((LocalDate) -> Unit)?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val colors = LocalLukaColors.current
    val today = LocalDate.now(BOGOTA)
    val incomeTotal = rows.sumOf { row ->
        if (row.direction == "income" && row.origin != "adjustment") row.amount else 0L
    }
    val expenseTotal = rows.sumOf { row ->
        if (row.direction == "expense" && row.origin != "adjustment") row.amount else 0L
    }
    LaunchedEffect(initialCategoryId) { viewModel.setInitialCategory(initialCategoryId) }
    LaunchedEffect(viewModel.undoId) {
        val id = viewModel.undoId ?: return@LaunchedEffect
        val result = snackbar.showSnackbar("Movimiento eliminado", actionLabel = "Deshacer")
        if (result == SnackbarResult.ActionPerformed && viewModel.undoId == id) viewModel.undo()
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Space.s20, end = Space.s20, top = Space.s20, bottom = Space.s32),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Movimientos",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    TextButton(
                        onClick = { filtersOpen = true },
                        contentPadding = PaddingValues(horizontal = Space.s12, vertical = Space.s4),
                    ) { Text("Filtrar") }
                }
            }
            item {
                FlowRow(
                    Modifier.fillMaxWidth().padding(top = Space.s12),
                    horizontalArrangement = Arrangement.spacedBy(Space.s8),
                    verticalArrangement = Arrangement.spacedBy(Space.s8),
                ) {
                    LukaChip(filter.direction == null, { viewModel.updateFilter { it.copy(direction = null) } }, "Todos")
                    LukaChip(filter.direction == "expense", { viewModel.updateFilter { it.copy(direction = "expense") } }, "Gastos")
                    LukaChip(filter.direction == "income", { viewModel.updateFilter { it.copy(direction = "income") } }, "Ingresos")
                }
            }
            item {
                LukaCard(Modifier.fillMaxWidth().padding(top = Space.s16)) {
                    Text(
                        if (rows.size == 1) "1 movimiento" else "${rows.size} movimientos",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(top = Space.s12),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(Space.s4)) {
                            Text("Ingresos", color = colors.muted, style = MaterialTheme.typography.bodyMedium)
                            MontoText(incomeTotal, Direction.INCOME)
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Space.s4)) {
                            Text("Gastos", color = colors.muted, style = MaterialTheme.typography.bodyMedium)
                            MontoText(expenseTotal, Direction.EXPENSE)
                        }
                    }
                }
            }
            if (rows.isEmpty()) {
                item {
                    PastelEmpty(
                        icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                        title = "Sin movimientos",
                        message = "Todavía no hay movimientos con ese filtro.",
                    )
                }
            } else {
                rows.groupBy { it.occurredDay }.forEach { (day, dayRows) ->
                    item(key = "day-$day") {
                        Text(
                            movementDayLabel(day, today),
                            color = colors.muted,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(top = Space.s16, bottom = Space.s4),
                        )
                    }
                    items(dayRows, key = { it.id }) { row ->
                        val state = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                when (value) {
                                    SwipeToDismissBoxValue.StartToEnd -> {
                                        viewModel.edit(row.id)
                                        false
                                    }
                                    SwipeToDismissBoxValue.EndToStart -> {
                                        viewModel.delete(row.id)
                                        true
                                    }
                                    else -> false
                                }
                            },
                        )
                        SwipeToDismissBox(
                            state = state,
                            backgroundContent = {
                                // Deslizar a la derecha deja ver Editar (índigo). Deslizar a la izquierda, Eliminar (coral).
                                Row(Modifier.fillMaxSize()) {
                                    Box(
                                        Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .background(MaterialTheme.colorScheme.primary)
                                            .padding(start = Space.s16),
                                        contentAlignment = Alignment.CenterStart,
                                    ) {
                                        Text(
                                            "Editar",
                                            color = Color.White,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        )
                                    }
                                    Box(
                                        Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .background(colors.expense)
                                            .padding(end = Space.s16),
                                        contentAlignment = Alignment.CenterEnd,
                                    ) {
                                        Text(
                                            "Eliminar",
                                            color = Color.White,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        )
                                    }
                                }
                            },
                        ) {
                            Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
                                val iconKey = categories.firstOrNull { it.id == row.categoryId }?.icon?.ifBlank { null } ?: "category"
                                TransactionItem(row, iconKey, movementDayLabel(row.occurredDay, today), hidden = false)
                            }
                        }
                    }
                }
            }
        }
        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = Space.s16))
    }
    if (filtersOpen) {
        FilterSheet(
            filter,
            categories,
            sources,
            onDismiss = { filtersOpen = false },
            onApply = { next ->
                viewModel.updateFilter { next }
                filtersOpen = false
            },
            onPickDate = { _, picked ->
                pendingDate = picked
                showDate = true
            },
        )
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
    viewModel.editing?.let { movement ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissEdit() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = LukaSheetShape,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            MovementForm(
                categories = categories,
                sources = sources,
                error = viewModel.error,
                initialDirection = when (movement.direction) {
                    "transfer" -> Direction.TRANSFER
                    "income" -> Direction.INCOME
                    else -> Direction.EXPENSE
                },
                initialAmount = movement.amount.toString(),
                initialCategoryId = movement.categoryId,
                initialNote = movement.note.orEmpty(),
                initialCounterparty = movement.counterparty.orEmpty(),
                initialOccurredAt = movement.occurredAt,
                initialSourceId = movement.sourceId,
                initialCounterSourceId = movement.counterSourceId,
                allowTransfer = movement.origin != "adjustment",
                onPickDate = { _, picked ->
                    pendingDate = picked
                    showDate = true
                },
                onSave = viewModel::save,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(
    current: MovementFilter,
    categories: List<CategoryEntity>,
    sources: List<SourceEntity>,
    onDismiss: () -> Unit,
    onApply: (MovementFilter) -> Unit,
    onPickDate: (LocalDate, (LocalDate) -> Unit) -> Unit,
) {
    var fromDay by remember { mutableStateOf(current.fromDay.orEmpty()) }
    var toDay by remember { mutableStateOf(current.toDay.orEmpty()) }
    var direction by remember { mutableStateOf(current.direction) }
    var categoryId by remember { mutableStateOf(current.categoryId) }
    var sourceId by remember { mutableStateOf(current.sourceId) }
    var minAmount by remember { mutableStateOf(current.minAmount?.toString().orEmpty()) }
    var maxAmount by remember { mutableStateOf(current.maxAmount?.toString().orEmpty()) }
    val muted = LocalLukaColors.current.muted
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = LukaSheetShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = Space.s20)
                .padding(bottom = Space.s32),
            verticalArrangement = Arrangement.spacedBy(Space.s12),
        ) {
            Text("Filtros", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
            DateBoundRow(
                title = "Desde",
                day = fromDay,
                onPick = {
                    val seed = runCatching { LocalDate.parse(fromDay) }.getOrNull() ?: LocalDate.now(BOGOTA)
                    onPickDate(seed) { fromDay = it.toString() }
                },
                onClear = { fromDay = "" },
            )
            DateBoundRow(
                title = "Hasta",
                day = toDay,
                onPick = {
                    val seed = runCatching { LocalDate.parse(toDay) }.getOrNull() ?: LocalDate.now(BOGOTA)
                    onPickDate(seed) { toDay = it.toString() }
                },
                onClear = { toDay = "" },
            )
            Text("Tipo", color = muted, style = MaterialTheme.typography.bodyMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Space.s8),
                verticalArrangement = Arrangement.spacedBy(Space.s8),
            ) {
                LukaChip(direction == null, { direction = null }, "Todos")
                LukaChip(direction == "expense", { direction = "expense" }, "Gastos")
                LukaChip(direction == "income", { direction = "income" }, "Ingresos")
            }
            if (categories.isNotEmpty()) {
                Text("Categoría", color = muted, style = MaterialTheme.typography.bodyMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Space.s8),
                    verticalArrangement = Arrangement.spacedBy(Space.s8),
                ) {
                    categories.forEach { category ->
                        LukaChip(
                            categoryId == category.id,
                            { categoryId = if (categoryId == category.id) null else category.id },
                            category.name,
                        )
                    }
                }
            }
            if (sources.isNotEmpty()) {
                Text("Fuente", color = muted, style = MaterialTheme.typography.bodyMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Space.s8),
                    verticalArrangement = Arrangement.spacedBy(Space.s8),
                ) {
                    sources.forEach { source ->
                        LukaChip(
                            sourceId == source.id,
                            { sourceId = if (sourceId == source.id) null else source.id },
                            source.name,
                        )
                    }
                }
            }
            OutlinedTextField(
                minAmount,
                { minAmount = it.filter { ch -> ch.isDigit() } },
                label = { Text("Monto mínimo") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                maxAmount,
                { maxAmount = it.filter { ch -> ch.isDigit() } },
                label = { Text("Monto máximo") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            PrimaryButton(
                text = "Aplicar",
                onClick = {
                    onApply(
                        MovementFilter(
                            fromDay = fromDay.ifBlank { null },
                            toDay = toDay.ifBlank { null },
                            categoryId = categoryId,
                            sourceId = sourceId,
                            direction = direction,
                            minAmount = minAmount.toLongOrNull(),
                            maxAmount = maxAmount.toLongOrNull(),
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DateBoundRow(
    title: String,
    day: String,
    onPick: () -> Unit,
    onClear: () -> Unit,
) {
    val shown = remember(day) {
        val date = runCatching { LocalDate.parse(day) }.getOrNull()
        if (date == null) "Cualquier día" else Periods.label(Periods.resolve(PeriodKind.TODAY, date))
    }
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier
                .weight(1f)
                .clickable(role = Role.Button, onClick = onPick)
                .padding(vertical = Space.s8),
        ) {
            Text(title, color = LocalLukaColors.current.muted, style = MaterialTheme.typography.bodyMedium)
            Text(shown, style = MaterialTheme.typography.bodyLarge)
        }
        TextButton(onClick = onClear) { Text("Quitar") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LukaChip(selected: Boolean, onClick: () -> Unit, label: String) {
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
private fun PastelEmpty(icon: ImageVector, title: String, message: String) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = Space.s32),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.s8),
    ) {
        Box(
            Modifier.size(72.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        }
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
        Text(message, color = LocalLukaColors.current.muted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    }
}

private fun movementDayLabel(day: String, today: LocalDate): String {
    val date = runCatching { LocalDate.parse(day) }.getOrNull() ?: return day
    return when (date) {
        today -> "Hoy"
        today.minusDays(1) -> "Ayer"
        else -> Periods.label(Periods.resolve(PeriodKind.TODAY, date))
    }
}
