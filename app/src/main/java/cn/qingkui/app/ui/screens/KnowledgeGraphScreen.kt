package cn.qingkui.app.ui.screens

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.itemsIndexed as lazyItemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.qingkui.app.ui.components.QkIconButton
import cn.qingkui.app.ui.components.MathRichText
import cn.qingkui.app.ui.model.KnowledgeKind
import cn.qingkui.app.ui.model.KnowledgeCatalogScope
import cn.qingkui.app.ui.model.KnowledgeNode
import cn.qingkui.app.ui.model.KnowledgeRelation
import cn.qingkui.app.ui.model.KnowledgeSource
import cn.qingkui.app.ui.model.KnowledgeStatus
import cn.qingkui.app.ui.model.KnowledgeTreeChapter
import cn.qingkui.app.ui.model.RelationType
import cn.qingkui.app.ui.model.UnderstandingCheck

private enum class GraphDisplayMode(val label: String) {
    Graph("图谱"),
    Cards("知识卡"),
    Outline("章节"),
}

private enum class DetailTab(val label: String) {
    Overview("概览"),
    Relations("关联"),
    Cards("卡片"),
}

@Composable
fun KnowledgeGraphScreen(
    compact: Boolean,
    nodes: List<KnowledgeNode>,
    relations: List<KnowledgeRelation>,
    selectedNodeId: String?,
    onSelectNode: (String) -> Unit,
    onClearSelection: () -> Unit = {},
    onAskNode: (String) -> Unit,
    onMarkStatus: (KnowledgeStatus) -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    understandingCheck: UnderstandingCheck? = null,
    understandingCheckSubmitting: Boolean = false,
    onStartUnderstandingCheck: () -> Unit = {},
    onSubmitUnderstandingCheck: (String) -> Unit = {},
    onDismissUnderstandingCheck: () -> Unit = {},
    note: String = "",
    onNoteChange: (String) -> Unit = {},
    onSearch: (String) -> Unit = {},
    subject: String = "数学",
    catalog: List<KnowledgeCatalogScope> = emptyList(),
    selectedScope: KnowledgeCatalogScope? = null,
    chapters: List<KnowledgeTreeChapter> = emptyList(),
    onSelectScope: (KnowledgeCatalogScope) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var displayMode by remember { mutableStateOf(GraphDisplayMode.Graph) }
    var relationFilter by remember { mutableStateOf<RelationType?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    // The API can contain the same concept under multiple source records. Present one
    // canonical node and remap its branches so the canvas remains readable.
    val canonicalData = remember(nodes, relations) { canonicalizeGraph(nodes, relations) }
    val canonicalNodes = canonicalData.first
    val canonicalRelations = canonicalData.second
    val canonicalSelectedId = canonicalData.third[selectedNodeId] ?: selectedNodeId
    val visibleNodes = remember(canonicalNodes, searchQuery) {
        if (searchQuery.isBlank()) canonicalNodes else canonicalNodes.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                it.subtitle.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
        }
    }
    val selectedNode = canonicalNodes.firstOrNull { it.id == canonicalSelectedId }
    LaunchedEffect(searchQuery) {
        kotlinx.coroutines.delay(280)
        onSearch(searchQuery)
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = if (compact) 12.dp else 0.dp)) {
        GraphHeader(
            compact = compact,
            displayMode = displayMode,
            searchQuery = searchQuery,
            onDisplayModeChange = { displayMode = it },
            onSearchQueryChange = { searchQuery = it },
            subject = subject,
        )
        if (displayMode == GraphDisplayMode.Outline) {
            KnowledgeScopePicker(catalog, selectedScope, onSelectScope)
        } else {
            RelationFilterRow(selected = relationFilter, onSelect = { relationFilter = it })
        }
        Spacer(Modifier.height(12.dp))

        Box(Modifier.fillMaxWidth().weight(1f).clipToBounds()) {
            GraphContent(
                displayMode,
                visibleNodes,
                canonicalNodes,
                canonicalRelations,
                relationFilter,
                canonicalSelectedId,
                chapters,
                onSelectNode,
                onClearSelection,
                Modifier.fillMaxSize(),
            )
            selectedNode?.let { node ->
                NodeDetailPanel(
                    node = node,
                    nodes = canonicalNodes,
                    relations = canonicalRelations,
                    compact = compact,
                    onAsk = { onAskNode(node.id) },
                    onDismiss = onClearSelection,
                    onMarkStatus = onMarkStatus,
                    onToggleFavorite = onToggleFavorite,
                    onStartUnderstandingCheck = onStartUnderstandingCheck,
                    note = note,
                    onNoteChange = onNoteChange,
                    modifier = if (compact) {
                        Modifier.align(Alignment.BottomCenter).fillMaxWidth().heightIn(max = 370.dp).padding(8.dp)
                    } else {
                        Modifier.align(Alignment.TopEnd).width(360.dp).heightIn(max = 540.dp).padding(12.dp)
                    },
                )
            }
        }
    }
    understandingCheck?.let { check ->
        UnderstandingCheckDialog(
            check = check,
            submitting = understandingCheckSubmitting,
            onSubmit = onSubmitUnderstandingCheck,
            onDismiss = onDismissUnderstandingCheck,
        )
    }
}

