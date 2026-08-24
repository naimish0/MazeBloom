package com.rameshta.mazebloom.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.rameshta.mazebloom.core.Direction
import com.rameshta.mazebloom.data.CompanionDefinition
import com.rameshta.mazebloom.data.CompanionFamily
import com.rameshta.mazebloom.data.CompanionMotion
import com.rameshta.mazebloom.data.CompanionTrait
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun CompanionAvatar(
    companion: CompanionDefinition,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false,
    animate: Boolean = false,
) {
    val transition = rememberInfiniteTransition(label = "companion-${companion.id}")
    val animatedPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_250, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "companion-motion-${companion.id}",
    )
    Canvas(
        modifier.semantics { contentDescription = companion.displayName },
    ) {
        drawCompanionCharacter(
            companion = companion,
            center = center,
            diameter = size.minDimension * .8f,
            phase = if (animate && !reducedMotion) animatedPhase else .5f,
            traveling = false,
            direction = null,
        )
    }
}

internal fun DrawScope.drawCompanionCharacter(
    companion: CompanionDefinition,
    center: Offset,
    diameter: Float,
    phase: Float,
    traveling: Boolean,
    direction: Direction?,
) {
    val wave = sin(phase * PI.toFloat() * 2f)
    val bob = when (companion.motion) {
        CompanionMotion.HOVER, CompanionMotion.FLAP -> wave * diameter * .055f
        CompanionMotion.HOP -> -kotlin.math.abs(wave) * diameter * .075f
        CompanionMotion.SWIM, CompanionMotion.PULSE -> wave * diameter * .025f
        else -> wave * diameter * .018f
    }
    val travelStretch = if (traveling) .12f else 0f
    val horizontal = direction?.dx?.let { it != 0 } == true
    val scaleX = 1f + if (horizontal) travelStretch else -travelStretch * .35f
    val scaleY = 1f + if (horizontal) -travelStretch * .35f else travelStretch
    val tilt = if (traveling) ((direction?.dx ?: 0) * 6f + (direction?.dy ?: 0) * -4f) else wave * 2f
    val artCenter = center.copy(y = center.y + bob)

    drawOval(
        color = Color.Black.copy(alpha = .18f),
        topLeft = Offset(center.x - diameter * .31f, center.y + diameter * .29f),
        size = Size(diameter * .62f, diameter * .18f),
    )
    withTransform({
        translate(artCenter.x, artCenter.y)
        rotate(tilt, Offset.Zero)
        scale(scaleX, scaleY, Offset.Zero)
    }) {
        when (companion.family) {
            CompanionFamily.MAMMAL -> drawMammal(companion, diameter, wave)
            CompanionFamily.BIRD -> drawBird(companion, diameter, wave)
            CompanionFamily.INSECT -> drawInsect(companion, diameter, wave)
            CompanionFamily.GARDEN -> drawGardenCreature(companion, diameter, wave)
            CompanionFamily.WATER -> drawWaterCreature(companion, diameter, wave)
        }
    }
}

