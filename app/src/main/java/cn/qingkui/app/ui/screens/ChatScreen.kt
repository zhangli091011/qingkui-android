package cn.qingkui.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.qingkui.app.R
import cn.qingkui.app.ui.components.QkIconButton
import cn.qingkui.app.ui.components.QkSvgAsset
import cn.qingkui.app.ui.components.MathRichText
import cn.qingkui.app.ui.model.ChatMessage
import cn.qingkui.app.ui.model.AnswerFeedbackAction
import cn.qingkui.app.ui.model.MessageAuthor
import cn.qingkui.app.ui.model.QaHelpLevel
import cn.qingkui.app.ui.model.QaMode
import cn.qingkui.app.ui.model.QaClarification
import cn.qingkui.app.ui.model.QaClarificationOption

@Composable
fun ChatScreen(
    compact: Boolean,
    draft: String,
    messages: List<ChatMessage>,
    credits: Int,
    sending: Boolean,
    aiAvailable: Boolean?,
    aiUnavailableMessage: String?,
    authenticated: Boolean,
    subject: String,
    helpLevel: QaHelpLevel,
    qaMode: QaMode,
    clarification: QaClarification?,
    onDraftChange: (String) -> Unit,
    onHelpLevelChange: (QaHelpLevel) -> Unit,
    onQaModeChange: (QaMode) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onClarification: (QaClarificationOption) -> Unit,
    onDismissClarification: () -> Unit,
    onAttach: () -> Unit,
    onFeedback: (Long, AnswerFeedbackAction) -> Unit,
    onContentError: (Long, String) -> Unit,
    onRetry: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (messages.isEmpty()) {
            EmptyChatHero(
                compact = compact,
                modifier = Modifier.weight(1f),
            )
        } else {
            ConversationList(
                messages = messages,
                compact = compact,
                sending = sending,
                onFeedback = onFeedback,
                onContentError = onContentError,
                onRetry = onRetry,
                modifier = Modifier.weight(1f),
            )
        }
        PromptComposer(
            compact = compact,
            draft = draft,
            credits = credits,
            sending = sending,
            aiAvailable = aiAvailable,
            aiUnavailableMessage = aiUnavailableMessage,
            authenticated = authenticated,
            subject = subject,
            helpLevel = helpLevel,
            qaMode = qaMode,
            onDraftChange = onDraftChange,
            onHelpLevelChange = onHelpLevelChange,
            onQaModeChange = onQaModeChange,
            onSend = onSend,
            onStop = onStop,
            onAttach = onAttach,
        )
    }
    if (clarification != null) {
        AlertDialog(
            onDismissRequest = onDismissClarification,
            title = { Text(clarification.prompt) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    clarification.options.forEach { option ->
                        TextButton(onClick = { onClarification(option) }, modifier = Modifier.fillMaxWidth()) {
                            Text(option.label, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = onDismissClarification) { Text("取消") } },
        )
    }
}

@Composable
private fun EmptyChatHero(compact: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(horizontal = if (compact) 24.dp else 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        QkSvgAsset(
            resourceId = if (compact) R.raw.qk_graph_portrait else R.raw.qk_graph_landscape,
            contentDescription = null,
            modifier = Modifier
                .width(if (compact) 320.dp else 720.dp)
                .height(if (compact) 300.dp else 260.dp),
            contentScale = ContentScale.FillBounds,
        )
        TypingPrompt(
            style = if (compact) {
                MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp, lineHeight = 40.sp)
            } else {
                MaterialTheme.typography.headlineMedium
            },
        )
    }
}

/**
 * Empty-state prompt with a lightweight terminal-style typewriter treatment.
 * The prompt types in, pauses for reading, then erases itself and repeats.
 * A separate blinking caret makes the interaction feel alive without
 * affecting the rest of the composer or conversation state.
 */