private fun canonicalizeGraph(
    nodes: List<KnowledgeNode>,
    relations: List<KnowledgeRelation>,
): Triple<List<KnowledgeNode>, List<KnowledgeRelation>, Map<String, String>> {
    val idMap = linkedMapOf<String, String>()
    val unique = linkedMapOf<String, KnowledgeNode>()
    nodes.forEach { node ->
        val key = node.title.trim().lowercase().ifBlank { node.id }
        val canonical = unique[key]
        if (canonical == null) {
            unique[key] = node
            idMap[node.id] = node.id
        } else {
            idMap[node.id] = canonical.id
        }
    }
    val mergedRelations = relations.mapNotNull { relation ->
        val from = idMap[relation.fromId] ?: relation.fromId
        val to = idMap[relation.toId] ?: relation.toId
        if (from == to) null else relation.copy(fromId = from, toId = to)
    }.distinctBy { Triple(it.fromId, it.toId, it.type) }
    return Triple(unique.values.toList(), mergedRelations, idMap)
}

@Composable
private fun GraphHeader(
    compact: Boolean,
    displayMode: GraphDisplayMode,
    searchQuery: String,
    onDisplayModeChange: (GraphDisplayMode) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    subject: String,
) {
    if (compact) {
        Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${subject}知识库", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("当前由系统自动识别学科", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                ModeSwitch(displayMode, onDisplayModeChange)
            }
            Spacer(Modifier.height(10.dp))
            GraphSearchField(searchQuery, onSearchQueryChange, Modifier.fillMaxWidth())
        }
    } else {
        Row(modifier = Modifier.fillMaxWidth().height(76.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${subject}知识库", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("当前由系统自动识别学科", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ModeSwitch(displayMode, onDisplayModeChange)
            Spacer(Modifier.width(12.dp))
            GraphSearchField(searchQuery, onSearchQueryChange, Modifier.width(240.dp))
        }
    }
}

