package cn.qingkui.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.qingkui.app.ui.model.CorpusItem

@Composable
fun CorpusScreen(items: List<CorpusItem>, loading: Boolean, onGenerate: (String, String, String?, String, Int) -> Unit) {
    var subject by remember { mutableStateOf("语文") }
    var category by remember { mutableStateOf("综合") }
    var topic by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("语料收集", style = MaterialTheme.typography.headlineSmall)
        Text("根据近期新闻主题生成语文/英语学习语料，密钥由服务器统一管理。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(subject == "语文", { subject = "语文" }, label = { Text("语文") }); FilterChip(subject == "英语", { subject = "英语" }, label = { Text("英语") }) }
        OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("分类") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = topic, onValueChange = { topic = it }, label = { Text("新闻主题（可选）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(onClick = { onGenerate(subject, category, topic.ifBlank { null }, "高中", 3) }, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text(if (loading) "生成中…" else "生成近期语料") }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(items) { item -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(item.title, style = MaterialTheme.typography.titleMedium); Text(item.content); Text(item.keywords.joinToString(" · "), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall); Text("${item.subject} · ${item.grade} · ${item.sourceDate}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } } }
    }
}
