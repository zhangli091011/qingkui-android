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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
    searchQuery: String = "",
    onClose: () -> Unit,
    onOpenChat: () -> Unit,
    onRestoreSession: (String) -> Unit = {},
    onDeleteSession: (String) -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onSearch: (String) -> Unit = {},
    onLogout: () -> Unit,
    onLogin: () -> Unit,
) {
    Surface(
        modifier = Modifier.width(336.dp).fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .navigationBarsPadding(),
        ) {
            Text("青葵计划", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "学习会话",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(28.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = authenticated,
                singleLine = true,
                label = { Text("搜索会话") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = {
                            onSearchQueryChange("")
                            onSearch("")
                        }) {
                            Icon(Icons.Outlined.Close, contentDescription = "清空搜索")
                        }
                    } else {
                        IconButton(onClick = { onSearch(searchQuery) }) {
                            Icon(Icons.Outlined.Search, contentDescription = "搜索")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch(searchQuery) }),
            )
            Spacer(Modifier.height(12.dp))
            if (sessions.isEmpty()) {
                DrawerRow(
                    Icons.Outlined.History,
                    if (searchQuery.isBlank()) "暂无历史会话" else "未找到匹配会话",
                    onOpenChat,
                )
            } else {
                sessions.take(20).forEach { session ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        DrawerRow(Icons.Outlined.History, session.title, { onRestoreSession(session.id) }, Modifier.weight(1f))
                        IconButton(onClick = { onDeleteSession(session.id) }) {
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
