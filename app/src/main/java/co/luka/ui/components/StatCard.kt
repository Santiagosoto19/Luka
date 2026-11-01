package co.luka.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.luka.domain.model.Direction
import co.luka.ui.theme.LocalLukaColors
import co.luka.ui.theme.LukaTheme
import co.luka.ui.theme.Space
import co.luka.ui.theme.lukaWhiteCard

@Composable
fun StatCard(
    amount: Long,
    direction: Direction,
    variation: String?,
    variationColor: Color?,
    hidden: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLukaColors.current
    val income = direction == Direction.INCOME
    val tint = if (income) colors.income else colors.expense
    val bubble = if (income) colors.incomeContainer else colors.expenseContainer
    val label = if (income) "Ingreso" else "Gasto"
    Column(
        modifier
            .lukaWhiteCard(MaterialTheme.shapes.small)
            .padding(Space.s16),
        verticalArrangement = Arrangement.spacedBy(Space.s8),
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(bubble),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (income) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                contentDescription = if (income) "Ingreso" else "Gasto",
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(label, color = colors.muted, style = MaterialTheme.typography.bodyMedium)
        if (hidden) {
            Text(
                text = "••••",
                color = tint,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                modifier = Modifier.semantics { contentDescription = "Monto oculto" },
            )
        } else {
            CountingBalance(
                amount = amount,
                style = MaterialTheme.typography.titleLarge,
                color = tint,
            )
        }
        if (!variation.isNullOrBlank()) {
            Text(
                text = variation,
                color = variationColor ?: colors.muted,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
fun IncomeExpenseBar(income: Long, expense: Long, modifier: Modifier = Modifier) {
    val colors = LocalLukaColors.current
    val shape = RoundedCornerShape(99.dp)
    if (income <= 0L && expense <= 0L) {
        Box(
            modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(colors.border, shape),
        )
        return
    }
    Row(
        modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(shape),
    ) {
        if (income > 0L) {
            Box(Modifier.weight(income.toFloat()).fillMaxHeight().background(colors.income))
        }
        if (expense > 0L) {
            Box(Modifier.weight(expense.toFloat()).fillMaxHeight().background(colors.expense))
        }
    }
}

@Preview(name = "Stats con datos", showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 360)
@Composable
private fun StatDataPreview() {
    LukaTheme("light") {
        val colors = LocalLukaColors.current
        Row(Modifier.padding(Space.s16), horizontalArrangement = Arrangement.spacedBy(Space.s12)) {
            StatCard(2_450_000, Direction.INCOME, percentLabel(8), colors.income, false, Modifier.weight(1f))
            StatCard(1_170_000, Direction.EXPENSE, percentLabel(-4), colors.income, false, Modifier.weight(1f))
        }
    }
}

@Preview(name = "Stats vacías", showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 360)
@Composable
private fun StatEmptyPreview() {
    LukaTheme("light") {
        Row(Modifier.padding(Space.s16), horizontalArrangement = Arrangement.spacedBy(Space.s12)) {
            StatCard(0, Direction.INCOME, null, null, false, Modifier.weight(1f))
            StatCard(0, Direction.EXPENSE, null, null, false, Modifier.weight(1f))
        }
    }
}
