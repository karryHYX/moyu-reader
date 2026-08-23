package com.moyu.reader.ui.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moyu.reader.model.ReaderTheme

object MoyuColors {
    val Paper0 = Color(0xFFFFFFFF)
    val Paper25 = Color(0xFFFCFBF7)
    val Paper50 = Color(0xFFF8F5ED)
    val Paper100 = Color(0xFFF1EBDD)
    val Ink950 = Color(0xFF151412)
    val Ink900 = Color(0xFF201E1B)
    val Ink800 = Color(0xFF34312D)
    val Gray100 = Color(0xFFEFEEEA)
    val Gray200 = Color(0xFFDEDDD7)
    val Gray300 = Color(0xFFC6C4BD)
    val Gray500 = Color(0xFF85827B)
    val Gray700 = Color(0xFF4E4B46)
    val Gray850 = Color(0xFF2A2927)
    val Gray950 = Color(0xFF11110F)
    val EditorialRed = Color(0xFFD83A2E)
    val EditorialRedDark = Color(0xFFB92D24)
    val SoftRed = Color(0xFFF2C9C3)
    val Teal = Color(0xFF3A8D82)
    val SoftTeal = Color(0xFFA9D5CE)
    val Success = Color(0xFF2E7D5B)
    val Warning = Color(0xFFB66A18)
    val Error = Color(0xFFBA2D2D)
}

@Immutable
data class MoyuExtendedColors(
    val readerBackground: Color,
    val readerText: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val divider: Color,
    val selection: Color,
)

val LocalMoyuColors = staticCompositionLocalOf {
    MoyuExtendedColors(
        readerBackground = MoyuColors.Paper25,
        readerText = MoyuColors.Ink900,
        textSecondary = MoyuColors.Gray700,
        textTertiary = MoyuColors.Gray500,
        divider = MoyuColors.Gray200,
        selection = MoyuColors.SoftRed,
    )
}

object MoyuSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
    val huge = 40.dp
    val giant = 48.dp
    val monumental = 64.dp
}

object MoyuMotion {
    const val Fast = 150
    const val Standard = 240
    const val Slow = 340
}

private val MoyuTypography = Typography(
    displayLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 42.sp,
        lineHeight = 50.sp,
    ),
    headlineLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 43.sp,
    ),
    headlineMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 27.sp,
        lineHeight = 35.sp,
    ),
    titleLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 29.sp,
    ),
    titleMedium = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 23.sp,
    ),
    bodyLarge = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, lineHeight = 27.sp),
    bodyMedium = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, lineHeight = 23.sp),
    labelLarge = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = .2.sp),
    labelMedium = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = .6.sp),
    bodySmall = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
)

@Composable
fun MoyuTheme(
    readerTheme: ReaderTheme,
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val actualTheme = readerTheme
    val target = palette(actualTheme)
    val duration = if (reducedMotion) 0 else MoyuMotion.Slow
    @Composable fun animated(targetColor: Color) = animateColorAsState(targetColor, tween(duration), label = "theme-color").value
    val scheme = target.scheme.copy(
        primary = animated(target.scheme.primary),
        onPrimary = animated(target.scheme.onPrimary),
        background = animated(target.scheme.background),
        onBackground = animated(target.scheme.onBackground),
        surface = animated(target.scheme.surface),
        onSurface = animated(target.scheme.onSurface),
        surfaceVariant = animated(target.scheme.surfaceVariant),
        onSurfaceVariant = animated(target.scheme.onSurfaceVariant),
        outlineVariant = animated(target.scheme.outlineVariant),
    )
    val extended = target.extended.copy(
        readerBackground = animated(target.extended.readerBackground),
        readerText = animated(target.extended.readerText),
        textSecondary = animated(target.extended.textSecondary),
        textTertiary = animated(target.extended.textTertiary),
        divider = animated(target.extended.divider),
        selection = animated(target.extended.selection),
    )
    CompositionLocalProvider(LocalMoyuColors provides extended) {
        MaterialTheme(colorScheme = scheme, typography = MoyuTypography, content = content)
    }
}

private data class Palette(val scheme: ColorScheme, val extended: MoyuExtendedColors)

private fun palette(theme: ReaderTheme): Palette = when (theme) {
    ReaderTheme.LIGHT -> Palette(
        lightColorScheme(
            primary = MoyuColors.EditorialRed,
            onPrimary = Color.White,
            primaryContainer = MoyuColors.SoftRed,
            onPrimaryContainer = MoyuColors.Ink950,
            background = MoyuColors.Paper25,
            onBackground = MoyuColors.Ink950,
            surface = MoyuColors.Paper0,
            onSurface = MoyuColors.Ink950,
            surfaceVariant = MoyuColors.Gray100,
            onSurfaceVariant = MoyuColors.Gray700,
            outline = MoyuColors.Gray500,
            outlineVariant = MoyuColors.Gray200,
            error = MoyuColors.Error,
        ),
        MoyuExtendedColors(MoyuColors.Paper25, MoyuColors.Ink900, MoyuColors.Gray700, MoyuColors.Gray500, MoyuColors.Gray200, MoyuColors.SoftRed),
    )
    ReaderTheme.DARK -> Palette(
        darkColorScheme(
            primary = MoyuColors.SoftRed,
            onPrimary = MoyuColors.Gray950,
            primaryContainer = Color(0xFF7E201B),
            background = MoyuColors.Gray950,
            onBackground = MoyuColors.Paper50,
            surface = MoyuColors.Gray850,
            onSurface = MoyuColors.Paper50,
            surfaceVariant = MoyuColors.Ink800,
            onSurfaceVariant = MoyuColors.Gray300,
            outline = MoyuColors.Gray500,
            outlineVariant = MoyuColors.Gray700,
        ),
        MoyuExtendedColors(MoyuColors.Gray950, MoyuColors.Paper100, MoyuColors.Gray300, MoyuColors.Gray500, MoyuColors.Gray700, Color(0xFF7E201B)),
    )
    ReaderTheme.OLED -> Palette(
        darkColorScheme(
            primary = MoyuColors.SoftTeal,
            onPrimary = Color.Black,
            primaryContainer = Color(0xFF23685F),
            background = Color.Black,
            onBackground = MoyuColors.Paper50,
            surface = MoyuColors.Gray950,
            onSurface = MoyuColors.Paper50,
            surfaceVariant = MoyuColors.Ink950,
            onSurfaceVariant = MoyuColors.Gray300,
            outline = MoyuColors.Gray500,
            outlineVariant = MoyuColors.Gray850,
        ),
        MoyuExtendedColors(Color.Black, MoyuColors.Paper100, MoyuColors.Gray300, MoyuColors.Gray500, MoyuColors.Gray850, Color(0xFF23685F)),
    )
    ReaderTheme.PAPER -> Palette(
        lightColorScheme(
            primary = MoyuColors.EditorialRedDark,
            onPrimary = Color.White,
            primaryContainer = MoyuColors.SoftRed,
            background = MoyuColors.Paper50,
            onBackground = MoyuColors.Ink900,
            surface = MoyuColors.Paper25,
            onSurface = MoyuColors.Ink900,
            surfaceVariant = MoyuColors.Paper100,
            onSurfaceVariant = MoyuColors.Gray700,
            outline = MoyuColors.Gray700,
            outlineVariant = MoyuColors.Gray300,
        ),
        MoyuExtendedColors(MoyuColors.Paper50, MoyuColors.Ink800, MoyuColors.Gray700, Color(0xFF6A6761), MoyuColors.Gray300, MoyuColors.SoftRed),
    )
}
