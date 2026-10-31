package co.luka.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.Surface
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import co.luka.ui.theme.LocalLukaColors
import co.luka.ui.theme.LukaTheme
import co.luka.ui.theme.Space
import co.luka.ui.theme.lukaIndigoShadow
import co.luka.ui.theme.lukaSoftShadow

@Composable
fun ExpandableFab(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onExpense: () -> Unit,
    onIncome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        AnimatedVisibility(
            visible = expanded,
            modifier = Modifier.matchParentSize(),
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(140)),
        ) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.28f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onExpandedChange(false) },
                    ),
            )
        }
        Column(
            Modifier.align(Alignment.BottomEnd).padding(end = Space.s20, bottom = Space.s16),
            horizontalAlignment = Alignment.End,
        ) {
            FabAction(
                visible = expanded,
                delay = 60,
                label = "Gasto",
                onClick = {
                    onExpandedChange(false)
                    onExpense()
                },
            )
            FabAction(
                visible = expanded,
                delay = 0,
                label = "Ingreso",
                income = true,
                onClick = {
                    onExpandedChange(false)
                    onIncome()
                },
            )
            AddPill(expanded = expanded, onClick = { onExpandedChange(!expanded) })
        }
    }
}

@Composable
private fun FabAction(
    visible: Boolean,
    delay: Int,
    label: String,
    onClick: () -> Unit,
    income: Boolean = false,
) {
    val colors = LocalLukaColors.current
    val bubble = if (income) colors.incomeContainer else colors.expenseContainer
    val tint = if (income) colors.income else colors.expense
    val icon = if (income) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180, delayMillis = delay)) +
            slideInVertically(tween(220, delayMillis = delay)) { it / 2 } +
            scaleIn(tween(180, delayMillis = delay), initialScale = 0.86f),
        exit = fadeOut(tween(120)),
    ) {
        Column(horizontalAlignment = Alignment.End) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.s8),
            modifier = Modifier.clickable(onClick = onClick),
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 6.dp,
            ) {
                Text(label, modifier = Modifier.padding(horizontal = Space.s16, vertical = Space.s8), style = MaterialTheme.typography.titleMedium)
            }
            Box(
                Modifier
                    .size(52.dp)
                    .lukaSoftShadow(CircleShape)
                    .clip(CircleShape)
                    .background(bubble),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = label, tint = tint)
            }
        }
        Spacer(Modifier.height(Space.s12))
        }
    }
}

@Composable
private fun AddPill(expanded: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, tween(90), label = "fabPress")
    val shape = RoundedCornerShape(28.dp)
    val brush = Brush.horizontalGradient(
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
    )
    Row(
        Modifier
            .scale(scale)
            .height(56.dp)
            .lukaIndigoShadow(shape)
            .clip(shape)
            .background(brush)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = if (expanded) Space.s16 else Space.s20),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (expanded) Icons.Filled.Close else Icons.Filled.Add,
            contentDescription = if (expanded) "Cerrar" else "Agregar",
            tint = MaterialTheme.colorScheme.onPrimary,
        )
        if (!expanded) {
            Text(
                "Agregar",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = Space.s8),
            )
        }
    }
}

@Preview(name = "Fab en reposo", showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 360, heightDp = 280)
@Composable
private fun FabClosedPreview() {
    LukaTheme("light") {
        ExpandableFab(expanded = false, onExpandedChange = {}, onExpense = {}, onIncome = {}, modifier = Modifier.height(280.dp))
    }
}

@Preview(name = "Fab abierto", showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 360, heightDp = 360)
@Composable
private fun FabOpenPreview() {
    LukaTheme("light") {
        ExpandableFab(expanded = true, onExpandedChange = {}, onExpense = {}, onIncome = {}, modifier = Modifier.height(360.dp))
    }
}
