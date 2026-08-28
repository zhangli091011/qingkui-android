package cn.qingkui.app.ui.screens

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.qingkui.app.ui.components.QkIconButton
import cn.qingkui.app.ui.model.KnowledgeNode
import cn.qingkui.app.ui.model.KnowledgeStatus
import cn.qingkui.app.ui.theme.QingkuiError
import cn.qingkui.app.ui.theme.QingkuiGreen
import cn.qingkui.app.ui.theme.QingkuiGreenSoft
import cn.qingkui.app.ui.theme.QingkuiOrange

@Composable
fun KnowledgeGraphScreen(
    compact: Boolean,
    nodes: List<KnowledgeNode>,
    selectedNodeId: String?,
    onSelectNode: (String) -> Unit,
    onAskNode: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = if (compact) 20.dp else 40.dp),
    ) {
        GraphHeader(compact)
        if (compact) {
            GraphCanvas(
                nodes = nodes,
                selectedNodeId = selectedNodeId,
                onSelectNode = onSelectNode,
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
            selectedNodeId?.let { id ->
                NodeDetailPanel(
                    node = nodes.first { it.id == id },
                    compact = true,
                    onAsk = { onAskNode(id) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            Row(Modifier.fillMaxSize()) {
                GraphCanvas(
                    nodes = nodes,
                    selectedNodeId = selectedNodeId,
                    onSelectNode = onSelectNode,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                selectedNodeId?.let { id ->
                    Spacer(Modifier.width(24.dp))
                    NodeDetailPanel(
                        node = nodes.first { it.id == id },
                        compact = false,
                        onAsk = { onAskNode(id) },
                        modifier = Modifier.width(320.dp).fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun GraphHeader(compact: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().height(if (compact) 76.dp else 72.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("高一数学", style = MaterialTheme.typography.headlineSmall)
            Text(
                "函数 > 二次函数",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!compact) {
            OutlinedButton(onClick = {}) {
                Icon(Icons.Outlined.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("节点网络")
            }
            Spacer(Modifier.width(8.dp))
        }
        QkIconButton(Icons.Outlined.Search, "搜索知识点", onClick = {})
    }
}

@Composable
private fun GraphCanvas(
    nodes: List<KnowledgeNode>,
    selectedNodeId: String?,
    onSelectNode: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var zoomFactor by remember { mutableFloatStateOf(1f) }
    var translation by remember { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    val density = LocalDensity.current
    val nodeRadiusPx = with(density) { 34.dp.toPx() }
    val labelPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
    }
    val subtitlePaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
    }

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = .68f), RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
    ) {
        val primary = MaterialTheme.colorScheme.primary
        val ink = MaterialTheme.colorScheme.onSurface
        val secondary = MaterialTheme.colorScheme.onSurfaceVariant
        val outline = MaterialTheme.colorScheme.outline
        val surface = MaterialTheme.colorScheme.surface
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(nodes) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val newScale = (zoomFactor * zoom).coerceIn(.7f, 2.2f)
                        translation = (translation + pan) + (centroid - translation) * (1f - newScale / zoomFactor)
                        zoomFactor = newScale
                    }
                }
                .pointerInput(nodes, viewport, zoomFactor, translation) {
                    detectTapGestures { tap ->
                        val graphTap = (tap - translation) / zoomFactor
                        nodes.minByOrNull { node ->
                            val point = Offset(viewport.width * node.x, viewport.height * node.y)
                            (graphTap - point).getDistance()
                        }?.takeIf { node ->
                            val point = Offset(viewport.width * node.x, viewport.height * node.y)
                            (graphTap - point).getDistance() <= nodeRadiusPx * 1.5f
                        }?.let { onSelectNode(it.id) }
                    }
                },
        ) {
            viewport = size
            labelPaint.textSize = 14.dp.toPx()
            labelPaint.color = ink.toArgbCompat()
            subtitlePaint.textSize = 11.dp.toPx()
            subtitlePaint.color = secondary.toArgbCompat()
            withTransform({
                translate(translation.x, translation.y)
                scale(zoomFactor, zoomFactor)
            }) {
                val current = nodes.firstOrNull { it.id == "quadratic_function" } ?: nodes.first()
                val currentPoint = Offset(size.width * current.x, size.height * current.y)
                nodes.filterNot { it.id == current.id }.forEach { node ->
                    val point = Offset(size.width * node.x, size.height * node.y)
                    drawLine(outline, currentPoint, point, strokeWidth = 2.dp.toPx())
                }
                nodes.forEach { node ->
                    val point = Offset(size.width * node.x, size.height * node.y)
                    val active = node.id == selectedNodeId
                    val fill = statusColor(node.status)
                    drawCircle(
                        color = if (node.status == KnowledgeStatus.Unexplored) surface else fill.copy(alpha = .18f),
                        radius = nodeRadiusPx + if (active) 5.dp.toPx() else 0f,
                        center = point,
                    )
                    drawCircle(
                        color = if (active) primary else fill,
                        radius = nodeRadiusPx + if (active) 5.dp.toPx() else 0f,
                        center = point,
                        style = Stroke(width = if (active) 3.dp.toPx() else 2.dp.toPx()),
                    )
                    drawContext.canvas.nativeCanvas.drawText(node.title, point.x, point.y + 5.dp.toPx(), labelPaint)
                    drawContext.canvas.nativeCanvas.drawText(node.subtitle, point.x, point.y + 54.dp.toPx(), subtitlePaint)
                }
            }
        }
        Row(
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QkIconButton(
                Icons.Outlined.CenterFocusStrong,
                "回到当前节点",
                onClick = { zoomFactor = 1f; translation = Offset.Zero },
            )
            QkIconButton(Icons.Outlined.ZoomIn, "展开一层", onClick = { zoomFactor = (zoomFactor * 1.2f).coerceAtMost(2.2f) })
            QkIconButton(Icons.Outlined.Share, "切换视图", onClick = {})
        }
    }
}

@Composable
private fun NodeDetailPanel(
    node: KnowledgeNode,
    compact: Boolean,
    onAsk: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            .padding(if (compact) 20.dp else 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(node.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    node.status.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor(node.status),
                )
            }
            if (!compact) {
                QkIconButton(Icons.AutoMirrored.Outlined.ArrowBack, "收起详情", onClick = {})
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "一般地，形如 y = ax² + bx + c（a ≠ 0）的函数叫作二次函数。",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(16.dp))
        Divider(color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(12.dp))
        Text("来源", style = MaterialTheme.typography.labelLarge, color = QingkuiGreen)
        Text("校本知识库 · 必修第一册", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(if (compact) 16.dp else 24.dp))
        Button(
            onClick = onAsk,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = QingkuiGreen),
        ) {
            Icon(Icons.Outlined.QuestionAnswer, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("开始提问")
        }
    }
}

private fun statusColor(status: KnowledgeStatus): Color = when (status) {
    KnowledgeStatus.Unexplored -> Color(0xFF91A098)
    KnowledgeStatus.Explored -> QingkuiGreen
    KnowledgeStatus.Understood -> Color(0xFF3979A8)
    KnowledgeStatus.Verified -> Color(0xFF476F59)
    KnowledgeStatus.Unstable -> QingkuiOrange
    KnowledgeStatus.ErrorProne -> QingkuiError
}

private fun Color.toArgbCompat(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(),
    (red * 255).toInt(),
    (green * 255).toInt(),
    (blue * 255).toInt(),
)