private fun DrawScope.drawMammal(companion: CompanionDefinition, d: Float, wave: Float) {
    val primary = companion.primaryColor()
    val secondary = companion.secondaryColor()
    val accent = companion.accentColor()
    val outline = Color(0xFF26362E).copy(alpha = .58f)
    val traits = companion.traits

    if (CompanionTrait.QUILLS in traits) {
        val quills = Path().apply {
            moveTo(-d * .34f, d * .12f)
            lineTo(-d * .42f, -d * .08f); lineTo(-d * .27f, -d * .09f)
            lineTo(-d * .30f, -d * .28f); lineTo(-d * .13f, -d * .18f)
            lineTo(0f, -d * .38f); lineTo(d * .1f, -d * .18f)
            lineTo(d * .3f, -d * .28f); lineTo(d * .27f, -d * .06f)
            lineTo(d * .42f, d * .02f); lineTo(d * .3f, d * .18f)
            close()
        }
        drawPath(quills, accent)
    }
    if (CompanionTrait.FLUFFY_TAIL in traits) {
        drawCircle(outline, d * .28f, Offset(d * .34f, d * .03f))
        drawCircle(primary, d * .255f, Offset(d * .34f, d * .03f))
        drawCircle(accent.copy(alpha = .72f), d * .14f, Offset(d * .38f, d * .01f))
    }
    if (CompanionTrait.WOOL in traits) {
        listOf(
            Offset(-.27f, -.18f), Offset(-.13f, -.3f), Offset(.04f, -.32f), Offset(.21f, -.25f),
            Offset(.3f, -.08f), Offset(.28f, .12f), Offset(.14f, .26f), Offset(-.08f, .28f),
            Offset(-.27f, .17f), Offset(-.34f, -.01f),
        ).forEach { offset -> drawCircle(secondary, d * .17f, Offset(offset.x * d, offset.y * d)) }
    }
    if (CompanionTrait.MANE in traits) drawCircle(accent, d * .42f, Offset(0f, d * .015f))

    when {
        CompanionTrait.LONG_EARS in traits -> {
            drawOval(primary, Offset(-d * .28f, -d * .49f), Size(d * .2f, d * .44f))
            drawOval(primary, Offset(d * .08f, -d * .49f), Size(d * .2f, d * .44f))
            drawOval(accent.copy(alpha = .65f), Offset(-d * .235f, -d * .43f), Size(d * .1f, d * .29f))
            drawOval(accent.copy(alpha = .65f), Offset(d * .135f, -d * .43f), Size(d * .1f, d * .29f))
        }
        CompanionTrait.FLOPPY_EARS in traits -> {
            drawOval(primary, Offset(-d * .43f, -d * .23f), Size(d * .23f, d * .45f))
            drawOval(primary, Offset(d * .2f, -d * .23f), Size(d * .23f, d * .45f))
        }
        CompanionTrait.POINT_EARS in traits -> {
            drawPath(Path().apply { moveTo(-d * .34f, -d * .12f); lineTo(-d * .27f, -d * .46f); lineTo(-d * .06f, -d * .23f); close() }, primary)
            drawPath(Path().apply { moveTo(d * .34f, -d * .12f); lineTo(d * .27f, -d * .46f); lineTo(d * .06f, -d * .23f); close() }, primary)
        }
        else -> {
            drawCircle(primary, d * .18f, Offset(-d * .25f, -d * .19f))
            drawCircle(primary, d * .18f, Offset(d * .25f, -d * .19f))
            drawCircle(accent.copy(alpha = .45f), d * .09f, Offset(-d * .25f, -d * .19f))
            drawCircle(accent.copy(alpha = .45f), d * .09f, Offset(d * .25f, -d * .19f))
        }
    }

    if (CompanionTrait.ANTLERS in traits) {
        listOf(-1f, 1f).forEach { side ->
            val x = side * d * .2f
            drawLine(accent, Offset(x, -d * .25f), Offset(side * d * .31f, -d * .51f), d * .035f)
            drawLine(accent, Offset(side * d * .26f, -d * .39f), Offset(side * d * .39f, -d * .43f), d * .028f)
            drawLine(accent, Offset(side * d * .28f, -d * .44f), Offset(side * d * .21f, -d * .53f), d * .028f)
        }
    }
    if (CompanionTrait.HORNS in traits) {
        drawPath(Path().apply { moveTo(-d * .25f, -d * .23f); lineTo(-d * .18f, -d * .48f); lineTo(-d * .04f, -d * .24f); close() }, accent)
        drawPath(Path().apply { moveTo(d * .25f, -d * .23f); lineTo(d * .18f, -d * .48f); lineTo(d * .04f, -d * .24f); close() }, accent)
    }

    drawOval(primary, Offset(-d * .37f, -d * .3f), Size(d * .74f, d * .72f))
    drawOval(outline, Offset(-d * .37f, -d * .3f), Size(d * .74f, d * .72f), style = Stroke(d * .025f))
    if (CompanionTrait.PATCHES in traits) {
        drawOval(secondary, Offset(-d * .27f, -d * .2f), Size(d * .24f, d * .28f))
        drawOval(secondary.copy(alpha = .78f), Offset(d * .1f, d * .04f), Size(d * .2f, d * .2f))
    }
    if (CompanionTrait.STRIPES in traits) {
        repeat(3) { index ->
            val x = (index - 1) * d * .13f
            drawLine(accent, Offset(x - d * .045f, -d * .28f), Offset(x, -d * .12f), d * .045f)
        }
    }
    if (CompanionTrait.SPOTS in traits) {
        drawCircle(accent.copy(alpha = .7f), d * .04f, Offset(-d * .22f, d * .1f))
        drawCircle(accent.copy(alpha = .7f), d * .035f, Offset(d * .24f, d * .14f))
    }
    if (CompanionTrait.MASK in traits) {
        drawOval(accent.copy(alpha = .82f), Offset(-d * .3f, -d * .14f), Size(d * .6f, d * .23f))
        drawOval(primary, Offset(-d * .055f, -d * .13f), Size(d * .11f, d * .2f))
    }

    val eyeY = -d * .04f
    drawEye(Offset(-d * .15f, eyeY), d, outline)
    drawEye(Offset(d * .15f, eyeY), d, outline)
    drawOval(secondary, Offset(-d * .18f, d * .045f), Size(d * .36f, d * .23f))
    if (CompanionTrait.SNOUT in traits) {
        drawCircle(outline, d * .026f, Offset(-d * .065f, d * .12f))
        drawCircle(outline, d * .026f, Offset(d * .065f, d * .12f))
    } else {
        drawCircle(outline, d * .045f, Offset(0f, d * .1f))
    }
    drawArc(outline, 18f, 54f, false, Offset(-d * .08f, d * .09f), Size(d * .08f, d * .09f), style = Stroke(d * .018f))
    drawArc(outline, 108f, 54f, false, Offset(0f, d * .09f), Size(d * .08f, d * .09f), style = Stroke(d * .018f))

    if (CompanionTrait.TRUNK in traits) {
        val trunk = Path().apply {
            moveTo(-d * .06f, d * .11f)
            quadraticTo(-d * .08f, d * .39f, d * .08f, d * .36f)
            quadraticTo(d * .14f, d * .33f, d * .1f, d * .27f)
        }
        drawPath(trunk, secondary, style = Stroke(d * .1f))
    }
    if (companion.id == "meadow_mouse" || companion.id == "garden_cat") {
        listOf(-1f, 1f).forEach { side ->
            drawLine(outline.copy(alpha = .65f), Offset(side * d * .08f, d * .12f), Offset(side * d * .36f, d * (.08f + wave * .01f)), d * .012f)
            drawLine(outline.copy(alpha = .65f), Offset(side * d * .08f, d * .15f), Offset(side * d * .34f, d * .2f), d * .012f)
        }
    }
}

