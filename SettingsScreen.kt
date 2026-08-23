package com.moyu.reader.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moyu.reader.data.FontRepository
import com.moyu.reader.data.BackupOptions
import com.moyu.reader.data.BackupRepository
import com.moyu.reader.data.LibraryRepository
import com.moyu.reader.data.LocalFont
import com.moyu.reader.data.preferences.SettingsRepository
import com.moyu.reader.model.PageAnimation
import com.moyu.reader.model.ReaderMode
import com.moyu.reader.model.ReaderPreferences
import com.moyu.reader.model.ReaderOrientation
import com.moyu.reader.model.ReaderTheme
import com.moyu.reader.ui.designsystem.EditorialSectionTitle
import com.moyu.reader.ui.designsystem.LocalMoyuColors
import com.moyu.reader.ui.designsystem.MoyuGlyph
import com.moyu.reader.ui.designsystem.MoyuGlyphIcon
import com.moyu.reader.ui.designsystem.MoyuPrimaryButton
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

private enum class SettingsPage { GENERAL, READING, FONTS, THEME, STATISTICS, BACKUP, ABOUT }

@Composable
fun SettingsScreen(
    settings: SettingsRepository,
    library: LibraryRepository,
    fonts: FontRepository,
    backup: BackupRepository,
) {
    val preferences by settings.preferences.collectAsStateWithLifecycle(initialValue = com.moyu.reader.data.preferences.AppPreferences())
    var page by remember { mutableStateOf(SettingsPage.GENERAL) }
    Column(Modifier.fillMaxSize().statusBarsPadding().background(MaterialTheme.colorScheme.background)) {
        if (page != SettingsPage.GENERAL) SettingsBackBar(titleFor(page)) { page = SettingsPage.GENERAL }
        AnimatedContent(
            targetState = page,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                val forward = initialState == SettingsPage.GENERAL
                (fadeIn(tween(190)) + slideInHorizontally(tween(240)) { if (forward) it / 8 else -it / 8 })
                    .togetherWith(fadeOut(tween(130)) + slideOutHorizontally(tween(210)) { if (forward) -it / 12 else it / 12 })
            },
            label = "settings-page",
        ) { destination ->
            when (destination) {
                SettingsPage.GENERAL -> GeneralSettings { page = it }
                SettingsPage.READING -> ReadingSettings(preferences.reader, settings)
                SettingsPage.FONTS -> FontManager(preferences.reader, settings, fonts)
                SettingsPage.THEME -> ThemeSettings(preferences.reader, settings)
                SettingsPage.STATISTICS -> Statistics(library)
                SettingsPage.BACKUP -> BackupScreen(backup)
                SettingsPage.ABOUT -> AboutPrivacy()
            }
        }
    }
}

@Composable
private fun GeneralSettings(onOpen: (SettingsPage) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
        EditorialSectionTitle("SETTINGS", "阅读，按你的习惯。")
        Spacer(Modifier.height(24.dp))
        SettingsRow("阅读设置", "字号、行距、翻页") { onOpen(SettingsPage.READING) }
        SettingsRow("字体管理", "导入 TTF / OTF") { onOpen(SettingsPage.FONTS) }
        SettingsRow("主题与外观", "浅色、深色、OLED、纸张") { onOpen(SettingsPage.THEME) }
        SettingsRow("阅读统计", "只在本机汇总") { onOpen(SettingsPage.STATISTICS) }
        SettingsRow("备份与恢复", "导出 MoyuBackup.zip") { onOpen(SettingsPage.BACKUP) }
        SettingsRow("关于与隐私", "完全离线") { onOpen(SettingsPage.ABOUT) }
    }
}

