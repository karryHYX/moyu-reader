package com.moyu.reader.ui.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.absoluteValue

enum class MoyuGlyph { LIBRARY, IMPORT, SETTINGS, SORT, SEARCH, THEME, DIRECTORY, BOOKMARK, BACK, MORE, GRID, LIST, PROGRESS, AUDIO, FONT }

@Composable
fun MoyuGlyphIcon(
    glyph: MoyuGlyph,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    Canvas(modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()
        when (glyph) {
            MoyuGlyph.LIBRARY -> {
                drawLine(color, Offset(w * .24f, h * .2f), Offset(w * .24f, h * .82f), stroke, StrokeCap.Square)
                drawLine(accent, Offset(w * .5f, h * .12f), Offset(w * .5f, h * .82f), stroke, StrokeCap.Square)
                drawLine(color, Offset(w * .76f, h * .28f), Offset(w * .76f, h * .82f), stroke, StrokeCap.Square)
            }
            MoyuGlyph.IMPORT -> {
                drawLine(color, Offset(w * .5f, h * .12f), Offset(w * .5f, h * .64f), stroke, StrokeCap.Round)
                drawLine(accent, Offset(w * .31f, h * .46f), Offset(w * .5f, h * .66f), stroke, StrokeCap.Round)
                drawLine(accent, Offset(w * .69f, h * .46f), Offset(w * .5f, h * .66f), stroke, StrokeCap.Round)
                drawLine(color, Offset(w * .2f, h * .82f), Offset(w * .8f, h * .82f), stroke, StrokeCap.Round)
            }
            MoyuGlyph.SETTINGS -> {
                // Two quiet vertical controls read more cleanly at navigation size
                // than the former dense three-slider mark.
                drawLine(color, Offset(w * .36f, h * .18f), Offset(w * .36f, h * .82f), stroke, StrokeCap.Round)
                drawLine(color, Offset(w * .64f, h * .18f), Offset(w * .64f, h * .82f), stroke, StrokeCap.Round)
                drawCircle(accent, stroke * 1.65f, Offset(w * .36f, h * .39f), style = Stroke(stroke))
                drawCircle(accent, stroke * 1.65f, Offset(w * .64f, h * .63f), style = Stroke(stroke))
            }
            MoyuGlyph.SORT -> {
                listOf(.28f, .5f, .72f).forEachIndexed { index, y ->
                    val end = listOf(.76f, .62f, .48f)[index]
                    drawLine(color, Offset(w * .22f, h * y), Offset(w * end, h * y), stroke, StrokeCap.Round)
                    drawCircle(if (index == 1) accent else color, stroke * .9f, Offset(w * .18f, h * y))
                }
            }
            MoyuGlyph.SEARCH -> {
                drawCircle(color, w * .26f, Offset(w * .43f, h * .4f), style = Stroke(stroke))
                drawLine(accent, Offset(w * .62f, h * .59f), Offset(w * .84f, h * .82f), stroke, StrokeCap.Round)
            }
            MoyuGlyph.THEME -> {
                drawCircle(color, w * .34f, center, style = Stroke(stroke))
                val path = Path().apply {
                    moveTo(center.x, center.y - w * .34f)
                    arcTo(androidx.compose.ui.geometry.Rect(center.x - w * .34f, center.y - w * .34f, center.x + w * .34f, center.y + w * .34f), -90f, -180f, false)
                    close()
                }
                drawPath(path, accent)
            }
            MoyuGlyph.DIRECTORY -> {
                listOf(.25f, .5f, .75f).forEachIndexed { index, y ->
                    drawCircle(if (index == 1) accent else color, stroke, Offset(w * .2f, h * y))
                    drawLine(color, Offset(w * .34f, h * y), Offset(w * .84f, h * y), stroke, StrokeCap.Round)
                }
            }
            MoyuGlyph.BOOKMARK -> {
                val path = Path().apply {
                    moveTo(w * .28f, h * .12f); lineTo(w * .72f, h * .12f); lineTo(w * .72f, h * .86f)
                    lineTo(w * .5f, h * .68f); lineTo(w * .28f, h * .86f); close()
                }
                drawPath(path, color, style = Stroke(stroke))
                drawLine(accent, Offset(w * .62f, h * .14f), Offset(w * .62f, h * .38f), stroke, StrokeCap.Round)
            }
            MoyuGlyph.BACK -> {
                drawLine(color, Offset(w * .18f, h * .5f), Offset(w * .84f, h * .5f), stroke, StrokeCap.Round)
                drawLine(accent, Offset(w * .18f, h * .5f), Offset(w * .43f, h * .25f), stroke, StrokeCap.Round)
                drawLine(accent, Offset(w * .18f, h * .5f), Offset(w * .43f, h * .75f), stroke, StrokeCap.Round)
            }
            MoyuGlyph.MORE -> listOf(.24f, .5f, .76f).forEachIndexed { index, x -> drawCircle(if (index == 1) accent else color, stroke * 1.15f, Offset(w * x, h * .5f)) }
            MoyuGlyph.GRID -> for (x in listOf(.3f, .7f)) for (y in listOf(.3f, .7f)) drawRect(if (x == .3f && y == .3f) accent else color, Offset(w * (x - .12f), h * (y - .12f)), androidx.compose.ui.geometry.Size(w * .24f, h * .24f))
            MoyuGlyph.LIST -> listOf(.28f, .5f, .72f).forEachIndexed { index, y -> drawLine(if (index == 1) accent else color, Offset(w * .18f, h * y), Offset(w * .82f, h * y), stroke, StrokeCap.Round) }
            MoyuGlyph.PROGRESS -> {
                drawCircle(color, w * .34f, center, style = Stroke(stroke))
                drawArc(accent, -90f, 235f, false, topLeft = Offset(w * .16f, h * .16f), size = androidx.compose.ui.geometry.Size(w * .68f, h * .68f), style = Stroke(stroke * 1.45f, cap = StrokeCap.Round))
                drawLine(color, center, Offset(w * .5f, h * .29f), stroke, StrokeCap.Round)
                drawLine(accent, center, Offset(w * .67f, h * .57f), stroke, StrokeCap.Round)
            }
            MoyuGlyph.AUDIO -> {
                val speaker = Path().apply {
                    moveTo(w * .18f, h * .42f); lineTo(w * .34f, h * .42f); lineTo(w * .53f, h * .25f)
                    lineTo(w * .53f, h * .75f); lineTo(w * .34f, h * .58f); lineTo(w * .18f, h * .58f); close()
                }
                drawPath(speaker, color, style = Stroke(stroke, cap = StrokeCap.Round))
                drawArc(accent, -48f, 96f, false, topLeft = Offset(w * .43f, h * .30f), size = androidx.compose.ui.geometry.Size(w * .32f, h * .40f), style = Stroke(stroke, cap = StrokeCap.Round))
                drawArc(accent, -44f, 88f, false, topLeft = Offset(w * .43f, h * .19f), size = androidx.compose.ui.geometry.Size(w * .48f, h * .62f), style = Stroke(stroke, cap = StrokeCap.Round))
            }
            MoyuGlyph.FONT -> {
                drawLine(color, Offset(w * .22f, h * .22f), Offset(w * .78f, h * .22f), stroke, StrokeCap.Round)
                drawLine(accent, Offset(w * .5f, h * .22f), Offset(w * .5f, h * .80f), stroke * 1.25f, StrokeCap.Round)
                drawLine(color, Offset(w * .36f, h * .80f), Offset(w * .64f, h * .80f), stroke, StrokeCap.Round)
            }
        }
    }
}

