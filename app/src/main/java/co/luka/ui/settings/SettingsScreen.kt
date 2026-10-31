package co.luka.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import co.luka.data.backup.BackupRepository
import co.luka.data.db.CategoryEntity
import co.luka.data.export.Exporters
import co.luka.data.export.PdfReport
import co.luka.data.repository.CatalogRepository
import co.luka.data.repository.StatsRepository
import co.luka.data.settings.SettingsStore
import co.luka.data.settings.UserSettings
import co.luka.domain.money.CopMoney
import co.luka.domain.security.PinHasher
import co.luka.domain.time.BOGOTA
import co.luka.domain.time.PeriodKind
import co.luka.domain.time.Periods
import co.luka.ui.components.CategoryBadge
import co.luka.ui.components.LukaCard
import co.luka.ui.components.PrimaryButton
import co.luka.ui.components.SecondaryButton
import co.luka.ui.theme.LocalLukaColors
import co.luka.ui.theme.Space
import co.luka.ui.theme.colorFromHex
import co.luka.ui.theme.iconChoices
import co.luka.ui.theme.iconFor
import co.luka.ui.theme.swatches
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    private val catalog: CatalogRepository,
    private val stats: StatsRepository,
    private val exporters: Exporters,
    private val backup: BackupRepository,
) : ViewModel() {
    val settings = settingsStore.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())
    val sources = catalog.observeSources().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val categories = catalog.observeCategories(true).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val messages = MutableStateFlow<String?>(null)
    val message = messages.asStateFlow()
    var exportSteps by mutableStateOf(0)

    fun theme(value: String) { viewModelScope.launch { settingsStore.setTheme(value) } }

    fun toggleSource(sourceId: Long, enabled: Boolean) {
        viewModelScope.launch {
            val source = sources.value.firstOrNull { it.id == sourceId } ?: return@launch
            catalog.updateSource(source.copy(monitorNotifications = enabled))
        }
    }

    fun packageName(sourceId: Long, value: String) {
        viewModelScope.launch {
            val source = sources.value.firstOrNull { it.id == sourceId } ?: return@launch
            catalog.updateSource(source.copy(packageName = value.trim().ifBlank { null }))
        }
    }

    fun saveCategory(name: String, direction: String, color: String, icon: String) {
        viewModelScope.launch {
            try {
                catalog.saveCategory(name, direction, color, icon)
                messages.value = "Categoría guardada."
            } catch (exception: IllegalArgumentException) {
                messages.value = exception.message
            }
        }
    }

    fun archiveCategory(category: CategoryEntity, archived: Boolean) {
        viewModelScope.launch { catalog.updateCategory(category.copy(archived = archived)) }
    }

    fun setLock(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            if (enabled && !PinHasher.isConfigured(current.pinSalt, current.pinHash)) {
                messages.value = "Guarda un PIN de al menos 4 dígitos antes de activar el bloqueo."
                return@launch
            }
            settingsStore.setLockEnabled(enabled)
        }
    }

    fun setPin(pin: String, confirm: String) {
        viewModelScope.launch {
            if (pin.length < 4) {
                messages.value = "El PIN necesita al menos 4 dígitos."
                return@launch
            }
            if (pin != confirm) {
                messages.value = "Los PIN no coinciden."
                return@launch
            }
            val salt = PinHasher.newSalt()
            settingsStore.setPin(PinHasher.hash(pin, salt), salt)
            messages.value = "PIN guardado."
        }
    }

    fun exportCsv(uri: Uri) = exportPeriod(uri, pdf = false)
    fun exportPdf(uri: Uri) = exportPeriod(uri, pdf = true)

    private fun exportPeriod(uri: Uri, pdf: Boolean) {
        viewModelScope.launch {
            try {
                val window = Periods.shift(Periods.resolve(PeriodKind.MONTH, LocalDate.now(BOGOTA)), exportSteps)
                if (pdf) {
                    val data = stats.load(window.start.toString(), window.endExclusive.toString(), window.start.toString().take(7), window.start.toString().take(7))
                    val income = data.totals.firstOrNull { it.direction == "income" }?.total ?: 0L
                    val expense = data.totals.firstOrNull { it.direction == "expense" }?.total ?: 0L
                    exporters.pdf(
                        uri,
                        PdfReport(
                            title = Periods.label(window),
                            income = income,
                            expense = expense,
                            balance = income - expense,
                            bars = data.categories.map { (it.name ?: "Sin categoría") to it.total },
                        ),
                    )
                } else {
                    exporters.csv(uri, window.start.toString(), window.endExclusive.toString())
                }
                messages.value = "Archivo guardado."
            } catch (exception: Exception) {
                messages.value = exception.message ?: "No se pudo exportar."
            }
        }
    }

    fun exportBackup(uri: Uri, phrase: String) {
        viewModelScope.launch {
            try {
                if (phrase.length < 4) error("La frase necesita al menos 4 caracteres.")
                backup.export(uri, phrase.toCharArray())
                messages.value = "Copia guardada."
            } catch (exception: Exception) {
                messages.value = exception.message ?: "No se pudo crear la copia."
            }
        }
    }

    fun importBackup(uri: Uri, phrase: String) {
        viewModelScope.launch {
            try {
                backup.import(uri, phrase.toCharArray())
                messages.value = "Copia restaurada."
            } catch (exception: Exception) {
                messages.value = exception.message ?: "No se pudo leer el archivo. Revisa la frase."
            }
        }
    }

    fun wipe() {
        viewModelScope.launch {
            exporters.wipe()
            messages.value = "Se borraron los datos de este teléfono."
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onReview: () -> Unit,
    onBudgets: () -> Unit,
    onGoals: () -> Unit,
    onRecurring: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val sources by viewModel.sources.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var categoryName by remember { mutableStateOf("") }
    var categoryDirection by remember { mutableStateOf("expense") }
    var color by remember { mutableStateOf(swatches.first()) }
    var icon by remember { mutableStateOf(iconChoices.first()) }
    var pin by remember { mutableStateOf("") }
    var pinAgain by remember { mutableStateOf("") }
    var phrase by remember { mutableStateOf("") }
    var confirmWipe by remember { mutableStateOf(false) }
    val month = Periods.shift(Periods.resolve(PeriodKind.MONTH, LocalDate.now(BOGOTA)), viewModel.exportSteps)
    val colors = LocalLukaColors.current
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) viewModel.exportCsv(uri)
    }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) viewModel.exportPdf(uri)
    }
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) viewModel.exportBackup(uri, phrase)
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importBackup(uri, phrase)
    }

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = Space.s20, end = Space.s20, top = Space.s20, bottom = Space.s32),
        verticalArrangement = Arrangement.spacedBy(Space.s12),
    ) {
        item {
            Text("Ajustes", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s12)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s12)) {
                    SettingsLink(
                        icon = Icons.Filled.Inbox,
                        tint = colors.info,
                        background = colors.info.copy(alpha = 0.16f),
                        title = "Por revisar",
                        onClick = onReview,
                        modifier = Modifier.weight(1f),
                    )
                    SettingsLink(
                        icon = Icons.Filled.PieChart,
                        tint = MaterialTheme.colorScheme.primary,
                        background = MaterialTheme.colorScheme.secondaryContainer,
                        title = "Presupuestos",
                        onClick = onBudgets,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s12)) {
                    SettingsLink(
                        icon = Icons.Filled.Flag,
                        tint = colors.warning,
                        background = colors.warning.copy(alpha = 0.16f),
                        title = "Metas",
                        onClick = onGoals,
                        modifier = Modifier.weight(1f),
                    )
                    SettingsLink(
                        icon = Icons.Filled.EventRepeat,
                        tint = MaterialTheme.colorScheme.secondary,
                        background = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                        title = "Recurrentes",
                        onClick = onRecurring,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        item {
            LukaCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.s16)) {
                    Text("Apps monitoreadas", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    AppsBlock(sources, viewModel)
                }
            }
        }
        item {
            LukaCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.s12)) {
                    Text("Categorías", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    OutlinedTextField(categoryName, { categoryName = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Space.s8),
                        verticalArrangement = Arrangement.spacedBy(Space.s8),
                    ) {
                        LukaChip(categoryDirection == "expense", { categoryDirection = "expense" }, "Gasto")
                        LukaChip(categoryDirection == "income", { categoryDirection = "income" }, "Ingreso")
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Space.s8),
                        verticalArrangement = Arrangement.spacedBy(Space.s8),
                    ) {
                        swatches.forEachIndexed { index, hex ->
                            val selected = color == hex
                            CategoryBadge(
                                icon = if (selected) Icons.Filled.Check else Icons.Filled.Circle,
                                color = colorFromHex(hex),
                                contentDescription = if (selected) "Color ${index + 1} seleccionado" else "Elegir color ${index + 1}",
                                onClick = { color = hex },
                                modifier = Modifier.border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else colors.border,
                                    shape = CircleShape,
                                ),
                            )
                        }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Space.s8),
                        verticalArrangement = Arrangement.spacedBy(Space.s8),
                    ) {
                        iconChoices.forEach { key ->
                            val selected = icon == key
                            CategoryBadge(
                                icon = iconFor(key),
                                color = MaterialTheme.colorScheme.primary,
                                contentDescription = iconLabel(key),
                                onClick = { icon = key },
                                modifier = Modifier.border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else colors.border,
                                    shape = CircleShape,
                                ),
                            )
                        }
                    }
                    PrimaryButton(
                        text = "Crear categoría",
                        onClick = { viewModel.saveCategory(categoryName, categoryDirection, color, icon) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    CategoriesBlock(categories, viewModel)
                }
            }
        }
        item {
            LukaCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.s8)) {
                    Text("Tema", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("Luka se ve siempre en claro.", color = colors.muted, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            LukaCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.s12)) {
                    Text("Bloqueo", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Pedir huella o PIN al abrir", modifier = Modifier.weight(1f))
                        Switch(checked = settings.appLockEnabled, onCheckedChange = viewModel::setLock)
                    }
                    if (!PinHasher.isConfigured(settings.pinSalt, settings.pinHash)) {
                        Text("Guarda un PIN de al menos 4 dígitos para activar el bloqueo.", color = MaterialTheme.colorScheme.error)
                    }
                    OutlinedTextField(
                        pin,
                        { pin = it.filter { ch -> ch.isDigit() }.take(8) },
                        label = { Text("PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        pinAgain,
                        { pinAgain = it.filter { ch -> ch.isDigit() }.take(8) },
                        label = { Text("Repite el PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PrimaryButton(text = "Guardar PIN", onClick = { viewModel.setPin(pin, pinAgain) }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        item {
            LukaCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.s12)) {
                    Text("Exportar", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.exportSteps -= 1 }) {
                            Icon(Icons.Filled.ChevronLeft, contentDescription = "Mes anterior")
                        }
                        Text(
                            Periods.label(month),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                        IconButton(onClick = { viewModel.exportSteps += 1 }) {
                            Icon(Icons.Filled.ChevronRight, contentDescription = "Mes siguiente")
                        }
                    }
                    PrimaryButton(text = "Exportar CSV", onClick = { csvLauncher.launch("luka-movimientos.csv") }, modifier = Modifier.fillMaxWidth())
                    PrimaryButton(text = "Exportar PDF", onClick = { pdfLauncher.launch("luka-periodo.pdf") }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        item {
            LukaCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.s12)) {
                    Text("Copia cifrada", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("La frase no se guarda. Sin ella no se puede abrir el archivo.", color = colors.muted, style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(phrase, { phrase = it }, label = { Text("Frase") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                    PrimaryButton(text = "Crear copia", onClick = { backupLauncher.launch("luka-copia.luka") }, modifier = Modifier.fillMaxWidth())
                    SecondaryButton(text = "Restaurar copia", onClick = { restoreLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        item {
            LukaCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.s12)) {
                    Text("Datos", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    DangerButton("Borrar todos los datos", onClick = { confirmWipe = true })
                }
            }
        }
        message?.let { text ->
            item { Text(text, style = MaterialTheme.typography.bodyLarge) }
        }
        item {
            Text(
                "Montos en ${settings.currency}. Ejemplo ${CopMoney.format(1_250_000)}.",
                color = colors.muted,
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
            )
        }
    }
    if (confirmWipe) {
        AlertDialog(
            onDismissRequest = { confirmWipe = false },
            title = { Text("Borrar todos los datos") },
            text = { Text("Se borran movimientos, metas, presupuestos y ajustes de este teléfono. Las categorías base se vuelven a crear.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmWipe = false
                    viewModel.wipe()
                }) { Text("Borrar") }
            },
            dismissButton = { TextButton(onClick = { confirmWipe = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun SettingsLink(
    icon: ImageVector,
    tint: Color,
    background: Color,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LukaCard(
        modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Box(
            Modifier.size(40.dp).background(background, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = Space.s12),
        )
    }
}

@Composable
private fun AppsBlock(sources: List<co.luka.data.db.SourceEntity>, viewModel: SettingsViewModel) {
    sources.filter { it.kind == "app" }.forEach { source ->
        var pkg by remember(source.packageName) { mutableStateOf(source.packageName.orEmpty()) }
        Column(verticalArrangement = Arrangement.spacedBy(Space.s8)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(source.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Switch(checked = source.monitorNotifications, onCheckedChange = { viewModel.toggleSource(source.id, it) })
            }
            OutlinedTextField(pkg, { pkg = it }, label = { Text("Paquete") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            SecondaryButton(text = "Guardar paquete", onClick = { viewModel.packageName(source.id, pkg) }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun CategoriesBlock(categories: List<CategoryEntity>, viewModel: SettingsViewModel) {
    categories.forEach { category ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            CategoryBadge(
                icon = iconFor(category.icon),
                color = colorFromHex(category.color),
                contentDescription = category.name,
            )
            Text(
                if (category.archived) "${category.name} archivada" else category.name,
                modifier = Modifier.padding(start = Space.s12).weight(1f),
                style = MaterialTheme.typography.bodyLarge,
            )
            TextButton(onClick = { viewModel.archiveCategory(category, !category.archived) }) {
                Text(if (category.archived) "Restaurar" else "Archivar")
            }
        }
    }
}

@Composable
private fun DangerButton(text: String, onClick: () -> Unit) {
    val colors = LocalLukaColors.current
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(colors.expenseContainer)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = colors.expense,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

private fun iconLabel(key: String): String = when (key) {
    "restaurant" -> "Comida"
    "bus" -> "Transporte"
    "home" -> "Casa"
    "bolt" -> "Servicios"
    "health" -> "Salud"
    "bag" -> "Compras"
    "movie" -> "Ocio"
    "repeat" -> "Suscripciones"
    "payments" -> "Pagos"
    "store" -> "Tienda"
    "wallet" -> "Billetera"
    "spark" -> "Destacado"
    else -> "General"
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
