package cn.qingkui.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.qingkui.app.ui.model.AppDestination
import cn.qingkui.app.ui.theme.QingkuiDarkSurface

@Composable
fun DynamicIslandNav(
    selected: AppDestination,
    compact: Boolean,
    onSelect: (AppDestination) -> Unit,
    showWorkspace: Boolean = false,
    showTeacher: Boolean = false,
    showContent: Boolean = false,
    showOperations: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val destinations = buildList {
        addAll(listOf(AppDestination.Chat, AppDestination.Graph, AppDestination.Learning))
        add(AppDestination.Mistakes)
        add(AppDestination.Corpus)
        if (!compact) add(AppDestination.Account)
    }
    val height = if (compact) 56.dp else 64.dp
    val horizontalPadding = if (compact) 6.dp else 8.dp
    val gap = if (compact) 2.dp else 4.dp
    val itemWidth = if (compact) 78.dp else 112.dp

    Row(
        modifier = modifier
            .widthIn(min = (itemWidth * destinations.size) + (gap * (destinations.size - 1)) + (horizontalPadding * 2))
            .height(height)
            .background(QingkuiDarkSurface, RoundedCornerShape(28.dp))
            .padding(horizontal = horizontalPadding, vertical = if (compact) 6.dp else 8.dp),
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        destinations.forEach { destination ->
            val active = destination == selected
            Box(
                modifier = Modifier
                    .width(itemWidth)
                    .fillMaxHeight()
                    .background(
                        color = if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        shape = RoundedCornerShape(22.dp),
                    )
                    .clickable(
                        role = Role.Tab,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = { onSelect(destination) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (!compact && destination == AppDestination.Account) "更多" else destination.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else Color.White.copy(alpha = .72f),
                )
            }
        }
    }
}