@Composable
private fun TypingPrompt(
    style: androidx.compose.ui.text.TextStyle,
) {
    val prompts = remember {
        listOf(
            "今天想弄懂什么？",
            "哪道题卡住了？",
            "把难点交给青葵吧。",
            "从一个知识点开始吧。",
        )
    }
    var promptIndex by remember { mutableStateOf(0) }
    var prompt by remember { mutableStateOf(prompts.first()) }
    var characterCount by remember { mutableStateOf(0) }
    var cursorVisible by remember { mutableStateOf(true) }

    LaunchedEffect(prompts) {
        while (true) {
            // Leave a small breathing space before each new sentence so the
            // transition does not feel like one continuous stream of text.
            kotlinx.coroutines.delay(720)
            prompt = prompts[promptIndex]
            characterCount = 0
            for (count in 1..prompt.length) {
                characterCount = count
                kotlinx.coroutines.delay(105)
            }
            // Give the complete sentence enough time to be read.
            kotlinx.coroutines.delay(1_650)
            for (count in (prompt.length - 1) downTo 0) {
                characterCount = count
                kotlinx.coroutines.delay(72)
            }
            // Pause on the empty state before moving to a different prompt.
            kotlinx.coroutines.delay(900)
            promptIndex = (promptIndex + 1) % prompts.size
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(480)
            cursorVisible = !cursorVisible
        }
    }

    Text(
        text = prompt.take(characterCount) + if (cursorVisible) "▌" else " ",
        style = style,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ConversationList(
    messages: List<ChatMessage>,
    compact: Boolean,
    sending: Boolean,
    onFeedback: (Long, AnswerFeedbackAction) -> Unit,
    onContentError: (Long, String) -> Unit,
    onRetry: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size, sending) {
        val lastIndex = messages.size + if (sending) 1 else 0
        if (lastIndex > 0) listState.animateScrollToItem(lastIndex - 1)
    }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = if (compact) 20.dp else 40.dp,
            end = if (compact) 20.dp else 40.dp,
            top = 24.dp,
            bottom = 20.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        items(messages, key = { it.id }) { message ->
            MessageRow(
                message = message,
                onFeedback = onFeedback,
                onContentError = onContentError,
                onRetry = onRetry,
            )
        }
        if (sending) {
            item(key = "assistant-thinking") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("正在结合知识库生成回答…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun MessageRow(
    message: ChatMessage,
    onFeedback: (Long, AnswerFeedbackAction) -> Unit,
    onContentError: (Long, String) -> Unit,
    onRetry: (Long) -> Unit,
) {
    var contentErrorDialog by remember { mutableStateOf(false) }
    var contentErrorDetail by remember { mutableStateOf("") }
    val student = message.author == MessageAuthor.Student
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (student) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxWidth(.88f),
            horizontalAlignment = if (student) Alignment.End else Alignment.Start,
        ) {
            Box(
                modifier = Modifier
                    .then(
                        if (student) {
                            Modifier.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                        } else {
                            Modifier
                        },
                    )
                    .padding(if (student) 16.dp else 0.dp),
            ) {
                MathRichText(message.text, style = MaterialTheme.typography.bodyLarge)
            }
            if (!student && (message.citations.isNotEmpty() || message.source != null)) {
                Spacer(Modifier.height(12.dp))
                Text("参考来源", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                if (message.citations.isNotEmpty()) {
                    message.citations.forEach { citation ->
                        Spacer(Modifier.height(6.dp))
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                        ) {
                            Text("${citation.nodeName} · ${citation.sourceTitle}", style = MaterialTheme.typography.bodySmall)
                            Text(citation.sourceLocation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (citation.excerpt.isNotBlank()) {
                                MathRichText(
                                    citation.excerpt,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                } else {
                    Text(message.source.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    InlineAction(Icons.Outlined.ThumbUp, "有帮助") { onFeedback(message.id, AnswerFeedbackAction.Helpful) }
                    InlineAction(Icons.Outlined.ThumbDown, "没帮助") { onFeedback(message.id, AnswerFeedbackAction.Unhelpful) }
                    InlineAction(Icons.Outlined.Flag, "内容有误") {
                        contentErrorDetail = ""
                        contentErrorDialog = true
                    }
                    InlineAction(Icons.Outlined.EventRepeat, "标记复习") { onFeedback(message.id, AnswerFeedbackAction.Review) }
                    InlineAction(Icons.Outlined.Refresh, "重新回答") { onRetry(message.id) }
                }
            }
        }
    }
    if (contentErrorDialog) {
        AlertDialog(
            onDismissRequest = { contentErrorDialog = false },
            title = { Text("指出回答问题") },
            text = {
                OutlinedTextField(
                    value = contentErrorDetail,
                    onValueChange = { contentErrorDetail = it.take(1000) },
                    label = { Text("错误说明") },
                    placeholder = { Text("例如：公式条件不完整") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = contentErrorDetail.trim().length >= 2,
                    onClick = {
                        onContentError(message.id, contentErrorDetail.trim())
                        contentErrorDialog = false
                    },
                ) { Text("提交") }
            },
            dismissButton = { TextButton(onClick = { contentErrorDialog = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun InlineAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(modifier = Modifier.clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PromptComposer(
    compact: Boolean,
    draft: String,
    credits: Int,
    sending: Boolean,
    aiAvailable: Boolean?,
    aiUnavailableMessage: String?,
    authenticated: Boolean,
    subject: String,
    helpLevel: QaHelpLevel,
    qaMode: QaMode,
    onDraftChange: (String) -> Unit,
    onHelpLevelChange: (QaHelpLevel) -> Unit,
    onQaModeChange: (QaMode) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onAttach: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var helpMenuOpen by remember { mutableStateOf(false) }
    val hasCredits = !authenticated || credits >= helpLevel.creditCost
    val enabled = sending || (draft.isNotBlank() && hasCredits && aiAvailable != false)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 226.dp else 200.dp)
            .padding(
                start = if (compact) 20.dp else 40.dp,
                end = if (compact) 20.dp else 40.dp,
                top = 16.dp,
                bottom = if (compact) 20.dp else 28.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().widthIn(max = 840.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                var modeMenuOpen by remember { mutableStateOf(false) }
                Box {
                    TextButton(onClick = { modeMenuOpen = true }, modifier = Modifier.height(32.dp)) { Text("${qaMode.label} · $subject") }
                    DropdownMenu(expanded = modeMenuOpen, onDismissRequest = { modeMenuOpen = false }) {
                        QaMode.entries.forEach { mode ->
                            DropdownMenuItem(text = { Text(mode.label) }, onClick = { onQaModeChange(mode); modeMenuOpen = false })
                        }
                    }
                }
                Box {
                    TextButton(onClick = { helpMenuOpen = true }, modifier = Modifier.height(32.dp)) {
                        Text("${helpLevel.label} · ${helpLevel.creditCost}额度")
                    }
                    DropdownMenu(expanded = helpMenuOpen, onDismissRequest = { helpMenuOpen = false }) {
                        QaHelpLevel.entries.forEach { level ->
                            DropdownMenuItem(
                                text = { Text("${level.label} · ${level.creditCost}额度") },
                                onClick = {
                                    onHelpLevelChange(level)
                                    helpMenuOpen = false
                                },
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 72.dp else 64.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .padding(start = 12.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QkIconButton(
                    imageVector = Icons.Outlined.AttachFile,
                    contentDescription = "添加图片",
                    onClick = onAttach,
                    containerColor = Color.Transparent,
                    border = null,
                )
                Spacer(Modifier.width(if (compact) 8.dp else 10.dp))
                BasicTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (enabled) {
                            onSend()
                            focusManager.clearFocus()
                        }
                    }),
                    singleLine = true,
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (draft.isEmpty()) {
                                Text(
                                    if (compact) "输入问题" else "输入你的问题",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            inner()
                        }
                    },
                )
                QkIconButton(
                    imageVector = if (sending) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.Send,
                    contentDescription = if (sending) "停止生成" else "发送",
                    onClick = {
                        if (sending) onStop() else onSend()
                        focusManager.clearFocus()
                    },
                    containerColor = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    border = null,
                    enabled = enabled,
                )
            }
            Text(
                text = if (aiAvailable == false) {
                    aiUnavailableMessage ?: "AI 服务暂时不可用"
                } else if (!authenticated) {
                    "登录后发送并同步问答与学习记录"
                } else if (!hasCredits) {
                    "当前额度不足，${helpLevel.label}需要 ${helpLevel.creditCost} 额度"
                } else if (compact) {
                    "额度 ${"%,d".format(credits)} · 本次预计 ${helpLevel.creditCost} 额度"
                } else {
                    "额度 ${"%,d".format(credits)} · ${helpLevel.label}预计消耗 ${helpLevel.creditCost} 额度"
                },
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = if (aiAvailable == false || (authenticated && !hasCredits)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
