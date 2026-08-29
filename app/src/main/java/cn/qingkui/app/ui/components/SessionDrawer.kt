package cn.qingkui.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.qingkui.app.ui.model.ConversationSummary

@Composable
fun SessionDrawer(
    authenticated: Boolean,
    sessions: List<ConversationSummary> = emptyList(),
    onClose: () -> Unit,
    onOpenChat: () -> Unit,
    onRestoreSession: (String) -> Unit = {},
    onDeleteSession: (String) -> Unit = {},
    onLogout: () -> Unit,
    onLogin: () -> Unit,
) {
    Surface(
        modifier = Modifier.width(336.dp).fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 32.dp)) {
            Text("青葵计划", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "学习会话",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(28.dp))
            DrawerRow(Icons.Outlined.Search, "搜索会话", onClose)
            if (sessions.isEmpty()) {
                DrawerRow(Icons.Outlined.History, "暂无历史会话", onOpenChat)
            } else {
                sessions.take(8).forEach { session ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        DrawerRow(Icons.Outlined.History, session.title, { onRestoreSession(session.id) }, Modifier.weight(1f))
                        androidx.compose.material3.IconButton(onClick = { onDeleteSession(session.id) }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "删除会话")
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Divider(color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(8.dp))
            DrawerRow(Icons.Outlined.HelpOutline, "帮助与数据说明", onClose)
            if (authenticated) {
                DrawerRow(Icons.Outlined.Logout, "退出登录", onLogout)
            } else {
                DrawerRow(Icons.Outlined.Person, "登录或注册", onLogin)
            }
        }
    }
}

@Composable
private fun DrawerRow(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(14.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
    }
}
