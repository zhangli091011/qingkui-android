package cn.qingkui.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.qingkui.app.ui.model.*

@Composable
fun AdminConsoleScreen(
    users: List<AdminUserItem> = emptyList(), sessions: List<AdminSessionItem> = emptyList(), section: String = "总览", loading: Boolean = false,
    nodes: List<AdminNodeItem> = emptyList(), edges: List<AdminEdgeItem> = emptyList(), documents: List<AdminDocumentItem> = emptyList(), formulas: List<AdminFormulaItem> = emptyList(), ocrTasks: List<AdminOcrItem> = emptyList(), auditLogs: List<AdminAuditItem> = emptyList(), alerts: List<AdminAlertItem> = emptyList(), checks: List<AdminCheckItem> = emptyList(), feedback: List<AdminFeedbackItem> = emptyList(), error: String? = null,
    onSection: (String) -> Unit = {}, onRefresh: () -> Unit = {}, onToggleUser: (String, Boolean) -> Unit = { _, _ -> }, onRevokeSession: (String) -> Unit = {}, onApproveNode: (String) -> Unit = {}, onWithdrawNode: (String) -> Unit = {}, onDeleteEdge: (String) -> Unit = {}, onReviewFormula: (String, String) -> Unit = { _, _ -> }, onRetryOcr: (String) -> Unit = {}, onCancelOcr: (String) -> Unit = {}, onClose: () -> Unit = {},
) {
    val sections = listOf("总览", "用户", "会话", "知识节点", "关系图谱", "文档", "OCR/公式", "错题任务", "反馈", "审计日志", "系统告警", "发布门禁")
    var query by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text("管理控制台", style = MaterialTheme.typography.headlineSmall); Row { IconButton(onClick = onRefresh) { Icon(Icons.Outlined.Refresh, "刷新") }; IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, "关闭") } } }
        if (!error.isNullOrBlank()) Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { sections.forEach { FilterChip(selected = section == it, onClick = { onSection(it) }, label = { Text(it) }) } }
        Spacer(Modifier.height(8.dp))
        if (loading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } else {
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("搜索当前模块") })
            Spacer(Modifier.height(8.dp)); val q = query.trim()
            when (section) {
                "总览" -> Overview(users, sessions, nodes, documents, formulas, ocrTasks, alerts, checks)
                "用户" -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(users.filter { q.isBlank() || it.username.contains(q, true) || it.nickname.contains(q, true) }) { UserRow(it, onToggleUser) } }
                "会话" -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(sessions.filter { q.isBlank() || it.username.contains(q, true) || it.deviceName.contains(q, true) }) { SessionRow(it, onRevokeSession) } }
                "知识节点" -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(nodes.filter { q.isBlank() || it.name.contains(q, true) || it.subject.contains(q, true) || it.grade.contains(q, true) }) { NodeRow(it, onApproveNode, onWithdrawNode) } }
                "关系图谱" -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(edges.filter { q.isBlank() || it.sourceNodeId.contains(q, true) || it.targetNodeId.contains(q, true) }) { EdgeRow(it, onDeleteEdge) } }
                "文档" -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(documents.filter { q.isBlank() || it.title.contains(q, true) }) { DocumentRow(it) } }
                "OCR/公式" -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(formulas.filter { q.isBlank() || it.documentTitle.contains(q, true) }) { FormulaRow(it, onReviewFormula) } }
                "错题任务" -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(ocrTasks.filter { q.isBlank() || it.status.contains(q, true) }) { OcrRow(it, onRetryOcr, onCancelOcr) } }
                "反馈" -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(feedback) { SimpleRow("${it.category} · ${it.status}", it.content, it.createdAt) } }
                "审计日志" -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(auditLogs) { SimpleRow(it.action, "${it.targetType} ${it.targetId ?: ""}", it.createdAt) } }
                "系统告警" -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(alerts) { AlertRow(it) } }
                "发布门禁" -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { item { Text(if (checks.all { it.passed }) "可发布" else "发布被阻断", style = MaterialTheme.typography.headlineSmall, color = if (checks.all { it.passed }) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) }; items(checks) { CheckRow(it) } }
            }
        }
    }
}

