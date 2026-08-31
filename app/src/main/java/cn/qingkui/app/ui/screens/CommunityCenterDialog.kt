package cn.qingkui.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.qingkui.app.ui.model.ClassOverviewItem
import cn.qingkui.app.ui.model.ContributionItem
import cn.qingkui.app.ui.model.CreditCampaignItem
import cn.qingkui.app.ui.model.CreditRedemptionItem
import cn.qingkui.app.ui.model.SchoolClassItem
import cn.qingkui.app.ui.model.SchoolMembershipItem
import kotlin.math.roundToInt

private enum class CommunityTab(val label: String) {
    School("学校班级"),
    Credits("活动额度"),
    Contributions("内容共建"),
}

@Composable
fun CommunityCenterDialog(
    organizationsAvailable: Boolean?,
    memberships: List<SchoolMembershipItem>,
    classes: List<SchoolClassItem>,
    classOverview: ClassOverviewItem?,
    creditCampaignsAvailable: Boolean?,
    campaigns: List<CreditCampaignItem>,
    redemptions: List<CreditRedemptionItem>,
    contributionsAvailable: Boolean?,
    contributions: List<ContributionItem>,
    onRedeemInvite: (String) -> Unit,
    onLeaveSchool: (String) -> Unit,
    onLoadOverview: (String) -> Unit,
    onRedeemCode: (String) -> Unit,
    onSubmitContribution: (String, String, String, String?) -> Unit,
    onDeleteContribution: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var tab by remember { mutableStateOf(CommunityTab.School) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("校园与共建") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 580.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CommunityTabs(tab) { tab = it }
                when (tab) {
                    CommunityTab.School -> SchoolPanel(
                        organizationsAvailable,
                        memberships,
                        classes,
                        classOverview,
                        onRedeemInvite,
                        onLeaveSchool,
                        onLoadOverview,
                    )
                    CommunityTab.Credits -> CreditPanel(creditCampaignsAvailable, campaigns, redemptions, onRedeemCode)
                    CommunityTab.Contributions -> ContributionPanel(contributionsAvailable, contributions, onSubmitContribution, onDeleteContribution)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } },
    )
}

