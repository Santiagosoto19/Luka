package co.luka.ui.planning

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import co.luka.data.repository.CatalogRepository
import co.luka.data.repository.PlanningRepository
import co.luka.domain.money.CopMoney
import co.luka.domain.stats.Stats
import co.luka.domain.time.BOGOTA
import co.luka.ui.components.LukaCard
import co.luka.ui.components.PrimaryButton
import co.luka.ui.components.ProgressLine
import co.luka.ui.components.SecondaryButton
import co.luka.ui.theme.LocalLukaColors
import co.luka.ui.theme.Space
import co.luka.ui.theme.colorFromHex
import co.luka.ui.theme.swatches
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val planning: PlanningRepository,
    catalog: CatalogRepository,
) : ViewModel() {
    val categories = catalog.observeCategories(false).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val month = "%04d-%02d".format(LocalDate.now(BOGOTA).year, LocalDate.now(BOGOTA).monthValue)
    var rows by mutableStateOf<List<co.luka.data.db.BudgetProgress>>(emptyList())
        private set
    var error by mutableStateOf<String?>(null)
        private set

    init { refresh() }

    fun refresh() { viewModelScope.launch { rows = planning.budgets(month) } }

    fun save(categoryId: Long, amount: Long) {
        viewModelScope.launch {
            try {
                planning.saveBudget(categoryId, amount, 0.8)
                error = null
                refresh()
            } catch (exception: IllegalArgumentException) {
                error = exception.message
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(viewModel: BudgetViewModel = hiltViewModel()) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var categoryId by remember { mutableStateOf<Long?>(null) }
    var amount by remember { mutableStateOf("") }
    val expense = categories.filter { it.direction == "expense" }
    val colors = LocalLukaColors.current
    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = Space.s20, end = Space.s20, top = Space.s20, bottom = Space.s32),
        verticalArrangement = Arrangement.spacedBy(Space.s12),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s8)) {
                Text("Presupuestos", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                Text("El aviso sale al cruzar el 80% del límite del mes.", color = colors.muted, style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            LukaCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.s12)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Space.s8),
                        verticalArrangement = Arrangement.spacedBy(Space.s8),
                    ) {
                        expense.forEach { category ->
                            LukaChip(categoryId == category.id, { categoryId = category.id }, category.name)
                        }
                    }
                    OutlinedTextField(
                        amount,
                        { amount = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Límite del mes") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    viewModel.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    val selectedCategory = categoryId
                    val limit = amount.toLongOrNull()
                    val canSave = selectedCategory != null && limit != null && limit > 0L
                    if (!canSave) {
                        Text(
                            if (selectedCategory == null) "Elige una categoría." else "Escribe un monto válido.",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    PrimaryButton(
                        text = "Guardar presupuesto",
                        onClick = {
                            if (selectedCategory != null && limit != null && limit > 0L) viewModel.save(selectedCategory, limit)
                        },
                        enabled = canSave,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        if (viewModel.rows.isEmpty()) {
            item {
                PastelEmpty(
                    Icons.Filled.PieChart,
                    "Sin presupuestos",
                    "Crea un presupuesto para una categoría.",
                )
            }
        }
        items(viewModel.rows, key = { it.id }) { row ->
            LukaCard(Modifier.fillMaxWidth()) {
                ProgressLine(row.categoryName, row.spent, row.limitAmount, colorFromHex(row.color))
            }
        }
    }
}

@HiltViewModel
class GoalViewModel @Inject constructor(
    private val planning: PlanningRepository,
) : ViewModel() {
    val goals = planning.observeGoals().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    var error by mutableStateOf<String?>(null)
        private set

    fun save(name: String, target: Long) {
        viewModelScope.launch {
            try {
                planning.saveGoal(name, target, null, swatches.first(), "spark")
                error = null
            } catch (exception: IllegalArgumentException) {
                error = exception.message
            }
        }
    }

    fun contribute(goalId: Long, amount: Long) {
        viewModelScope.launch {
            try {
                planning.contribute(goalId, amount, null)
                error = null
            } catch (exception: IllegalArgumentException) {
                error = exception.message
            }
        }
    }

    fun archive(id: Long) { viewModelScope.launch { planning.archiveGoal(id) } }
}

@Composable
fun GoalsScreen(viewModel: GoalViewModel = hiltViewModel()) {
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var contribution by remember { mutableStateOf("") }
    val colors = LocalLukaColors.current
    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = Space.s20, end = Space.s20, top = Space.s20, bottom = Space.s32),
        verticalArrangement = Arrangement.spacedBy(Space.s12),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s8)) {
                Text("Metas", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                Text("Los aportes quedan en Luka. No se mueve dinero.", color = colors.muted, style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            LukaCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.s12)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(target, { target = it.filter { ch -> ch.isDigit() } }, label = { Text("Monto") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    viewModel.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    PrimaryButton(
                        text = "Crear meta",
                        onClick = { viewModel.save(name, target.toLongOrNull() ?: 0L) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        contribution,
                        { contribution = it.filter { ch -> ch.isDigit() || ch == '-' }.take(12) },
                        label = { Text("Aporte") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
            }
        }
        if (goals.isEmpty()) {
            item { PastelEmpty(Icons.Filled.Flag, "Sin metas", "Define una meta de ahorro.") }
        }
        items(goals, key = { it.id }) { goal ->
            LukaCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.s8)) {
                    ProgressLine(goal.name, goal.saved, goal.targetAmount, colorFromHex(goal.color))
                    SecondaryButton(
                        text = "Sumar aporte a ${goal.name}",
                        onClick = { viewModel.contribute(goal.id, contribution.toLongOrNull() ?: 0L) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(onClick = { viewModel.archive(goal.id) }, modifier = Modifier.fillMaxWidth()) { Text("Archivar") }
                }
            }
        }
    }
}

@HiltViewModel
class RecurringViewModel @Inject constructor(
    private val planning: PlanningRepository,
) : ViewModel() {
    val items = planning.observeSubscriptions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    init { viewModelScope.launch { planning.refreshSuggestions() } }

    fun status(id: Long, value: String) { viewModelScope.launch { planning.setSubscriptionStatus(id, value) } }
}

@Composable
fun RecurringScreen(viewModel: RecurringViewModel = hiltViewModel()) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val active = items.filter { it.status == "active" }
    val month = active.sumOf { Stats.monthlyCost(it.typicalAmount, it.cadence) }
    val year = active.sumOf { Stats.yearlyCost(it.typicalAmount, it.cadence) }
    val colors = LocalLukaColors.current
    val amountStyle = MaterialTheme.typography.headlineMedium.copy(
        fontWeight = FontWeight.Bold,
        fontFeatureSettings = "tnum",
        fontFamily = colors.displayFont,
    )
    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = Space.s20, end = Space.s20, top = Space.s20, bottom = Space.s32),
        verticalArrangement = Arrangement.spacedBy(Space.s12),
    ) {
        item {
            Text("Recurrentes", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
        }
        item {
            LukaCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.s4)) {
                    Text("Al mes", color = colors.muted, style = MaterialTheme.typography.bodyMedium)
                    Text(CopMoney.format(month), style = amountStyle)
                    Text("Al año", color = colors.muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = Space.s8))
                    Text(CopMoney.format(year), style = amountStyle)
                }
            }
        }
        if (items.isEmpty()) {
            item {
                PastelEmpty(
                    Icons.Filled.EventRepeat,
                    "Sin recurrentes",
                    "Cuando un comercio se repita cada mes, Luka lo sugerirá aquí.",
                )
            }
        }
        items(items, key = { it.id }) { item ->
            LukaCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.s8)) {
                    Text(item.displayName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        "${CopMoney.format(item.typicalAmount)} ${cadenceLabel(item.cadence)} · ${statusLabel(item.status)}",
                        color = colors.muted,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                    )
                    if (item.status == "suggested") {
                        PrimaryButton(
                            text = "Confirmar",
                            onClick = { viewModel.status(item.id, "active") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (item.status == "paused") {
                        PrimaryButton(
                            text = "Reanudar",
                            onClick = { viewModel.status(item.id, "active") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (item.status == "active" || item.status == "suggested") {
                        SecondaryButton(
                            text = "Pausar",
                            onClick = { viewModel.status(item.id, "paused") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (item.status != "cancelled") {
                        TextButton(onClick = { viewModel.status(item.id, "cancelled") }) { Text("Cancelar") }
                    }
                }
            }
        }
    }
}

@Composable
private fun PastelEmpty(icon: ImageVector, title: String, message: String) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = Space.s16),
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

private fun cadenceLabel(value: String) = when (value) {
    "weekly" -> "cada semana"
    "yearly" -> "cada año"
    else -> "cada mes"
}

private fun statusLabel(value: String) = when (value) {
    "active" -> "activa"
    "paused" -> "en pausa"
    "cancelled" -> "cancelada"
    else -> "sugerida"
}
