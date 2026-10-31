package co.luka.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.luka.domain.money.CopMoney
import co.luka.ui.theme.LocalLukaColors
import co.luka.ui.theme.LukaTheme
import co.luka.ui.theme.Space

@Composable
fun WalletChip(
    label: String,
    balance: Long,
    color: Color,
    selected: Boolean,
    hidden: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    val background by animateColorAsState(
        if (selected) color else MaterialTheme.colorScheme.background,
        tween(180),
        label = "chipFill",
    )
    val content by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
        tween(180),
        label = "chipText",
    )
    val border by animateColorAsState(
        if (selected) color else LocalLukaColors.current.border,
        tween(180),
        label = "chipBorder",
    )
    val amount = if (hidden) "••••" else signedBalance(balance)
    Surface(
        modifier = modifier
            .border(1.dp, border, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = if (hidden) "$label, saldo oculto" else "$label, $amount"
            },
        shape = shape,
        color = background,
        contentColor = content,
        shadowElevation = 0.dp,
    ) {
        Column(Modifier.padding(horizontal = Space.s16, vertical = Space.s8)) {
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            Text(
                text = amount,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                maxLines = 1,
            )
        }
    }
}

fun signedBalance(balance: Long): String = when {
    balance > 0L -> "+${CopMoney.format(balance)}"
    else -> CopMoney.format(balance)
}

@Preview(name = "Chips con datos", showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 360)
@Composable
private fun ChipDataPreview() {
    LukaTheme("light") {
        Row(Modifier.padding(Space.s16), horizontalArrangement = Arrangement.spacedBy(Space.s8)) {
            WalletChip("Todas", 1_280_000, MaterialTheme.colorScheme.primary, true, false, {})
            WalletChip("Nequi", 340_000, LocalLukaColors.current.expense, false, false, {})
        }
    }
}

@Preview(name = "Chips vacíos", showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 360)
@Composable
private fun ChipEmptyPreview() {
    LukaTheme("light") {
        Row(Modifier.padding(Space.s16), horizontalArrangement = Arrangement.spacedBy(Space.s8)) {
            WalletChip("Todas", 0, MaterialTheme.colorScheme.primary, false, false, {})
            WalletChip("Efectivo", 0, LocalLukaColors.current.muted, false, false, {})
        }
    }
}