@Composable
private fun SettingsBackBar(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(onClick = onBack, color = Color.Transparent, shape = RoundedCornerShape(12.dp), modifier = Modifier.size(44.dp)) {
            Box(contentAlignment = Alignment.Center) { MoyuGlyphIcon(MoyuGlyph.BACK) }
        }
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun ReadingSettings(preferences: ReaderPreferences, settings: SettingsRepository) {
    val scope = rememberCoroutineScope()
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 28.dp)) {
        item {
            LiveReaderPreview(preferences.theme)
            ValueSetting("字号", "${preferences.fontSizeSp.toInt()} sp")
            Slider(preferences.fontSizeSp, { scope.launch { settings.setFontSize(it) } }, valueRange = 14f..34f)
            ValueSetting("行距", "%.2f".format(preferences.lineHeightMultiplier))
            Slider(preferences.lineHeightMultiplier, { scope.launch { settings.setLineHeight(it) } }, valueRange = 1.3f..2.4f)
            ValueSetting("页边距", "${preferences.horizontalMarginDp.toInt()} dp")
            Slider(preferences.horizontalMarginDp, { scope.launch { settings.setHorizontalMargin(it) } }, valueRange = 16f..56f)
            ValueSetting("段间距", "${preferences.paragraphSpacingDp.toInt()} dp")
            Slider(preferences.paragraphSpacingDp, { scope.launch { settings.setParagraphSpacing(it) } }, valueRange = 0f..28f)
            ValueSetting("首行缩进", "${preferences.firstLineIndentEm.toInt()} 字")
            FreeIndentSetting(preferences.firstLineIndentEm) { scope.launch { settings.setFirstLineIndent(it) } }
            SectionLabel("阅读模式")
            ChoiceRow(ReaderMode.entries, preferences.mode, { if (it == ReaderMode.PAGED) "分页" else "滚动" }) { scope.launch { settings.setReaderMode(it) } }
            SectionLabel("翻页动效")
            ChoiceRow(PageAnimation.entries, preferences.pageAnimation, { when (it) { PageAnimation.INSTANT -> "无动画"; PageAnimation.SLIDE -> "滑动"; PageAnimation.FADE -> "淡入"; PageAnimation.COVER -> "覆盖"; PageAnimation.PAPER -> "纸页" } }) { scope.launch { settings.setPageAnimation(it) } }
            ValueSetting("翻页速度", "${preferences.pageTurnDurationMs} ms")
            ChoiceRow(listOf(160, 280, 420, 650), preferences.pageTurnDurationMs, { "$it ms" }) { scope.launch { settings.setPageTurnDuration(it) } }
            SectionLabel("屏幕方向")
            ChoiceRow(ReaderOrientation.entries, preferences.orientation, { when (it) { ReaderOrientation.SYSTEM -> "跟随系统"; ReaderOrientation.PORTRAIT -> "竖屏"; ReaderOrientation.LANDSCAPE -> "横屏" } }) { scope.launch { settings.setOrientation(it) } }
            Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("屏幕亮度", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Surface(onClick = { scope.launch { settings.setBrightness(-1f) } }, color = if (preferences.brightness < 0f) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp)) {
                    Text(if (preferences.brightness < 0f) "已跟随系统" else "跟随系统", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium)
                }
            }
            Slider(if (preferences.brightness < 0f) .5f else preferences.brightness, { scope.launch { settings.setBrightness(it) } }, valueRange = 0f..1f)
            ValueSetting("本地朗读速度", "%.1f ×".format(preferences.ttsRate))
            Slider(preferences.ttsRate, { scope.launch { settings.setTtsRate(it) } }, valueRange = .6f..1.8f, steps = 5)
            ToggleRow("两端对齐", "适合较宽的阅读区域", preferences.justified) { scope.launch { settings.setJustified(it) } }
            ToggleRow("保持屏幕常亮", "只在阅读页生效", preferences.keepScreenOn) { scope.launch { settings.setKeepScreenOn(it) } }
            ToggleRow("显示阅读时钟", "在页脚显示当前时间", preferences.showReaderClock) { scope.launch { settings.setShowReaderClock(it) } }
            ToggleRow("音量键翻页", "音量加上一页，音量减下一页", preferences.volumeKeyPageTurn) { scope.launch { settings.setVolumeKeyPageTurn(it) } }
        }
    }
}

@Composable
private fun ThemeSettings(preferences: ReaderPreferences, settings: SettingsRepository) {
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        LiveReaderPreview(preferences.theme)
        Spacer(Modifier.height(18.dp))
        ReaderTheme.entries.chunked(2).forEach { themes ->
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                themes.forEach { theme -> FullThemeCard(theme, theme == preferences.theme, Modifier.weight(1f)) { scope.launch { settings.setTheme(theme) } } }
            }
        }
        ToggleRow("减少动态效果", "保留状态反馈，移除大幅位移", preferences.reducedMotion) { scope.launch { settings.setReducedMotion(it) } }
    }
}

