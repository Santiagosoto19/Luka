package co.luka.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.luka.ui.theme.LocalLukaColors
import co.luka.ui.theme.LukaTheme
import co.luka.ui.theme.Space
import co.luka.ui.theme.lukaWhiteCard

@Composable
fun QuickAccessCard(
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    title: String,
    detail: String,
    badge: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .lukaWhiteCard(MaterialTheme.shapes.small)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(Space.s16),
        verticalArrangement = Arrangement.spacedBy(Space.s8),
    ) {
        Box {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(iconBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            if (badge > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-4).dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (badge > 9) "9+" else badge.toString(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            text = detail,
            color = LocalLukaColors.current.muted,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(name = "Acceso con datos", showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 360)
@Composable
private fun QuickDataPreview() {
    LukaTheme("light") {
        val colors = LocalLukaColors.current
        Row(Modifier.padding(Space.s16), horizontalArrangement = Arrangement.spacedBy(Space.s12)) {
            QuickAccessCard(
                Icons.Filled.Inbox,
                colors.info,
                colors.info.copy(alpha = 0.16f),
                "Por revisar",
                "2 por revisar",
                badge = 2,
                onClick = {},
                modifier = Modifier.weight(1f),
            )
            QuickAccessCard(
                Icons.Filled.Flag,
                colors.warning,
                colors.warning.copy(alpha = 0.16f),
                "Metas",
                "40 %",
                badge = 0,
                onClick = {},
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Preview(name = "Acceso vacío", showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 360)
@Composable
private fun QuickEmptyPreview() {
    LukaTheme("light") {
        val colors = LocalLukaColors.current
        Row(Modifier.padding(Space.s16), horizontalArrangement = Arrangement.spacedBy(Space.s12)) {
            QuickAccessCard(
                Icons.Filled.Inbox,
                colors.info,
                colors.info.copy(alpha = 0.16f),
                "Por revisar",
                "Al día",
                badge = 0,
                onClick = {},
                modifier = Modifier.weight(1f),
            )
            QuickAccessCard(
                Icons.Filled.Flag,
                colors.warning,
                colors.warning.copy(alpha = 0.16f),
                "Metas",
                "Sin meta",
                badge = 0,
                onClick = {},
                modifier = Modifier.weight(1f),
            )
        }
    }
}
