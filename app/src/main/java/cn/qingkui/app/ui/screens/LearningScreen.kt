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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import cn.qingkui.app.ui.model.MistakeWeeklyReview
import cn.qingkui.app.ui.components.MathRichText
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
    aiAvailable: Boolean?,
    weeklyReview: MistakeWeeklyReview?,
    onOpenItem: (String) -> Unit,
    onShowLearning: () -> Unit,
    onFilterChange: (LearningFilter) -> Unit,
    onShowMistakes: () -> Unit,
    onCaptureMistake: () -> Unit,
    onRefreshMistakes: () -> Unit,
    onRetryDraft: (String) -> Unit,
    onDeleteDraft: (String) -> Unit,
    onCancelOcr: (String, String) -> Unit,
    onRetryOcr: (String, String) -> Unit,
    onDeleteMistake: (String) -> Unit,
    onConfirmOcr: (String, String, String) -> Unit,
    onAnalyzeMistake: (String) -> Unit,
    onGeneratePractice: (String) -> Unit,
    onSubmitPractice: (String, String, String) -> Unit,
    onAskMistake: (MistakeItem) -> Unit,
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
                    aiAvailable = aiAvailable,
                    weeklyReview = weeklyReview,
                    onCapture = onCaptureMistake,
                    onRefresh = onRefreshMistakes,
                    onRetryDraft = onRetryDraft,
                    onDeleteDraft = onDeleteDraft,
                    onCancelOcr = onCancelOcr,
                    onRetryOcr = onRetryOcr,
                    onDeleteMistake = onDeleteMistake,
                    onConfirm = onConfirmOcr,
                    onAnalyze = onAnalyzeMistake,
                    onGeneratePractice = onGeneratePractice,
                    onSubmitPractice = onSubmitPractice,
                    onAskMistake = onAskMistake,
                    onOpenKnowledgeNode = onOpenItem,
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
    aiAvailable: Boolean?,
    weeklyReview: MistakeWeeklyReview?,
    onCapture: () -> Unit,
    onRefresh: () -> Unit,
    onRetryDraft: (String) -> Unit,
    onDeleteDraft: (String) -> Unit,
    onCancelOcr: (String, String) -> Unit,
    onRetryOcr: (String, String) -> Unit,
    onDeleteMistake: (String) -> Unit,
    onConfirm: (String, String, String) -> Unit,
    onAnalyze: (String) -> Unit,
    onGeneratePractice: (String) -> Unit,
    onSubmitPractice: (String, String, String) -> Unit,
    onAskMistake: (MistakeItem) -> Unit,
    onOpenKnowledgeNode: (String) -> Unit,
) {
    var editing by remember { mutableStateOf<MistakeItem?>(null) }
    var mistakeToDelete by remember { mutableStateOf<MistakeItem?>(null) }
    var draftToDelete by remember { mutableStateOf<MistakeDraftItem?>(null) }
    var correction by remember { mutableStateOf("") }
    var answering by remember { mutableStateOf<Pair<MistakeItem, MistakePracticeItem>?>(null) }
    var practiceAnswer by remember { mutableStateOf("") }
    var showPracticeHint by remember { mutableStateOf(false) }
    var focusedMistakeId by remember { mutableStateOf<String?>(null) }
    val pendingDrafts = drafts.filter { it.status != "uploaded" }
    val mistakeListState = rememberLazyListState()
    LaunchedEffect(focusedMistakeId) {
        val id = focusedMistakeId ?: return@LaunchedEffect
        val index = mistakes.indexOfFirst { it.id == id }
        if (index >= 0) mistakeListState.animateScrollToItem(pendingDrafts.size + index)
    }
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
    weeklyReview?.let { report ->
        Column(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("本周复盘", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                "新增 ${report.newMistakes} 题 · 待复习 ${report.dueReviewCount} 题 · 主要错因 ${errorCategoryLabel(report.topErrorCategory)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (report.dueReviews.isNotEmpty()) {
                Text("待复习入口", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                report.dueReviews.take(5).forEach { link ->
                    val linkedMistake = mistakes.firstOrNull { it.id == link.mistakeId }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = { focusedMistakeId = link.mistakeId },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                "${link.title} · ${reviewStageLabel(link.reviewStage)}",
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                        }
                        if (linkedMistake != null) {
                            val nextPractice = linkedMistake.practices.firstOrNull { it.status == "pending" }
                            if (nextPractice != null) {
                                TextButton(onClick = {
                                    focusedMistakeId = link.mistakeId
                                    answering = linkedMistake to nextPractice
                                    practiceAnswer = ""
                                    showPracticeHint = false
                                }) { Text("开始复习") }
                            }
                        }
                    }
                }
            }
            if (report.weakKnowledgePoints.isNotEmpty()) {
                Text("薄弱知识点", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                report.weakKnowledgePoints.take(5).forEach { point ->
                    TextButton(
                        onClick = { onOpenKnowledgeNode(point.knowledgeNodeId) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(point.name, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Text("${point.mistakeCount} 题", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Text(
                "上传成功 ${(report.uploadSuccessRate * 100).toInt()}% · OCR校对 ${(report.ocrCorrectionRate * 100).toInt()}% · 练习完成 ${(report.practiceCompletionRate * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "权威正确 ${(report.authoritativeAccuracy * 100).toInt()}% · 二次正确 ${(report.secondAttemptAccuracy * 100).toInt()}% · 7日回访 ${(report.sevenDayFollowupRate * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(16.dp))
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    if (pendingDrafts.isEmpty() && mistakes.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
            Text("还没有错题", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(state = mistakeListState) {
            items(pendingDrafts, key = { "draft-${it.id}" }) { draft ->
                Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (draft.imagePath.isNotBlank()) {
                        AsyncImage(model = File(draft.imagePath), contentDescription = null, modifier = Modifier.size(72.dp).background(MaterialTheme.colorScheme.surfaceVariant))
                    } else {
                        Box(Modifier.size(72.dp).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.EditNote, contentDescription = "手动输入题目")
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(draft.questionText.ifBlank { "图片题目待识别" }, style = MaterialTheme.typography.bodyLarge, maxLines = 2)
                        Text(draftStatusLabel(draft.status), style = MaterialTheme.typography.bodySmall, color = if (draft.status == "failed") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!draft.errorMessage.isNullOrBlank()) Text(draft.errorMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, maxLines = 2)
                    }
                    if (draft.status == "failed") {
                        TextButton(onClick = { onRetryDraft(draft.id) }) { Text("重试") }
                    }
                    IconButton(onClick = { draftToDelete = draft }) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "删除本地草稿")
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
            items(mistakes, key = { "remote-${it.id}" }) { item ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            if (item.id == focusedMistakeId) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .45f)
                            else Color.Transparent,
                        )
                        .padding(vertical = 16.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(item.subject, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                ocrStatusLabel(item.ocrStatus),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (item.ocrStatus in setOf("failed", "blocked")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            IconButton(onClick = { mistakeToDelete = item }) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "删除错题")
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    MathRichText(item.questionText, style = MaterialTheme.typography.bodyLarge)
                    if (item.ocrStatus == "blocked") {
                        Text(
                            item.ocrErrorMessage ?: "识别内容已被安全隔离，可删除图片或联系管理员复核。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { onAskMistake(item) }, enabled = aiAvailable != false) { Text("带入 AI 继续追问") }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (item.confidence != null) Text("置信度 ${(item.confidence * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (item.requiresReview && item.ocrTaskId != null && item.ocrStatus == "succeeded") {
                            TextButton(onClick = { editing = item; correction = item.questionText }) { Text("校对确认") }
                        }
                        if (item.ocrTaskId != null && item.ocrStatus in setOf("queued", "recognizing")) {
                            TextButton(onClick = { onCancelOcr(item.id, item.ocrTaskId) }) { Text("取消识别") }
                        }
                        if (item.ocrTaskId != null && item.ocrStatus in setOf("failed", "cancelled")) {
                            TextButton(onClick = { onRetryOcr(item.id, item.ocrTaskId) }) { Text("重试识别") }
                        }
                        if (!item.requiresReview && item.analysisStatus != "completed") {
                            TextButton(onClick = { onAnalyze(item.id) }, enabled = !loading && aiAvailable != false) { Text("分析错因") }
                        }
                        if (item.analysisStatus == "completed" && item.practices.isEmpty()) {
                            TextButton(onClick = { onGeneratePractice(item.id) }, enabled = !loading && aiAvailable != false) { Text("生成同类练习") }
                        }
                        if (
                            item.analysisStatus == "completed" && item.practices.isNotEmpty() &&
                            item.practices.none { it.status == "pending" } && item.studyStatus != "mastered"
                        ) {
                            TextButton(onClick = { onGeneratePractice(item.id) }, enabled = !loading && aiAvailable != false) { Text("开始下一轮") }
                        }
                    }
                    if (item.requiresReview && item.reviewReasons.isNotEmpty()) {
                        Text(
                            item.reviewReasons.joinToString(" · ") { ocrReviewReasonLabel(it) },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Text(
                        "复习阶段：${reviewStageLabel(item.reviewStage)} · 连续通过 ${item.reviewStreak} 轮",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    item.nextReviewAt?.let {
                        Text("下次复习：${it.replace('T', ' ').take(16)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (!item.analysisDiagnosis.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text("错因分析", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        MathRichText(item.analysisDiagnosis, style = MaterialTheme.typography.bodyMedium)
                        if (!item.errorNote.isNullOrBlank()) {
                            Text(item.errorNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        item.correctionSteps.forEachIndexed { index, step ->
                            MathRichText("${index + 1}. $step", style = MaterialTheme.typography.bodySmall)
                        }
                        if (!item.knowledgeNodeId.isNullOrBlank()) {
                            Text("关联知识点：${item.knowledgeNodeId}（待确认）", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    val currentRoundId = item.practices.lastOrNull { it.roundId != null }?.roundId
                    val visiblePractices = item.practices.filter { currentRoundId == null || it.roundId == currentRoundId }
                    if (visiblePractices.isNotEmpty()) {
                        val completedCount = visiblePractices.count { it.status == "completed" }
                        Spacer(Modifier.height(10.dp))
                        Text("本轮进度 $completedCount/${visiblePractices.size}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                    visiblePractices.forEach { practice ->
                        Spacer(Modifier.height(10.dp))
                        Text("第 ${practice.position ?: 1} 题", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        MathRichText(practice.questionText, style = MaterialTheme.typography.bodyMedium)
                        if (practice.status == "pending") {
                            TextButton(onClick = {
                                answering = item to practice
                                practiceAnswer = ""
                                showPracticeHint = false
                            }) { Text("开始作答") }
                        } else {
                            Text(
                                practiceResultLabel(practice),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (practice.isCorrect == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            )
                            if (!practice.answerReference.isNullOrBlank()) {
                                MathRichText(
                                    "参考：${practice.answerReference}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
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
    mistakeToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { mistakeToDelete = null },
            title = { Text("删除这道错题？") },
            text = { Text("错题记录、OCR 任务、练习结果和原图将一并删除，无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteMistake(target.id)
                    mistakeToDelete = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { mistakeToDelete = null }) { Text("取消") } },
        )
    }
    draftToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { draftToDelete = null },
            title = { Text("删除本地草稿？") },
            text = { Text("尚未完成上传的图片和题目草稿将从本机删除。") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteDraft(target.id)
                    draftToDelete = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { draftToDelete = null }) { Text("取消") } },
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
                    MathRichText(practice.questionText, style = MaterialTheme.typography.bodyMedium)
                    if (!practice.hint.isNullOrBlank()) {
                        TextButton(onClick = { showPracticeHint = !showPracticeHint }) {
                            Text(if (showPracticeHint) "收起提示" else "查看提示")
                        }
                        if (showPracticeHint) {
                            MathRichText(
                                practice.hint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
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

private fun ocrReviewReasonLabel(reason: String): String = when (reason) {
    "low_confidence" -> "识别置信度偏低"
    "sparse_text" -> "识别内容过少"
    "multiple_questions" -> "图片包含多道题，请检查完整性"
    "question_number_gap" -> "检测到题号缺失"
    "subquestion_number_gap" -> "检测到小题编号缺失"
    "model_detected_omission" -> "识别服务检测到内容可能缺失"
    "model_structure_uncertain" -> "识别服务无法确认题目结构"
    "content_safety_blocked" -> "内容需要管理员复核"
    else -> "识别结果需要复核"
}

private fun reviewStageLabel(stage: String): String = when (stage) {
    "correction" -> "订正"
    "next_day" -> "隔天复习"
    "next_week" -> "隔周复习"
    "completed" -> "已掌握"
    else -> stage
}

private fun errorCategoryLabel(category: String?): String = when (category) {
    "concept" -> "概念"
    "reading" -> "审题"
    "method" -> "方法"
    "calculation" -> "计算"
    "expression" -> "表达"
    "unclassified", null -> "待归类"
    else -> category
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
    "blocked" -> "内容已隔离"
    else -> "手动录入"
}