@Composable
private fun LiveReaderPreview(theme: ReaderTheme) {
    val palette = themePalette(theme)
    Column(Modifier.fillMaxWidth().height(156.dp).background(palette.first).padding(18.dp)) {
        Text("第十二章　雨夜来信", color = palette.second, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Text("雨落在旧城的屋檐上，像有人翻动一本很厚的书。远处的灯火隔着薄雾，一盏一盏亮起来。", color = palette.second.copy(alpha = .88f), fontFamily = FontFamily.Serif, fontSize = 14.sp, lineHeight = 25.sp, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun FullThemeCard(theme: ReaderTheme, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val palette = themePalette(theme)
    Surface(
        onClick = onClick,
        modifier = modifier.height(116.dp),
        color = palette.first,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(if (selected) 2.dp else 1.dp, if (selected) palette.third else LocalMoyuColors.current.divider),
    ) {
        Column(Modifier.padding(13.dp)) {
            Text(themeLabel(theme), color = palette.second, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Box(Modifier.fillMaxWidth(.8f).height(2.dp).background(palette.second.copy(alpha = .45f)))
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth(.55f).height(2.dp).background(palette.third))
        }
    }
}

@Composable
private fun FontManager(preferences: ReaderPreferences, settings: SettingsRepository, fonts: FontRepository) {
    val scope = rememberCoroutineScope()
    val localFonts by fonts.fonts.collectAsStateWithLifecycle()
    var message by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val result = fonts.import(uri)
            message = result.fold({ "已导入 ${it.name}" }, { it.message ?: "字体导入没有完成" })
        }
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 28.dp)) {
        item {
            Text("山川异域，风月同天。", fontFamily = FontFamily.Serif, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 20.dp))
            MoyuPrimaryButton("导入字体", { launcher.launch(arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "application/x-font-opentype")) }, Modifier.fillMaxWidth())
            message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 10.dp)) }
            Spacer(Modifier.height(16.dp))
            FontRow("系统宋体", "随系统提供 · 稳定回退", preferences.customFontPath == null, onUse = { scope.launch { settings.setCustomFont(null) } })
        }
        items(localFonts, key = { it.path }) { font ->
            FontRow(font.name, formatBytes(font.size), preferences.customFontPath == font.path, onUse = { scope.launch { settings.setCustomFont(font.path) } }, onDelete = { scope.launch { fonts.delete(font, preferences.customFontPath) } })
        }
    }
}

@Composable
private fun FontRow(name: String, detail: String, selected: Boolean, onUse: () -> Unit, onDelete: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().height(70.dp).clickable(onClick = onUse), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(46.dp).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { Text("字", fontFamily = FontFamily.Serif, fontSize = 22.sp) }
        Column(Modifier.weight(1f).padding(horizontal = 13.dp)) {
            Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = LocalMoyuColors.current.textTertiary)
        }
        if (selected) Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        else if (onDelete != null) Text("删除", Modifier.clickable(onClick = onDelete).padding(8.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun Statistics(library: LibraryRepository) {
    val summary by library.observeWeeklySummary().collectAsStateWithLifecycle(initialValue = com.moyu.reader.model.ReadingSummary())
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("THIS WEEK", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(formatDuration(summary.totalMillis), style = MaterialTheme.typography.displayLarge, modifier = Modifier.padding(top = 12.dp))
        Text("所有统计只保存在设备本地。", color = LocalMoyuColors.current.textSecondary)
        Spacer(Modifier.height(38.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatCell(summary.sessions.toString(), "阅读次数")
            StatCell(formatDuration(if (summary.sessions == 0) 0 else summary.totalMillis / summary.sessions), "平均时长")
            StatCell(formatCharacters(summary.charactersRead), "阅读字符")
        }
    }
}

@Composable
private fun BackupScreen(backup: BackupRepository) {
    val scope = rememberCoroutineScope()
    var includeSources by remember { mutableStateOf(false) }
    var includeFonts by remember { mutableStateOf(true) }
    var running by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) scope.launch {
            running = true
            message = runCatching { backup.create(uri, BackupOptions(includeSources, includeFonts)) }
                .fold({ "备份完成：${it.books} 本书，${it.bookmarks} 条书签" }, { it.message ?: "备份没有完成" })
            running = false
        }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            running = true
            message = runCatching { backup.restore(uri) }
                .fold({ "恢复完成：新增 ${it.restoredBooks} 本，匹配 ${it.matchedBooks} 本，跳过 ${it.skippedBooks} 本" }, { it.message ?: "恢复没有完成" })
            running = false
        }
    }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("把阅读状态留一份副本。", style = MaterialTheme.typography.headlineMedium)
        Text("备份通过系统文件选择器写入你指定的位置，全程本地。", color = LocalMoyuColors.current.textSecondary, modifier = Modifier.padding(top = 12.dp, bottom = 26.dp))
        ToggleRow("包含小说源文件", "备份会更大", includeSources) { includeSources = it }
        ToggleRow("包含字体", "保留自定义排版", includeFonts) { includeFonts = it }
        Spacer(Modifier.height(22.dp))
        MoyuPrimaryButton(if (running) "正在处理" else "创建备份", { create.launch("MoyuBackup-${java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.ROOT).format(java.util.Date())}.zip") }, Modifier.fillMaxWidth(), enabled = !running)
        Surface(onClick = { restore.launch(arrayOf("application/zip", "application/octet-stream")) }, enabled = !running, color = Color.Transparent, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) { Text("从备份恢复", Modifier.padding(14.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) }
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp)) }
    }
}

