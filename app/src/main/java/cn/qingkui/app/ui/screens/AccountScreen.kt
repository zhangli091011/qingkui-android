package cn.qingkui.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import cn.qingkui.app.ui.model.DeviceSessionItem
import cn.qingkui.app.ui.model.WorkspaceCapabilities
import cn.qingkui.app.ui.model.AppDestination
import cn.qingkui.app.ui.model.FeedbackItem
import cn.qingkui.app.ui.model.ClassOverviewItem
import cn.qingkui.app.ui.model.ContributionItem
import cn.qingkui.app.ui.model.CreditCampaignItem
import cn.qingkui.app.ui.model.CreditRedemptionItem
import cn.qingkui.app.ui.model.SchoolClassItem
import cn.qingkui.app.ui.model.SchoolMembershipItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AccountScreen(
    compact: Boolean,
    credits: Int,
    userName: String,
    authenticated: Boolean,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onChangePassword: (String, String) -> Unit = { _, _ -> },
    onDeleteAccount: () -> Unit = {},
    ledger: List<cn.qingkui.app.ui.model.CreditLedgerItem> = emptyList(),
    deviceSessions: List<DeviceSessionItem> = emptyList(),
    feedbackItems: List<FeedbackItem> = emptyList(),
    onRefreshAccount: () -> Unit = {},
    onRevokeDevice: (String) -> Unit = {},
    onSubmitFeedback: (String, String) -> Unit = { _, _ -> },
    organizationsAvailable: Boolean? = null,
    schoolMemberships: List<SchoolMembershipItem> = emptyList(),
    schoolClasses: List<SchoolClassItem> = emptyList(),
    classOverview: ClassOverviewItem? = null,
    creditCampaignsAvailable: Boolean? = null,
    creditCampaigns: List<CreditCampaignItem> = emptyList(),
    creditRedemptions: List<CreditRedemptionItem> = emptyList(),
    contributionsAvailable: Boolean? = null,
    contributions: List<ContributionItem> = emptyList(),
    onRefreshCommunity: () -> Unit = {},
    onRedeemInvite: (String) -> Unit = {},
    onLeaveSchool: (String) -> Unit = {},
    onLoadClassOverview: (String) -> Unit = {},
    onRedeemCreditCode: (String) -> Unit = {},
    onSubmitContribution: (String, String, String, String?) -> Unit = { _, _, _, _ -> },
    onDeleteContribution: (String) -> Unit = {},
    workspaceCapabilities: WorkspaceCapabilities = WorkspaceCapabilities(),
    onOpenWorkspace: (AppDestination) -> Unit = {},
    onOpenAdminConsole: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var passwordDialog by remember { mutableStateOf(false) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var deleteDialog by remember { mutableStateOf(false) }
    var deviceDialog by remember { mutableStateOf(false) }
    var feedbackDialog by remember { mutableStateOf(false) }
    var feedbackCategory by remember { mutableStateOf("product_issue") }
    var feedbackContent by remember { mutableStateOf("") }
    var dataDialog by remember { mutableStateOf(false) }
    var privacyDialog by remember { mutableStateOf(false) }
    var minorDialog by remember { mutableStateOf(false) }
    var communityDialog by remember { mutableStateOf(false) }
    var ledgerDialog by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = if (compact) 20.dp else 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.fillMaxWidth().widthIn(max = 760.dp)) {
            Spacer(Modifier.height(if (compact) 16.dp else 32.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(64.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        if (authenticated) userName else "未登录",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (authenticated) "独立学习账户" else "登录后同步额度与学习记录",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!authenticated) {
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onLogin,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("登录或注册")
                }
            }
            Spacer(Modifier.height(28.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.CreditCard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("可用额度", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (authenticated) "${"%,d".format(credits)}" else "--",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                TextButton(onClick = {
                    if (authenticated) {
                        onRefreshAccount()
                        ledgerDialog = true
                    } else {
                        onLogin()
                    }
                }) {
                    Text(
                    if (authenticated) "查看流水" else "登录后查看",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            SettingsRow(Icons.Outlined.Key, "账户与密码", "修改账户密码", onClick = { if (authenticated) passwordDialog = true else onLogin() })
            SettingsRow(Icons.Outlined.Devices, "设备会话", "查看并撤销已登录设备", onClick = {
                if (authenticated) {
                    onRefreshAccount()
                    deviceDialog = true
                } else onLogin()
            })
            SettingsRow(Icons.Outlined.Brightness6, "外观", "跟随系统深色模式")
            SettingsRow(Icons.Outlined.DataUsage, "数据说明", "学习记录、错题和删除范围", onClick = { dataDialog = true })
            SettingsRow(Icons.Outlined.PrivacyTip, "隐私说明", "账户、图片与模型调用边界", onClick = { privacyDialog = true })
            SettingsRow(Icons.Outlined.School, "未成年人及试点", "授权、教师可见范围与退出方式", onClick = { minorDialog = true })
            if (authenticated && (workspaceCapabilities.teacherWorkspace || workspaceCapabilities.contentWorkspace || workspaceCapabilities.operationsWorkspace)) {
                Spacer(Modifier.height(8.dp))
                Text("工作台", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (workspaceCapabilities.teacherWorkspace) {
                    SettingsRow(Icons.Outlined.Groups, "教师工作台", "班级趋势与知识点聚合", onClick = { onOpenWorkspace(AppDestination.Teacher) })
                }
                if (workspaceCapabilities.contentWorkspace) {
                    SettingsRow(Icons.Outlined.DataUsage, "内容治理", "文档、公式、节点、关系与发布", onClick = { onOpenWorkspace(AppDestination.Content) })
                }
                if (workspaceCapabilities.operationsWorkspace) {
                    SettingsRow(Icons.Outlined.Devices, "系统运维", "队列、告警、审计与发布门", onClick = { onOpenWorkspace(AppDestination.Operations) })
                    SettingsRow(Icons.Outlined.Brightness6, "运营驾驶舱", "指标、趋势、待办与风险", onClick = { onOpenWorkspace(AppDestination.Workspace) })
                }
                SettingsRow(Icons.Outlined.Settings, "完整管理控制台", "在应用内执行审核、发布、权限、额度和运维操作", onClick = onOpenAdminConsole)
            }
            SettingsRow(Icons.Outlined.Groups, "校园与共建", "学校班级、活动额度与内容投稿", onClick = {
                if (authenticated) {
                    onRefreshCommunity()
                    communityDialog = true
                } else onLogin()
            })
            SettingsRow(Icons.Outlined.Feedback, "问题反馈", "提交反馈并查看处理状态", onClick = {
                if (authenticated) {
                    onRefreshAccount()
                    feedbackDialog = true
                } else onLogin()
            })
            if (authenticated) {
                SettingsRow(Icons.AutoMirrored.Outlined.Logout, "退出登录", "清除本机登录凭证", onLogout)
                SettingsRow(Icons.Outlined.Person, "注销账户", "删除账户及学习数据", onClick = { deleteDialog = true })
                Spacer(Modifier.height(16.dp))
                Text("额度流水", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                ledger.take(10).forEach { item ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(item.entryType, style = MaterialTheme.typography.bodySmall)
                        Text("${if (item.amount >= 0) "+" else ""}${item.amount}", style = MaterialTheme.typography.bodySmall, color = if (item.amount >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
    if (passwordDialog) {
        AlertDialog(
            onDismissRequest = { passwordDialog = false },
            title = { Text("修改密码") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(currentPassword, { currentPassword = it }, label = { Text("当前密码") })
                    OutlinedTextField(newPassword, { newPassword = it }, label = { Text("新密码") })
                }
            },
            confirmButton = { TextButton(onClick = { onChangePassword(currentPassword, newPassword); passwordDialog = false }) { Text("确认") } },
            dismissButton = { TextButton(onClick = { passwordDialog = false }) { Text("取消") } },
        )
    }
    if (ledgerDialog) {
        AlertDialog(
            onDismissRequest = { ledgerDialog = false },
            title = { Text("额度流水") },
            text = {
                Column(
                    Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (ledger.isEmpty()) {
                        Text("暂无额度流水", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        ledger.forEach { item ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.entryType, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        item.createdAt,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    "${if (item.amount >= 0) "+" else ""}${item.amount}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (item.amount >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { ledgerDialog = false }) { Text("完成") } },
        )
    }
    if (deleteDialog) {
        AlertDialog(
            onDismissRequest = { deleteDialog = false },
            title = { Text("确认注销账户？") },
            text = { Text("账户、会话和学习记录将被删除。") },
            confirmButton = { TextButton(onClick = { onDeleteAccount(); deleteDialog = false }) { Text("注销") } },
            dismissButton = { TextButton(onClick = { deleteDialog = false }) { Text("取消") } },
        )
    }
    if (deviceDialog) {
        AlertDialog(
            onDismissRequest = { deviceDialog = false },
            title = { Text("设备会话") },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    if (deviceSessions.isEmpty()) {
                        Text("没有可用的设备会话", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    deviceSessions.forEach { session ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(session.deviceName, fontWeight = FontWeight.Medium)
                                Text(
                                    "有效期至 ${session.expiresAt}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(onClick = { onRevokeDevice(session.id) }) { Text("撤销") }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { deviceDialog = false }) { Text("完成") } },
        )
    }
    if (feedbackDialog) {
        AlertDialog(
            onDismissRequest = { feedbackDialog = false },
            title = { Text("问题反馈") },
            text = {
                Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { feedbackCategory = "product_issue" }) {
                            Text(if (feedbackCategory == "product_issue") "[使用问题]" else "使用问题")
                        }
                        TextButton(onClick = { feedbackCategory = "content_error" }) {
                            Text(if (feedbackCategory == "content_error") "[内容错误]" else "内容错误")
                        }
                    }
                    OutlinedTextField(
                        value = feedbackContent,
                        onValueChange = { feedbackContent = it },
                        label = { Text("反馈内容") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = {
                            onSubmitFeedback(feedbackCategory, feedbackContent)
                            feedbackContent = ""
                        },
                        enabled = feedbackContent.trim().length >= 2,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) { Text("提交反馈") }
                    Spacer(Modifier.height(16.dp))
                    Text("处理记录", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    feedbackItems.forEach { item ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                            Text(feedbackCategoryLabel(item.category), fontWeight = FontWeight.Medium)
                            Text(item.content, style = MaterialTheme.typography.bodySmall)
                            Text(
                                "${feedbackStatusLabel(item.status)} · ${item.createdAt}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            item.reviewNote?.takeIf { it.isNotBlank() }?.let {
                                Text("回复：$it", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { feedbackDialog = false }) { Text("完成") } },
        )
    }
    if (dataDialog) {
        InfoDialog(
            title = "数据说明",
            sections = listOf(
                InfoSection("账户数据", "学习状态、笔记、收藏、问答会话、额度流水和错题记录保存在账户中。"),
                InfoSection("图片与文档", "错题原图存放于私有对象存储，应用通过登录后的服务端接口访问，设备不会持有对象存储密钥。"),
                InfoSection("删除范围", "删除错题会同时删除对应图片；注销会删除在线账户数据、会话和错题图片，并将必须保留的审计事实匿名化。灾备副本会按运维保留周期自然过期，不用于日常检索。"),
            ),
            onDismiss = { dataDialog = false },
        )
    }
    if (privacyDialog) {
        InfoDialog(
            title = "隐私说明",
            sections = listOf(
                InfoSection("处理目的", "题目、作答过程和提问内容只在完成问答、检索、OCR 与错因分析时发送至服务端。"),
                InfoSection("第三方模型", "服务端只向配置的模型服务发送完成当前任务所需的内容。模型输出可能有误，重要结论应结合教材和引用来源核验。"),
                InfoSection("安全边界", "应用不会把对象存储密钥或模型密钥写入设备。请勿上传身份证件、联系方式、精确住址或与学习无关的个人信息。"),
                InfoSection("你的控制", "你可以删除错题和会话、退出登录或注销账户，也可以通过问题反馈报告内容错误或隐私问题。"),
            ),
            onDismiss = { privacyDialog = false },
        )
    }
    if (minorDialog) {
        InfoDialog(
            title = "未成年人及试点说明",
            sections = listOf(
                InfoSection("参加条件", "未成年人参加学校或组织试点前，应由试点负责人完成适用的学校和监护人告知、授权及退出安排。应用内登录不代替这些手续。"),
                InfoSection("最少信息", "试点不要求填写真实姓名、身份证号、手机号或精确位置。账户名和昵称也不应包含这些信息。"),
                InfoSection("教师可见范围", "学校与班级功能当前默认关闭。将来仅在明确授权后开启；教师端默认只提供班级聚合和匿名学生标识，不展示提问、回答、笔记、反馈正文或错题图片。"),
                InfoSection("退出试点", "参与者可联系试点负责人停止参与，并在应用中删除个人内容或注销账户。试点管理员可按批准名单执行数据清理。"),
            ),
            onDismiss = { minorDialog = false },
        )
    }
    if (communityDialog) {
        CommunityCenterDialog(
            organizationsAvailable = organizationsAvailable,
            memberships = schoolMemberships,
            classes = schoolClasses,
            classOverview = classOverview,
            creditCampaignsAvailable = creditCampaignsAvailable,
            campaigns = creditCampaigns,
            redemptions = creditRedemptions,
            contributionsAvailable = contributionsAvailable,
            contributions = contributions,
            onRedeemInvite = onRedeemInvite,
            onLeaveSchool = onLeaveSchool,
            onLoadOverview = onLoadClassOverview,
            onRedeemCode = onRedeemCreditCode,
            onSubmitContribution = onSubmitContribution,
            onDeleteContribution = onDeleteContribution,
            onDismiss = { communityDialog = false },
        )
    }
}

private data class InfoSection(val title: String, val content: String)

@Composable
private fun InfoDialog(title: String, sections: List<InfoSection>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                sections.forEach { section ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(section.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            section.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("知道了") } },
    )
}

private fun feedbackStatusLabel(status: String): String = when (status) {
    "accepted" -> "已受理"
    "resolved" -> "已解决"
    "rejected" -> "未采纳"
    else -> "待处理"
}

private fun feedbackCategoryLabel(category: String): String = when (category) {
    "content_error" -> "内容错误"
    "relation_error" -> "关系错误"
    "answer_error" -> "回答错误"
    "version_outdated" -> "版本过期"
    "product_issue" -> "使用问题"
    else -> "其他反馈"
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().height(72.dp).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
}