@Composable
private fun ModeSwitch(selected: GraphDisplayMode, onSelect: (GraphDisplayMode) -> Unit) {
    Row(
        modifier = Modifier
            .height(44.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        GraphDisplayMode.entries.forEach { mode ->
            val active = mode == selected
            Row(
                modifier = Modifier
                    .height(36.dp)
                    .background(if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, RoundedCornerShape(6.dp))
                    .clickable(role = Role.Tab) { onSelect(mode) }
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    when (mode) {
                        GraphDisplayMode.Graph -> Icons.Outlined.Hub
                        GraphDisplayMode.Cards -> Icons.Outlined.GridView
                        GraphDisplayMode.Outline -> Icons.AutoMirrored.Outlined.MenuBook
                    },
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(mode.label, style = MaterialTheme.typography.labelLarge, color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun KnowledgeScopePicker(
    catalog: List<KnowledgeCatalogScope>,
    selected: KnowledgeCatalogScope?,
    onSelect: (KnowledgeCatalogScope) -> Unit,
) {
    if (catalog.isEmpty()) {
        Text("暂无可浏览的教材目录", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val current = selected ?: catalog.first()
    val subjects = catalog.map { it.subject }.distinct()
    val grades = catalog.filter { it.subject == current.subject }.map { it.grade }.distinct()
    val versions = catalog.filter { it.subject == current.subject && it.grade == current.grade }.map { it.textbookVersion }.distinct()
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ScopeMenu("学科", current.subject, subjects) { subject ->
            catalog.firstOrNull { it.subject == subject }?.let(onSelect)
        }
        ScopeMenu("年级", current.grade, grades) { grade ->
            catalog.firstOrNull { it.subject == current.subject && it.grade == grade }?.let(onSelect)
        }
        ScopeMenu("教材", current.textbookVersion, versions) { version ->
            catalog.firstOrNull {
                it.subject == current.subject && it.grade == current.grade && it.textbookVersion == version
            }?.let(onSelect)
        }
        Text(
            "${current.nodeCount} 个知识点",
            modifier = Modifier.align(Alignment.CenterVertically).padding(horizontal = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ScopeMenu(label: String, value: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 10.dp)) {
            Text("$label：$value", maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Outlined.ExpandMore, contentDescription = "选择$label", modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { expanded = false; onSelect(option) },
                )
            }
        }
    }
}

@Composable
private fun GraphSearchField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(44.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text("搜索节点或知识卡", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    inner()
                }
            },
        )
    }
}

@Composable
private fun RelationFilterRow(selected: RelationType?, onSelect: (RelationType?) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        RelationChip("全部关系", null, selected == null) { onSelect(null) }
        RelationType.entries.forEach { type ->
            RelationChip(type.label, relationColor(type), selected == type) { onSelect(type) }
        }
        Spacer(Modifier.width(8.dp))
        Text("节点边框表示掌握状态", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RelationChip(label: String, color: Color?, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(34.dp)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, RoundedCornerShape(8.dp))
            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        color?.let { Box(Modifier.size(7.dp).background(it, CircleShape)) }
        Text(label, style = MaterialTheme.typography.bodySmall, color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GraphContent(
    displayMode: GraphDisplayMode,
    nodes: List<KnowledgeNode>,
    allNodes: List<KnowledgeNode>,
    relations: List<KnowledgeRelation>,
    relationFilter: RelationType?,
    selectedNodeId: String?,
    chapters: List<KnowledgeTreeChapter>,
    onSelectNode: (String) -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (displayMode) {
        GraphDisplayMode.Graph -> GraphCanvas(nodes, allNodes, relations, relationFilter, selectedNodeId, onSelectNode, onClearSelection, modifier)
        GraphDisplayMode.Cards -> KnowledgeCardGrid(nodes, selectedNodeId, onSelectNode, modifier)
        GraphDisplayMode.Outline -> KnowledgeOutline(chapters, selectedNodeId, onSelectNode, modifier)
    }
}

@Composable
private fun KnowledgeOutline(
    chapters: List<KnowledgeTreeChapter>,
    selectedNodeId: String?,
    onSelectNode: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (chapters.isEmpty()) {
        Box(
            modifier = modifier
                .background(MaterialTheme.colorScheme.surface.copy(alpha = .78f), RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("当前教材暂无已审核的章节", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = .78f), RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        lazyItemsIndexed(chapters, key = { index, chapter -> "${chapter.name}-$index" }) { _, chapter ->
            ChapterRow(chapter, selectedNodeId, onSelectNode)
        }
    }
}

@Composable
private fun ChapterRow(
    chapter: KnowledgeTreeChapter,
    selectedNodeId: String?,
    onSelectNode: (String) -> Unit,
) {
    var expanded by remember(chapter.name) { mutableStateOf(true) }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = if (expanded) "收起章节" else "展开章节",
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(8.dp))
            Text(chapter.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text("${chapter.sections.sumOf { it.nodes.size }}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (expanded) {
            chapter.sections.forEach { section ->
                Text(
                    section.name,
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background.copy(alpha = .7f)).padding(horizontal = 46.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                section.nodes.forEach { node ->
                    val active = node.id == selectedNodeId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable { onSelectNode(node.id) }
                            .padding(start = 46.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(8.dp).background(statusColor(node.status), CircleShape))
                        Spacer(Modifier.width(10.dp))
                        Text(node.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = "打开知识点",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .6f))
    }
}

@Composable
private fun GraphCanvas(
    nodes: List<KnowledgeNode>,
    allNodes: List<KnowledgeNode>,
    relations: List<KnowledgeRelation>,
    relationFilter: RelationType?,
    selectedNodeId: String?,
    onSelectNode: (String) -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var zoomFactor by remember { mutableFloatStateOf(1f) }
    var translation by remember { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(Size.Zero) }
    val hitRadiusPx = with(LocalDensity.current) { 58.dp.toPx() }
    val labelPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
    }
    val metaPaint = remember { Paint().apply { isAntiAlias = true; textAlign = Paint.Align.CENTER } }
    val allNodesById = remember(allNodes) { allNodes.associateBy { it.id } }

    Box(modifier = modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = .92f))) {
        val ink = MaterialTheme.colorScheme.onSurface
        val secondary = MaterialTheme.colorScheme.onSurfaceVariant
        val outline = MaterialTheme.colorScheme.outline
        val surface = MaterialTheme.colorScheme.surface
        val primary = MaterialTheme.colorScheme.primary
        val primaryContainer = MaterialTheme.colorScheme.primaryContainer
        val relationColors = RelationType.entries.associateWith { relationColor(it) }
        val statusColors = KnowledgeStatus.entries.associateWith { statusColor(it) }
        val sourceColors = KnowledgeSource.entries.associateWith { sourceColor(it) }
        val nodeIds = nodes.mapTo(hashSetOf()) { it.id }
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(allNodes) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val newScale = (zoomFactor * zoom).coerceIn(.65f, 2.4f)
                        translation = (translation + pan) + (centroid - translation) * (1f - newScale / zoomFactor)
                        zoomFactor = newScale
                    }
                }
                .pointerInput(nodes, viewport, zoomFactor, translation) {
                    detectTapGestures { tap ->
                        val graphTap = (tap - translation) / zoomFactor
                        val selected = nodes.minByOrNull { node ->
                            val point = Offset(viewport.width * node.x, viewport.height * node.y)
                            (graphTap - point).getDistance()
                        }?.takeIf { node ->
                            val point = Offset(viewport.width * node.x, viewport.height * node.y)
                            (graphTap - point).getDistance() <= hitRadiusPx
                        }
                        if (selected != null) onSelectNode(selected.id) else onClearSelection()
                    }
                },
        ) {
            viewport = size
            labelPaint.textSize = 13.dp.toPx()
            labelPaint.color = ink.toArgbCompat()
            metaPaint.textSize = 10.dp.toPx()
            metaPaint.color = secondary.toArgbCompat()

            val gridStep = 48.dp.toPx()
            var gridX = gridStep
            while (gridX < size.width) {
                var gridY = gridStep
                while (gridY < size.height) {
                    drawCircle(outline.copy(alpha = .28f), radius = 1.dp.toPx(), center = Offset(gridX, gridY))
                    gridY += gridStep
                }
                gridX += gridStep
            }

            withTransform({ translate(translation.x, translation.y); scale(zoomFactor, zoomFactor) }) {
                relations
                    .filter { relationFilter == null || it.type == relationFilter }
                    .filter { it.fromId in nodeIds && it.toId in nodeIds }
                    .forEach { relation ->
                        val from = allNodesById[relation.fromId] ?: return@forEach
                        val to = allNodesById[relation.toId] ?: return@forEach
                        drawLine(
                            color = relationColors.getValue(relation.type).copy(alpha = .78f),
                            start = Offset(size.width * from.x, size.height * from.y),
                            end = Offset(size.width * to.x, size.height * to.y),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = if (relation.type == RelationType.Confusable) PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 7.dp.toPx())) else null,
                        )
                    }

                nodes.forEach { node ->
                    val center = Offset(size.width * node.x, size.height * node.y)
                    val active = node.id == selectedNodeId
                    val nodeWidth = when (node.kind) {
                        KnowledgeKind.Concept -> 92.dp.toPx()
                        KnowledgeKind.Formula -> 100.dp.toPx()
                        KnowledgeKind.Method -> 112.dp.toPx()
                        KnowledgeKind.QuestionType -> 104.dp.toPx()
                    }
                    val nodeHeight = 64.dp.toPx()
                    val topLeft = Offset(center.x - nodeWidth / 2f, center.y - nodeHeight / 2f)
                    val radius = when (node.kind) {
                        KnowledgeKind.Concept -> CornerRadius(28.dp.toPx())
                        KnowledgeKind.Formula -> CornerRadius(8.dp.toPx())
                        KnowledgeKind.Method -> CornerRadius(16.dp.toPx())
                        KnowledgeKind.QuestionType -> CornerRadius(22.dp.toPx())
                    }
                    drawRoundRect(if (active) primaryContainer else surface, topLeft, Size(nodeWidth, nodeHeight), radius)
                    drawRoundRect(
                        if (active) primary else statusColors.getValue(node.status), topLeft, Size(nodeWidth, nodeHeight), radius,
                        style = Stroke(width = if (active) 3.dp.toPx() else 2.dp.toPx()),
                    )
                    drawCircle(
                        sourceColors.getValue(node.source), 5.dp.toPx(),
                        Offset(topLeft.x + nodeWidth - 9.dp.toPx(), topLeft.y + 9.dp.toPx()),
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        node.title.fitToWidth(labelPaint, nodeWidth - 16.dp.toPx()),
                        center.x,
                        center.y + 1.dp.toPx(),
                        labelPaint,
                    )
                    drawContext.canvas.nativeCanvas.drawText(node.kind.label, center.x, center.y + 19.dp.toPx(), metaPaint)
                }
            }
        }
        SourceLegend(Modifier.align(Alignment.TopStart).padding(14.dp))
        Text(
            "${(zoomFactor * 100).toInt()}%",
            modifier = Modifier.align(Alignment.TopEnd).padding(14.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.align(Alignment.BottomStart).padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QkIconButton(Icons.Outlined.CenterFocusStrong, "回到当前节点", onClick = { zoomFactor = 1f; translation = Offset.Zero })
            QkIconButton(Icons.Outlined.ZoomIn, "放大图谱", onClick = { zoomFactor = (zoomFactor * 1.2f).coerceAtMost(2.4f) })
        }
    }
}

private fun String.fitToWidth(paint: Paint, maxWidth: Float): String {
    if (paint.measureText(this) <= maxWidth) return this
    val suffix = "…"
    var low = 0
    var high = length
    while (low < high) {
        val middle = (low + high + 1) / 2
        if (paint.measureText(take(middle) + suffix) <= maxWidth) low = middle else high = middle - 1
    }
    return take(low) + suffix
}

@Composable
private fun SourceLegend(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = .94f), RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        KnowledgeSource.entries.forEach { source ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(7.dp).background(sourceColor(source), CircleShape))
                Text(source.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun KnowledgeCardGrid(
    nodes: List<KnowledgeNode>,
    selectedNodeId: String?,
    onSelectNode: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (nodes.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("没有匹配的知识内容", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(220.dp),
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(nodes, key = { index, node -> "${node.id}-$index" }) { _, node ->
            KnowledgeExplorerCard(node, selected = node.id == selectedNodeId, onClick = { onSelectNode(node.id) })
        }
    }
}

@Composable
private fun KnowledgeExplorerCard(node: KnowledgeNode, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(196.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .border(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(4.dp).height(28.dp).background(sourceColor(node.source), RoundedCornerShape(2.dp)))
            Spacer(Modifier.width(10.dp))
            Text(node.kind.label, style = MaterialTheme.typography.bodySmall, color = sourceColor(node.source))
            Spacer(Modifier.weight(1f))
            Icon(
                if (node.saved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = if (node.saved) "已收藏" else "未收藏",
                modifier = Modifier.size(20.dp),
                tint = if (node.saved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(node.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(6.dp))
        Text(node.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.weight(1f))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(node.source.label, style = MaterialTheme.typography.bodySmall, color = sourceColor(node.source))
            Spacer(Modifier.weight(1f))
            Text(node.status.label, style = MaterialTheme.typography.bodySmall, color = statusColor(node.status))
        }
    }
}

@Composable
private fun NodeDetailPanel(
    node: KnowledgeNode,
    nodes: List<KnowledgeNode>,
    relations: List<KnowledgeRelation>,
    compact: Boolean,
    onAsk: () -> Unit,
    onDismiss: () -> Unit,
    onMarkStatus: (KnowledgeStatus) -> Unit,
    onToggleFavorite: () -> Unit,
    onStartUnderstandingCheck: () -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var activeTab by remember(node.id) { mutableStateOf(DetailTab.Overview) }
    val nodeRelations = remember(node.id, relations) { relations.filter { it.fromId == node.id || it.toId == node.id } }
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .padding(if (compact) 16.dp else 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(node.kind.label, style = MaterialTheme.typography.bodySmall, color = sourceColor(node.source))
                    Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(node.status.label, style = MaterialTheme.typography.bodySmall, color = statusColor(node.status))
                }
                Text(node.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            QkIconButton(
                imageVector = if (node.saved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = if (node.saved) "取消收藏" else "添加到我的库",
                onClick = onToggleFavorite,
                containerColor = if (node.saved) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                contentColor = if (node.saved) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            )
            QkIconButton(
                imageVector = Icons.Outlined.Close,
                contentDescription = "关闭节点信息",
                onClick = onDismiss,
            )
        }
        Spacer(Modifier.height(14.dp))
        DetailTabRow(activeTab) { activeTab = it }
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
                .verticalScroll(rememberScrollState()),
        ) {
            when (activeTab) {
                DetailTab.Overview -> OverviewContent(node)
                DetailTab.Relations -> RelationsContent(node, nodes, nodeRelations)
                DetailTab.Cards -> CardContent(node)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onMarkStatus(KnowledgeStatus.Unstable) }, modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(8.dp)) {
                Text("还不懂")
            }
            OutlinedButton(onClick = { onMarkStatus(KnowledgeStatus.Understood) }, modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(8.dp)) {
                Text("已理解")
            }
            Button(
                onClick = onAsk,
                modifier = Modifier.weight(1.35f).height(46.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Icon(Icons.Outlined.QuestionAnswer, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("开始提问")
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onStartUnderstandingCheck,
            modifier = Modifier.fillMaxWidth().height(42.dp),
            shape = RoundedCornerShape(8.dp),
        ) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (node.status == KnowledgeStatus.Verified) "再次理解检查" else "开始理解检查")
        }
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = note,
            onValueChange = onNoteChange,
            modifier = Modifier.fillMaxWidth().height(40.dp).background(MaterialTheme.colorScheme.background, RoundedCornerShape(6.dp)).padding(8.dp),
            textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onBackground),
            decorationBox = { inner ->
                if (note.isBlank()) Text("添加学习笔记", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                inner()
            },
        )
    }
}

@Composable
private fun UnderstandingCheckDialog(
    check: UnderstandingCheck,
    submitting: Boolean,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedChoiceId by remember(check.id) { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        title = { Text("理解检查") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(check.prompt, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                check.choices.forEach { choice ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .clickable(enabled = !submitting) { selectedChoiceId = choice.id }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedChoiceId == choice.id,
                            onClick = { selectedChoiceId = choice.id },
                            enabled = !submitting,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(choice.text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedChoiceId?.let(onSubmit) },
                enabled = selectedChoiceId != null && !submitting,
            ) { Text(if (submitting) "提交中" else "提交") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !submitting) { Text("取消") }
        },
    )
}

@Composable
private fun DetailTabRow(selected: DetailTab, onSelect: (DetailTab) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().height(38.dp).background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp)).padding(3.dp)) {
        DetailTab.entries.forEach { tab ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (tab == selected) MaterialTheme.colorScheme.surface else Color.Transparent, RoundedCornerShape(6.dp))
                    .clickable(role = Role.Tab) { onSelect(tab) },
                contentAlignment = Alignment.Center,
            ) {
                Text(tab.label, style = MaterialTheme.typography.bodySmall, color = if (tab == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun OverviewContent(node: KnowledgeNode) {
    Column {
        MathRichText(node.description, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(12.dp))
        Text("内容来源", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text(node.evidence, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(7.dp).background(sourceColor(node.source), CircleShape))
            Text(node.source.label, style = MaterialTheme.typography.bodySmall, color = sourceColor(node.source))
        }
    }
}

@Composable
private fun RelationsContent(node: KnowledgeNode, nodes: List<KnowledgeNode>, relations: List<KnowledgeRelation>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (relations.isEmpty()) Text("暂时没有已审核的关联", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        relations.take(4).forEach { relation ->
            val otherId = if (relation.fromId == node.id) relation.toId else relation.fromId
            val other = nodes.firstOrNull { it.id == otherId } ?: return@forEach
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(relationColor(relation.type), CircleShape))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(other.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(relation.type.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CardContent(node: KnowledgeNode) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("快速记忆", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        KnowledgeBullet("核心定义", node.description)
        KnowledgeBullet("学习提示", when (node.kind) {
            KnowledgeKind.Formula -> "先理解公式来源，再代入具体条件。"
            KnowledgeKind.QuestionType -> "先识别条件对应的知识关系，再列式。"
            KnowledgeKind.Method -> "用一个例题验证方法的适用范围。"
            KnowledgeKind.Concept -> "尝试用自己的话解释，并连接一个前置知识。"
        })
    }
}

@Composable
private fun KnowledgeBullet(title: String, body: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 7.dp).size(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            MathRichText(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun relationColor(type: RelationType): Color = when (type) {
    RelationType.Prerequisite -> MaterialTheme.colorScheme.primary
    RelationType.Related -> MaterialTheme.colorScheme.tertiary
    RelationType.Confusable -> MaterialTheme.colorScheme.secondary
    RelationType.QuestionType -> MaterialTheme.colorScheme.error
    RelationType.Extension -> MaterialTheme.colorScheme.onSurfaceVariant
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
private fun sourceColor(source: KnowledgeSource): Color = when (source) {
    KnowledgeSource.Official -> MaterialTheme.colorScheme.primary
    KnowledgeSource.Personal -> MaterialTheme.colorScheme.tertiary
    KnowledgeSource.AiCandidate -> MaterialTheme.colorScheme.secondary
}

private fun Color.toArgbCompat(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt(),
)
