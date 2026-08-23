package com.moyu.reader.ui.importbook

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moyu.reader.data.ImportResult
import com.moyu.reader.ui.designsystem.EditorialSectionTitle
import com.moyu.reader.ui.designsystem.LocalMoyuColors
import com.moyu.reader.ui.designsystem.MoyuGlyph
import com.moyu.reader.ui.designsystem.MoyuGlyphIcon
import com.moyu.reader.ui.designsystem.MoyuPrimaryButton

@Composable
fun ImportScreen(viewModel: ImportViewModel, onViewLibrary: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val files = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri -> runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
        viewModel.importUris(uris)
    }
    val folder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
            viewModel.scanAndImport(it)
        }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp)) {
        EditorialSectionTitle("IMPORT", "把书带进来。")
        Text("系统文件选择器负责授权，墨屿只复制你确认的内容。", style = MaterialTheme.typography.bodyLarge, color = LocalMoyuColors.current.textSecondary, modifier = Modifier.padding(top = 12.dp, bottom = 28.dp))
        ImportOption(MoyuGlyph.IMPORT, "选择文件", "TXT / EPUB · 可多选") { files.launch(arrayOf("text/plain", "application/epub+zip", "application/octet-stream")) }
        ImportOption(MoyuGlyph.DIRECTORY, "扫描文件夹", "递归查找支持的本地书籍") { folder.launch(null) }
        AnimatedVisibility(state.running || state.completed.isNotEmpty()) {
            Column(Modifier.padding(top = 30.dp)) {
                if (state.running) {
                    Text("IMPORTING ${state.completed.size + 1} / ${state.discoveredCount.coerceAtLeast(1)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(state.current?.fileName ?: "正在扫描…", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
                    Text(state.current?.stage?.let { stageLabel(it) } ?: "查找可导入文件", color = LocalMoyuColors.current.textSecondary)
                    Spacer(Modifier.height(14.dp))
                    val fraction = state.current?.fraction
                    if (fraction != null) LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary, trackColor = LocalMoyuColors.current.divider)
                    else LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary, trackColor = LocalMoyuColors.current.divider)
                }
                if (state.completed.isNotEmpty()) {
                    Spacer(Modifier.height(22.dp))
                    state.completed.forEach { result ->
                        val success = result is ImportResult.Success
                        val title = when (result) {
                            is ImportResult.Success -> result.title
                            is ImportResult.Duplicate -> result.title
                            is ImportResult.Failure -> result.fileName
                        }
                        val detail = when (result) {
                            is ImportResult.Success -> "导入完成"
                            is ImportResult.Duplicate -> "文件内容重复，已跳过"
                            is ImportResult.Failure -> result.message
                        }
                        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(24.dp).background(if (success) Color(0xFF2E7D5B).copy(alpha = .13f) else MaterialTheme.colorScheme.error.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                                Text(if (success) "✓" else "!", color = if (success) Color(0xFF2E7D5B) else MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                            Column(Modifier.padding(start = 12.dp)) {
                                Text(title, fontWeight = FontWeight.SemiBold)
                                Text(detail, style = MaterialTheme.typography.bodySmall, color = LocalMoyuColors.current.textSecondary)
                            }
                        }
                    }
                    if (state.completed.any { it is ImportResult.Failure }) {
                        Text("TXT 编码不正确？使用指定编码重新解析：", style = MaterialTheme.typography.bodySmall, color = LocalMoyuColors.current.textSecondary, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("GB18030", "UTF-8", "GBK", "Big5").forEach { charset ->
                                Surface(onClick = { viewModel.retryWithCharset(charset) }, color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
                                    Text(charset, Modifier.padding(horizontal = 9.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        if (!state.running && state.completed.any { it is ImportResult.Success }) MoyuPrimaryButton("查看书架", onViewLibrary, Modifier.fillMaxWidth())
        Text("文件会保存在应用私有目录。卸载应用会同时删除这些副本，请使用备份功能保留数据。", style = MaterialTheme.typography.bodySmall, color = LocalMoyuColors.current.textTertiary, modifier = Modifier.padding(vertical = 18.dp))
    }
}

@Composable
private fun ImportOption(glyph: MoyuGlyph, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { MoyuGlyphIcon(glyph) }
            Column(Modifier.weight(1f).padding(horizontal = 15.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LocalMoyuColors.current.textSecondary)
            }
            Text("›", color = MaterialTheme.colorScheme.primary)
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(LocalMoyuColors.current.divider))
}

private fun stageLabel(stage: com.moyu.reader.data.ImportProgress.Stage) = when (stage) {
    com.moyu.reader.data.ImportProgress.Stage.COPYING -> "正在复制到本地书库"
    com.moyu.reader.data.ImportProgress.Stage.HASHING -> "正在校验文件"
    com.moyu.reader.data.ImportProgress.Stage.PARSING -> "正在识别编码与章节"
    com.moyu.reader.data.ImportProgress.Stage.SAVING -> "正在保存书籍信息"
    com.moyu.reader.data.ImportProgress.Stage.INDEXING -> "正在建立全文索引"
}
