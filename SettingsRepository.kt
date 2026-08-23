package com.moyu.reader.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.moyu.reader.model.LibraryLayout
import com.moyu.reader.model.LibrarySort
import com.moyu.reader.model.PageAnimation
import com.moyu.reader.model.ReaderMode
import com.moyu.reader.model.ReaderPreferences
import com.moyu.reader.model.ReaderOrientation
import com.moyu.reader.model.ReaderTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.moyuDataStore by preferencesDataStore(name = "moyu_settings")

data class AppPreferences(
    val onboardingComplete: Boolean = false,
    val libraryLayout: LibraryLayout = LibraryLayout.GRID,
    val librarySort: LibrarySort = LibrarySort.RECENT,
    val reader: ReaderPreferences = ReaderPreferences(),
)

class SettingsRepository(private val context: Context) {
    val preferences: Flow<AppPreferences> = context.moyuDataStore.data.map { values ->
        AppPreferences(
            onboardingComplete = values[Keys.ONBOARDING] ?: false,
            libraryLayout = values[Keys.LIBRARY_LAYOUT].enumOrDefault(LibraryLayout.GRID),
            librarySort = values[Keys.LIBRARY_SORT].enumOrDefault(LibrarySort.RECENT),
            reader = ReaderPreferences(
                theme = values[Keys.THEME].enumOrDefault(ReaderTheme.LIGHT),
                mode = values[Keys.READER_MODE].enumOrDefault(ReaderMode.PAGED),
                pageAnimation = values[Keys.PAGE_ANIMATION].enumOrDefault(PageAnimation.COVER),
                pageTurnDurationMs = values[Keys.PAGE_TURN_DURATION] ?: 300,
                orientation = values[Keys.ORIENTATION].enumOrDefault(ReaderOrientation.SYSTEM),
                fontSizeSp = values[Keys.FONT_SIZE] ?: 19f,
                fontWeight = values[Keys.FONT_WEIGHT] ?: 400,
                lineHeightMultiplier = values[Keys.LINE_HEIGHT] ?: 1.68f,
                paragraphSpacingDp = values[Keys.PARAGRAPH_SPACING] ?: 2f,
                horizontalMarginDp = values[Keys.HORIZONTAL_MARGIN] ?: 24f,
                firstLineIndentEm = values[Keys.FIRST_LINE_INDENT] ?: 0f,
                justified = values[Keys.JUSTIFIED] ?: false,
                brightness = values[Keys.BRIGHTNESS] ?: -1f,
                keepScreenOn = values[Keys.KEEP_SCREEN_ON] ?: false,
                customFontPath = values[Keys.CUSTOM_FONT],
                reducedMotion = values[Keys.REDUCED_MOTION] ?: false,
                ttsRate = values[Keys.TTS_RATE] ?: 1f,
                showReaderClock = values[Keys.SHOW_READER_CLOCK] ?: true,
                volumeKeyPageTurn = values[Keys.VOLUME_KEY_PAGE_TURN] ?: true,
            ),
        )
    }

    suspend fun completeOnboarding() = update { it[Keys.ONBOARDING] = true }
    suspend fun setLibraryLayout(value: LibraryLayout) = update { it[Keys.LIBRARY_LAYOUT] = value.name }
    suspend fun setLibrarySort(value: LibrarySort) = update { it[Keys.LIBRARY_SORT] = value.name }
    suspend fun setTheme(value: ReaderTheme) = update { it[Keys.THEME] = value.name }
    suspend fun setReaderMode(value: ReaderMode) = update { it[Keys.READER_MODE] = value.name }
    suspend fun setPageAnimation(value: PageAnimation) = update { it[Keys.PAGE_ANIMATION] = value.name }
    suspend fun setPageTurnDuration(value: Int) = update { it[Keys.PAGE_TURN_DURATION] = value.coerceIn(120, 900) }
    suspend fun setOrientation(value: ReaderOrientation) = update { it[Keys.ORIENTATION] = value.name }
    suspend fun setFontSize(value: Float) = update { it[Keys.FONT_SIZE] = value.coerceIn(14f, 34f) }
    suspend fun setFontWeight(value: Int) = update { it[Keys.FONT_WEIGHT] = value.coerceIn(300, 700) }
    suspend fun setLineHeight(value: Float) = update { it[Keys.LINE_HEIGHT] = value.coerceIn(1.3f, 2.4f) }
    suspend fun setParagraphSpacing(value: Float) = update { it[Keys.PARAGRAPH_SPACING] = value.coerceIn(0f, 28f) }
    suspend fun setHorizontalMargin(value: Float) = update { it[Keys.HORIZONTAL_MARGIN] = value.coerceIn(16f, 56f) }
    suspend fun setFirstLineIndent(value: Float) = update { it[Keys.FIRST_LINE_INDENT] = value.coerceIn(0f, 4f) }
    suspend fun setJustified(value: Boolean) = update { it[Keys.JUSTIFIED] = value }
    suspend fun setBrightness(value: Float) = update { it[Keys.BRIGHTNESS] = value.coerceIn(-1f, 1f) }
    suspend fun setKeepScreenOn(value: Boolean) = update { it[Keys.KEEP_SCREEN_ON] = value }
    suspend fun setReducedMotion(value: Boolean) = update { it[Keys.REDUCED_MOTION] = value }
    suspend fun setTtsRate(value: Float) = update { it[Keys.TTS_RATE] = value.coerceIn(.6f, 1.8f) }
    suspend fun setShowReaderClock(value: Boolean) = update { it[Keys.SHOW_READER_CLOCK] = value }
    suspend fun setVolumeKeyPageTurn(value: Boolean) = update { it[Keys.VOLUME_KEY_PAGE_TURN] = value }
    suspend fun setCustomFont(path: String?) = update {
        if (path == null) it.remove(Keys.CUSTOM_FONT) else it[Keys.CUSTOM_FONT] = path
    }

    private suspend inline fun update(crossinline block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.moyuDataStore.edit { block(it) }
    }

    private inline fun <reified T : Enum<T>> String?.enumOrDefault(default: T): T =
        this?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

    private object Keys {
        val ONBOARDING = booleanPreferencesKey("onboarding_complete")
        val LIBRARY_LAYOUT = stringPreferencesKey("library_layout")
        val LIBRARY_SORT = stringPreferencesKey("library_sort")
        val THEME = stringPreferencesKey("theme")
        val READER_MODE = stringPreferencesKey("reader_mode")
        val PAGE_ANIMATION = stringPreferencesKey("page_animation")
        val PAGE_TURN_DURATION = intPreferencesKey("page_turn_duration")
        val ORIENTATION = stringPreferencesKey("orientation")
        val FONT_SIZE = floatPreferencesKey("font_size")
        val FONT_WEIGHT = intPreferencesKey("font_weight")
        val LINE_HEIGHT = floatPreferencesKey("line_height")
        val PARAGRAPH_SPACING = floatPreferencesKey("paragraph_spacing")
        val HORIZONTAL_MARGIN = floatPreferencesKey("horizontal_margin")
        val FIRST_LINE_INDENT = floatPreferencesKey("first_line_indent")
        val JUSTIFIED = booleanPreferencesKey("justified")
        val BRIGHTNESS = floatPreferencesKey("brightness")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val CUSTOM_FONT = stringPreferencesKey("custom_font")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val TTS_RATE = floatPreferencesKey("tts_rate")
        val SHOW_READER_CLOCK = booleanPreferencesKey("show_reader_clock")
        val VOLUME_KEY_PAGE_TURN = booleanPreferencesKey("volume_key_page_turn")
    }
}
