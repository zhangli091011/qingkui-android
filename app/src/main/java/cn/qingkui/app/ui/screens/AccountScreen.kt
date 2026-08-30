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
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import cn.qingkui.app.ui.model.DeviceSessionItem
import cn.qingkui.app.ui.model.FeedbackItem
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
                TextButton(onClick = { }) {
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
            content = "学习状态、笔记、收藏、问答会话、额度流水和错题记录保存在账户中。错题原图存放于私有对象存储，仅通过登录后的接口访问。删除错题会同时删除其图片；注销账户会删除个人学习数据、会话和错题图片，并匿名化必要审计记录。",
            onDismiss = { dataDialog = false },
        )
    }
    if (privacyDialog) {
        InfoDialog(
            title = "隐私说明",
            content = "应用不会把对象存储密钥或模型密钥写入设备。题目、作答过程和提问内容只在完成问答、OCR 与错因分析时发送至服务端。请勿上传身份证件、联系方式或与学习无关的个人信息。你可以随时删除错题、会话或注销账户。",
            onDismiss = { privacyDialog = false },
        )
    }
}

@Composable
private fun InfoDialog(title: String, content: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(content, style = MaterialTheme.typography.bodyMedium) },
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