private fun DrawScope.drawBird(companion: CompanionDefinition, d: Float, wave: Float) {
    val primary = companion.primaryColor()
    val secondary = companion.secondaryColor()
    val accent = companion.accentColor()
    val outline = Color(0xFF26362E).copy(alpha = .58f)
    val flap = .7f + kotlin.math.abs(wave) * .45f

    if (CompanionTrait.FAN_TAIL in companion.traits) {
        repeat(5) { index ->
            val x = (index - 2) * d * .11f
            drawOval(secondary.copy(alpha = .9f), Offset(x - d * .1f, d * .04f), Size(d * .2f, d * .42f))
            drawCircle(accent, d * .04f, Offset(x, d * .31f))
        }
    }
    drawOval(primary, Offset(-d * .28f, -d * .12f), Size(d * .56f, d * .62f))
    if (CompanionTrait.WINGS in companion.traits) {
        drawOval(secondary, Offset(-d * (.39f + flap * .04f), -d * .02f), Size(d * .27f, d * .4f * flap))
        drawOval(secondary, Offset(d * (.12f + flap * .04f), -d * .02f), Size(d * .27f, d * .4f * flap))
    }
    drawCircle(primary, d * .27f, Offset(0f, -d * .2f))
    if (CompanionTrait.CREST in companion.traits) {
        repeat(3) { index ->
            drawOval(accent, Offset((index - 1) * d * .08f - d * .05f, -d * (.52f - index * .025f)), Size(d * .1f, d * .25f))
        }
    }
    if (CompanionTrait.OWL_EYES in companion.traits) {
        drawCircle(secondary, d * .13f, Offset(-d * .12f, -d * .23f))
        drawCircle(secondary, d * .13f, Offset(d * .12f, -d * .23f))
    }
    drawEye(Offset(-d * .105f, -d * .22f), d, outline)
    drawEye(Offset(d * .105f, -d * .22f), d, outline)
    val beakLength = if (CompanionTrait.LONG_BEAK in companion.traits) d * .29f else d * .16f
    drawPath(Path().apply {
        moveTo(0f, -d * .12f)
        lineTo(beakLength, -d * .06f)
        lineTo(0f, d * .005f)
        close()
    }, accent)
    drawOval(outline, Offset(-d * .28f, -d * .12f), Size(d * .56f, d * .62f), style = Stroke(d * .022f))
    if (CompanionTrait.LONG_LEGS in companion.traits) {
        drawLine(accent, Offset(-d * .09f, d * .43f), Offset(-d * .09f, d * .56f), d * .025f)
        drawLine(accent, Offset(d * .09f, d * .43f), Offset(d * .09f, d * .56f), d * .025f)
    }
}

