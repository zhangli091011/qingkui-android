package cn.qingkui.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import cn.qingkui.app.ui.model.ChatMessage
import cn.qingkui.app.ui.model.MessageAuthor
import cn.qingkui.app.ui.theme.QingkuiDisabled
import cn.qingkui.app.ui.theme.QingkuiGreen
import cn.qingkui.app.ui.theme.QingkuiGreenSoft
import cn.qingkui.app.ui.theme.QingkuiInk

@Composable
fun ChatScreen(
    compact: Boolean,
    draft: String,
    messages: List<ChatMessage>,
    credits: Int,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
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
                modifier = Modifier.weight(1f),
            )
        }
        PromptComposer(
            compact = compact,
            draft = draft,
            credits = credits,
            onDraftChange = onDraftChange,
            onSend = onSend,
            onAttach = onAttach,
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
        Text(
            text = "今天想弄懂什么？",
            style = if (compact) {
                MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp, lineHeight = 40.sp)
            } else {
                MaterialTheme.typography.headlineMedium
            },
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ConversationList(
    messages: List<ChatMessage>,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = if (compact) 20.dp else 40.dp,
            end = if (compact) 20.dp else 40.dp,
            top = 24.dp,
            bottom = 20.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        items(messages, key = { it.id }) { message ->
            MessageRow(message = message)
        }
    }
}

@Composable
private fun MessageRow(message: ChatMessage) {
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
                            Modifier.background(QingkuiGreenSoft, RoundedCornerShape(8.dp))
                        } else {
                            Modifier
                        },
                    )
                    .padding(if (student) 16.dp else 0.dp),
            ) {
                Text(message.text, style = MaterialTheme.typography.bodyLarge)
            }
            if (!student && message.source != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "来源 · ${message.source}",
                    style = MaterialTheme.typography.bodySmall,
                    color = QingkuiGreen,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InlineAction(Icons.Outlined.ThumbUp, "有帮助")
                    InlineAction(Icons.Outlined.ThumbDown, "没帮助")
                    InlineAction(Icons.Outlined.Refresh, "重新回答")
                }
            }
        }
    }
}

@Composable
private fun InlineAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
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
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val enabled = draft.isNotBlank() && credits > 0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 210.dp else 184.dp)
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
                modifier = Modifier.fillMaxWidth().height(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("问知识 · 高一数学", style = MaterialTheme.typography.labelLarge, color = QingkuiGreen)
                if (!compact) {
                    Text("范围：校本知识库", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = "发送",
                    onClick = {
                        onSend()
                        focusManager.clearFocus()
                    },
                    containerColor = if (enabled) QingkuiGreen else QingkuiDisabled,
                    contentColor = if (enabled) Color.White else QingkuiInk,
                    border = null,
                    enabled = enabled,
                )
            }
            Text(
                text = if (compact) {
                    "额度 ${"%,d".format(credits)} · 内容仅用于学习辅助"
                } else {
                    "额度 ${"%,d".format(credits)} · 你的问题仅用于本次学习辅助"
                },
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
