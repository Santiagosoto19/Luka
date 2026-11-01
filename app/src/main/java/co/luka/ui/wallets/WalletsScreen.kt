package co.luka.ui.wallets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import co.luka.data.db.MovementListRow
import co.luka.data.db.SourceEntity
import co.luka.data.repository.CatalogRepository
import co.luka.data.repository.MovementRepository
import co.luka.data.repository.StatsRepository
import co.luka.ui.components.CountingBalance
import co.luka.ui.components.EnterPiece
import co.luka.ui.components.PressScale
import co.luka.ui.components.PrimaryButton
import co.luka.ui.components.TransactionItem
import co.luka.ui.components.animationsEnabled
import co.luka.ui.components.signedBalance
import co.luka.ui.theme.LocalLukaColors
import co.luka.ui.theme.LukaSheetShape
import co.luka.ui.theme.LukaTheme
import co.luka.ui.theme.Space
import co.luka.ui.theme.colorFromHex
import co.luka.ui.theme.lukaIndigoShadow
import co.luka.ui.theme.lukaWhiteCard
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WalletsUi(
    val sources: List<SourceEntity>,
    val balances: Map<Long, Long>,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WalletsViewModel @Inject constructor(
    private val catalog: CatalogRepository,
    stats: StatsRepository,
    private val movements: MovementRepository,
) : ViewModel() {
    val ui = combine(catalog.observeSources(), stats.revision()) { sources, _ -> sources }
        .mapLatest { sources ->
            WalletsUi(
                sources = sources.filter { !it.archived },
                balances = movements.balances(),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    var recent by mutableStateOf<Map<Long, List<MovementListRow>>>(emptyMap())
        private set

    fun loadRecent(sourceId: Long) {
        viewModelScope.launch {
            val rows = movements.recentInRange("0001-01-01", "9999-12-31", sourceId, 8)
            recent = recent + (sourceId to rows)
        }
    }

    fun saveOpening(sourceId: Long, amount: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            catalog.setOpeningBalance(sourceId, amount)
            onDone()
        }
    }

    fun reconcile(sourceId: Long, amount: Long, onResult: (matched: Boolean) -> Unit) {
        viewModelScope.launch {
            onResult(movements.reconcile(sourceId, amount))
        }
    }
}

@Composable
fun WalletsScreen(viewModel: WalletsViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    var opening by remember { mutableStateOf<SourceEntity?>(null) }
    var reconcile by remember { mutableStateOf<SourceEntity?>(null) }
    var reconcileNote by remember { mutableStateOf<String?>(null) }
    var checkedId by remember { mutableStateOf<Long?>(null) }
    var sheetCheck by remember { mutableStateOf(false) }
    var checkToken by remember { mutableIntStateOf(0) }
    LaunchedEffect(checkToken) {
        if (checkToken == 0) return@LaunchedEffect
        delay(1100)
        checkedId = null
        sheetCheck = false
    }
    WalletsContent(
        ui,
        recent = viewModel.recent,
        checkedId = checkedId,
        onOpening = { opening = it },
        onReconcile = {
            reconcileNote = null
            sheetCheck = false
            reconcile = it
        },
        onNeedRecent = viewModel::loadRecent,
    )
    opening?.let { source ->
        AmountSheet(
            title = "Saldo inicial",
            help = "El número que ves tú. Luka no entra a la otra app.",
            initialDigits = source.openingBalance.toString(),
            message = null,
            onDismiss = { opening = null },
            onSave = { amount -> viewModel.saveOpening(source.id, amount) { opening = null } },
        )
    }
    reconcile?.let { source ->
        AmountSheet(
            title = "¿Cuánto hay en ${source.name}?",
            help = null,
            initialDigits = "",
            message = reconcileNote,
            showCheck = sheetCheck,
            onDismiss = {
                reconcile = null
                reconcileNote = null
            },
            onSave = { amount ->
                viewModel.reconcile(source.id, amount) { matched ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    checkedId = source.id
                    checkToken += 1
                    if (matched) {
                        reconcileNote = "Ya coincide."
                        sheetCheck = true
                    } else {
                        reconcileNote = null
                        sheetCheck = false
                        reconcile = null
                    }
                }
            },
        )
    }
}

@Composable
private fun WalletsContent(
    ui: WalletsUi?,
    recent: Map<Long, List<MovementListRow>> = emptyMap(),
    checkedId: Long? = null,
    onOpening: (SourceEntity) -> Unit = {},
    onReconcile: (SourceEntity) -> Unit = {},
    onNeedRecent: (Long) -> Unit = {},
) {
    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = Space.s20, end = Space.s20, top = Space.s20, bottom = Space.s32),
        verticalArrangement = Arrangement.spacedBy(Space.s12),
    ) {
        if (ui != null && ui.sources.isEmpty()) {
            item {
                Box(Modifier.fillParentMaxSize()) {
                    WalletsHeader(Modifier.align(Alignment.TopStart))
                    WalletsEmpty(Modifier.align(Alignment.Center))
                }
            }
        } else {
            item { WalletsHeader() }
            if (ui != null && ui.sources.isNotEmpty()) {
                item { WalletTotal(ui) }
            }
            itemsIndexed(ui?.sources.orEmpty(), key = { _, source -> source.id }) { index, source ->
                WalletCard(
                    source = source,
                    balance = ui?.balances?.get(source.id) ?: 0L,
                    onOpening = { onOpening(source) },
                    onReconcile = { onReconcile(source) },
                    index = index,
                    recent = recent[source.id],
                    checked = checkedId == source.id,
                    onNeedRecent = { onNeedRecent(source.id) },
                )
            }
        }
    }
}

