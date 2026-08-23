package com.moyu.reader.ui.onboarding

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moyu.reader.ui.designsystem.LocalMoyuColors
import com.moyu.reader.ui.designsystem.MoyuPrimaryButton
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    completeOnboarding: suspend () -> Unit,
) {
    val pager = rememberPagerState { 3 }
    val scope = rememberCoroutineScope()
    val pages = listOf(
        Triple("MOYU READER", "把一座书架，\n放回口袋。", "专为中文小说排版打磨的本地阅读器。安静、克制，也足够可靠。"),
        Triple("PRIVATE BY DESIGN", "阅读只发生\n在这台设备。", "不需要账户，不上传小说和阅读记录，不含广告、统计 SDK，也没有网络权限。"),
        Triple("START WITH A BOOK", "从你的第一本书\n开始。", "支持 TXT 与 DRM-Free EPUB。文件会复制到应用私有空间，原文件移动后仍可继续阅读。"),
    )
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            val item = pages[page]
            Column(
                Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 44.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(item.first, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))
                    Text(item.second, style = MaterialTheme.typography.displayLarge)
                    Spacer(Modifier.height(18.dp))
                    Text(item.third, style = MaterialTheme.typography.bodyLarge, color = LocalMoyuColors.current.textSecondary)
                }
                if (page == 1) PrivacyMarks() else EditorialIllustration(page)
                MoyuPrimaryButton(
                    text = if (page == 2) "进入书架" else "继续",
                    onClick = {
                        scope.launch {
                            if (page < 2) pager.animateScrollToPage(page + 1)
                            else {
                                completeOnboarding()
                                withContext(Dispatchers.Main.immediate) { onComplete() }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Row(Modifier.align(Alignment.TopEnd).padding(top = 54.dp, end = 28.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { index ->
                Box(Modifier.size(width = if (pager.currentPage == index) 22.dp else 6.dp, height = 3.dp).background(if (pager.currentPage == index) MaterialTheme.colorScheme.primary else LocalMoyuColors.current.divider))
            }
        }
    }
}

@Composable
private fun EditorialIllustration(page: Int) {
    val accent = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().height(250.dp)) {
        val ink = if (page == 0) androidx.compose.ui.graphics.Color(0xFF151412) else androidx.compose.ui.graphics.Color(0xFF4E4B46)
        drawRect(ink.copy(alpha = .08f), topLeft = androidx.compose.ui.geometry.Offset(size.width * .18f, size.height * .08f), size = androidx.compose.ui.geometry.Size(size.width * .64f, size.height * .82f))
        drawLine(ink, androidx.compose.ui.geometry.Offset(size.width * .27f, size.height * .2f), androidx.compose.ui.geometry.Offset(size.width * .27f, size.height * .74f), 3.dp.toPx(), StrokeCap.Square)
        drawLine(accent, androidx.compose.ui.geometry.Offset(size.width * .47f, size.height * .14f), androidx.compose.ui.geometry.Offset(size.width * .47f, size.height * .74f), 3.dp.toPx(), StrokeCap.Square)
        drawLine(ink, androidx.compose.ui.geometry.Offset(size.width * .67f, size.height * .28f), androidx.compose.ui.geometry.Offset(size.width * .67f, size.height * .74f), 3.dp.toPx(), StrokeCap.Square)
    }
}

@Composable
private fun PrivacyMarks() {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        listOf("不需要账户", "不连接服务器", "所有数据保存在设备本地").forEach { label ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(23.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                    Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Text(label, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
