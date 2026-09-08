package cn.qingkui.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.qingkui.app.ui.model.AdminSessionItem
import cn.qingkui.app.ui.model.AdminUserItem

@Composable
fun AdminConsoleScreen(
    users: List<AdminUserItem> = emptyList(), sessions: List<AdminSessionItem> = emptyList(), section: String = "总览", loading: Boolean = false,
    onSection: (String) -> Unit = {}, onRefresh: () -> Unit = {}, onToggleUser: (String, Boolean) -> Unit = { _, _ -> },
    onRoleChange: (String, String) -> Unit = { _, _ -> }, onRevokeSession: (String) -> Unit = {}, onClose: () -> Unit = {},
) {
    val sections = listOf("总览", "用户", "会话", "内容审核", "关系图谱", "发布门禁", "系统告警")
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("管理工作台", style = MaterialTheme.typography.headlineSmall)
            Row { IconButton(onClick = onRefresh) { Icon(Icons.Outlined.Refresh, "刷新") }; IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, "关闭") } }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { sections.forEach { item -> FilterChip(selected = section == item, onClick = { onSection(item) }, label = { Text(item) }) } }
        Spacer(Modifier.height(8.dp))
        if (loading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else when (section) {
            "用户" -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(users, key = { it.id }) { user -> UserRow(user, onToggleUser) } }
            "会话" -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(sessions, key = { it.id }) { session -> SessionRow(session, onRevokeSession) } }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) { item { AdminSummary(users.size, sessions.count { it.active }) }; if (section != "总览") item { InfoCard(section) } }
        }
    }
}

@Composable private fun AdminSummary(users: Int, activeSessions: Int) { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("运营总览", style = MaterialTheme.typography.titleLarge); Text("用户数：$users"); Text("活跃会话：$activeSessions"); Text("权限由后端实时校验，当前会话自动携带令牌。", color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
@Composable private fun InfoCard(title: String) { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Text(title, style = MaterialTheme.typography.titleLarge); Text("该模块已接入原生管理工作台。", color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
@Composable private fun UserRow(user: AdminUserItem, toggle: (String, Boolean) -> Unit) { Card(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(user.nickname); Text("@${user.username} · ${user.role} · 额度 ${user.balance}", style = MaterialTheme.typography.bodySmall) }; Switch(checked = user.isActive, onCheckedChange = { toggle(user.id, it) }) } } }
@Composable private fun SessionRow(session: AdminSessionItem, revoke: (String) -> Unit) { Card(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(session.username); Text("${session.deviceName} · 到期 ${session.expiresAt}", style = MaterialTheme.typography.bodySmall) }; if (session.active) TextButton(onClick = { revoke(session.id) }) { Text("撤销") } } } }
