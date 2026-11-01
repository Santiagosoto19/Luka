package co.luka.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private fun <T> motionSpec(animate: Boolean, springSpec: AnimationSpec<T>): AnimationSpec<T> =
    if (animate) springSpec else snap()

@Composable
fun EnterPiece(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val animate = animationsEnabled()
    var started by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (!animate || started) 1f else 0f) }
    LaunchedEffect(animate) {
        if (!animate || started) {
            progress.snapTo(1f)
            return@LaunchedEffect
        }
        started = true
        if (index > 0) delay(index * 40L)
        progress.animateTo(
            1f,
            spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        )
    }
    val distance = with(LocalDensity.current) { 12.dp.toPx() }
    val travel = progress.value
    Box(
        modifier.graphicsLayer {
            alpha = if (animate) travel else 1f
            translationY = if (animate) distance * (1f - travel) else 0f
            val scale = if (animate) 0.98f + 0.02f * travel else 1f
            scaleX = scale
            scaleY = scale
        },
    ) {
        content()
    }
}

@Composable
fun PressScale(
    interaction: MutableInteractionSource,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val pressed by interaction.collectIsPressedAsState()
    val animate = animationsEnabled()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = motionSpec(
            animate,
            spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
        ),
        label = "pressScale",
    )
    Box(
        modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
    ) {
        content()
    }
}

@Composable
fun ShineOverlay(modifier: Modifier = Modifier) {
    val animate = animationsEnabled()
    if (!animate) return
    val shine = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMillis = 720, easing = LinearEasing))
    }
    val shift = progress.value
    Canvas(modifier) {
        val band = size.width * 0.32f
        val x = -band + (size.width + band) * shift
        drawRect(
            color = shine,
            topLeft = Offset(x, 0f),
            size = Size(band, size.height),
        )
    }
}
