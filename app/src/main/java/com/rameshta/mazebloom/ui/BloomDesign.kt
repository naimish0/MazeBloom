package com.rameshta.mazebloom.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val BloomCardShape = RoundedCornerShape(26.dp)
internal val BloomSmallShape = RoundedCornerShape(17.dp)

@Composable
internal fun GardenBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val background = MaterialTheme.colorScheme.background
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    Box(modifier.fillMaxSize().background(background)) {
        Canvas(Modifier.fillMaxSize().clearAndSetSemantics { }) {
            drawCircle(
                color = primary.copy(alpha = .075f),
                radius = size.minDimension * .62f,
                center = Offset(size.width * .92f, -size.height * .04f),
            )
            drawCircle(
                color = secondary.copy(alpha = .055f),
                radius = size.minDimension * .48f,
                center = Offset(-size.width * .12f, size.height * .88f),
            )
            repeat(11) { index ->
                val x = size.width * (((index * 37 + 11) % 100) / 100f)
                val y = size.height * (((index * 61 + 8) % 100) / 100f)
                drawCircle(primary.copy(alpha = .09f), 2.5f + index % 3, Offset(x, y))
                if (index % 2 == 0) {
                    drawLine(
                        primary.copy(alpha = .055f),
                        Offset(x, y),
                        Offset(x + size.width * .09f, y - size.height * .055f),
                        1.4f,
                    )
                }
            }
        }
        content()
    }
}

@Composable
internal fun BloomPanel(
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = BloomCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = .18f)),
    ) { content() }
}

@Composable
internal fun BloomClickablePanel(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit,
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = BloomCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .72f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp, pressedElevation = 0.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = if (enabled) .2f else .08f)),
    ) { content() }
}

@Composable
internal fun BloomPill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    filled: Boolean = true,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = if (filled) color.copy(alpha = .13f) else Color.Transparent,
        border = BorderStroke(1.dp, color.copy(alpha = .25f)),
    ) {
        Text(
            text = text.uppercase(),
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun BloomProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Box(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
            .padding(3.dp),
    ) {
        if (progress > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .background(
                        Brush.horizontalGradient(listOf(color.copy(alpha = .72f), color)),
                        CircleShape,
                    )
                    .padding(vertical = 3.dp),
            )
        }
    }
}

@Composable
internal fun BloomMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
internal fun ModeGlyph(
    symbol: String,
    accent: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier
            .size(52.dp)
            .background(accent.copy(alpha = if (enabled) .14f else .07f), RoundedCornerShape(18.dp))
            .border(1.dp, accent.copy(alpha = if (enabled) .22f else .09f), RoundedCornerShape(18.dp))
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, fontSize = 24.sp, color = if (enabled) accent else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun BloomMark(modifier: Modifier = Modifier, compact: Boolean = false) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val surface = MaterialTheme.colorScheme.surface
    Canvas(modifier.size(if (compact) 36.dp else 96.dp).clearAndSetSemantics { }) {
        val center = Offset(size.width / 2f, size.height / 2f)
        if (!compact) drawCircle(primary.copy(alpha = .09f), size.minDimension * .48f, center)
        repeat(6) { index ->
            val angle = Math.toRadians(index * 60.0 - 90.0)
            val px = center.x + kotlin.math.cos(angle).toFloat() * size.minDimension * .2f
            val py = center.y + kotlin.math.sin(angle).toFloat() * size.minDimension * .2f
            drawOval(
                color = if (index % 2 == 0) primary else secondary,
                topLeft = Offset(px - size.minDimension * .105f, py - size.minDimension * .17f),
                size = Size(size.minDimension * .21f, size.minDimension * .34f),
            )
        }
        drawCircle(tertiary, size.minDimension * .13f, center)
        drawCircle(surface, size.minDimension * .045f, center)
    }
}

@Composable
internal fun BloomLeafArt(modifier: Modifier = Modifier, accent: Color = MaterialTheme.colorScheme.primary) {
    val secondary = MaterialTheme.colorScheme.secondary
    Canvas(modifier.clearAndSetSemantics { }) {
        val stem = Path().apply {
            moveTo(size.width * .2f, size.height * .92f)
            cubicTo(size.width * .42f, size.height * .68f, size.width * .48f, size.height * .36f, size.width * .78f, size.height * .1f)
        }
        drawPath(stem, accent.copy(alpha = .48f), style = Stroke(size.minDimension * .035f))
        listOf(.3f to .7f, .44f to .52f, .59f to .34f, .72f to .18f).forEachIndexed { index, (x, y) ->
            drawOval(
                if (index % 2 == 0) accent.copy(alpha = .38f) else secondary.copy(alpha = .32f),
                Offset(size.width * x, size.height * y),
                Size(size.width * .25f, size.height * .12f),
            )
        }
    }
}
