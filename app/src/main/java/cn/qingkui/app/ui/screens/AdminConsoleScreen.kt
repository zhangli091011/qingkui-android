package cn.qingkui.app.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import cn.qingkui.app.BuildConfig
import org.json.JSONObject

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AdminConsoleScreen(
    accessToken: String?,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var failed by remember { mutableStateOf(false) }
    val adminUrl = remember { BuildConfig.API_BASE_URL.adminConsoleUrl() }
    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                    injectAdminToken(view, accessToken)
                }

                override fun onPageFinished(view: WebView, url: String) {
                    failed = false
                    view.postDelayed({ injectAdminToken(view, accessToken) }, 150)
                }

                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    if (request.isForMainFrame) failed = true
                }
            }
            loadUrl(adminUrl)
        }
    }
    DisposableEffect(webView) {
        onDispose { webView.destroy() }
    }
    LaunchedEffect(accessToken) {
        injectAdminToken(webView, accessToken)
    }
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("完整管理控制台", style = MaterialTheme.typography.titleMedium)
            Row {
                IconButton(onClick = { failed = false; webView.reload() }) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "刷新管理控制台")
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Outlined.Close, contentDescription = "关闭管理控制台")
                }
            }
        }
        Box(Modifier.fillMaxSize()) {
            AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
            if (failed) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("管理控制台暂时无法连接", color = MaterialTheme.colorScheme.error)
                    IconButton(onClick = { failed = false; webView.reload() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "重新加载")
                    }
                }
            }
        }
    }
}

private fun injectAdminToken(webView: WebView, accessToken: String?) {
    val tokenLiteral = JSONObject.quote(accessToken.orEmpty())
    webView.evaluateJavascript(
        "try { var i=document.getElementById('token'); if(i) i.value=$tokenLiteral; sessionStorage.setItem('adminToken', $tokenLiteral); if (typeof saveToken === 'function') saveToken(); else { var s=document.getElementById('status'); if(s) s.textContent='令牌已注入'; } } catch(e) {}",
        null,
    )
}

private fun String.adminConsoleUrl(): String = when {
    endsWith("/api/") -> removeSuffix("api/") + "admin"
    endsWith("/api") -> removeSuffix("api") + "admin"
    else -> trimEnd('/') + "/admin"
}
