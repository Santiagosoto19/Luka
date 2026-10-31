package co.luka.ui.movements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import co.luka.data.db.CategoryEntity
import co.luka.data.db.SourceEntity
import co.luka.data.repository.ManualEntry
import co.luka.domain.model.Direction
import co.luka.domain.money.CopMoney
import co.luka.domain.time.BOGOTA
import co.luka.domain.time.PeriodKind
import co.luka.domain.time.Periods
import co.luka.ui.components.PrimaryButton
import co.luka.ui.theme.LocalLukaColors
import co.luka.ui.theme.Space
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MovementForm(
    categories: List<CategoryEntity>,
    sources: List<SourceEntity>,
    error: String?,
    initialDirection: Direction = Direction.EXPENSE,
    initialAmount: String = "",
    initialCategoryId: Long? = null,
    initialNote: String = "",
    initialCounterparty: String = "",
    initialOccurredAt: Long = System.currentTimeMillis(),
    initialSourceId: Long? = null,
    initialCounterSourceId: Long? = null,
    allowTransfer: Boolean = true,
    onPickDate: (LocalDate, (LocalDate) -> Unit) -> Unit,
    onSave: (ManualEntry) -> Unit,
) {
    var digits by remember { mutableStateOf(initialAmount.filter { it.isDigit() }) }
    var direction by remember { mutableStateOf(initialDirection) }
    var categoryId by remember { mutableStateOf(initialCategoryId) }
    var note by remember { mutableStateOf(initialNote) }
    var counterparty by remember { mutableStateOf(initialCounterparty) }
    var occurredAt by remember { mutableStateOf(initialOccurredAt) }
    var sourceId by remember { mutableStateOf(initialSourceId) }
    var counterSourceId by remember { mutableStateOf(initialCounterSourceId) }
    var localError by remember { mutableStateOf<String?>(null) }
    val day = remember(occurredAt) { Instant.ofEpochMilli(occurredAt).atZone(BOGOTA).toLocalDate() }
    val amountValue = digits.toLongOrNull()
    val amountReady = amountValue != null && amountValue > 0L
    val shown = amountValue?.let { CopMoney.format(it) } ?: "$"
    val transfer = direction == Direction.TRANSFER
    val visible = categories.filter { it.direction == direction.wire || it.direction == "both" }
    val wallets = sources.filter { !it.archived || it.id == initialSourceId || it.id == initialCounterSourceId }
    val muted = LocalLukaColors.current.muted
    fun chooseDirection(next: Direction) {
        direction = next
        localError = null
        if (next == Direction.TRANSFER) {
            categoryId = null
            return
        }
        if (categoryId != null && categories.none { it.id == categoryId && (it.direction == next.wire || it.direction == "both") }) {
            categoryId = null
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.s20)
            .padding(top = Space.s8, bottom = Space.s32),
        verticalArrangement = Arrangement.spacedBy(Space.s12),
    ) {
        Text("Registrar", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
        Text(
            shown,
            style = MaterialTheme.typography.displayLarge.copy(
                fontFeatureSettings = "tnum",
                fontSize = MaterialTheme.typography.headlineMedium.fontSize,
            ),
        )
        OutlinedTextField(
            value = digits,
            onValueChange = { digits = it.filter { char -> char.isDigit() }.take(12) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Monto") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Space.s8),
            verticalArrangement = Arrangement.spacedBy(Space.s8),
        ) {
            LukaChip(direction == Direction.EXPENSE, { chooseDirection(Direction.EXPENSE) }, "Gasto")
            LukaChip(direction == Direction.INCOME, { chooseDirection(Direction.INCOME) }, "Ingreso")
            if (allowTransfer || transfer) {
                LukaChip(transfer, { chooseDirection(Direction.TRANSFER) }, "Traslado")
            }
        }
        TextButton(onClick = {
            onPickDate(day) { picked ->
                occurredAt = picked.atStartOfDay(BOGOTA).toInstant().toEpochMilli()
            }
        }) { Text(Periods.label(Periods.resolve(PeriodKind.TODAY, day))) }
        if (transfer) {
            if (wallets.isNotEmpty()) {
                Text("Desde", color = muted, style = MaterialTheme.typography.bodyMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Space.s8),
                    verticalArrangement = Arrangement.spacedBy(Space.s8),
                ) {
                    wallets.forEach { source ->
                        LukaChip(sourceId == source.id, {
                            sourceId = if (sourceId == source.id) null else source.id
                            localError = null
                        }, source.name)
                    }
                }
                Text("Hacia", color = muted, style = MaterialTheme.typography.bodyMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Space.s8),
                    verticalArrangement = Arrangement.spacedBy(Space.s8),
                ) {
                    wallets.forEach { source ->
                        LukaChip(counterSourceId == source.id, {
                            counterSourceId = if (counterSourceId == source.id) null else source.id
                            localError = null
                        }, source.name)
                    }
                }
            }
        } else if (wallets.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Space.s8),
                verticalArrangement = Arrangement.spacedBy(Space.s8),
            ) {
                wallets.forEach { source ->
                    LukaChip(sourceId == source.id, {
                        sourceId = if (sourceId == source.id) null else source.id
                    }, source.name)
                }
            }
        }
        if (!transfer && visible.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Space.s8),
                verticalArrangement = Arrangement.spacedBy(Space.s8),
            ) {
                visible.forEach { category ->
                    LukaChip(
                        categoryId == category.id,
                        { categoryId = if (categoryId == category.id) null else category.id },
                        category.name,
                    )
                }
            }
        }
        OutlinedTextField(value = counterparty, onValueChange = { counterparty = it }, label = { Text("Comercio o persona") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Nota") }, modifier = Modifier.fillMaxWidth())
        if (!amountReady) {
            Text("Escribe un monto mayor que cero.", color = MaterialTheme.colorScheme.error)
        }
        (localError ?: error)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        PrimaryButton(
            text = "Guardar",
            enabled = amountReady,
            onClick = {
                val amount = digits.toLongOrNull()
                if (amount == null || amount <= 0L) return@PrimaryButton
                if (transfer && (sourceId == null || counterSourceId == null || sourceId == counterSourceId)) {
                    localError = "Elige dos carteras distintas."
                    return@PrimaryButton
                }
                localError = null
                onSave(
                    ManualEntry(
                        amount = amount,
                        direction = direction,
                        categoryId = if (transfer) null else categoryId,
                        note = note,
                        counterparty = counterparty,
                        occurredAt = occurredAt,
                        sourceId = sourceId,
                        counterSourceId = if (transfer) counterSourceId else null,
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
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

fun localDateFromPicker(millis: Long?): LocalDate? =
    millis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