private fun DrawScope.drawInsect(companion: CompanionDefinition, d: Float, wave: Float) {
    val primary = companion.primaryColor()
    val secondary = companion.secondaryColor()
    val accent = companion.accentColor()
    val outline = Color(0xFF26362E).copy(alpha = .62f)
    val wingScale = .7f + kotlin.math.abs(wave) * .45f

    if (CompanionTrait.FOUR_WINGS in companion.traits) {
        listOf(-1f, 1f).forEach { side ->
            drawOval(secondary.copy(alpha = .78f), Offset(side * d * .08f - if (side < 0) d * .34f else 0f, -d * .25f), Size(d * .3f * wingScale, d * .32f))
            drawOval(secondary.copy(alpha = .66f), Offset(side * d * .1f - if (side < 0) d * .31f else -d * .01f, d * .02f), Size(d * .25f * wingScale, d * .28f))
        }
    } else if (CompanionTrait.WINGS in companion.traits) {
        drawOval(secondary.copy(alpha = .72f), Offset(-d * .37f, -d * .13f), Size(d * .3f * wingScale, d * .42f))
        drawOval(secondary.copy(alpha = .72f), Offset(d * .07f, -d * .13f), Size(d * .3f * wingScale, d * .42f))
    }

    if (CompanionTrait.SEGMENTS in companion.traits || CompanionTrait.LONG_BODY in companion.traits) {
        repeat(if (CompanionTrait.LONG_BODY in companion.traits) 5 else 3) { index ->
            val y = -d * .12f + index * d * .13f
            drawCircle(if (index % 2 == 0) primary else secondary, d * .14f, Offset(0f, y))
        }
    } else {
        drawOval(primary, Offset(-d * .24f, -d * .22f), Size(d * .48f, d * .66f))
    }
    if (CompanionTrait.STRIPES in companion.traits) {
        repeat(3) { index -> drawLine(outline, Offset(-d * .2f, index * d * .13f - d * .04f), Offset(d * .2f, index * d * .13f - d * .04f), d * .045f) }
    }
    if (CompanionTrait.SPOTS in companion.traits) {
        listOf(Offset(-d * .1f, -d * .02f), Offset(d * .11f, d * .05f), Offset(-d * .09f, d * .2f)).forEach { drawCircle(outline, d * .045f, it) }
    }
    if (CompanionTrait.GLOW in companion.traits) {
        drawCircle(accent.copy(alpha = .24f), d * .24f, Offset(0f, d * .3f))
        drawCircle(accent, d * .11f, Offset(0f, d * .3f))
    }
    drawCircle(primary, d * .2f, Offset(0f, -d * .28f))
    if (CompanionTrait.LONG_SNOUT in companion.traits) {
        drawPath(Path().apply {
            moveTo(d * .1f, -d * .34f)
            lineTo(d * .42f, -d * .27f)
            lineTo(d * .1f, -d * .19f)
            close()
        }, accent)
        drawCircle(outline, d * .025f, Offset(d * .4f, -d * .27f))
    }
    drawEye(Offset(-d * .07f, -d * .3f), d, outline, .032f)
    drawEye(Offset(d * .07f, -d * .3f), d, outline, .032f)
    if (CompanionTrait.ANTENNAE in companion.traits) {
        drawLine(outline, Offset(-d * .06f, -d * .43f), Offset(-d * .19f, -d * .57f), d * .018f)
        drawLine(outline, Offset(d * .06f, -d * .43f), Offset(d * .19f, -d * .57f), d * .018f)
        drawCircle(accent, d * .025f, Offset(-d * .19f, -d * .57f))
        drawCircle(accent, d * .025f, Offset(d * .19f, -d * .57f))
    }
    if (CompanionTrait.LONG_LEGS in companion.traits || CompanionTrait.PINCERS in companion.traits) {
        listOf(-1f, 1f).forEach { side ->
            drawLine(primary, Offset(side * d * .13f, d * .02f), Offset(side * d * .39f, d * .2f), d * .032f)
            drawLine(primary, Offset(side * d * .12f, d * .18f), Offset(side * d * .36f, d * .4f), d * .032f)
        }
    }
}

