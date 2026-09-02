package cn.qingkui.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.qingkui.app.ui.model.WorkspaceAlert
import cn.qingkui.app.ui.model.WorkspaceDashboard
import cn.qingkui.app.ui.model.WorkspaceTask
import cn.qingkui.app.ui.model.AppDestination
import java.util.Locale

@Composable
fun WorkspaceScreen(
    destination: AppDestination = AppDestination.Workspace,
    dashboard: WorkspaceDashboard?,
    tasks: List<WorkspaceTask>,
    alerts: List<WorkspaceAlert>,
    compact: Boolean,
    onRefresh: () -> Unit,
) {
    val metrics = dashboard?.metrics
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = if (compact) 16.dp else 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(
                        when (destination) {
                            AppDestination.Teacher -> "教师班级工作台"
                            AppDestination.Content -> "内容治理工作台"
                            AppDestination.Operations -> "系统运维工作台"
                            else -> "运营驾驶舱"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(dashboard?.rangeLabel ?: "正在加载指标", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AssistChip(onClick = onRefresh, label = { Text("刷新") })
            }
        }
        if (metrics == null) {
            item { Text("暂无工作台数据，请先登录或稍后重试", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            item {
                val cards = listOf(
                    "活跃学生" to metrics.activeStudents.toString(),
                    "7 日回访率" to percent(metrics.sevenDayReturnRate),
                    "错题上传成功率" to percent(metrics.mistakeUploadSuccessRate),
                    "OCR 校对完成率" to percent(metrics.ocrCorrectionRate),
                    "分析完成率" to percent(metrics.mistakeAnalysisCompletionRate),
                    "同类练习完成率" to percent(metrics.samePracticeCompletionRate),
                    "二次正确率" to percent(metrics.secondAttemptAccuracy),
                    "AI 有帮助率" to percent(metrics.aiHelpfulRate),
                    "AI 失败率" to percent(metrics.aiFailureRate),
                    "待审核内容" to (metrics.pendingContent?.toString() ?: "受限"),
                    "待复习" to metrics.ocrNeedsReview.toString(),
                    "安全事件" to metrics.securityEvents.toString(),
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    cards.chunked(if (compact) 2 else 4).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { (label, value) -> MetricCard(label, value, Modifier.weight(1f)) }
                            repeat((if (compact) 2 else 4) - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            item {
                when (destination) {
                    AppDestination.Content -> ContentGovernanceCard(metrics.pendingContent, metrics.pendingFormulas, metrics.approvedNodes, metrics.documents, metrics.knowledgeEdges)
                    AppDestination.Operations -> OperationsCard(metrics.queuedOcr, metrics.processingOcr, metrics.failedOcr, metrics.aiFailureRate, metrics.auditEvents, metrics.releaseGatePassed)
                    AppDestination.Teacher -> TeacherSummaryCard(metrics.activeStudents, metrics.mistakesCreated, metrics.samePracticeCompletionRate, metrics.secondAttemptAccuracy)
                    else -> QueueCard(metrics.queuedOcr, metrics.processingOcr, metrics.failedOcr, metrics.ocrNeedsReview)
                }
            }
        }
        item { SectionTitle("风险告警", alerts.size) }
        if (alerts.isEmpty()) item { Text("当前没有需要处理的告警", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        itemsIndexed(alerts, key = { index, alert -> alert.code.ifBlank { "alert-$index" } }) { _, alert -> AlertRow(alert) }
        item { SectionTitle("待办任务", tasks.size) }
        if (tasks.isEmpty()) item { Text("当前没有待办任务", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(tasks, key = { "${it.type}-${it.id}" }) { TaskRow(it) }
    }
}

@Composable private fun ContentGovernanceCard(pending: Int?, formulas: Int?, approved: Int?, documents: Int?, edges: Int?) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("内容治理", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("文档 ${documents?.toString() ?: "受限"}  · 待审核节点 ${pending?.toString() ?: "受限"}  · 公式队列 ${formulas?.toString() ?: "受限"}")
            Text("正式节点 ${approved?.toString() ?: "受限"}  · 知识关系 ${edges?.toString() ?: "受限"}")
            Text("文档、公式、知识点、关系和版本发布均由后端权限接口校验", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun OperationsCard(queued: Int, processing: Int, failed: Int, aiFailureRate: Double, auditEvents: Int?, releaseGatePassed: Boolean?) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("运行状态", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("OCR 排队 $queued  · 处理中 $processing  · 失败 $failed  · AI 失败率 ${percent(aiFailureRate)}")
            Text("审计事件 ${auditEvents?.toString() ?: "受限"}  · 发布门 ${releaseGatePassed?.let { if (it) "通过" else "阻断" } ?: "待检查"}")
            Text("告警、任务重试、服务健康和审计记录请在对应管理接口继续处理", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun TeacherSummaryCard(active: Int, mistakes: Int, practice: Double, accuracy: Double) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("班级趋势", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("活跃学生 $active  · 本期错题 $mistakes  · 练习完成 ${percent(practice)}  · 二次正确 ${percent(accuracy)}")
            Text("教师默认只看到班级聚合和匿名状态，不展示学生原图或对话正文", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable private fun QueueCard(queued: Int, processing: Int, failed: Int, review: Int) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("OCR 队列", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("排队 $queued  · 处理中 $processing  · 失败 $failed  · 待人工确认 $review", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable private fun SectionTitle(title: String, count: Int) {
    Text("$title（$count）", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable private fun AlertRow(alert: WorkspaceAlert) {
    Card(colors = CardDefaults.cardColors(containerColor = if (alert.severity == "critical") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(alert.message, fontWeight = FontWeight.Medium)
            Text("${alert.code}${alert.value?.let { " · ${String.format(Locale.US, "%.2f", it)}" }.orEmpty()}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun TaskRow(task: WorkspaceTask) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(if (task.type == "ocr") "OCR 任务" else "内容审核", fontWeight = FontWeight.Medium)
                Text(task.id.take(12), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(task.status, style = MaterialTheme.typography.labelLarge)
        }
    }
}

private fun percent(value: Double): String = "${(value * 100).toInt()}%"
