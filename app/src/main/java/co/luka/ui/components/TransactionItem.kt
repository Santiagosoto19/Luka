package co.luka.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import co.luka.data.db.MovementListRow
import co.luka.domain.model.Direction
import co.luka.domain.money.CopMoney
import co.luka.ui.theme.FallbackCategoryHex
import co.luka.ui.theme.LocalLukaColors
import co.luka.ui.theme.LukaTheme
import co.luka.ui.theme.Space
import co.luka.ui.theme.colorFromHex
import co.luka.ui.theme.iconFor

@Composable
fun TransactionItem(
    row: MovementListRow,
    iconKey: String,
    whenLabel: String,
    hidden: Boolean,
    modifier: Modifier = Modifier,
    enterIndex: Int? = null,
) {
    if (enterIndex != null) {
        EnterPiece(enterIndex) { TransactionItemBody(row, iconKey, whenLabel, hidden, modifier) }
    } else {
        TransactionItemBody(row, iconKey, whenLabel, hidden, modifier)
    }
}

@Composable
private fun TransactionItemBody(
    row: MovementListRow,
    iconKey: String,
    whenLabel: String,
    hidden: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLukaColors.current
    val transfer = row.direction == "transfer"
    val adjustment = row.origin == "adjustment"
    val direction = if (row.direction == "income") Direction.INCOME else Direction.EXPENSE
    val merchant = when {
        transfer -> "Traslado"
        adjustment -> "Ajuste"
        else -> row.counterparty?.takeIf { it.isNotBlank() } ?: "Sin comercio"
    }
    val meta = if (transfer) {
        listOfNotNull(row.sourceName?.let { "Desde $it" }, row.counterSourceName?.let { "Hacia $it" }).joinToString(", ")
    } else {
        listOfNotNull(row.categoryName, row.sourceName).joinToString(", ")
    }
    val tint = colorFromHex(row.categoryColor ?: FallbackCategoryHex)
    val amountColor = when {
        transfer -> MaterialTheme.colorScheme.onSurface
        direction == Direction.INCOME -> colors.income
        else -> colors.expense
    }
    Row(
        modifier.fillMaxWidth().padding(vertical = Space.s8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryBadge(iconFor(iconKey), tint, contentDescription = row.categoryName)
        Column(Modifier.weight(1f).padding(start = Space.s12)) {
            Text(
                merchant,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (meta.isNotBlank()) {
                Text(
                    meta,
                    color = colors.muted,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Space.s4)) {
            if (hidden) {
                Text(
                    "••••",
                    color = amountColor,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                    modifier = Modifier.semantics { contentDescription = "Monto oculto" },
                )
            } else if (transfer) {
                Text(
                    CopMoney.format(row.amount),
                    color = amountColor,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                )
            } else {
                MontoText(row.amount, direction)
            }
            Text(whenLabel, color = colors.muted, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun sampleRow(counterparty: String?, category: String?) = MovementListRow(
    id = 1,
    direction = "expense",
    amount = 18_500,
    occurredAt = 0L,
    occurredDay = "2026-10-30",
    categoryId = 1,
    categoryName = category,
    categoryColor = "#E36B45",
    counterparty = counterparty,
    note = null,
    sourceId = 2,
    sourceName = if (category == null) null else "Nequi",
    incomeKind = null,
    status = "confirmed",
    origin = "manual",
    counterSourceName = null,
)

@Preview(name = "Movimiento con datos", showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 360)
@Composable
private fun TransactionDataPreview() {
    LukaTheme("light") {
        TransactionItem(sampleRow("Éxito", "Comida"), "restaurant", "18:40", hidden = false, modifier = Modifier.padding(Space.s16))
    }
}

@Preview(name = "Movimiento vacío", showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 360)
@Composable
private fun TransactionEmptyPreview() {
    LukaTheme("light") {
        TransactionItem(sampleRow(null, null), "category", "Hoy", hidden = false, modifier = Modifier.padding(Space.s16))
    }
}