@Composable
private fun CommunityTabs(selected: CommunityTab, onSelect: (CommunityTab) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp)).padding(3.dp),
    ) {
        CommunityTab.entries.forEach { tab ->
            Box(
                modifier = Modifier.weight(1f).height(38.dp)
                    .background(if (tab == selected) MaterialTheme.colorScheme.surface else Color.Transparent, RoundedCornerShape(6.dp))
                    .clickable { onSelect(tab) },
                contentAlignment = Alignment.Center,
            ) { Text(tab.label, style = MaterialTheme.typography.labelLarge, color = if (tab == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun SchoolPanel(
    available: Boolean?,
    memberships: List<SchoolMembershipItem>,
    classes: List<SchoolClassItem>,
    overview: ClassOverviewItem?,
    onRedeemInvite: (String) -> Unit,
    onLeaveSchool: (String) -> Unit,
    onLoadOverview: (String) -> Unit,
) {
    FeatureState(available, "学校与班级")
    if (available == false) return
    var inviteCode by remember { mutableStateOf("") }
    OutlinedTextField(
        value = inviteCode,
        onValueChange = { inviteCode = it },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text("邀请码") },
        trailingIcon = {
            TextButton(onClick = { onRedeemInvite(inviteCode); inviteCode = "" }, enabled = inviteCode.trim().length >= 8) { Text("加入") }
        },
    )
    if (memberships.isEmpty()) {
        Text("加入学校后可查看本人所属班级；教师趋势仅展示匿名聚合数据。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    memberships.forEach { membership ->
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(membership.schoolName, fontWeight = FontWeight.Medium)
                    Text("${roleLabel(membership.role)} · ${membership.schoolCode}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { onLeaveSchool(membership.schoolId) }) { Text("退出") }
            }
            val schoolClasses = classes.filter { it.schoolId == membership.schoolId }
            schoolClasses.forEach { classroom ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 32.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(classroom.name, style = MaterialTheme.typography.bodyMedium)
                        Text(listOf(classroom.grade, classroom.academicYear).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (membership.role == "teacher" || membership.role == "school_admin") {
                        TextButton(onClick = { onLoadOverview(classroom.id) }) {
                            Icon(Icons.Outlined.Visibility, contentDescription = "查看匿名趋势", modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("趋势")
                        }
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
    overview?.let { ClassOverviewCard(it) }
}

@Composable
private fun ClassOverviewCard(overview: ClassOverviewItem) {
    Column(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("${overview.classroom.name} · 匿名班级趋势", fontWeight = FontWeight.Bold)
        Text("${overview.studentCount} 名学生 · 近 7 日活跃 ${overview.active7dStudents} 人", style = MaterialTheme.typography.bodySmall)
        Text("提问 ${overview.questions} · 错题 ${overview.mistakes} · 已验证知识点 ${overview.verifiedNodes}", style = MaterialTheme.typography.bodySmall)
        Text(
            "练习完成 ${(overview.practiceCompletionRate * 100).roundToInt()}% · 二次作答正确 ${(overview.secondAttemptAccuracy * 100).roundToInt()}% · 待复习 ${overview.dueReviewCount}",
            style = MaterialTheme.typography.bodySmall,
        )
        if (overview.topErrorCategories.isNotEmpty()) {
            Text(
                "主要错因：" + overview.topErrorCategories.joinToString("、") { "${errorCategoryLabel(it.label)} ${it.count}" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (overview.weakKnowledgePoints.isNotEmpty()) {
            Text(
                "薄弱知识点：" + overview.weakKnowledgePoints.joinToString("、") { "${it.label} ${it.count}" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        overview.students.take(5).forEach { student ->
            Text("学生 ${student.anonymousId.take(8)} · 提问 ${student.questions} · 错题 ${student.mistakes} · 知识点 ${student.verifiedNodes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun errorCategoryLabel(value: String): String = when (value) {
    "concept" -> "概念"
    "reading" -> "审题"
    "method" -> "方法"
    "calculation" -> "计算"
    "expression" -> "表达"
    else -> "未分类"
}

@Composable
private fun CreditPanel(
    available: Boolean?,
    campaigns: List<CreditCampaignItem>,
    redemptions: List<CreditRedemptionItem>,
    onRedeem: (String) -> Unit,
) {
    FeatureState(available, "活动额度")
    if (available == false) return
    var code by remember { mutableStateOf("") }
    OutlinedTextField(
        value = code,
        onValueChange = { code = it },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text("兑换码") },
        trailingIcon = { TextButton(onClick = { onRedeem(code); code = "" }, enabled = code.trim().length >= 8) { Text("兑换") } },
    )
    Text("进行中的活动", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    if (campaigns.isEmpty()) Text("当前没有可参加的活动", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    campaigns.forEach { campaign ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.LocalOffer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(campaign.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("+${campaign.amount} 额度 · 剩余 ${campaign.remaining} · 截止 ${campaign.endsAt}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    Text("兑换记录", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    if (redemptions.isEmpty()) Text("暂无兑换记录", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    redemptions.forEach { item -> Text("${item.campaignName} · +${item.amount} · ${item.createdAt}", style = MaterialTheme.typography.bodySmall) }
}

@Composable
private fun ContributionPanel(
    available: Boolean?,
    contributions: List<ContributionItem>,
    onSubmit: (String, String, String, String?) -> Unit,
    onDelete: (String) -> Unit,
) {
    FeatureState(available, "内容共建")
    if (available == false) return
    var type by remember { mutableStateOf("explanation") }
    var typeMenu by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("") }
    Box {
        OutlinedButton(onClick = { typeMenu = true }, contentPadding = PaddingValues(horizontal = 10.dp)) { Text("类型：${contributionTypeLabel(type)}") }
        DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
            listOf("explanation", "correction", "question", "source").forEach { value ->
                DropdownMenuItem(text = { Text(contributionTypeLabel(value)) }, onClick = { type = value; typeMenu = false })
            }
        }
    }
    OutlinedTextField(title, { title = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("标题") })
    OutlinedTextField(content, { content = it }, modifier = Modifier.fillMaxWidth(), minLines = 4, label = { Text("内容（至少 20 字）") })
    OutlinedTextField(source, { source = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("来源（可选）") })
    Button(
        onClick = { onSubmit(type, title, content, source.ifBlank { null }); title = ""; content = ""; source = "" },
        enabled = title.trim().length >= 2 && content.trim().length >= 20,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("提交投稿")
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    Text("我的投稿", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    if (contributions.isEmpty()) Text("暂无投稿", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    contributions.forEach { item ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${contributionStatusLabel(item.status)} · ${item.createdAt}${rewardLabel(item)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                item.reviewNote?.takeIf { it.isNotBlank() }?.let { Text("审核：$it", style = MaterialTheme.typography.bodySmall) }
            }
            if (item.status !in setOf("approved", "rejected")) {
                TextButton(onClick = { onDelete(item.id) }) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "删除投稿", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun FeatureState(available: Boolean?, name: String) {
    if (available == false) {
        Text("${name}当前尚未开放", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else if (available == null) {
        Text("正在检查${name}状态...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun roleLabel(value: String): String = when (value) {
    "school_admin" -> "学校管理员"
    "teacher" -> "教师"
    else -> "学生"
}

private fun contributionTypeLabel(value: String): String = when (value) {
    "correction" -> "内容纠错"
    "question" -> "题目补充"
    "source" -> "资料来源"
    else -> "知识讲解"
}

private fun contributionStatusLabel(value: String): String = when (value) {
    "queued" -> "等待初审"
    "screened" -> "等待人工审核"
    "review_failed" -> "初审失败"
    "approved" -> "已通过"
    "rejected" -> "已拒绝"
    else -> value
}

private fun rewardLabel(item: ContributionItem): String = when (item.rewardStatus) {
    "pending" -> " · 奖励待结算 ${item.rewardAmount}"
    "settled" -> " · 已获得奖励 ${item.rewardAmount}"
    else -> ""
}
