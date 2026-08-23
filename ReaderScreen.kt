@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.moyu.reader.ui.reader

import android.graphics.Typeface
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.WindowManager
import android.view.KeyEvent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moyu.reader.data.preferences.SettingsRepository
import com.moyu.reader.MainActivity
import com.moyu.reader.model.PageAnimation
import com.moyu.reader.model.ReaderMode
import com.moyu.reader.model.ReaderPreferences
import com.moyu.reader.model.ReaderOrientation
import com.moyu.reader.model.ReaderTheme
import com.moyu.reader.reader.AndroidPaginator
import com.moyu.reader.reader.LocalTtsController
import com.moyu.reader.reader.LocalTtsState
import com.moyu.reader.reader.PageSlice
import com.moyu.reader.reader.PaginationSpec
import com.moyu.reader.reader.ReaderGestureAction
import com.moyu.reader.reader.ReaderInteraction
import com.moyu.reader.reader.ReaderTextFormatter
import com.moyu.reader.ui.designsystem.LocalMoyuColors
import com.moyu.reader.ui.designsystem.MoyuGlyph
import com.moyu.reader.ui.designsystem.MoyuGlyphIcon
import com.moyu.reader.ui.designsystem.MoyuMotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.absoluteValue
import kotlin.math.abs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class PaginationResult(val pages: List<PageSlice>, val ready: Boolean)

