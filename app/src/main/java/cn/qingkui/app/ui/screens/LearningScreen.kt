package cn.qingkui.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.qingkui.app.ui.model.KnowledgeStatus
import cn.qingkui.app.ui.model.LearningItem
import cn.qingkui.app.ui.model.LearningFilter
import cn.qingkui.app.ui.model.MistakeDraftItem
import cn.qingkui.app.ui.model.MistakeItem
import cn.qingkui.app.ui.model.MistakePracticeItem
import coil.compose.AsyncImage
import java.io.File

@Composable
fun LearningScreen(
    compact: Boolean,
    items: List<LearningItem>,
    selectedFilter: LearningFilter,
    mistakeDrafts: List<MistakeDraftItem>,
    mistakes: List<MistakeItem>,
    showMistakes: Boolean,
    mistakeLoading: Boolean,
    onOpenItem: (String) -> Unit,
    onShowLearning: () -> Unit,
    onFilterChange: (LearningFilter) -> Unit,
    onShowMistakes: () -> Unit,
    onCaptureMistake: () -> Unit,
    onRefreshMistakes: () -> Unit,
    onConfirmOcr: (String, String, String) -> Unit,
    onAnalyzeMistake: (String) -> Unit,
    onGeneratePractice: (String) -> Unit,
    onSubmitPractice: (String, String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = if (compact) 20.dp else 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.fillMaxWidth().widthIn(max = 960.dp)) {
            Spacer(Modifier.height(if (compact) 20.dp else 32.dp))
            Text("今天继续", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                if (showMistakes) "校对识别结果，按错因持续复习" else "回到具体知识点，保持学习连续",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onShowLearning) { Text("学习记录", color = if (!showMistakes) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                TextButton(onClick = onShowMistakes) { Text("错题本", color = if (showMistakes) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Spacer(Modifier.height(24.dp))
            if (showMistakes) {
                MistakeBookContent(
                    drafts = mistakeDrafts,
                    mistakes = mistakes,
                    loading = mistakeLoading,
                    onCapture = onCaptureMistake,
                    onRefresh = onRefreshMistakes,
                    onConfirm = onConfirmOcr,
                    onAnalyze = onAnalyzeMistake,
                    onGeneratePractice = onGeneratePractice,
                    onSubmitPractice = onSubmitPractice,
                )
            } else {
                LearningFilters(selectedFilter = selectedFilter, onFilterChange = onFilterChange)
                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                if (items.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                        Text("还没有学习记录", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn {
                        items(items, key = { it.nodeId }) { item ->
                            KnowledgeStateRow(item = item, onClick = { onOpenItem(item.nodeId) })
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LearningFilters(
    selectedFilter: LearningFilter,
    onFilterChange: (LearningFilter) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip("最近学习", Icons.Outlined.History, selectedFilter == LearningFilter.Recent) { onFilterChange(LearningFilter.Recent) }
        FilterChip("待复习", Icons.Outlined.Replay, selectedFilter == LearningFilter.Review) { onFilterChange(LearningFilter.Review) }
        FilterChip("易错", Icons.Outlined.ErrorOutline, selectedFilter == LearningFilter.ErrorProne) { onFilterChange(LearningFilter.ErrorProne) }
        FilterChip("已验证", Icons.Outlined.CheckCircle, selectedFilter == LearningFilter.Verified) { onFilterChange(LearningFilter.Verified) }
    }
}

@Composable
private fun FilterChip(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(40.dp)
            .clickable(onClick = onClick)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun KnowledgeStateRow(item: LearningItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(item.lastStudied, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            item.status.label,
            modifier = Modifier
                .background(statusColor(item.status).copy(alpha = .12f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = statusColor(item.status),
        )
        Spacer(Modifier.width(18.dp))
        Text(item.action, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(6.dp))
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun statusColor(status: KnowledgeStatus): Color = when (status) {
    KnowledgeStatus.Unexplored -> MaterialTheme.colorScheme.onSurfaceVariant
    KnowledgeStatus.Explored -> MaterialTheme.colorScheme.primary
    KnowledgeStatus.Understood -> MaterialTheme.colorScheme.tertiary
    KnowledgeStatus.Verified -> MaterialTheme.colorScheme.primary
    KnowledgeStatus.Unstable -> MaterialTheme.colorScheme.secondary
    KnowledgeStatus.ErrorProne -> MaterialTheme.colorScheme.error
    KnowledgeStatus.ToExplore -> MaterialTheme.colorScheme.secondary
}

@Composable
private fun MistakeBookContent(
    drafts: List<MistakeDraftItem>,
    mistakes: List<MistakeItem>,
    loading: Boolean,
    onCapture: () -> Unit,
    onRefresh: () -> Unit,
    onConfirm: (String, String, String) -> Unit,
    onAnalyze: (String) -> Unit,
    onGeneratePractice: (String) -> Unit,
    onSubmitPractice: (String, String, String) -> Unit,
) {
    var editing by remember { mutableStateOf<MistakeItem?>(null) }
    var correction by remember { mutableStateOf("") }
    var answering by remember { mutableStateOf<Pair<MistakeItem, MistakePracticeItem>?>(null) }
    var practiceAnswer by remember { mutableStateOf("") }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onCapture) {
            Icon(Icons.Outlined.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("录入错题")
        }
        TextButton(onClick = onRefresh, enabled = !loading) {
            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (loading) "刷新中" else "刷新状态")
        }
    }
    Spacer(Modifier.height(16.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    val pendingDrafts = drafts.filter { it.status != "uploaded" }
    if (pendingDrafts.isEmpty() && mistakes.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
            Text("还没有错题", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn {
            items(pendingDrafts, key = { "draft-${it.id}" }) { draft ->
                Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(model = File(draft.imagePath), contentDescription = null, modifier = Modifier.size(72.dp).background(MaterialTheme.colorScheme.surfaceVariant))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(draft.questionText.ifBlank { "图片题目待识别" }, style = MaterialTheme.typography.bodyLarge, maxLines = 2)
                        Text(draftStatusLabel(draft.status), style = MaterialTheme.typography.bodySmall, color = if (draft.status == "failed") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!draft.errorMessage.isNullOrBlank()) Text(draft.errorMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, maxLines = 2)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
            items(mistakes, key = { "remote-${it.id}" }) { item ->
                Column(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(item.subject, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(ocrStatusLabel(item.ocrStatus), style = MaterialTheme.typography.bodySmall, color = if (item.ocrStatus == "failed") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(item.questionText, style = MaterialTheme.typography.bodyLarge, maxLines = 5)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (item.confidence != null) Text("置信度 ${(item.confidence * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (item.requiresReview && item.ocrTaskId != null && item.ocrStatus == "succeeded") {
                            TextButton(onClick = { editing = item; correction = item.questionText }) { Text("校对确认") }
                        }
                        if (!item.requiresReview && item.analysisStatus != "completed") {
                            TextButton(onClick = { onAnalyze(item.id) }, enabled = !loading) { Text("分析错因") }
                        }
                        if (item.analysisStatus == "completed" && item.practices.isEmpty()) {
                            TextButton(onClick = { onGeneratePractice(item.id) }, enabled = !loading) { Text("生成同类练习") }
                        }
                    }
                    if (!item.analysisDiagnosis.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text("错因分析", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(item.analysisDiagnosis, style = MaterialTheme.typography.bodyMedium)
                        if (!item.errorNote.isNullOrBlank()) {
                            Text(item.errorNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        item.correctionSteps.forEachIndexed { index, step ->
                            Text("${index + 1}. $step", style = MaterialTheme.typography.bodySmall)
                        }
                        if (!item.knowledgeNodeId.isNullOrBlank()) {
                            Text("关联知识点：${item.knowledgeNodeId}（待确认）", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    item.practices.forEach { practice ->
                        Spacer(Modifier.height(10.dp))
                        Text("同类练习", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text(practice.questionText, style = MaterialTheme.typography.bodyMedium)
                        if (practice.status == "pending") {
                            TextButton(onClick = {
                                answering = item to practice
                                practiceAnswer = ""
                            }) { Text("开始作答") }
                        } else {
                            Text(
                                practiceResultLabel(practice),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (practice.isCorrect == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            )
                            if (!practice.answerReference.isNullOrBlank()) {
                                Text("参考：${practice.answerReference}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
        }
    }
    val current = editing
    if (current != null && current.ocrTaskId != null) {
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("校对 OCR 结果") },
            text = {
                OutlinedTextField(
                    value = correction,
                    onValueChange = { correction = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 6,
                    label = { Text("确认后的题目文字") },
                )
            },
            confirmButton = {
                Button(onClick = { onConfirm(current.id, current.ocrTaskId, correction); editing = null }, enabled = correction.isNotBlank()) { Text("确认") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("取消") } },
        )
    }
    val practiceTarget = answering
    if (practiceTarget != null) {
        val (mistake, practice) = practiceTarget
        AlertDialog(
            onDismissRequest = { answering = null },
            title = { Text("同类练习") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(practice.questionText, style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = practiceAnswer,
                        onValueChange = { practiceAnswer = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        label = { Text("我的答案") },
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSubmitPractice(mistake.id, practice.id, practiceAnswer)
                        answering = null
                    },
                    enabled = practiceAnswer.isNotBlank(),
                ) { Text("提交校验") }
            },
            dismissButton = { TextButton(onClick = { answering = null }) { Text("取消") } },
        )
    }
}

private fun practiceResultLabel(practice: MistakePracticeItem): String = when (practice.isCorrect) {
    true -> if (practice.validationMethod == "self_report") "已提交自评" else "服务端校验通过"
    false -> if (practice.validationMethod == "self_report") "自评需要订正" else "服务端校验未通过"
    null -> "已提交，等待人工核验"
}

private fun draftStatusLabel(status: String): String = when (status) {
    "waiting" -> "等待网络上传"
    "uploading" -> "正在上传"
    "failed" -> "上传失败"
    else -> "本地草稿"
}

private fun ocrStatusLabel(status: String): String = when (status) {
    "queued" -> "排队识别"
    "recognizing" -> "正在识别"
    "succeeded" -> "识别完成"
    "failed" -> "识别失败"
    "cancelled" -> "已取消"
    else -> "手动录入"
}