@Composable
private fun WalletsHeader(modifier: Modifier = Modifier) {
    Text(
        "Carteras",
        modifier = modifier,
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
    )
}

@Composable
private fun WalletsEmpty(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = Space.s12),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.s8),
    ) {
        Box(
            Modifier.size(72.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.AccountBalanceWallet,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
        }
        Text("Todavía no hay carteras", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
        Text(
            "Cuando haya una fuente, verás aquí su saldo.",
            color = LocalLukaColors.current.muted,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WalletTotal(ui: WalletsUi) {
    val total = ui.sources.sumOf { ui.balances[it.id] ?: 0L }
    Column(verticalArrangement = Arrangement.spacedBy(Space.s8)) {
        Text("Total", color = LocalLukaColors.current.muted, style = MaterialTheme.typography.bodyMedium)
        CountingBalance(
            amount = total,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        WalletShareBar(ui)
    }
}

@Composable
private fun WalletShareBar(ui: WalletsUi) {
    val parts = ui.sources.map { source -> source to abs(ui.balances[source.id] ?: 0L) }
    val sum = parts.sumOf { it.second }
    if (sum == 0L) return
    Row(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(99.dp)),
    ) {
        parts.forEach { (source, amount) ->
            if (amount > 0L) {
                Box(
                    Modifier
                        .weight(amount.toFloat())
                        .fillMaxHeight()
                        .background(colorFromHex(source.color)),
                )
            }
        }
    }
}

@Composable
private fun WalletCard(
    source: SourceEntity,
    balance: Long,
    onOpening: () -> Unit,
    onReconcile: () -> Unit,
    index: Int = 0,
    recent: List<MovementListRow>? = emptyList(),
    checked: Boolean = false,
    onNeedRecent: () -> Unit = {},
) {
    val colors = LocalLukaColors.current
    val motion = animationsEnabled()
    val interaction = remember { MutableInteractionSource() }
    var expanded by remember { mutableStateOf(false) }
    val tone = when {
        balance > 0L -> colors.income
        balance < 0L -> colors.expense
        else -> MaterialTheme.colorScheme.onSurface
    }
    LaunchedEffect(expanded) {
        if (expanded) onNeedRecent()
    }
    EnterPiece(index) {
        PressScale(interaction) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .lukaWhiteCard(RoundedCornerShape(28.dp)),
            ) {
                Box(Modifier.width(8.dp).fillMaxHeight().background(colorFromHex(source.color)))
                Column(Modifier.weight(1f)) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 112.dp)
                            .clickable(
                                interactionSource = interaction,
                                indication = ripple(),
                                onClick = { expanded = !expanded },
                            )
                            .padding(horizontal = Space.s20, vertical = Space.s16),
                        verticalArrangement = Arrangement.spacedBy(Space.s8),
                    ) {
                        Text(source.name, style = MaterialTheme.typography.titleMedium)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Space.s8),
                        ) {
                            Text(
                                signedBalance(balance),
                                color = tone,
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum",
                                    fontFamily = colors.displayFont,
                                ),
                            )
                            if (balance < 0L) ExpensePulse()
                        }
                        Text("Saldo", color = colors.muted, style = MaterialTheme.typography.bodyMedium)
                    }
                    AnimatedVisibility(
                        visible = expanded,
                        enter = if (motion) fadeIn() + expandVertically() else EnterTransition.None,
                        exit = if (motion) fadeOut() + shrinkVertically() else ExitTransition.None,
                    ) {
                        Column(
                            Modifier.padding(horizontal = Space.s20).padding(bottom = Space.s8),
                            verticalArrangement = Arrangement.spacedBy(Space.s4),
                        ) {
                            when {
                                recent == null -> Unit
                                recent.isEmpty() -> Text(
                                    "Sin movimientos",
                                    color = colors.muted,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                else -> recent.forEach { row ->
                                    TransactionItem(row, "category", row.occurredDay, hidden = false)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(Space.s8)) {
                                TextButton(onClick = onOpening) { Text("Saldo inicial") }
                                TextButton(onClick = onReconcile) {
                                    if (checked) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = "Listo",
                                            tint = colors.income,
                                        )
                                    } else {
                                        Text("Cuadrar")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpensePulse() {
    val color = LocalLukaColors.current.expense
    val animate = animationsEnabled()
    val alpha = remember { Animatable(if (animate) 0.4f else 1f) }
    LaunchedEffect(animate) {
        if (!animate) {
            alpha.snapTo(1f)
            return@LaunchedEffect
        }
        while (isActive) {
            alpha.animateTo(1f, tween(800))
            alpha.animateTo(0.4f, tween(800))
        }
    }
    Box(
        Modifier
            .size(8.dp)
            .background(color.copy(alpha = alpha.value), CircleShape),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AmountSheet(
    title: String,
    help: String?,
    initialDigits: String,
    message: String?,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit,
    showCheck: Boolean = false,
) {
    var digits by remember { mutableStateOf(initialDigits.filter { it.isDigit() }) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = LukaSheetShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.s20)
                .padding(top = Space.s8, bottom = Space.s32),
            verticalArrangement = Arrangement.spacedBy(Space.s12),
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
            if (help != null) {
                Text(help, color = LocalLukaColors.current.muted, style = MaterialTheme.typography.bodyMedium)
            }
            OutlinedTextField(
                value = digits,
                onValueChange = { digits = it.filter { char -> char.isDigit() }.take(12) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Monto") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            if (message != null) {
                Text(message, color = LocalLukaColors.current.muted, style = MaterialTheme.typography.bodyMedium)
            }
            if (showCheck) {
                val shape = RoundedCornerShape(16.dp)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .lukaIndigoShadow(shape)
                        .clip(shape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "Listo",
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            } else {
                PrimaryButton(
                    text = "Guardar",
                    enabled = digits.isNotEmpty(),
                    onClick = {
                        val amount = digits.toLongOrNull() ?: return@PrimaryButton
                        onSave(amount)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Preview(name = "Carteras con datos", showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 360)
@Composable
private fun WalletsDataPreview() {
    LukaTheme("light") {
        Column(Modifier.padding(Space.s20), verticalArrangement = Arrangement.spacedBy(Space.s12)) {
            WalletCard(previewSource("Nequi", "#5B5BF0"), 340_000, {}, {})
            WalletCard(previewSource("Efectivo", "#12B886"), -12_000, {}, {})
            WalletCard(previewSource("Nu", "#6B5B95"), 0, {}, {})
        }
    }
}

@Preview(name = "Carteras vacías", showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 360, heightDp = 420)
@Composable
private fun WalletsEmptyPreview() {
    LukaTheme("light") {
        WalletsContent(WalletsUi(emptyList(), emptyMap()))
    }
}

private fun previewSource(name: String, color: String) = SourceEntity(
    slug = name.lowercase(),
    name = name,
    kind = "app",
    packageName = null,
    parserId = null,
    color = color,
    icon = "wallet",
    monitorNotifications = false,
    smsSender = null,
    sortOrder = 0,
    archived = false,
)
