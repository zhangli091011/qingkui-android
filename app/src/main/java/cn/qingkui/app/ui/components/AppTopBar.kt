package cn.qingkui.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.qingkui.app.ui.model.AppDestination

@Composable
fun AppTopBar(
    selected: AppDestination,
    credits: Int,
    compact: Boolean,
    onMenuClick: () -> Unit,
    onSelect: (AppDestination) -> Unit,
    showWorkspace: Boolean = false,
    showTeacher: Boolean = false,
    showContent: Boolean = false,
    showOperations: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(if (compact) 96.dp else 104.dp)
            .padding(horizontal = if (compact) 20.dp else 40.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (compact) {
            QkIconButton(
                imageVector = Icons.Rounded.Menu,
                contentDescription = "打开会话菜单",
                onClick = onMenuClick,
            )
        } else {
            Row(
                modifier = Modifier.width(260.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                QkIconButton(
                    imageVector = Icons.Rounded.Menu,
                    contentDescription = "打开会话菜单",
                    onClick = onMenuClick,
                )
                Text(
                    text = "青葵计划",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        DynamicIslandNav(
            selected = selected,
            compact = compact,
            onSelect = onSelect,
            showWorkspace = showWorkspace,
            showTeacher = showTeacher,
            showContent = showContent,
            showOperations = showOperations,
        )

        if (compact) {
            QkIconButton(
                imageVector = Icons.Outlined.AccountCircle,
                contentDescription = "账户",
                onClick = { onSelect(AppDestination.Account) },
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            )
        } else {
            Row(
                modifier = Modifier.width(260.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(18.dp))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)),
                    )
                    Text(
                        text = "额度 ${"%,d".format(credits)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                QkIconButton(
                    imageVector = Icons.Outlined.AccountCircle,
                    contentDescription = "账户",
                    onClick = { onSelect(AppDestination.Account) },
                )
            }
        }
    }
}
