package com.moyu.reader.model

import androidx.compose.runtime.Immutable

enum class BookFormat { TXT, EPUB }
enum class ReaderTheme { LIGHT, DARK, OLED, PAPER }
enum class ReaderMode { PAGED, SCROLL }
enum class PageAnimation { INSTANT, SLIDE, FADE, COVER, PAPER }
enum class ReaderOrientation { SYSTEM, PORTRAIT, LANDSCAPE }
enum class LibraryLayout { GRID, LIST }
enum class LibrarySort { RECENT, ADDED, TITLE, AUTHOR, PROGRESS }
enum class LibraryFilter { ALL, UNREAD, READING, FINISHED, FAVORITE }

@Immutable
data class Book(
    val id: String,
    val title: String,
    val author: String,
    val format: BookFormat,
    val originalFileName: String,
    val fileSize: Long,
    val charset: String?,
    val addedAt: Long,
    val lastReadAt: Long?,
    val totalCharacters: Long,
    val chapterCount: Int,
    val readingProgress: Float,
    val currentChapter: Int,
    val currentPosition: Int,
    val coverPath: String?,
    val favorite: Boolean,
    val searchIndexed: Boolean,
)

@Immutable
data class Chapter(
    val id: Long,
    val bookId: String,
    val index: Int,
    val title: String,
    val locator: String,
    val byteOffset: Long,
    val byteLength: Long,
    val characterCount: Long,
    val synthetic: Boolean,
)

@Immutable
data class ReaderAnchor(
    val bookId: String,
    val chapterId: Long,
    val characterOffset: Int,
    val percentage: Float,
)

@Immutable
data class ReaderPreferences(
    val theme: ReaderTheme = ReaderTheme.LIGHT,
    val mode: ReaderMode = ReaderMode.PAGED,
    val pageAnimation: PageAnimation = PageAnimation.COVER,
    val pageTurnDurationMs: Int = 300,
    val orientation: ReaderOrientation = ReaderOrientation.SYSTEM,
    val fontSizeSp: Float = 19f,
    val fontWeight: Int = 400,
    val lineHeightMultiplier: Float = 1.68f,
    val paragraphSpacingDp: Float = 2f,
    val horizontalMarginDp: Float = 24f,
    val firstLineIndentEm: Float = 0f,
    val justified: Boolean = false,
    val brightness: Float = -1f,
    val keepScreenOn: Boolean = false,
    val customFontPath: String? = null,
    val reducedMotion: Boolean = false,
    val ttsRate: Float = 1f,
    val showReaderClock: Boolean = true,
    val volumeKeyPageTurn: Boolean = true,
)

@Immutable
data class SearchHit(
    val chapterId: Long,
    val chapterTitle: String,
    val characterOffset: Int,
    val excerpt: String,
)

@Immutable
data class Bookmark(
    val id: Long,
    val bookId: String,
    val chapterId: Long,
    val characterOffset: Int,
    val excerpt: String,
    val createdAt: Long,
)

@Immutable
data class ReadingSummary(
    val totalMillis: Long = 0,
    val sessions: Int = 0,
    val charactersRead: Long = 0,
)