private fun DrawScope.drawGardenCreature(companion: CompanionDefinition, d: Float, wave: Float) {
    val primary = companion.primaryColor()
    val secondary = companion.secondaryColor()
    val accent = companion.accentColor()
    val outline = Color(0xFF26362E).copy(alpha = .58f)
    when (companion.id) {
        "dewdrop_snail" -> {
            drawOval(primary, Offset(-d * .38f, d * .05f), Size(d * .76f, d * .34f))
            drawCircle(secondary, d * .27f, Offset(-d * .05f, -d * .02f))
            drawArc(accent, 20f, 300f, false, Offset(-d * .22f, -d * .19f), Size(d * .34f, d * .34f), style = Stroke(d * .045f))
            drawCircle(primary, d * .17f, Offset(d * .3f, -d * .01f))
            drawLine(outline, Offset(d * .28f, -d * .14f), Offset(d * .22f, -d * .33f), d * .016f)
            drawLine(outline, Offset(d * .36f, -d * .13f), Offset(d * .42f, -d * .31f), d * .016f)
            drawEye(Offset(d * .22f, -d * .34f), d, outline, .025f)
            drawEye(Offset(d * .42f, -d * .32f), d, outline, .025f)
        }
        "lily_frog", "golden_toad" -> {
            drawOval(primary, Offset(-d * .34f, -d * .2f), Size(d * .68f, d * .62f))
            drawCircle(primary, d * .17f, Offset(-d * .2f, -d * .24f)); drawCircle(primary, d * .17f, Offset(d * .2f, -d * .24f))
            drawEye(Offset(-d * .2f, -d * .26f), d, outline, .045f); drawEye(Offset(d * .2f, -d * .26f), d, outline, .045f)
            drawArc(accent, 18f, 144f, false, Offset(-d * .18f, -d * .02f), Size(d * .36f, d * .25f), style = Stroke(d * .025f))
            drawOval(secondary, Offset(-d * .45f, d * .23f), Size(d * .3f, d * .15f)); drawOval(secondary, Offset(d * .15f, d * .23f), Size(d * .3f, d * .15f))
        }
        "moss_turtle", "pebble_tortoise" -> {
            drawOval(secondary, Offset(-d * .38f, -d * .24f), Size(d * .7f, d * .58f))
            drawOval(primary, Offset(-d * .31f, -d * .19f), Size(d * .56f, d * .48f))
            drawLine(accent, Offset(-d * .2f, 0f), Offset(d * .15f, 0f), d * .025f)
            drawLine(accent, Offset(-d * .03f, -d * .16f), Offset(-d * .03f, d * .22f), d * .025f)
            drawCircle(secondary, d * .16f, Offset(d * .34f, -d * .02f)); drawEye(Offset(d * .39f, -d * .05f), d, outline, .032f)
        }
        "vine_snake" -> {
            drawArc(primary, 25f, 300f, false, Offset(-d * .38f, -d * .2f), Size(d * .67f, d * .62f), style = Stroke(d * .16f))
            drawArc(secondary.copy(alpha = .8f), 40f, 265f, false, Offset(-d * .29f, -d * .1f), Size(d * .48f, d * .42f), style = Stroke(d * .045f))
            drawCircle(primary, d * .2f, Offset(d * .27f, -d * .2f))
            drawEye(Offset(d * .34f, -d * .24f), d, outline, .034f)
            drawLine(accent, Offset(d * .45f, -d * .16f), Offset(d * .57f, -d * .14f), d * .018f)
            drawLine(accent, Offset(d * .57f, -d * .14f), Offset(d * .62f, -d * .19f), d * .012f)
            drawLine(accent, Offset(d * .57f, -d * .14f), Offset(d * .63f, -d * .1f), d * .012f)
        }
        "velvet_spider" -> {
            listOf(-1f, 1f).forEach { side ->
                repeat(4) { index ->
                    val y = -d * .18f + index * d * .13f
                    val lift = if (index < 2) -d * .1f else d * .1f
                    drawLine(primary, Offset(side * d * .14f, y), Offset(side * d * .36f, y + lift), d * .035f)
                    drawLine(primary, Offset(side * d * .36f, y + lift), Offset(side * d * .5f, y + lift * .45f), d * .028f)
                }
            }
            drawOval(primary, Offset(-d * .25f, -d * .2f), Size(d * .5f, d * .55f))
            drawCircle(secondary, d * .17f, Offset(0f, -d * .25f))
            drawCircle(accent, d * .045f, Offset(-d * .1f, d * .04f))
            drawCircle(accent, d * .035f, Offset(d * .11f, d * .13f))
            drawEye(Offset(-d * .065f, -d * .28f), d, outline, .025f)
            drawEye(Offset(d * .065f, -d * .28f), d, outline, .025f)
        }
        "mushroom_slug" -> {
            drawOval(primary, Offset(-d * .42f, d * .02f), Size(d * .82f, d * .3f))
            drawCircle(primary, d * .2f, Offset(d * .29f, -d * .02f))
            drawOval(secondary.copy(alpha = .72f), Offset(-d * .26f, d * .08f), Size(d * .42f, d * .12f))
            drawLine(outline, Offset(d * .25f, -d * .16f), Offset(d * .2f, -d * .36f), d * .016f)
            drawLine(outline, Offset(d * .36f, -d * .14f), Offset(d * .43f, -d * .34f), d * .016f)
            drawEye(Offset(d * .2f, -d * .37f), d, outline, .026f)
            drawEye(Offset(d * .43f, -d * .35f), d, outline, .026f)
            drawCircle(accent, d * .04f, Offset(-d * .12f, d * .1f))
        }
        "clover_worm" -> {
            repeat(6) { index ->
                val x = -d * .31f + index * d * .125f
                val y = if (index % 2 == 0) d * .03f else -d * .015f
                drawCircle(if (index % 2 == 0) primary else secondary, d * .145f, Offset(x, y))
            }
            drawCircle(primary, d * .18f, Offset(d * .36f, -d * .06f))
            drawLine(outline, Offset(d * .31f, -d * .2f), Offset(d * .24f, -d * .37f), d * .016f)
            drawLine(outline, Offset(d * .4f, -d * .2f), Offset(d * .48f, -d * .36f), d * .016f)
            drawCircle(accent, d * .025f, Offset(d * .24f, -d * .37f))
            drawCircle(accent, d * .025f, Offset(d * .48f, -d * .36f))
            drawEye(Offset(d * .31f, -d * .08f), d, outline, .026f)
            drawEye(Offset(d * .41f, -d * .08f), d, outline, .026f)
        }
        "moss_alligator" -> {
            drawOval(primary, Offset(-d * .43f, -d * .13f), Size(d * .7f, d * .37f))
            drawRoundRect(primary, Offset(d * .12f, -d * .2f), Size(d * .45f, d * .3f), CornerRadius(d * .09f))
            repeat(4) { index ->
                val x = -d * .25f + index * d * .16f
                drawPath(Path().apply {
                    moveTo(x - d * .06f, -d * .12f)
                    lineTo(x, -d * .3f)
                    lineTo(x + d * .06f, -d * .12f)
                    close()
                }, accent)
            }
            drawEye(Offset(d * .29f, -d * .14f), d, outline, .032f)
            drawCircle(secondary, d * .025f, Offset(d * .48f, -d * .05f))
            drawLine(secondary, Offset(-d * .39f, d * .05f), Offset(-d * .57f, d * .2f), d * .07f)
        }
        "thistle_hedgehog" -> drawMammal(companion, d, wave)
        else -> {
            drawOval(primary, Offset(-d * .39f, -d * .16f), Size(d * .78f, d * .43f))
            if (CompanionTrait.SPOTS in companion.traits) {
                drawCircle(accent.copy(alpha = .75f), d * .045f, Offset(-d * .2f, -d * .03f))
                drawCircle(accent.copy(alpha = .75f), d * .035f, Offset(d * .02f, d * .05f))
            }
            if (CompanionTrait.STRIPES in companion.traits) {
                repeat(3) { index ->
                    val x = -d * .22f + index * d * .17f
                    drawLine(accent.copy(alpha = .72f), Offset(x, -d * .12f), Offset(x + d * .04f, d * .14f), d * .035f)
                }
            }
            if (CompanionTrait.SPIKES in companion.traits) {
                repeat(4) { index ->
                    val x = -d * .25f + index * d * .16f
                    drawPath(Path().apply {
                        moveTo(x - d * .05f, -d * .13f)
                        lineTo(x, -d * .3f)
                        lineTo(x + d * .05f, -d * .13f)
                        close()
                    }, accent)
                }
            }
            drawCircle(primary, d * .2f, Offset(d * .28f, -d * .09f))
            if (CompanionTrait.TURRET_EYES in companion.traits) {
                drawCircle(secondary, d * .1f, Offset(d * .2f, -d * .25f)); drawCircle(secondary, d * .1f, Offset(d * .4f, -d * .2f))
            }
            drawEye(Offset(d * .23f, -d * .23f), d, outline, .028f); drawEye(Offset(d * .4f, -d * .19f), d, outline, .028f)
            repeat(4) { index ->
                val x = -d * .25f + index * d * .17f
                drawLine(secondary, Offset(x, d * .11f), Offset(x - d * .07f, d * .31f), d * .04f)
            }
            drawArc(accent, 205f, 235f, false, Offset(-d * .55f, -d * .19f), Size(d * .45f, d * .45f), style = Stroke(d * .055f))
        }
    }
}

