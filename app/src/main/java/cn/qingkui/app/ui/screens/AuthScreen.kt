package cn.qingkui.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cn.qingkui.app.ui.model.AuthMode

@Composable
fun AuthScreen(
    mode: AuthMode,
    username: String,
    password: String,
    nickname: String,
    email: String,
    loading: Boolean,
    errorMessage: String?,
    onModeChange: (AuthMode) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onNicknameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onRequestPasswordReset: (String) -> Unit,
    onConfirmPasswordReset: (String, String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
) {
    var resetOpen by remember { mutableStateOf(false) }
    var resetRequested by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var resetToken by remember { mutableStateOf("") }
    var resetPassword by remember { mutableStateOf("") }
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("青葵计划", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("连接你的知识图谱", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(28.dp))
            Row(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AuthMode.entries.forEach { item ->
                    TextButton(
                        onClick = { onModeChange(item) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = if (item == mode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (item == mode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) { Text(if (item == AuthMode.Login) "登录" else "注册") }
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(username, onUsernameChange, Modifier.fillMaxWidth(), label = { Text("用户名") }, singleLine = true, enabled = !loading)
            if (mode == AuthMode.Register) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(nickname, onNicknameChange, Modifier.fillMaxWidth(), label = { Text("昵称（可选）") }, singleLine = true, enabled = !loading)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    email, onEmailChange, Modifier.fillMaxWidth(), label = { Text("邮箱（用于找回密码）") },
                    singleLine = true, enabled = !loading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("密码") },
                singleLine = true,
                enabled = !loading,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            )
            errorMessage?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !loading,
                shape = RoundedCornerShape(8.dp),
            ) {
                if (loading) CircularProgressIndicator(modifier = Modifier.height(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else Text(if (mode == AuthMode.Login) "登录" else "创建账户")
            }
            if (mode == AuthMode.Login) {
                TextButton(onClick = { resetOpen = true }) { Text("忘记密码") }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onBack, enabled = !loading) {
                Text("暂不登录，返回首页")
            }
        }
    }
    if (resetOpen) {
        AlertDialog(
            onDismissRequest = { resetOpen = false },
            title = { Text("找回密码") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!resetRequested) {
                        OutlinedTextField(resetEmail, { resetEmail = it }, label = { Text("绑定邮箱") }, singleLine = true)
                    } else {
                        Text("请查看邮件并输入一次性令牌。", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(resetToken, { resetToken = it }, label = { Text("重置令牌") }, singleLine = true)
                        OutlinedTextField(
                            resetPassword, { resetPassword = it }, label = { Text("新密码") }, singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (!resetRequested) { onRequestPasswordReset(resetEmail); resetRequested = true }
                    else { onConfirmPasswordReset(resetToken, resetPassword); resetOpen = false }
                }) { Text(if (resetRequested) "重置密码" else "发送邮件") }
            },
            dismissButton = { TextButton(onClick = { resetOpen = false }) { Text("取消") } },
        )
    }
}
