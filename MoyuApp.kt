package com.moyu.reader.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.moyu.reader.AppContainer
import com.moyu.reader.data.preferences.AppPreferences
import com.moyu.reader.ui.book.BookDetailScreen
import com.moyu.reader.ui.book.BookDetailViewModel
import com.moyu.reader.ui.designsystem.LocalMoyuColors
import com.moyu.reader.ui.designsystem.MoyuGlyph
import com.moyu.reader.ui.designsystem.MoyuGlyphIcon
import com.moyu.reader.ui.designsystem.MoyuMotion
import com.moyu.reader.ui.importbook.ImportScreen
import com.moyu.reader.ui.importbook.ImportViewModel
import com.moyu.reader.ui.library.LibraryScreen
import com.moyu.reader.ui.library.LibraryViewModel
import com.moyu.reader.ui.onboarding.OnboardingScreen
import com.moyu.reader.ui.reader.ReaderScreen
import com.moyu.reader.ui.reader.ReaderViewModel
import com.moyu.reader.ui.settings.SettingsScreen

@Composable
fun MoyuApp(container: AppContainer, preferences: AppPreferences) {
    val navController = rememberNavController()
    val duration = if (preferences.reader.reducedMotion) 0 else MoyuMotion.Standard
    NavHost(
        navController = navController,
        startDestination = if (preferences.onboardingComplete) "main" else "onboarding",
        modifier = Modifier.fillMaxSize(),
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(duration)) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(duration)) },
        popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(duration)) },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(duration)) },
    ) {
        composable("onboarding") {
            OnboardingScreen(
                onComplete = {
                    navController.navigate("main") { popUpTo("onboarding") { inclusive = true } }
                },
                completeOnboarding = container.settings::completeOnboarding,
            )
        }
        composable("main") {
            MainShell(
                container = container,
                onOpenBook = { navController.navigate("book/$it") },
                onContinueReading = { navController.navigate("reader/$it") },
            )
        }
        composable(
            route = "book/{bookId}",
            arguments = listOf(navArgument("bookId") { type = NavType.StringType }),
        ) { entry ->
            val bookId = entry.arguments?.getString("bookId").orEmpty()
            val vm: BookDetailViewModel = viewModel(key = "book-$bookId", factory = viewModelFactory { BookDetailViewModel(bookId, container.library) })
            BookDetailScreen(vm, onBack = navController::popBackStack, onRead = { navController.navigate("reader/$bookId") })
        }
        composable(
            route = "reader/{bookId}",
            arguments = listOf(navArgument("bookId") { type = NavType.StringType }),
        ) { entry ->
            val bookId = entry.arguments?.getString("bookId").orEmpty()
            val vm: ReaderViewModel = viewModel(key = "reader-$bookId", factory = viewModelFactory { ReaderViewModel(bookId, container.library) })
            ReaderScreen(vm, preferences.reader, container.settings, onBack = navController::popBackStack)
        }
    }
}

private data class MainDestination(val label: String, val glyph: MoyuGlyph)

@Composable
private fun MainShell(
    container: AppContainer,
    onOpenBook: (String) -> Unit,
    onContinueReading: (String) -> Unit,
) {
    var selected by remember { mutableIntStateOf(0) }
    val destinations = remember {
        listOf(
            MainDestination("书架", MoyuGlyph.LIBRARY),
            MainDestination("导入", MoyuGlyph.IMPORT),
            MainDestination("设置", MoyuGlyph.SETTINGS),
        )
    }
    val libraryViewModel: LibraryViewModel = viewModel(factory = viewModelFactory { LibraryViewModel(container.library, container.settings) })
    val importViewModel: ImportViewModel = viewModel(factory = viewModelFactory { ImportViewModel(container.importer) })
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 0.dp) {
                Row(
                    Modifier.navigationBarsPadding().height(68.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    destinations.forEachIndexed { index, destination ->
                        Surface(
                            onClick = { selected = index },
                            color = Color.Transparent,
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(Modifier.height(58.dp), contentAlignment = Alignment.Center) {
                                androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    MoyuGlyphIcon(destination.glyph, Modifier.size(23.dp), color = if (selected == index) MaterialTheme.colorScheme.onSurface else LocalMoyuColors.current.textTertiary)
                                    Text(destination.label, style = MaterialTheme.typography.labelMedium, color = if (selected == index) MaterialTheme.colorScheme.onSurface else LocalMoyuColors.current.textTertiary)
                                    Box(
                                        Modifier.padding(top = 4.dp).width(18.dp).height(2.dp)
                                            .background(if (selected == index) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(2.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            AnimatedContent(
                targetState = selected,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    (fadeIn(tween(190)) + slideInHorizontally(tween(240)) { it * direction / 9 })
                        .togetherWith(fadeOut(tween(140)) + slideOutHorizontally(tween(210)) { -it * direction / 12 })
                },
                label = "main-destination",
            ) { destination ->
                when (destination) {
                    0 -> LibraryScreen(libraryViewModel, onOpenBook, onContinueReading, onImport = { selected = 1 })
                    1 -> ImportScreen(importViewModel, onViewLibrary = { selected = 0 })
                    else -> SettingsScreen(container.settings, container.library, container.fonts, container.backup)
                }
            }
        }
    }
}
