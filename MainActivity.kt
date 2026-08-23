package com.moyu.reader

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.view.WindowCompat
import com.moyu.reader.model.ReaderTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moyu.reader.data.preferences.AppPreferences
import com.moyu.reader.ui.MoyuApp
import com.moyu.reader.ui.designsystem.MoyuTheme
import com.moyu.reader.ui.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

class MainActivity : ComponentActivity() {
    private var volumePageHandler: ((keyCode: Int) -> Boolean)? = null

    fun setVolumePageHandler(handler: ((keyCode: Int) -> Boolean)?) {
        volumePageHandler = handler
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val isVolumeKey = keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        val handler = volumePageHandler
        if (isVolumeKey && handler != null) {
            if (event.repeatCount == 0) handler(keyCode)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        val isVolumeKey = keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        if (isVolumeKey && volumePageHandler != null) return true
        return super.onKeyUp(keyCode, event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as MoyuApplication).container
        setContent {
            val appViewModel: AppViewModel = viewModel(factory = viewModelFactory { AppViewModel(container) })
            val state by appViewModel.state.collectAsStateWithLifecycle()
            val preferences = state.preferences
            MoyuTheme(
                readerTheme = preferences.reader.theme,
                reducedMotion = preferences.reader.reducedMotion,
            ) {
                DisposableEffect(preferences.reader.theme) {
                    val lightBars = preferences.reader.theme == ReaderTheme.LIGHT || preferences.reader.theme == ReaderTheme.PAPER
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = lightBars
                        isAppearanceLightNavigationBars = lightBars
                    }
                    onDispose { }
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                ) {
                    if (state.loaded) MoyuApp(container, preferences)
                }
            }
        }
    }
}

data class AppUiState(val loaded: Boolean = false, val preferences: AppPreferences = AppPreferences())

class AppViewModel(container: AppContainer) : ViewModel() {
    val state: StateFlow<AppUiState> = container.settings.preferences
        .map { AppUiState(true, it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppUiState())
}
