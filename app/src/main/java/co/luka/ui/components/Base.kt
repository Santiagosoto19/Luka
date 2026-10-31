package co.luka.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.luka.domain.model.Direction
import co.luka.domain.money.CopMoney
import co.luka.ui.theme.LocalLukaColors
import co.luka.ui.theme.LukaTheme
import kotlin.math.abs

@Composable
fun MontoText(
    amount: Long,
    direction: Direction,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
) {
    val colors = LocalLukaColors.current
    val income = direction == Direction.INCOME
    val formatted = CopMoney.format(abs(amount))
    val text = (if (income) "+" else "\u2212") + formatted
    val spoken = "${if (income) "Ingreso" else "Gasto"} $formatted"
    Text(
        text = text,
        color = if (income) colors.income else colors.expense,
        style = style.copy(
            fontFeatureSettings = "tnum",
            fontFamily = colors.displayFont,
            fontWeight = FontWeight.Bold,
        ),
        modifier = modifier.clearAndSetSemantics { contentDescription = spoken },
    )
}

@Composable
fun CategoryBadge(
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val touch = if (onClick != null) {
        Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).clickable(onClick = onClick)
    } else {
        Modifier
    }
    Box(touch, contentAlignment = Alignment.Center) {
        Box(
            modifier.size(40.dp).background(color.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = color,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
fun LukaCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalLukaColors.current
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, colors.border),
        shadowElevation = 1.dp,
        tonalElevation = 0.dp,
        content = {
            Column(Modifier.padding(20.dp), content = content)
        },
    )
}

@Preview(name = "Claro", showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 360)
@Composable
private fun BaseLightPreview() {
    LukaTheme("light") { BasePreviewContent() }
}

@Preview(name = "Oscuro", showBackground = true, backgroundColor = 0xFF0E1014, widthDp = 360)
@Composable
private fun BaseDarkPreview() {
    LukaTheme("dark") { BasePreviewContent() }
}

@Composable
private fun BasePreviewContent() {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            MontoText(1_250_000, Direction.INCOME)
            MontoText(48_500, Direction.EXPENSE)
            CategoryBadge(Icons.Filled.Restaurant, MaterialTheme.colorScheme.primary, contentDescription = "Comida")
            CategoryBadge(
                Icons.Filled.Favorite,
                LocalLukaColors.current.expense,
                contentDescription = "Salud",
                onClick = {},
            )
            LukaCard(Modifier.fillMaxWidth()) {
                Text("Este mes", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                Text("Balance", style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}