@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    preferences: ReaderPreferences,
    settings: SettingsRepository,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalMoyuColors.current
    val context = LocalContext.current
    val activity = context.findActivity()
    val ttsController = remember(context.applicationContext) { LocalTtsController(context.applicationContext) }
    val ttsState by ttsController.state.collectAsStateWithLifecycle()
    DisposableEffect(ttsController) { onDispose { ttsController.release() } }
    DisposableEffect(activity, viewModel, preferences.volumeKeyPageTurn, state.panel) {
        val host = activity as? MainActivity
        if (preferences.volumeKeyPageTurn && state.panel == ReaderPanel.NONE) {
            host?.setVolumePageHandler { keyCode ->
                viewModel.requestPageTurn(
                    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) PageTurnCommand.PREVIOUS else PageTurnCommand.NEXT,
                )
                true
            }
        } else {
            host?.setVolumePageHandler(null)
        }
        onDispose { host?.setVolumePageHandler(null) }
    }
    DisposableEffect(activity, preferences.brightness, preferences.keepScreenOn, preferences.orientation) {
        if (activity != null) {
            activity.requestedOrientation = when (preferences.orientation) {
                ReaderOrientation.SYSTEM -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                ReaderOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                ReaderOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            activity.window.attributes = activity.window.attributes.apply { screenBrightness = preferences.brightness }
            if (preferences.keepScreenOn) activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            if (activity != null) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                activity.window.attributes = activity.window.attributes.apply { screenBrightness = -1f }
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
    Box(Modifier.fillMaxSize().background(colors.readerBackground)) {
        when {
            state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary)
            state.errorMessage != null -> ReaderError(state.errorMessage.orEmpty(), viewModel::retryChapter)
            state.chapters.isEmpty() -> Text("这本书没有可阅读的正文", Modifier.align(Alignment.Center), color = colors.textSecondary)
            else -> key(state.currentChapter?.id, preferences.mode) {
                if (preferences.mode == ReaderMode.PAGED) PagedReader(state, preferences, viewModel)
                else ScrollingReader(state, preferences, viewModel)
            }
        }
        ReaderControls(state, preferences.reducedMotion, ttsState.speaking, viewModel, onBack)
        if (state.panel == ReaderPanel.DIRECTORY) DirectoryDrawer(state, viewModel)
        state.transientMessage?.let { ReaderMessage(it, Modifier.align(Alignment.BottomCenter)) }
    }
    if (state.panel.isSheetPanel()) {
        ReaderSheet(viewModel::dismissPanel) {
            AnimatedContent(
                targetState = state.panel,
                transitionSpec = {
                    (fadeIn(androidx.compose.animation.core.tween(if (preferences.reducedMotion) 0 else 180)) +
                        slideInHorizontally(androidx.compose.animation.core.tween(if (preferences.reducedMotion) 0 else 220)) { it / 8 })
                        .togetherWith(fadeOut(androidx.compose.animation.core.tween(if (preferences.reducedMotion) 0 else 120)))
                },
                label = "reader-sheet-content",
            ) { panel ->
                Column(Modifier.fillMaxWidth()) {
                    when (panel) {
                        ReaderPanel.PROGRESS -> ProgressPanel(state, viewModel)
                        ReaderPanel.SEARCH -> SearchPanel(state, viewModel)
                        ReaderPanel.BOOKMARKS -> BookmarkPanel(state, viewModel)
                        ReaderPanel.READING_SETTINGS -> ReadingSettingsPanel(preferences, settings) { viewModel.showPanel(ReaderPanel.FONT) }
                        ReaderPanel.THEME -> ThemePanel(preferences, settings)
                        ReaderPanel.FONT -> FontPanel(preferences, settings)
                        ReaderPanel.TTS -> TtsPanel(state, preferences, settings, ttsState, ttsController)
                        else -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderError(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 32.dp).padding(top = 180.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("这一页暂时没有展开", style = MaterialTheme.typography.headlineMedium, color = LocalMoyuColors.current.readerText)
        Text(message, color = LocalMoyuColors.current.textSecondary, modifier = Modifier.padding(top = 10.dp, bottom = 20.dp), textAlign = TextAlign.Center)
        Surface(onClick = onRetry, color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary, shape = RoundedCornerShape(12.dp)) {
            Text("重新载入", Modifier.padding(horizontal = 24.dp, vertical = 12.dp), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ReaderMessage(message: String, modifier: Modifier = Modifier) {
    Surface(modifier.padding(bottom = 104.dp), color = MaterialTheme.colorScheme.inverseSurface, contentColor = MaterialTheme.colorScheme.inverseOnSurface, shape = RoundedCornerShape(999.dp), shadowElevation = 6.dp) {
        Text(message, Modifier.padding(horizontal = 18.dp, vertical = 10.dp), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun PagedReader(state: ReaderUiState, preferences: ReaderPreferences, viewModel: ReaderViewModel) {
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val typeface = remember(preferences.customFontPath) {
        preferences.customFontPath?.let { runCatching { Typeface.createFromFile(File(it)) }.getOrNull() } ?: Typeface.SERIF
    }
    val displayText = remember(state.text) { ReaderTextFormatter.forDisplay(state.text) }
    val pagination by produceState(
        initialValue = PaginationResult(emptyList(), ready = false),
        displayText,
        viewport,
        preferences.fontSizeSp,
        preferences.lineHeightMultiplier,
        preferences.paragraphSpacingDp,
        preferences.horizontalMarginDp,
        preferences.firstLineIndentEm,
        typeface,
    ) {
        // Never present a previous chapter/style's slices under the current
        // typography. That transient mismatch was the source of characters
        // flashing at the footer during a reflow.
        value = PaginationResult(emptyList(), ready = false)
        if (viewport.width <= 0 || viewport.height <= 0) return@produceState
        val horizontalPx = with(density) { preferences.horizontalMarginDp.dp.roundToPx() * 2 }
        val fontPx = with(density) { preferences.fontSizeSp.sp.toPx() }
        value = PaginationResult(
            pages = withContext(Dispatchers.Default) {
                AndroidPaginator().paginate(
                displayText,
                PaginationSpec(
                    viewportWidthPx = (viewport.width - horizontalPx).coerceAtLeast(1),
                    viewportHeightPx = viewport.height,
                    fontSizePx = fontPx,
                    lineHeightMultiplier = preferences.lineHeightMultiplier,
                    paragraphSpacingPx = with(density) { preferences.paragraphSpacingDp.dp.toPx() },
                    typeface = typeface,
                    fontWeight = preferences.fontWeight,
                    firstLineIndentPx = fontPx * preferences.firstLineIndentEm,
                    pageReservedPx = with(density) { 68.dp.roundToPx() },
                    firstPageReservedPx = with(density) { 90.dp.roundToPx() },
                ),
                )
            },
            ready = true,
        )
    }
    val pages = pagination.pages
    if (!pagination.ready || pages.isEmpty()) {
        Box(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .padding(top = 28.dp, bottom = 20.dp)
                .onSizeChanged { viewport = it }
                .background(LocalMoyuColors.current.readerBackground),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(Modifier.size(26.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
        }
        return
    }
    val pager = rememberPagerState { pages.size }
    var anchoredChapter by remember(state.currentChapter?.id) { mutableStateOf(false) }
    LaunchedEffect(pagination.ready, pages, state.currentChapter?.id) {
        if (pagination.ready && !anchoredChapter && pages.isNotEmpty()) {
            val page = ReaderInteraction.pageForOffset(pages, state.characterOffset)
            pager.scrollToPage(page)
            anchoredChapter = true
        }
        if (pagination.ready && anchoredChapter) {
            snapshotFlow { pager.settledPage }.distinctUntilChanged().collect { page ->
                pages.getOrNull(page)?.let { viewModel.updateAnchor(it.start) }
            }
        }
    }
    LaunchedEffect(pagination.ready, pages, pager, preferences.pageAnimation, preferences.pageTurnDurationMs, preferences.reducedMotion) {
        viewModel.pageTurns.collect { command ->
            if (!pagination.ready || pages.isEmpty()) return@collect
            when (command) {
                PageTurnCommand.PREVIOUS -> if (pager.currentPage == 0) viewModel.previousChapterAtEnd() else {
                    if (preferences.pageAnimation == PageAnimation.INSTANT || preferences.reducedMotion) pager.scrollToPage(pager.currentPage - 1)
                    else pager.animateScrollToPage(pager.currentPage - 1, animationSpec = tween(preferences.pageTurnDurationMs, easing = androidx.compose.animation.core.FastOutSlowInEasing))
                }
                PageTurnCommand.NEXT -> if (pager.currentPage == pages.lastIndex) viewModel.nextChapter() else {
                    if (preferences.pageAnimation == PageAnimation.INSTANT || preferences.reducedMotion) pager.scrollToPage(pager.currentPage + 1)
                    else pager.animateScrollToPage(pager.currentPage + 1, animationSpec = tween(preferences.pageTurnDurationMs, easing = androidx.compose.animation.core.FastOutSlowInEasing))
                }
            }
        }
    }
    LaunchedEffect(state.autoPageIntervalSeconds, state.controlsVisible, state.panel) {
        val interval = state.autoPageIntervalSeconds
        if (interval <= 0 || state.controlsVisible || state.panel != ReaderPanel.NONE) return@LaunchedEffect
        while (true) {
            delay(interval * 1_000L)
            viewModel.requestPageTurn(PageTurnCommand.NEXT)
        }
    }
    val gestureModifier = if (!state.controlsVisible && state.panel == ReaderPanel.NONE) {
        Modifier.pointerInput(state.currentChapter?.id, pages.size, preferences.pageAnimation, preferences.pageTurnDurationMs, preferences.reducedMotion) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val initialPage = pager.currentPage
                var finalPosition = down.position
                var finalUptime = down.uptimeMillis
                var pressed: Boolean
                do {
                    val event = awaitPointerEvent(PointerEventPass.Final)
                    val tracked = event.changes.firstOrNull { it.id == down.id }
                    if (tracked != null) {
                        finalPosition = tracked.position
                        finalUptime = tracked.uptimeMillis
                    }
                    pressed = event.changes.any { it.pressed }
                } while (pressed)

                val delta = finalPosition - down.position
                val isTap = abs(delta.x) <= viewConfiguration.touchSlop &&
                    abs(delta.y) <= viewConfiguration.touchSlop && finalUptime - down.uptimeMillis < 550L
                val action = if (isTap) ReaderInteraction.tapAction(down.position.x, size.width.toFloat())
                else ReaderInteraction.swipeAction(delta.x, delta.y, size.width.toFloat())

                when (action) {
                    ReaderGestureAction.PREVIOUS_PAGE -> when {
                        initialPage == 0 -> viewModel.previousChapterAtEnd()
                        isTap || preferences.pageAnimation == PageAnimation.INSTANT -> scope.launch {
                            if (preferences.pageAnimation == PageAnimation.INSTANT || preferences.reducedMotion) pager.scrollToPage(initialPage - 1)
                            else pager.animateScrollToPage(initialPage - 1, animationSpec = tween(preferences.pageTurnDurationMs, easing = androidx.compose.animation.core.FastOutSlowInEasing))
                        }
                        else -> Unit
                    }
                    ReaderGestureAction.NEXT_PAGE -> when {
                        initialPage == pages.lastIndex -> viewModel.nextChapter()
                        isTap || preferences.pageAnimation == PageAnimation.INSTANT -> scope.launch {
                            if (preferences.pageAnimation == PageAnimation.INSTANT || preferences.reducedMotion) pager.scrollToPage(initialPage + 1)
                            else pager.animateScrollToPage(initialPage + 1, animationSpec = tween(preferences.pageTurnDurationMs, easing = androidx.compose.animation.core.FastOutSlowInEasing))
                        }
                        else -> Unit
                    }
                    ReaderGestureAction.TOGGLE_CONTROLS -> viewModel.toggleControls()
                    ReaderGestureAction.NONE -> Unit
                }
            }
        }
    } else Modifier
    Box(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .padding(top = 28.dp, bottom = 20.dp)
            .onSizeChanged { viewport = it }
            .then(gestureModifier)
    ) {
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize(),
            key = { it },
            userScrollEnabled = preferences.pageAnimation != PageAnimation.INSTANT,
            beyondViewportPageCount = 1,
        ) { page ->
            val slice = pages[page]
            val pageOffset = (pager.currentPage - page) + pager.currentPageOffsetFraction
            val movingFraction = pageOffset.absoluteValue.coerceIn(0f, 1f)
            val colors = LocalMoyuColors.current
            val pageModifier = Modifier.fillMaxSize()
                .background(colors.readerBackground)
                .graphicsLayer {
                    shadowElevation = 0f
                    when (preferences.pageAnimation) {
                        PageAnimation.FADE -> {
                            alpha = 1f - movingFraction * .82f
                            scaleX = 1f - movingFraction * .025f
                            scaleY = 1f - movingFraction * .025f
                        }
                        PageAnimation.COVER -> {
                            // Clear book-spine pivot, intentionally different from
                            // the plain horizontal slide.
                            transformOrigin = TransformOrigin(if (pageOffset < 0f) 0f else 1f, .5f)
                            rotationY = pageOffset.coerceIn(-1f, 1f) * 46f
                            translationX = -pageOffset * size.width * .16f
                            scaleX = 1f - movingFraction * .045f
                            alpha = 1f - movingFraction * .16f
                            cameraDistance = 42f * density.density
                        }
                        PageAnimation.PAPER -> {
                            transformOrigin = TransformOrigin(if (pageOffset > 0) 1f else 0f, .5f)
                            rotationY = -pageOffset.coerceIn(-1f, 1f) * 30f
                            rotationZ = pageOffset.coerceIn(-1f, 1f) * 1.2f
                            translationX = -pageOffset * size.width * .09f
                            scaleX = 1f - movingFraction * .024f
                            scaleY = 1f - movingFraction * .012f
                            alpha = 1f - movingFraction * .08f
                            cameraDistance = 48f * density.density
                        }
                        else -> Unit
                    }
                }
            Box(pageModifier) {
                ReaderPage(
                    bookTitle = state.book?.title.orEmpty(),
                    chapterTitle = state.currentChapter?.title.orEmpty(),
                    text = displayText.substring(slice.start, slice.endExclusive),
                    chapterText = displayText,
                    pageStart = slice.start,
                    page = page,
                    isChapterEnd = page == pages.lastIndex,
                    preferences = preferences,
                    typeface = typeface,
                    modifier = Modifier.fillMaxSize(),
                )
                if (preferences.pageAnimation == PageAnimation.PAPER && movingFraction > .01f) {
                    Box(
                        Modifier.align(if (pageOffset < 0f) Alignment.CenterStart else Alignment.CenterEnd)
                            .fillMaxHeight().width(18.dp)
                            .background(colors.divider.copy(alpha = .28f * movingFraction)),
                    )
                }
            }
        }
        ReaderFooter(state, pager.currentPage, pages.size, preferences.showReaderClock, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ReaderPage(
    bookTitle: String,
    chapterTitle: String,
    text: String,
    chapterText: String,
    pageStart: Int,
    page: Int,
    isChapterEnd: Boolean,
    preferences: ReaderPreferences,
    typeface: Typeface,
    modifier: Modifier = Modifier,
) {
    val family = remember(typeface) { FontFamily(typeface) }
    val paragraphs = remember(text, chapterText, pageStart) { pageParagraphs(text, chapterText, pageStart) }
    Column(modifier.padding(horizontal = preferences.horizontalMarginDp.dp).padding(bottom = 34.dp)) {
        Text(bookTitle, style = MaterialTheme.typography.labelMedium, color = LocalMoyuColors.current.readerText.copy(alpha = .52f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(14.dp))
        if (page == 0) {
            Text(chapterTitle, style = MaterialTheme.typography.headlineMedium.copy(fontFamily = family), color = LocalMoyuColors.current.readerText, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(16.dp))
        }
        SelectionContainer {
            Column(verticalArrangement = Arrangement.spacedBy(preferences.paragraphSpacingDp.dp)) {
                paragraphs.forEach { paragraph ->
                    Text(
                        text = paragraph.text,
                        color = LocalMoyuColors.current.readerText,
                        style = TextStyle(
                            fontFamily = family,
                            fontSize = preferences.fontSizeSp.sp,
                            fontWeight = FontWeight(preferences.fontWeight),
                            lineHeight = (preferences.fontSizeSp * preferences.lineHeightMultiplier).sp,
                            textIndent = if (paragraph.isSourceParagraphStart) TextIndent(firstLine = (preferences.fontSizeSp * preferences.firstLineIndentEm).sp) else TextIndent(),
                            textAlign = if (preferences.justified) TextAlign.Justify else TextAlign.Start,
                        ),
                    )
                }
            }
        }
        if (isChapterEnd) {
            Spacer(Modifier.weight(1f))
            Text(
                "— 本章完 —",
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = LocalMoyuColors.current.readerText.copy(alpha = .42f),
            )
        }
    }
}

@Composable
private fun ScrollingReader(state: ReaderUiState, preferences: ReaderPreferences, viewModel: ReaderViewModel) {
    val typeface = remember(preferences.customFontPath) { preferences.customFontPath?.let { runCatching { Typeface.createFromFile(File(it)) }.getOrNull() } ?: Typeface.SERIF }
    val family = remember(typeface) { FontFamily(typeface) }
    val paragraphs = remember(state.text) {
        var offset = 0
        state.text.split(Regex("\\n+"), limit = 0).filter { it.isNotBlank() }.map { paragraph ->
            val found = state.text.indexOf(paragraph, offset).coerceAtLeast(offset)
            offset = found + paragraph.length
            found to paragraph.trimStart(' ', '\t', '　', '\u00A0')
        }
    }
    val initialIndex = remember(state.currentChapter?.id) {
        (paragraphs.indexOfLast { it.first <= state.characterOffset } + 1).coerceAtLeast(0)
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    LaunchedEffect(listState, paragraphs) {
        snapshotFlow { listState.firstVisibleItemIndex }.distinctUntilChanged().collect { itemIndex ->
            paragraphs.getOrNull(itemIndex - 1)?.let { viewModel.updateAnchor(it.first) }
        }
    }
    LaunchedEffect(listState, viewModel) {
        viewModel.pageTurns.collect { command ->
            val distance = listState.layoutInfo.viewportSize.height * .86f
            when (command) {
                PageTurnCommand.PREVIOUS -> if (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) viewModel.previousChapterAtEnd() else listState.animateScrollBy(-distance)
                PageTurnCommand.NEXT -> if (!listState.canScrollForward) viewModel.nextChapter() else listState.animateScrollBy(distance)
            }
        }
    }
    LaunchedEffect(state.autoPageIntervalSeconds, state.controlsVisible, state.panel) {
        val interval = state.autoPageIntervalSeconds
        if (interval <= 0 || state.controlsVisible || state.panel != ReaderPanel.NONE) return@LaunchedEffect
        while (true) {
            delay(interval * 1_000L)
            viewModel.requestPageTurn(PageTurnCommand.NEXT)
        }
    }
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .readerTapObserver(state.currentChapter?.id, enabled = !state.controlsVisible && state.panel == ReaderPanel.NONE) { viewModel.toggleControls() },
        state = listState,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = preferences.horizontalMarginDp.dp, vertical = 38.dp),
    ) {
        item { Text(state.currentChapter?.title.orEmpty(), style = MaterialTheme.typography.headlineMedium, color = LocalMoyuColors.current.readerText, modifier = Modifier.padding(bottom = 24.dp)) }
        items(paragraphs, key = { it.first }) { (offset, paragraph) ->
            Text(
                paragraph,
                color = LocalMoyuColors.current.readerText,
                style = TextStyle(
                    fontFamily = family,
                    fontSize = preferences.fontSizeSp.sp,
                    fontWeight = FontWeight(preferences.fontWeight),
                    lineHeight = (preferences.fontSizeSp * preferences.lineHeightMultiplier).sp,
                    textIndent = TextIndent(firstLine = (preferences.fontSizeSp * preferences.firstLineIndentEm).sp),
                    textAlign = if (preferences.justified) TextAlign.Justify else TextAlign.Start,
                ),
                modifier = Modifier.padding(bottom = preferences.paragraphSpacingDp.dp),
            )
        }
        item { Spacer(Modifier.height(64.dp)) }
    }
}

@Composable
private fun ReaderControls(state: ReaderUiState, reducedMotion: Boolean, speaking: Boolean, viewModel: ReaderViewModel, onBack: () -> Unit) {
    val duration = if (reducedMotion) 0 else MoyuMotion.Standard
    if (state.controlsVisible && state.panel == ReaderPanel.NONE) {
        Box(
            Modifier.fillMaxSize().pointerInput(state.currentChapter?.id) {
                detectTapGestures(onTap = { viewModel.hideControls() })
            }
        )
    }
    AnimatedVisibility(
        visible = state.controlsVisible,
        enter = fadeIn(androidx.compose.animation.core.tween(duration)) + slideInVertically(androidx.compose.animation.core.tween(duration)) { -it / 5 },
        exit = fadeOut(androidx.compose.animation.core.tween(duration)) + slideOutVertically(androidx.compose.animation.core.tween(duration)) { -it / 5 },
    ) {
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 5.dp) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().height(64.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                ReaderGlyphButton(MoyuGlyph.BACK, "返回", onBack)
                Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                    Text(state.book?.title.orEmpty(), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(state.currentChapter?.title.orEmpty(), style = MaterialTheme.typography.bodySmall, color = LocalMoyuColors.current.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                ReaderGlyphButton(MoyuGlyph.BOOKMARK, "添加书签", viewModel::addBookmark)
                ReaderGlyphButton(MoyuGlyph.AUDIO, if (speaking) "朗读控制" else "本地朗读", { viewModel.showPanel(ReaderPanel.TTS) })
            }
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = state.controlsVisible,
            enter = fadeIn(androidx.compose.animation.core.tween(duration)) + slideInVertically(androidx.compose.animation.core.tween(duration)) { it / 4 },
            exit = fadeOut(androidx.compose.animation.core.tween(duration)) + slideOutVertically(androidx.compose.animation.core.tween(duration)) { it / 4 },
        ) {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 10.dp, shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 10.dp, bottom = 8.dp)) {
                    Box(Modifier.align(Alignment.CenterHorizontally).width(34.dp).height(3.dp).background(LocalMoyuColors.current.divider, RoundedCornerShape(3.dp)))
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(onClick = viewModel::previousChapter, color = Color.Transparent, enabled = state.chapterIndex > 0) { Text("‹ 上一章", Modifier.padding(8.dp), style = MaterialTheme.typography.labelMedium, color = if (state.chapterIndex > 0) MaterialTheme.colorScheme.onSurface else LocalMoyuColors.current.textTertiary) }
                        EditorialProgressSlider(
                            value = overallProgress(state),
                            onValueChange = { value -> viewModel.goToChapter((value * state.chapters.size).toInt().coerceIn(0, state.chapters.lastIndex)) },
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        )
                        Surface(onClick = viewModel::nextChapter, color = Color.Transparent, enabled = state.chapterIndex < state.chapters.lastIndex) { Text("下一章 ›", Modifier.padding(8.dp), style = MaterialTheme.typography.labelMedium, color = if (state.chapterIndex < state.chapters.lastIndex) MaterialTheme.colorScheme.onSurface else LocalMoyuColors.current.textTertiary) }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(LocalMoyuColors.current.divider))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp)) {
                        ReaderAction(MoyuGlyph.DIRECTORY, "目录", Modifier.weight(1f)) { viewModel.showPanel(ReaderPanel.DIRECTORY) }
                        ReaderAction(MoyuGlyph.PROGRESS, "进度", Modifier.weight(1f)) { viewModel.showPanel(ReaderPanel.PROGRESS) }
                        ReaderAction(MoyuGlyph.SEARCH, "搜索", Modifier.weight(1f)) { viewModel.showPanel(ReaderPanel.SEARCH) }
                        ReaderAction(MoyuGlyph.BOOKMARK, "书签", Modifier.weight(1f)) { viewModel.showPanel(ReaderPanel.BOOKMARKS) }
                        ReaderAction(MoyuGlyph.THEME, "主题", Modifier.weight(1f)) { viewModel.showPanel(ReaderPanel.THEME) }
                        ReaderTypographyAction(Modifier.weight(1f)) { viewModel.showPanel(ReaderPanel.READING_SETTINGS) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderGlyphButton(glyph: MoyuGlyph, description: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .82f else 1f, spring(dampingRatio = .62f, stiffness = 760f), label = "reader-glyph-scale")
    Surface(onClick = onClick, interactionSource = interaction, color = Color.Transparent, shape = RoundedCornerShape(12.dp), modifier = Modifier.size(44.dp).semantics { contentDescription = description }) {
        Box(contentAlignment = Alignment.Center) { MoyuGlyphIcon(glyph, Modifier.size(22.dp).graphicsLayer { scaleX = scale; scaleY = scale; rotationZ = if (pressed) -5f else 0f }) }
    }
}

@Composable
private fun ReaderAction(glyph: MoyuGlyph, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .84f else 1f, spring(dampingRatio = .58f, stiffness = 820f), label = "reader-action-scale")
    Surface(onClick = onClick, interactionSource = interaction, modifier = modifier, color = Color.Transparent, shape = RoundedCornerShape(10.dp)) {
        Column(Modifier.padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            MoyuGlyphIcon(glyph, Modifier.size(20.dp).graphicsLayer { scaleX = scale; scaleY = scale; rotationZ = if (pressed) 7f else 0f })
            Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun ReaderTypographyAction(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .92f else 1f, spring(dampingRatio = .62f, stiffness = 820f), label = "typography-action-scale")
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        modifier = modifier.height(50.dp).graphicsLayer { scaleX = scale; scaleY = scale; rotationZ = if (pressed) -3f else 0f },
        color = Color.Transparent,
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("A", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                Text("a", fontFamily = FontFamily.Serif, fontSize = 12.sp, color = LocalMoyuColors.current.textSecondary, modifier = Modifier.padding(start = 1.dp, bottom = 1.dp))
            }
            Box(Modifier.width(20.dp).height(1.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = .7f)))
            Text("排版", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

@Composable
private fun DirectoryDrawer(state: ReaderUiState, viewModel: ReaderViewModel) {
    var query by remember { mutableStateOf("") }
    var reversed by remember { mutableStateOf(false) }
    val visibleChapters = remember(state.chapters, query, reversed) {
        state.chapters
            .filter { query.isBlank() || it.title.contains(query, ignoreCase = true) }
            .let { if (reversed) it.asReversed() else it }
    }
    val directoryState = rememberLazyListState()
    LaunchedEffect(state.chapterIndex, reversed, query, visibleChapters) {
        val current = visibleChapters.indexOfFirst { it.index == state.chapterIndex }
        if (current >= 0) directoryState.scrollToItem(current)
    }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .44f)).clickable(onClick = viewModel::dismissPanel))
        AnimatedVisibility(
            visible = true,
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = slideInHorizontally { it },
            exit = slideOutHorizontally { it },
        ) {
            Surface(Modifier.fillMaxHeight().fillMaxWidth(.75f), color = MaterialTheme.colorScheme.surface, shadowElevation = 12.dp) {
                Column(Modifier.statusBarsPadding().padding(top = 16.dp)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("目录", style = MaterialTheme.typography.headlineMedium)
                            Text("${state.book?.title} · ${state.chapters.size} 章", color = LocalMoyuColors.current.textSecondary, modifier = Modifier.padding(top = 4.dp))
                        }
                        TinyControl(if (reversed) "正序" else "倒序") { reversed = !reversed }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp).height(42.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MoyuGlyphIcon(MoyuGlyph.SEARCH, Modifier.size(18.dp))
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.weight(1f).padding(start = 9.dp),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                            singleLine = true,
                            decorationBox = { inner -> if (query.isBlank()) Text("搜索章节", color = LocalMoyuColors.current.textTertiary) else Unit; inner() },
                        )
                    }
                    LazyColumn(Modifier.fillMaxSize(), state = directoryState) {
                        items(visibleChapters, key = { it.id }) { chapter ->
                            val active = chapter.index == state.chapterIndex
                            Surface(onClick = { viewModel.goToChapter(chapter.index) }, color = if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .35f) else Color.Transparent) {
                                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.width(3.dp).height(if (active) 22.dp else 0.dp).background(MaterialTheme.colorScheme.primary))
                                    Text(chapter.title, Modifier.weight(1f).padding(start = 10.dp), color = if (active) MaterialTheme.colorScheme.onSurface else LocalMoyuColors.current.textSecondary, maxLines = 2)
                                    Text(
                                        when {
                                            active -> "当前"
                                            chapter.index < state.chapterIndex -> "已读"
                                            else -> "未读"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (active) MaterialTheme.colorScheme.primary else LocalMoyuColors.current.textTertiary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface, contentWindowInsets = { WindowInsets(0, 0, 0, 0) }) {
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 30.dp)) { content() }
    }
}

@Composable
private fun ProgressPanel(state: ReaderUiState, viewModel: ReaderViewModel) {
    val percentage = (state.chapterIndex + state.characterOffset.toFloat() / state.text.length.coerceAtLeast(1)) / state.chapters.size.coerceAtLeast(1)
    Text("阅读进度", style = MaterialTheme.typography.headlineMedium)
    Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.Bottom) {
        Text("${(percentage * 100).toInt()}%", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
        Text(" · 第 ${state.chapterIndex + 1} / ${state.chapters.size} 章", modifier = Modifier.padding(start = 8.dp, bottom = 7.dp), color = LocalMoyuColors.current.textSecondary)
    }
    Text(state.currentChapter?.title.orEmpty(), color = LocalMoyuColors.current.textSecondary, modifier = Modifier.padding(top = 4.dp))
    EditorialProgressSlider(
        value = percentage,
        onValueChange = { viewModel.goToChapter((it * state.chapters.size).toInt().coerceIn(0, state.chapters.lastIndex)) },
        modifier = Modifier.padding(vertical = 14.dp),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf(0, 25, 50, 75, 100).forEach { mark ->
            Surface(onClick = { viewModel.goToChapter(((mark / 100f) * state.chapters.lastIndex.coerceAtLeast(0)).toInt()) }, color = if (abs(percentage * 100 - mark) < 8) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp)) {
                Text("$mark%", Modifier.padding(horizontal = 10.dp, vertical = 7.dp), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
    Text("自动阅读", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 22.dp, bottom = 8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(0, 6, 12, 20).forEach { seconds ->
            val active = state.autoPageIntervalSeconds == seconds
            Surface(
                onClick = {
                    viewModel.setAutoPageInterval(seconds)
                    if (seconds > 0) viewModel.dismissPanel()
                },
                modifier = Modifier.weight(1f),
                color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(if (seconds == 0) "手动" else "$seconds 秒", Modifier.padding(vertical = 9.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun SearchPanel(state: ReaderUiState, viewModel: ReaderViewModel) {
    Text("全书搜索", style = MaterialTheme.typography.headlineMedium)
    Row(Modifier.fillMaxWidth().height(50.dp).padding(top = 8.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        MoyuGlyphIcon(MoyuGlyph.SEARCH, Modifier.size(20.dp))
        BasicTextField(
            value = state.searchQuery,
            onValueChange = viewModel::search,
            modifier = Modifier.weight(1f).padding(start = 10.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            singleLine = true,
            decorationBox = { inner -> if (state.searchQuery.isEmpty()) Text("输入正文关键词", color = LocalMoyuColors.current.textTertiary) else Unit; inner() },
        )
    }
    if (state.searching) CircularProgressIndicator(Modifier.padding(22.dp))
    LazyColumn(Modifier.fillMaxWidth().height(360.dp)) {
        items(state.searchHits, key = { "${it.chapterId}:${it.characterOffset}" }) { hit ->
            Surface(onClick = { viewModel.openSearchHit(hit) }, color = Color.Transparent) {
                Column(Modifier.fillMaxWidth().padding(vertical = 13.dp)) {
                    Text(hit.chapterTitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(highlightQuery(hit.excerpt, state.searchQuery), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 5.dp), maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun BookmarkPanel(state: ReaderUiState, viewModel: ReaderViewModel) {
    Text("书签", style = MaterialTheme.typography.headlineMedium)
    Text("全部 ${state.bookmarks.size}", color = LocalMoyuColors.current.textSecondary, modifier = Modifier.padding(top = 5.dp, bottom = 10.dp))
    if (state.bookmarks.isEmpty()) Text("轻点顶部书签图标保存当前段落。", color = LocalMoyuColors.current.textTertiary, modifier = Modifier.padding(vertical = 40.dp))
    LazyColumn(Modifier.fillMaxWidth().height(340.dp)) {
        items(state.bookmarks, key = { it.id }) { bookmark ->
            Surface(onClick = {
                val index = state.chapters.indexOfFirst { it.id == bookmark.chapterId }
                if (index >= 0) viewModel.goToChapter(index, bookmark.characterOffset)
            }, color = Color.Transparent) {
                Row(Modifier.fillMaxWidth().padding(vertical = 13.dp), verticalAlignment = Alignment.Top) {
                    MoyuGlyphIcon(MoyuGlyph.BOOKMARK, Modifier.size(20.dp), accent = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(state.chapters.firstOrNull { it.id == bookmark.chapterId }?.title.orEmpty(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(bookmark.excerpt, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                    }
                    Text("×", Modifier.clickable { viewModel.deleteBookmark(bookmark.id) }.padding(8.dp), color = LocalMoyuColors.current.textTertiary)
                }
            }
        }
    }
}

@Composable
private fun ReadingSettingsPanel(preferences: ReaderPreferences, settings: SettingsRepository, onOpenFont: () -> Unit) {
    val scope = rememberCoroutineScope()
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("阅读排版", style = MaterialTheme.typography.headlineMedium)
                Text("每组只处理一类设置，调整后保留当前阅读位置", style = MaterialTheme.typography.bodySmall, color = LocalMoyuColors.current.textTertiary)
            }
            TinyControl("恢复默认") {
                scope.launch {
                    settings.setFontSize(19f); settings.setFontWeight(400); settings.setLineHeight(1.68f)
                    settings.setParagraphSpacing(2f); settings.setHorizontalMargin(24f); settings.setFirstLineIndent(0f)
                }
            }
        }
        Surface(Modifier.fillMaxWidth(), color = LocalMoyuColors.current.readerBackground, shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text("排版预览", style = MaterialTheme.typography.labelMedium, color = LocalMoyuColors.current.readerText.copy(alpha = .52f))
                Text(
                    "雨落在旧城的屋檐上，像有人翻动一本很厚的书。\n下一页将在句末停住。",
                    color = LocalMoyuColors.current.readerText,
                    fontFamily = FontFamily.Serif,
                    fontSize = preferences.fontSizeSp.sp,
                    lineHeight = (preferences.fontSizeSp * preferences.lineHeightMultiplier).sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        LayoutGroup("文字", "字号、字体与字重") {
            SettingControl("字号", "${preferences.fontSizeSp.toInt()} sp") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TinyControl("A−") { scope.launch { settings.setFontSize(preferences.fontSizeSp - 1) } }
                    TinyControl("A+") { scope.launch { settings.setFontSize(preferences.fontSizeSp + 1) } }
                }
            }
            SettingControl("字体", if (preferences.customFontPath == null) "系统宋体" else File(preferences.customFontPath).name) { TinyControl("管理") { onOpenFont() } }
            LayoutOptionRow("字重", listOf(400, 500, 600), preferences.fontWeight, { "$it" }) { scope.launch { settings.setFontWeight(it) } }
        }

        LayoutGroup("段落", "行距、边距与段落节奏") {
            LayoutOptionRow("行距", listOf(1.45f, 1.68f, 1.95f), preferences.lineHeightMultiplier, { when (it) { 1.45f -> "紧凑"; 1.68f -> "舒适"; else -> "宽松" } }) { scope.launch { settings.setLineHeight(it) } }
            LayoutOptionRow("页边距", listOf(20f, 28f, 40f), preferences.horizontalMarginDp, { when (it) { 20f -> "窄"; 28f -> "标准"; else -> "宽" } }) { scope.launch { settings.setHorizontalMargin(it) } }
            FreeDecimalInput("首行缩进", preferences.firstLineIndentEm, "字", 0f..4f) { scope.launch { settings.setFirstLineIndent(it) } }
            Text("段间距 · ${preferences.paragraphSpacingDp.toInt()} dp", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
            LayoutOptionRow("", listOf(0f, 4f, 8f, 16f), preferences.paragraphSpacingDp, { if (it == 0f) "无" else "${it.toInt()} dp" }) { scope.launch { settings.setParagraphSpacing(it) } }
            Slider(value = preferences.paragraphSpacingDp, onValueChange = { scope.launch { settings.setParagraphSpacing(it) } }, valueRange = 0f..28f)
        }

        LayoutGroup("翻页", "不同方式有各自的视觉反馈") {
            PageAnimation.entries.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { animation ->
                        val selected = preferences.pageAnimation == animation
                        Surface(
                            onClick = { scope.launch { settings.setPageAnimation(animation) } },
                            modifier = Modifier.weight(1f).height(66.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else LocalMoyuColors.current.divider),
                        ) {
                            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                Text(pageAnimationName(animation), fontWeight = FontWeight.SemiBold)
                                Text(pageAnimationDetail(animation), style = MaterialTheme.typography.bodySmall, color = LocalMoyuColors.current.textTertiary, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            LayoutOptionRow("阅读模式", ReaderMode.entries.toList(), preferences.mode, { if (it == ReaderMode.PAGED) "分页" else "滚动" }) { scope.launch { settings.setReaderMode(it) } }
            LayoutOptionRow("翻页速度", listOf(160, 280, 420, 650), preferences.pageTurnDurationMs, { "${it}ms" }) { scope.launch { settings.setPageTurnDuration(it) } }
        }

        LayoutGroup("显示与按键", "设备显示与阅读操作") {
            LayoutOptionRow("方向", ReaderOrientation.entries.toList(), preferences.orientation, { when (it) { ReaderOrientation.SYSTEM -> "跟随系统"; ReaderOrientation.PORTRAIT -> "竖屏"; ReaderOrientation.LANDSCAPE -> "横屏" } }) { scope.launch { settings.setOrientation(it) } }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("屏幕亮度", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                TinyControl(if (preferences.brightness < 0f) "已跟随系统" else "跟随系统") { scope.launch { settings.setBrightness(-1f) } }
            }
            Slider(value = if (preferences.brightness < 0f) .5f else preferences.brightness, onValueChange = { scope.launch { settings.setBrightness(it) } }, valueRange = 0f..1f)
            ReaderSwitchRow("两端对齐", "在宽屏上获得更整齐的字面", preferences.justified) { scope.launch { settings.setJustified(it) } }
            ReaderSwitchRow("保持屏幕常亮", "阅读期间保持屏幕点亮", preferences.keepScreenOn) { scope.launch { settings.setKeepScreenOn(it) } }
            ReaderSwitchRow("显示阅读时钟", "页脚同时展示当前时间", preferences.showReaderClock) { scope.launch { settings.setShowReaderClock(it) } }
            ReaderSwitchRow("音量键翻页", "音量加上一页，音量减下一页", preferences.volumeKeyPageTurn) { scope.launch { settings.setVolumeKeyPageTurn(it) } }
        }
    }
}

@Composable
private fun ThemePanel(preferences: ReaderPreferences, settings: SettingsRepository) {
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
    Text("主题设置", style = MaterialTheme.typography.headlineMedium)
    Box(Modifier.fillMaxWidth().height(116.dp).padding(vertical = 12.dp).background(LocalMoyuColors.current.readerBackground).padding(16.dp)) {
        Text("雨落在旧城的屋檐上，像有人翻动一本很厚的书。", color = LocalMoyuColors.current.readerText, fontFamily = FontFamily.Serif, lineHeight = 27.sp)
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ReaderTheme.entries.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { theme -> ThemeChoice(theme, preferences.theme == theme, Modifier.weight(1f)) { scope.launch { settings.setTheme(theme) } } }
            }
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("减少动态效果", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Switch(checked = preferences.reducedMotion, onCheckedChange = { scope.launch { settings.setReducedMotion(it) } })
    }
    }
}

@Composable
private fun ThemeChoice(theme: ReaderTheme, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val preview = when (theme) {
        ReaderTheme.LIGHT -> Triple(Color(0xFFFCFBF7), Color(0xFF151412), Color(0xFFD83A2E))
        ReaderTheme.DARK -> Triple(Color(0xFF11110F), Color(0xFFF8F5ED), Color(0xFFF2C9C3))
        ReaderTheme.OLED -> Triple(Color.Black, Color(0xFFF8F5ED), Color(0xFFA9D5CE))
        ReaderTheme.PAPER -> Triple(Color(0xFFF8F5ED), Color(0xFF34312D), Color(0xFFB92D24))
    }
    Surface(onClick = onClick, modifier = modifier.height(92.dp), color = preview.first, shape = RoundedCornerShape(8.dp), border = androidx.compose.foundation.BorderStroke(if (selected) 2.dp else 1.dp, if (selected) preview.third else LocalMoyuColors.current.divider)) {
        Column(Modifier.padding(12.dp)) {
            Text(themeName(theme), color = preview.second, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { listOf(preview.first, preview.second, preview.third).forEach { Box(Modifier.size(10.dp).clip(RoundedCornerShape(10.dp)).background(it)) } }
        }
    }
}

@Composable
private fun FontPanel(preferences: ReaderPreferences, settings: SettingsRepository) {
    val scope = rememberCoroutineScope()
    Text("字体设置", style = MaterialTheme.typography.headlineMedium)
    Text("山川异域，风月同天。中文正文需要稳定的字面与重心。", fontFamily = FontFamily.Serif, fontSize = 20.sp, lineHeight = 34.sp, modifier = Modifier.padding(vertical = 18.dp))
    SettingControl("当前字体", if (preferences.customFontPath == null) "系统宋体" else File(preferences.customFontPath).name) { }
    SettingControl("字体粗细", preferences.fontWeight.toString()) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(400, 500, 600).forEach { weight -> TinyControl(weight.toString()) { scope.launch { settings.setFontWeight(weight) } } }
        }
    }
}

@Composable
private fun TtsPanel(
    state: ReaderUiState,
    preferences: ReaderPreferences,
    settings: SettingsRepository,
    ttsState: LocalTtsState,
    controller: LocalTtsController,
) {
    val scope = rememberCoroutineScope()
    var sleepMinutes by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
    Text("本地朗读", style = MaterialTheme.typography.headlineMedium)
    Text("由手机内置 TTS 从当前页开始朗读，小说正文不会离开设备。", color = LocalMoyuColors.current.textSecondary, modifier = Modifier.padding(top = 8.dp))
    Surface(Modifier.fillMaxWidth().padding(top = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            MoyuGlyphIcon(MoyuGlyph.AUDIO, Modifier.size(26.dp), accent = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(if (ttsState.speaking) "正在朗读" else "朗读引擎", style = MaterialTheme.typography.titleMedium)
                Text(ttsState.message.orEmpty(), style = MaterialTheme.typography.bodySmall, color = LocalMoyuColors.current.textTertiary)
            }
            Box(Modifier.size(8.dp).background(if (ttsState.ready) Color(0xFF2E7D5B) else MaterialTheme.colorScheme.error, RoundedCornerShape(8.dp)))
        }
    }
    Text("语速 · ${"%.1f".format(preferences.ttsRate)} ×", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 18.dp))
    Slider(preferences.ttsRate, { scope.launch { settings.setTtsRate(it) } }, valueRange = .6f..1.8f, steps = 5)
    Text("定时停止", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 6.dp, bottom = 8.dp))
    SegmentedChoices(listOf(0, 15, 30, 60), sleepMinutes, { if (it == 0) "不限时" else "$it 分" }) { sleepMinutes = it }
    Surface(
        onClick = {
            if (ttsState.speaking) controller.stop()
            else controller.speak(state.text.substring(state.characterOffset.coerceIn(0, state.text.length)), preferences.ttsRate, sleepMinutes)
        },
        enabled = ttsState.ready,
        modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
        color = if (ttsState.ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (ttsState.ready) MaterialTheme.colorScheme.onPrimary else LocalMoyuColors.current.textTertiary,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(Modifier.padding(vertical = 14.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            MoyuGlyphIcon(MoyuGlyph.AUDIO, Modifier.size(20.dp), color = if (ttsState.ready) MaterialTheme.colorScheme.onPrimary else LocalMoyuColors.current.textTertiary, accent = if (ttsState.ready) MaterialTheme.colorScheme.onPrimary else LocalMoyuColors.current.textTertiary)
            Text(if (ttsState.speaking) "暂停朗读" else "从当前页开始", Modifier.padding(start = 9.dp), fontWeight = FontWeight.SemiBold)
        }
    }
    Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SettingControl(label: String, value: String, trailing: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().height(62.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(label, style = MaterialTheme.typography.titleMedium); Text(value, style = MaterialTheme.typography.bodySmall, color = LocalMoyuColors.current.textTertiary) }
        trailing()
    }
}

@Composable
private fun LayoutGroup(title: String, summary: String, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .52f),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(summary, style = MaterialTheme.typography.bodySmall, color = LocalMoyuColors.current.textTertiary, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
            content()
        }
    }
}

@Composable
private fun <T> LayoutOptionRow(
    label: String,
    values: List<T>,
    selected: T,
    labelFor: (T) -> String,
    onSelect: (T) -> Unit,
) {
    if (label.isNotBlank()) Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp, bottom = 7.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        values.forEach { value ->
            val active = value == selected
            Surface(
                onClick = { onSelect(value) },
                modifier = Modifier.weight(1f),
                color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                contentColor = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(if (active) 1.2.dp else 1.dp, if (active) MaterialTheme.colorScheme.primary else LocalMoyuColors.current.divider),
            ) {
                Text(labelFor(value), Modifier.padding(vertical = 9.dp, horizontal = 4.dp), textAlign = TextAlign.Center, maxLines = 1, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun FreeDecimalInput(label: String, value: Float, suffix: String, range: ClosedFloatingPointRange<Float>, onValue: (Float) -> Unit) {
    var input by remember(value) { mutableStateOf(if (value % 1f == 0f) value.toInt().toString() else "%.1f".format(Locale.US, value)) }
    Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp, bottom = 7.dp))
    Row(
        Modifier.fillMaxWidth().height(46.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
            .padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = input,
            onValueChange = { raw ->
                if (raw.all { it.isDigit() || it == '.' }) {
                    input = raw
                    raw.toFloatOrNull()?.takeIf { it in range }?.let(onValue)
                }
            },
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            decorationBox = { inner -> if (input.isBlank()) Text("0–4", color = LocalMoyuColors.current.textTertiary) else Unit; inner() },
        )
        Text(suffix, style = MaterialTheme.typography.bodyMedium, color = LocalMoyuColors.current.textSecondary)
    }
}

@Composable
private fun ReaderSwitchRow(label: String, detail: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().height(66.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = LocalMoyuColors.current.textTertiary)
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun TinyControl(label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) { Text(label, Modifier.padding(horizontal = 13.dp, vertical = 9.dp), fontWeight = FontWeight.SemiBold) }
}

private fun pageAnimationName(animation: PageAnimation) = when (animation) {
    PageAnimation.INSTANT -> "即时"
    PageAnimation.SLIDE -> "滑动"
    PageAnimation.FADE -> "淡入"
    PageAnimation.COVER -> "覆盖"
    PageAnimation.PAPER -> "纸页"
}

private fun pageAnimationDetail(animation: PageAnimation) = when (animation) {
    PageAnimation.INSTANT -> "点按即切换"
    PageAnimation.SLIDE -> "平移过渡"
    PageAnimation.FADE -> "渐隐渐现"
    PageAnimation.COVER -> "书封翻开"
    PageAnimation.PAPER -> "纸张卷页"
}

@Composable
private fun EditorialProgressSlider(value: Float, onValueChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    val fraction = value.coerceIn(0f, 1f)
    Box(
        modifier.fillMaxWidth().height(40.dp).pointerInput(onValueChange) {
            detectTapGestures { point -> onValueChange((point.x / size.width).coerceIn(0f, 1f)) }
        },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(LocalMoyuColors.current.divider, RoundedCornerShape(2.dp)))
        Box(Modifier.fillMaxWidth(fraction).height(3.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
        Box(Modifier.fillMaxWidth(fraction).height(18.dp), contentAlignment = Alignment.CenterEnd) {
            Box(Modifier.size(9.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(9.dp)))
        }
    }
}

@Composable
private fun <T> SegmentedChoices(values: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(4.dp)) {
        values.forEach { value ->
            Surface(onClick = { onSelect(value) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), color = if (value == selected) MaterialTheme.colorScheme.surface else Color.Transparent) {
                Text(label(value), Modifier.padding(vertical = 9.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

private fun themeName(theme: ReaderTheme) = when (theme) {
    ReaderTheme.LIGHT -> "浅色"
    ReaderTheme.DARK -> "深色"
    ReaderTheme.OLED -> "OLED"
    ReaderTheme.PAPER -> "纸张"
}

@Composable
private fun ReaderFooter(state: ReaderUiState, page: Int, pageCount: Int, showClock: Boolean, modifier: Modifier = Modifier) {
    val clock by produceState(initialValue = currentClock(), showClock) {
        if (!showClock) return@produceState
        while (true) {
            value = currentClock()
            delay(30_000)
        }
    }
    val percentage = ((state.chapterIndex + (page + 1f) / pageCount.coerceAtLeast(1)) / state.chapters.size.coerceAtLeast(1) * 100).toInt().coerceIn(0, 100)
    Row(modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("${state.chapterIndex + 1} / ${state.chapters.size}", style = MaterialTheme.typography.bodySmall, color = LocalMoyuColors.current.readerText.copy(alpha = .50f))
        Text("${page + 1} / $pageCount · $percentage%", style = MaterialTheme.typography.bodySmall, color = LocalMoyuColors.current.readerText.copy(alpha = .58f))
        if (showClock) Text(clock, style = MaterialTheme.typography.bodySmall, color = LocalMoyuColors.current.readerText.copy(alpha = .50f))
        else Spacer(Modifier.width(28.dp))
    }
}

private data class PageParagraph(val text: String, val isSourceParagraphStart: Boolean)

private fun pageParagraphs(text: String, chapterText: String, pageStart: Int): List<PageParagraph> = buildList {
    var localStart = 0
    while (localStart <= text.length) {
        val breakIndex = text.indexOf('\n', localStart)
        val localEnd = if (breakIndex >= 0) breakIndex else text.length
        val paragraph = text.substring(localStart, localEnd)
        if (paragraph.any { it != '\u200B' && !it.isWhitespace() }) {
            add(
                PageParagraph(
                    text = paragraph,
                    isSourceParagraphStart = ReaderTextFormatter.isParagraphStart(chapterText, pageStart + localStart),
                )
            )
        }
        if (breakIndex < 0) break
        localStart = breakIndex + 1
    }
}

@Composable
private fun highlightQuery(text: String, query: String) = run {
    val accent = MaterialTheme.colorScheme.primary
    val background = LocalMoyuColors.current.selection.copy(alpha = .52f)
    remember(text, query, accent, background) {
        buildAnnotatedString {
            append(text)
            if (query.isNotBlank()) {
                var start = text.indexOf(query, ignoreCase = true)
                while (start >= 0) {
                    addStyle(SpanStyle(color = accent, background = background, fontWeight = FontWeight.Bold), start, start + query.length)
                    start = text.indexOf(query, start + query.length, ignoreCase = true)
                }
            }
        }
    }
}

private fun Modifier.readerTapObserver(key: Any?, enabled: Boolean, onTap: () -> Unit): Modifier =
    if (!enabled) this else pointerInput(key, onTap) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var finalPosition = down.position
        var finalUptime = down.uptimeMillis
        var pressed: Boolean
        do {
            val event = awaitPointerEvent(PointerEventPass.Final)
            event.changes.firstOrNull { it.id == down.id }?.let {
                finalPosition = it.position
                finalUptime = it.uptimeMillis
            }
            pressed = event.changes.any { it.pressed }
        } while (pressed)
        val delta = finalPosition - down.position
        if (abs(delta.x) <= viewConfiguration.touchSlop && abs(delta.y) <= viewConfiguration.touchSlop && finalUptime - down.uptimeMillis < 550L) onTap()
    }
    }

private fun ReaderPanel.isSheetPanel(): Boolean = this in setOf(
    ReaderPanel.PROGRESS,
    ReaderPanel.SEARCH,
    ReaderPanel.BOOKMARKS,
    ReaderPanel.READING_SETTINGS,
    ReaderPanel.THEME,
    ReaderPanel.FONT,
    ReaderPanel.TTS,
)

private fun currentClock(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

private fun overallProgress(state: ReaderUiState): Float =
    ((state.chapterIndex + state.characterOffset.toFloat() / state.text.length.coerceAtLeast(1)) /
        state.chapters.size.coerceAtLeast(1)).coerceIn(0f, 1f)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