@Composable
private fun AboutPrivacy() {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("MOYU READER · 1.2.0", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text("墨屿阅读", style = MaterialTheme.typography.displayLarge, modifier = Modifier.padding(vertical = 14.dp))
        Text("一个安静、离线、以中文排版为先的本地阅读器。", color = LocalMoyuColors.current.textSecondary)
        Column(Modifier.fillMaxWidth().padding(top = 30.dp).background(MaterialTheme.colorScheme.surfaceVariant).padding(18.dp)) {
            PrivacyRow("账户", "不需要")
            PrivacyRow("小说上传", "从不")
            PrivacyRow("统计 SDK", "没有")
            PrivacyRow("广告", "没有")
            PrivacyRow("网络权限", "未申请")
            PrivacyRow("数据位置", "设备本地")
        }
    }
}

@Composable
private fun SettingsRow(title: String, detail: String, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(detail, style = MaterialTheme.typography.bodySmall, color = LocalMoyuColors.current.textTertiary) }
            Text("›", color = MaterialTheme.colorScheme.primary, fontSize = 20.sp)
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(LocalMoyuColors.current.divider))
}

@Composable
private fun ValueSetting(title: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) { Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)); Text(value, color = MaterialTheme.colorScheme.primary) }
}

@Composable
private fun FreeIndentSetting(value: Float, onChange: (Float) -> Unit) {
    var input by remember(value) { mutableStateOf(if (value % 1f == 0f) value.toInt().toString() else "%.1f".format(value)) }
    OutlinedTextField(
        value = input,
        onValueChange = { raw ->
            if (raw.all { it.isDigit() || it == '.' }) {
                input = raw
                raw.toFloatOrNull()?.takeIf { it in 0f..4f }?.let(onChange)
            }
        },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        singleLine = true,
        label = { Text("输入 0–4 字，可填小数") },
        trailingIcon = { Text("字", color = LocalMoyuColors.current.textSecondary, modifier = Modifier.padding(end = 8.dp)) },
    )
}

@Composable
private fun SectionLabel(label: String) { Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 20.dp, bottom = 10.dp)) }

@Composable
private fun ToggleRow(title: String, detail: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().height(66.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(detail, style = MaterialTheme.typography.bodySmall, color = LocalMoyuColors.current.textTertiary) }
        Switch(checked, onChange)
    }
}

@Composable
private fun <T> ChoiceRow(values: List<T>, selected: T, label: (T) -> String, onChange: (T) -> Unit) {
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(4.dp)) {
        values.forEach { value ->
            Surface(onClick = { onChange(value) }, modifier = Modifier.weight(1f), color = if (value == selected) MaterialTheme.colorScheme.surface else Color.Transparent, shape = RoundedCornerShape(8.dp)) {
                Text(label(value), Modifier.padding(vertical = 10.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun StatCell(value: String, label: String) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(value, style = MaterialTheme.typography.titleLarge); Text(label, style = MaterialTheme.typography.bodySmall, color = LocalMoyuColors.current.textTertiary) } }
@Composable
private fun PrivacyRow(label: String, value: String) { Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) { Text(label, Modifier.weight(1f), color = LocalMoyuColors.current.textSecondary); Text(value, fontWeight = FontWeight.SemiBold) } }

private fun titleFor(page: SettingsPage) = when (page) {
    SettingsPage.READING -> "阅读设置"; SettingsPage.FONTS -> "字体管理"; SettingsPage.THEME -> "主题与外观"; SettingsPage.STATISTICS -> "阅读统计"; SettingsPage.BACKUP -> "备份与恢复"; SettingsPage.ABOUT -> "关于与隐私"; else -> "设置"
}
private fun themePalette(theme: ReaderTheme) = when (theme) {
    ReaderTheme.LIGHT -> Triple(Color(0xFFFCFBF7), Color(0xFF151412), Color(0xFFD83A2E))
    ReaderTheme.DARK -> Triple(Color(0xFF11110F), Color(0xFFF8F5ED), Color(0xFFF2C9C3))
    ReaderTheme.OLED -> Triple(Color.Black, Color(0xFFF8F5ED), Color(0xFFA9D5CE))
    ReaderTheme.PAPER -> Triple(Color(0xFFF8F5ED), Color(0xFF34312D), Color(0xFFB92D24))
}
private fun themeLabel(theme: ReaderTheme) = when (theme) { ReaderTheme.LIGHT -> "浅色"; ReaderTheme.DARK -> "深色"; ReaderTheme.OLED -> "OLED"; ReaderTheme.PAPER -> "纸张" }
private fun formatBytes(bytes: Long) = if (bytes >= 1024 * 1024) "%.1f MB".format(bytes / 1024f / 1024f) else "%.0f KB".format(bytes / 1024f)
private fun formatDuration(millis: Long): String { val minutes = TimeUnit.MILLISECONDS.toMinutes(millis); return if (minutes >= 60) "${minutes / 60} 小时 ${minutes % 60} 分" else "$minutes 分钟" }
private fun formatCharacters(value: Long) = if (value >= 10_000) "%.1f万".format(value / 10_000f) else value.toString()
