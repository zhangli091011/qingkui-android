package cn.qingkui.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.qingkui.app.ui.model.KnowledgeStatus
import cn.qingkui.app.ui.model.LearningItem

@Composable
fun LearningScreen(
    compact: Boolean,
    items: List<LearningItem>,
    onOpenItem: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = if (compact) 20.dp else 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.fillMaxWidth().widthIn(max = 960.dp)) {
            Spacer(Modifier.height(if (compact) 20.dp else 32.dp))
            Text("今天继续", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "回到具体知识点，保持学习连续",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            LearningFilters()
            Spacer(Modifier.height(20.dp))
            Divider(color = MaterialTheme.colorScheme.outline)
            if (items.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    Text("还没有学习记录", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn {
                    items(items, key = { it.nodeId }) { item ->
                        KnowledgeStateRow(item = item, onClick = { onOpenItem(item.nodeId) })
                        Divider(color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}

@Composable
private fun LearningFilters() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip("最近探索", Icons.Outlined.History, true)
        FilterChip("复习中", Icons.Outlined.Replay, false)
        FilterChip("易错", Icons.Outlined.ErrorOutline, false)
        FilterChip("已验证", Icons.Outlined.CheckCircle, false)
    }
}

@Composable
private fun FilterChip(label: String, icon: ImageVector, selected: Boolean) {
    Row(
        modifier = Modifier
            .height(40.dp)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun KnowledgeStateRow(item: LearningItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(item.lastStudied, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            item.status.label,
            modifier = Modifier
                .background(statusColor(item.status).copy(alpha = .12f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = statusColor(item.status),
        )
        Spacer(Modifier.width(18.dp))
        Text(item.action, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(6.dp))
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
    }
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