@Composable
fun MoyuPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .965f else 1f, spring(dampingRatio = .72f, stiffness = 720f), label = "button-scale")
    val strokeWidth by animateDpAsState(if (pressed) 30.dp else 14.dp, tween(MoyuMotion.Fast), label = "stroke-width")
    val shape = if (pressed) RoundedCornerShape(8.dp, 15.dp, 15.dp, 8.dp) else RoundedCornerShape(12.dp)
    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = source,
        modifier = modifier.height(50.dp).scale(scale),
        shape = shape,
        color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (enabled) MaterialTheme.colorScheme.onPrimary else LocalMoyuColors.current.textTertiary,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Box(Modifier.width(strokeWidth).height(2.dp).background(MaterialTheme.colorScheme.onPrimary, RoundedCornerShape(2.dp)))
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

private val coverPalettes = listOf(
    Triple(Color(0xFFE8E0D0), Color(0xFF26211D), Color(0xFFB92D24)),
    Triple(Color(0xFFDCE3DE), Color(0xFF1B2923), Color(0xFF2E7D5B)),
    Triple(Color(0xFFE5E1E8), Color(0xFF29232E), Color(0xFF76558A)),
    Triple(Color(0xFFE7DDD7), Color(0xFF2B211C), Color(0xFFB66A18)),
    Triple(Color(0xFFD9E2E8), Color(0xFF19262D), Color(0xFF3A718D)),
    Triple(Color(0xFFE8E5D8), Color(0xFF2A281D), Color(0xFF74752D)),
    Triple(Color(0xFFE4DAD9), Color(0xFF2B1F20), Color(0xFFA24949)),
    Triple(Color(0xFFDADFE7), Color(0xFF202631), Color(0xFF4D638A)),
)