private fun DrawScope.drawWaterCreature(companion: CompanionDefinition, d: Float, wave: Float) {
    val primary = companion.primaryColor()
    val secondary = companion.secondaryColor()
    val accent = companion.accentColor()
    val outline = Color(0xFF26362E).copy(alpha = .58f)
    when (companion.id) {
        "coral_octopus" -> {
            drawOval(primary, Offset(-d * .31f, -d * .35f), Size(d * .62f, d * .58f))
            repeat(4) { index ->
                val x = (index - 1.5f) * d * .14f
                val curl = if (index % 2 == 0) wave else -wave
                drawArc(primary, 180f, 190f, false, Offset(x - d * .1f, d * (.08f + curl * .015f)), Size(d * .2f, d * .4f), style = Stroke(d * .09f))
            }
            drawEye(Offset(-d * .11f, -d * .12f), d, outline); drawEye(Offset(d * .11f, -d * .12f), d, outline)
            drawCircle(accent, d * .035f, Offset(-d * .2f, d * .02f)); drawCircle(accent, d * .035f, Offset(d * .2f, -.01f))
        }
        "blush_axolotl" -> {
            drawOval(primary, Offset(-d * .37f, -d * .22f), Size(d * .74f, d * .55f))
            listOf(-1f, 1f).forEach { side ->
                repeat(3) { index -> drawLine(accent, Offset(side * d * .26f, -d * (.16f - index * .1f)), Offset(side * d * .47f, -d * (.25f - index * .12f)), d * .035f) }
            }
            drawEye(Offset(-d * .13f, -d * .08f), d, outline); drawEye(Offset(d * .13f, -d * .08f), d, outline)
            drawArc(outline, 20f, 140f, false, Offset(-d * .1f, d * .01f), Size(d * .2f, d * .15f), style = Stroke(d * .018f))
        }
        "river_otter" -> drawMammal(companion, d, wave)
        "pearl_seal", "arctic_walrus" -> {
            drawOval(primary, Offset(-d * .39f, -d * .08f), Size(d * .65f, d * .4f))
            drawCircle(primary, d * .25f, Offset(d * .22f, -d * .13f))
            drawPath(Path().apply {
                moveTo(-d * .15f, d * .18f)
                lineTo(-d * .31f, d * .4f)
                lineTo(d * .01f, d * .23f)
                close()
            }, secondary)
            drawPath(Path().apply {
                moveTo(-d * .35f, d * .03f)
                lineTo(-d * .56f, -d * .15f)
                lineTo(-d * .5f, d * .19f)
                close()
            }, secondary)
            drawEye(Offset(d * .27f, -d * .19f), d, outline, .035f)
            drawOval(secondary, Offset(d * .27f, -d * .08f), Size(d * .2f, d * .14f))
            drawCircle(outline, d * .025f, Offset(d * .43f, -d * .02f))
            if (CompanionTrait.TUSKS in companion.traits) {
                listOf(-1f, 1f).forEach { side ->
                    val x = d * (.34f + side * .055f)
                    drawPath(Path().apply {
                        moveTo(x - d * .035f, d * .02f)
                        lineTo(x, d * .24f)
                        lineTo(x + d * .035f, d * .02f)
                        close()
                    }, accent)
                }
            }
        }
        "tidepool_crab", "blue_lobster" -> {
            listOf(-1f, 1f).forEach { side ->
                repeat(3) { index ->
                    val y = index * d * .1f
                    drawLine(primary, Offset(side * d * .23f, y), Offset(side * d * .48f, y + d * .14f), d * .035f)
                }
                drawLine(primary, Offset(side * d * .24f, -d * .13f), Offset(side * d * .46f, -d * .3f), d * .05f)
                drawCircle(primary, d * .13f, Offset(side * d * .5f, -d * .34f))
                drawLine(outline, Offset(side * d * .48f, -d * .35f), Offset(side * d * .6f, -d * .42f), d * .025f)
            }
            drawOval(primary, Offset(-d * .34f, -d * .22f), Size(d * .68f, d * .48f))
            drawOval(secondary.copy(alpha = .65f), Offset(-d * .24f, -d * .13f), Size(d * .48f, d * .27f))
            drawLine(outline, Offset(-d * .14f, -d * .19f), Offset(-d * .16f, -d * .36f), d * .02f)
            drawLine(outline, Offset(d * .14f, -d * .19f), Offset(d * .16f, -d * .36f), d * .02f)
            drawEye(Offset(-d * .16f, -d * .38f), d, outline, .034f)
            drawEye(Offset(d * .16f, -d * .38f), d, outline, .034f)
        }
        "moon_jellyfish" -> {
            drawOval(primary, Offset(-d * .33f, -d * .37f), Size(d * .66f, d * .5f))
            drawOval(secondary.copy(alpha = .55f), Offset(-d * .23f, -d * .3f), Size(d * .46f, d * .26f))
            repeat(4) { index ->
                val x = (index - 1.5f) * d * .13f
                val curl = if (index % 2 == 0) wave else -wave
                drawArc(primary, 165f, 205f, false, Offset(x - d * .08f, d * (.02f + curl * .02f)), Size(d * .16f, d * .48f), style = Stroke(d * .055f))
            }
            drawCircle(accent.copy(alpha = .22f), d * .32f, Offset(0f, -d * .13f))
            drawEye(Offset(-d * .11f, -d * .13f), d, outline, .032f)
            drawEye(Offset(d * .11f, -d * .13f), d, outline, .032f)
        }
        "emerald_pufferfish" -> {
            repeat(12) { index ->
                val angle = index * PI * 2.0 / 12.0
                val inner = Offset((cos(angle) * d * .29f).toFloat(), (sin(angle) * d * .29f).toFloat())
                val outer = Offset((cos(angle) * d * .46f).toFloat(), (sin(angle) * d * .46f).toFloat())
                drawLine(accent, inner, outer, d * .045f)
            }
            drawCircle(primary, d * .34f)
            drawCircle(secondary.copy(alpha = .7f), d * .18f, Offset(-d * .06f, d * .07f))
            drawEye(Offset(d * .11f, -d * .1f), d, outline, .04f)
            drawCircle(outline, d * .03f, Offset(d * .31f, d * .02f))
            drawCircle(accent, d * .04f, Offset(-d * .18f, -d * .11f))
            drawCircle(accent, d * .035f, Offset(d * .03f, d * .17f))
        }
        "sapphire_manta" -> {
            drawPath(Path().apply {
                moveTo(-d * .52f, -d * .08f)
                quadraticTo(-d * .22f, -d * .38f, 0f, -d * .12f)
                quadraticTo(d * .22f, -d * .38f, d * .52f, -d * .08f)
                quadraticTo(d * .25f, d * .25f, 0f, d * .15f)
                quadraticTo(-d * .25f, d * .25f, -d * .52f, -d * .08f)
                close()
            }, primary)
            drawPath(Path().apply {
                moveTo(0f, d * .11f)
                lineTo(-d * .05f, d * .55f)
                lineTo(d * .06f, d * .14f)
                close()
            }, secondary)
            drawOval(secondary.copy(alpha = .55f), Offset(-d * .18f, -d * .16f), Size(d * .36f, d * .25f))
            drawEye(Offset(-d * .09f, -d * .11f), d, outline, .03f)
            drawEye(Offset(d * .09f, -d * .11f), d, outline, .03f)
        }
        "sunstar_starfish" -> {
            val star = Path().apply {
                repeat(10) { index ->
                    val angle = -PI / 2.0 + index * PI / 5.0
                    val radius = if (index % 2 == 0) d * .45f else d * .2f
                    val x = (cos(angle) * radius).toFloat()
                    val y = (sin(angle) * radius).toFloat()
                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawPath(star, primary)
            drawCircle(secondary.copy(alpha = .6f), d * .14f)
            drawCircle(accent, d * .035f, Offset(-d * .12f, d * .09f))
            drawCircle(accent, d * .03f, Offset(d * .13f, d * .13f))
            drawEye(Offset(-d * .07f, -d * .04f), d, outline, .027f)
            drawEye(Offset(d * .07f, -d * .04f), d, outline, .027f)
        }
        "golden_seahorse" -> {
            drawCircle(primary, d * .22f, Offset(d * .05f, -d * .24f))
            drawArc(primary, 240f, 245f, false, Offset(-d * .22f, -d * .19f), Size(d * .42f, d * .65f), style = Stroke(d * .15f))
            drawArc(accent, 10f, 285f, false, Offset(-d * .28f, d * .05f), Size(d * .35f, d * .35f), style = Stroke(d * .045f))
            drawPath(Path().apply { moveTo(d * .22f, -d * .28f); lineTo(d * .46f, -d * .2f); lineTo(d * .2f, -d * .12f); close() }, secondary)
            drawEye(Offset(d * .1f, -d * .29f), d, outline, .032f)
        }
        else -> {
            drawOval(primary, Offset(-d * .41f, -d * .23f), Size(d * .7f, d * .48f))
            drawPath(Path().apply { moveTo(-d * .32f, 0f); lineTo(-d * .55f, -d * .22f); lineTo(-d * .53f, d * .2f); close() }, secondary)
            if (CompanionTrait.FINS in companion.traits) {
                drawPath(Path().apply { moveTo(-d * .02f, -d * .18f); lineTo(d * .06f, -d * .42f); lineTo(d * .16f, -d * .17f); close() }, secondary)
                drawPath(Path().apply { moveTo(-d * .03f, d * .12f); lineTo(d * .09f, d * .36f); lineTo(d * .18f, d * .1f); close() }, secondary)
            }
            drawCircle(primary, d * .22f, Offset(d * .27f, -d * .05f))
            drawEye(Offset(d * .34f, -d * .1f), d, outline, .035f)
            if (CompanionTrait.SPOTS in companion.traits) {
                drawCircle(accent.copy(alpha = .8f), d * .045f, Offset(-d * .16f, -d * .08f))
                drawCircle(accent.copy(alpha = .8f), d * .038f, Offset(d * .03f, d * .08f))
            }
            if (CompanionTrait.STRIPES in companion.traits) {
                repeat(3) { index ->
                    val x = -d * .19f + index * d * .15f
                    drawLine(accent, Offset(x, -d * .18f), Offset(x + d * .035f, d * .17f), d * .04f)
                }
            }
            if (CompanionTrait.HORNS in companion.traits) {
                drawPath(Path().apply {
                    moveTo(d * .32f, -d * .2f)
                    lineTo(d * .63f, -d * .34f)
                    lineTo(d * .4f, -d * .08f)
                    close()
                }, accent)
            }
            if (companion.id == "cloud_whale") drawLine(accent, Offset(d * .43f, -d * .2f), Offset(d * .52f, -d * .36f), d * .025f)
        }
    }
}

private fun DrawScope.drawEye(center: Offset, d: Float, color: Color, radius: Float = .04f) {
    drawCircle(Color.White.copy(alpha = .92f), d * (radius + .025f), center)
    drawCircle(color, d * radius, center)
    drawCircle(Color.White.copy(alpha = .8f), d * radius * .28f, center - Offset(d * radius * .3f, d * radius * .3f))
}

// Catalog colors are conventional 32-bit ARGB values stored as Longs for platform-neutral data.
// Color(ULong) expects Compose's packed wide-gamut representation and interprets the low bits as a
// color-space ID, which crashes Android Canvas. Convert to the ARGB Int constructor explicitly.
private fun CompanionDefinition.primaryColor() = Color(primaryArgb.toInt())
private fun CompanionDefinition.secondaryColor() = Color(secondaryArgb.toInt())
private fun CompanionDefinition.accentColor() = Color(accentArgb.toInt())