@Composable private fun Overview(users: List<AdminUserItem>, sessions: List<AdminSessionItem>, nodes: List<AdminNodeItem>, docs: List<AdminDocumentItem>, formulas: List<AdminFormulaItem>, ocr: List<AdminOcrItem>, alerts: List<AdminAlertItem>, checks: List<AdminCheckItem>) { LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { item { Text("运营数据总览", style = MaterialTheme.typography.titleLarge) }; item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatCard("用户", users.size.toString(), Modifier.weight(1f)); StatCard("节点", nodes.size.toString(), Modifier.weight(1f)); StatCard("文档", docs.size.toString(), Modifier.weight(1f)) } }; item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatCard("活跃会话", sessions.count { it.active }.toString(), Modifier.weight(1f)); StatCard("待审公式", formulas.count { it.reviewStatus == "pending" }.toString(), Modifier.weight(1f)); StatCard("OCR任务", ocr.size.toString(), Modifier.weight(1f)) } }; item { Text("告警 ${alerts.size} · 发布检查 ${checks.count { it.passed }}/${checks.size}", color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
@Composable private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) { Card(modifier) { Column(Modifier.padding(12.dp)) { Text(label, style = MaterialTheme.typography.labelMedium); Text(value, style = MaterialTheme.typography.headlineMedium) } } }
@Composable private fun UserRow(u: AdminUserItem, toggle: (String, Boolean) -> Unit) { Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(u.nickname.ifBlank { u.username }); Text("@${u.username} · ${u.role} · 额度 ${u.balance}", style = MaterialTheme.typography.bodySmall) }; Switch(u.isActive, { toggle(u.id, it) }) } } }
@Composable private fun SessionRow(s: AdminSessionItem, revoke: (String) -> Unit) { Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(s.username); Text("${s.deviceName} · ${s.expiresAt}", style = MaterialTheme.typography.bodySmall) }; if (s.active) TextButton({ revoke(s.id) }) { Text("撤销") } } } }
@Composable private fun NodeRow(n: AdminNodeItem, approve: (String) -> Unit, withdraw: (String) -> Unit) { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Text(n.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)); AssistChip(onClick = {}, label = { Text(n.reviewStatus) }) }; Text("${n.subject} · ${n.grade} · ${n.chapter}", style = MaterialTheme.typography.bodySmall); Text(n.definition, maxLines = 2, overflow = TextOverflow.Ellipsis); Text("来源：${n.sourceExcerpt}", style = MaterialTheme.typography.bodySmall); Row { if (n.reviewStatus != "approved") TextButton({ approve(n.id) }) { Text("发布") }; if (n.isActive) TextButton({ withdraw(n.id) }) { Text("撤回") } } } } }
@Composable private fun EdgeRow(e: AdminEdgeItem, del: (String) -> Unit) { Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("${e.sourceNodeId}  →  ${e.targetNodeId}"); Text("${e.edgeType} · ${e.explanation}", style = MaterialTheme.typography.bodySmall) }; TextButton({ del(e.id) }) { Text("删除") } } } }
@Composable private fun DocumentRow(d: AdminDocumentItem) { SimpleRow(d.title, "${d.subject ?: "未分科"} · ${d.grade ?: "未分年级"} · ${d.status} · 授权 ${d.authorizationStatus}", "分块 ${d.chunkCount} · 公式 ${d.formulaCount}（待审 ${d.pendingFormulaCount}）") }
@Composable private fun FormulaRow(f: AdminFormulaItem, review: (String, String) -> Unit) { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { Text(f.documentTitle); Text(f.formulaLatex ?: "未识别公式", maxLines = 2, overflow = TextOverflow.Ellipsis); Text("置信度 ${f.confidence?.let { "%.2f".format(it) } ?: "-"} · ${f.reviewStatus ?: "pending"}", style = MaterialTheme.typography.bodySmall); Row { TextButton({ review(f.id, "approved") }) { Text("通过") }; TextButton({ review(f.id, "rejected") }) { Text("驳回") } } } } }
@Composable private fun OcrRow(t: AdminOcrItem, retry: (String) -> Unit, cancel: (String) -> Unit) { Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("任务 ${t.id}"); Text("错题 ${t.mistakeId} · 用户 ${t.userId} · ${t.status}", style = MaterialTheme.typography.bodySmall) }; if (t.status == "failed") TextButton({ retry(t.id) }) { Text("重试") }; if (t.status == "queued" || t.status == "processing") TextButton({ cancel(t.id) }) { Text("取消") } } } }
@Composable private fun AlertRow(a: AdminAlertItem) { SimpleRow("${a.severity} · ${a.code}", a.message, "${a.value} / 阈值 ${a.threshold}") }
@Composable private fun CheckRow(c: AdminCheckItem) { ListItem(headlineContent = { Text(if (c.passed) "✓ ${c.label}" else "✕ ${c.label}") }, supportingContent = { Text(c.detail) }) }
@Composable private fun SimpleRow(title: String, body: String, foot: String) { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(body, maxLines = 3, overflow = TextOverflow.Ellipsis); Text(foot, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