@Composable
fun GeneratedBookCover(
    title: String,
    author: String,
    key: String,
    modifier: Modifier = Modifier,
    progress: Float? = null,
) {
    val variant = key.hashCode().absoluteValue % coverPalettes.size
    val palette = coverPalettes[variant]
    Box(modifier.background(palette.first)) {
        Text("MOYU", Modifier.align(Alignment.TopEnd).padding(9.dp), color = palette.second.copy(alpha = .46f), fontSize = 7.sp, letterSpacing = 1.sp)
        when (variant) {
            0 -> {
                Box(Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 12.dp).width(2.dp).height(52.dp).background(palette.third))
                VerticalTitle(title, palette.second, Modifier.align(Alignment.Center))
            }
            1 -> Column(Modifier.align(Alignment.CenterStart).padding(16.dp)) {
                Box(Modifier.width(38.dp).height(3.dp).background(palette.third))
                Text(title.take(12), color = palette.second, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp, modifier = Modifier.padding(top = 10.dp))
            }
            2 -> {
                Box(Modifier.align(Alignment.Center).fillMaxWidth().height(1.dp).background(palette.second.copy(alpha = .22f)))
                Text(title.take(10), Modifier.align(Alignment.BottomStart).padding(start = 14.dp, end = 14.dp, bottom = 30.dp), color = palette.second, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 26.sp)
                Box(Modifier.align(Alignment.CenterStart).width(5.dp).height(74.dp).background(palette.third))
            }
            3 -> Box(Modifier.align(Alignment.Center).padding(14.dp).fillMaxSize().border(1.dp, palette.second.copy(alpha = .35f))) {
                VerticalTitle(title, palette.second, Modifier.align(Alignment.Center))
                Box(Modifier.align(Alignment.TopStart).size(8.dp).background(palette.third))
            }
            4 -> {
                Text((variant + 1).toString().padStart(2, '0'), Modifier.align(Alignment.Center).padding(bottom = 48.dp), color = palette.second.copy(alpha = .09f), fontSize = 64.sp, fontWeight = FontWeight.Bold)
                Text(title.take(12), Modifier.align(Alignment.Center).padding(horizontal = 14.dp, vertical = 32.dp), color = palette.second, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 26.sp, textAlign = TextAlign.Center)
            }
            5 -> Row(Modifier.align(Alignment.Center).padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                title.take(8).chunked(4).forEachIndexed { index, part ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { part.forEach { Text(it.toString(), color = if (index == 0) palette.second else palette.third, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 20.sp) } }
                }
            }
            6 -> {
                Box(Modifier.align(Alignment.TopStart).padding(top = 36.dp).fillMaxWidth(.72f).height(28.dp).background(palette.third.copy(alpha = .78f)))
                Text(title.take(10), Modifier.align(Alignment.CenterStart).padding(horizontal = 14.dp), color = palette.second, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 27.sp)
            }
            else -> {
                Column(Modifier.align(Alignment.Center).padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.width(54.dp).height(1.dp).background(palette.second.copy(alpha = .4f)))
                    Text(title.take(12), color = palette.second, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 25.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 14.dp))
                    Box(Modifier.width(18.dp).height(3.dp).background(palette.third))
                }
            }
        }
        Text(author.take(8), Modifier.align(Alignment.BottomStart).padding(10.dp), color = palette.second.copy(alpha = .65f), fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (progress != null) {
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(2.dp).background(palette.second.copy(alpha = .12f)))
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(progress.coerceIn(0f, 1f)).height(2.dp).background(palette.third))
        }
    }
}

@Composable
private fun VerticalTitle(title: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        title.take(7).forEach { char ->
            Text(char.toString(), color = color, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
fun EditorialSectionTitle(kicker: String, title: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(28.dp).height(3.dp).background(MaterialTheme.colorScheme.primary))
            Spacer(Modifier.width(12.dp))
            Text(kicker.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium)
    }
}
