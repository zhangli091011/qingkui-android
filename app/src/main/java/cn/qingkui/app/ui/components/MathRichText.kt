package cn.qingkui.app.ui.components

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.text.TextUtils
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.ByteArrayInputStream
import java.util.Locale

@Composable
fun MathRichText(
    value: String,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier,
) {
    val segments = remember(value) { parseMathText(value) }
    if (segments.none { it is MathTextSegment.Formula }) {
        Text(value, style = style, color = color, modifier = modifier)
        return
    }

    val colorCss = remember(color) { String.format(Locale.US, "#%06X", color.toArgb() and 0xFFFFFF) }
    val fontSize = style.fontSize.value.takeIf { it.isFinite() && it > 0f } ?: 16f
    val lineHeight = style.lineHeight.value.takeIf { it.isFinite() && it > 0f } ?: fontSize * 1.5f
    val html = remember(value, colorCss, fontSize, lineHeight) {
        mathHtml(value, colorCss, fontSize, lineHeight)
    }
    var contentHeight by remember(value) { mutableStateOf(48.dp) }
    val webViewHolder = remember { arrayOfNulls<WebView>(1) }

    DisposableEffect(Unit) {
        onDispose {
            webViewHolder[0]?.stopLoading()
            webViewHolder[0]?.removeAllViews()
            webViewHolder[0]?.destroy()
            webViewHolder[0] = null
        }
    }

    AndroidView(
        factory = { context ->
            val assetLoader = WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
                .build()
            secureMathWebView(context).also { view ->
                webViewHolder[0] = view
                view.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) = true

                    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                        val url = request?.url ?: return blockedResponse()
                        return assetLoader.shouldInterceptRequest(url) ?: blockedResponse()
                    }

                    override fun onPageFinished(view: WebView, url: String?) {
                        fun measure() {
                            view.evaluateJavascript(
                                "Math.ceil(Math.max(document.body.scrollHeight,document.documentElement.scrollHeight)).toString()",
                            ) { result ->
                                result.trim('"').toFloatOrNull()?.let { pixels ->
                                    contentHeight = pixels.coerceIn(34f, 1600f).dp
                                }
                            }
                        }
                        view.post(::measure)
                        view.postDelayed(::measure, 80)
                    }
                }
            }
        },
        update = { view ->
            val contentKey = html.hashCode()
            if (view.tag != contentKey) {
                view.tag = contentKey
                view.loadDataWithBaseURL(KATEX_ASSET_ROOT, html, "text/html", "UTF-8", null)
            }
        },
        modifier = modifier.fillMaxWidth().height(contentHeight),
    )
}

@SuppressLint("SetJavaScriptEnabled")
private fun secureMathWebView(context: android.content.Context) = WebView(context).apply {
    setBackgroundColor(AndroidColor.TRANSPARENT)
    isVerticalScrollBarEnabled = false
    isHorizontalScrollBarEnabled = false
    settings.javaScriptEnabled = true
    settings.blockNetworkLoads = true
    settings.allowContentAccess = false
    settings.allowFileAccess = false
    settings.domStorageEnabled = false
    settings.cacheMode = WebSettings.LOAD_NO_CACHE
    settings.setSupportZoom(false)
    settings.builtInZoomControls = false
    settings.displayZoomControls = false
    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
}

private fun blockedResponse() = WebResourceResponse(
    "text/plain",
    "UTF-8",
    ByteArrayInputStream(ByteArray(0)),
)

private fun mathHtml(value: String, color: String, fontSize: Float, lineHeight: Float): String {
    val escaped = TextUtils.htmlEncode(value)
    val dollar = '$'
    return """<!doctype html>
<html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1">
<meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline'; font-src 'self'; img-src data:">
<link rel="stylesheet" href="katex.min.css">
<style>html,body{margin:0;padding:0;background:transparent;color:$color;font:400 ${fontSize}px/${lineHeight}px system-ui,sans-serif;overflow:hidden}#content{white-space:pre-wrap;overflow-wrap:anywhere}.katex-display{overflow-x:auto;overflow-y:hidden;margin:.45em 0}</style>
</head><body><div id="content">$escaped</div><script src="katex.min.js"></script><script src="contrib/auto-render.min.js"></script>
<script>renderMathInElement(document.getElementById('content'),{delimiters:[{left:'${dollar}${dollar}',right:'${dollar}${dollar}',display:true},{left:'\\[',right:'\\]',display:true},{left:'\\(',right:'\\)',display:false},{left:'${dollar}',right:'${dollar}',display:false}],throwOnError:false,strict:'ignore'});</script>
</body></html>"""
}

private const val KATEX_ASSET_ROOT = "https://appassets.androidplatform.net/assets/katex/"
