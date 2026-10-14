package com.example.mydigidaw.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mydigidaw.ui.theme.AppColors
import com.example.mydigidaw.util.Money

/** Durasi & kurva standar agar semua animasi terasa satu keluarga. */
object Motion {
    const val SHORT = 220
    const val MEDIUM = 420
    const val LONG = 900
    val Emphasized = FastOutSlowInEasing
}

/** Muncul bertahap (pudar + naik) saat pertama kali tampil; [index] memberi jeda berurutan. */
fun Modifier.enterStagger(index: Int = 0, baseDelay: Int = 45): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            1f,
            tween(durationMillis = Motion.MEDIUM, delayMillis = index.coerceAtMost(10) * baseDelay, easing = Motion.Emphasized),
        )
    }
    graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 24.dp.toPx()
    }
}

/** Efek "membal" halus saat ditekan. */
@Composable
fun Modifier.pressScale(interactionSource: MutableInteractionSource, pressed: Float = 0.96f): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) pressed else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "pressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** Kartu standar aplikasi: sudut lembut, efek tekan, opsional latar gradien. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    brush: Brush? = null,
    border: BorderStroke? = null,
    shape: Shape = MaterialTheme.shapes.large,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val colors = CardDefaults.cardColors(containerColor = if (brush != null) Color.Transparent else containerColor)
    val inner = if (brush != null) Modifier.fillMaxWidth().background(brush, shape) else Modifier
    if (onClick != null) {
        Card(
            onClick = onClick,
            shape = shape,
            colors = colors,
            border = border,
            interactionSource = interaction,
            modifier = modifier.pressScale(interaction),
        ) { Box(inner) { androidx.compose.foundation.layout.Column(content = content) } }
    } else {
        Card(shape = shape, colors = colors, border = border, modifier = modifier) {
            Box(inner) { androidx.compose.foundation.layout.Column(content = content) }
        }
    }
}

/** Gradien merek untuk kartu utama (saldo, ringkasan). */
@Composable
fun heroBrush(): Brush = Brush.linearGradient(AppColors.current.brand)

/** Lingkaran dekoratif lembut di pojok kartu bergradien. */
fun Modifier.heroDecoration(color: Color = Color.White): Modifier = drawBehind {
    drawCircle(color.copy(alpha = 0.08f), radius = size.height * 0.9f, center = Offset(size.width * 1.02f, -size.height * 0.15f))
    drawCircle(color.copy(alpha = 0.06f), radius = size.height * 0.55f, center = Offset(size.width * 0.82f, size.height * 1.1f))
}

/** Angka uang yang "berhitung" menuju nilai baru. */
@Composable
fun AnimatedMoney(
    amount: Long,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    hidden: Boolean = false,
    autoSize: Boolean = false,
    minSize: TextUnit = 16.sp,
) {
    val from = remember { mutableLongStateOf(0L) }
    val progress = remember { Animatable(1f) }
    val target = remember { mutableLongStateOf(amount) }
    LaunchedEffect(amount) {
        from.longValue = from.longValue + ((target.longValue - from.longValue) * progress.value).toLong()
        target.longValue = amount
        progress.snapTo(0f)
        progress.animateTo(1f, tween(Motion.LONG, easing = Motion.Emphasized))
        from.longValue = amount
    }
    val shown = from.longValue + ((target.longValue - from.longValue) * progress.value).toLong()
    val text = if (hidden) Money.masked() else Money.format(shown)
    if (autoSize) AutoSizeText(text, style, modifier, color, minSize)
    else Text(text, style = style, color = color, modifier = modifier, maxLines = 1)
}

/** Animasi progres 0→1 sekali saat data berubah, untuk grafik yang "tumbuh". */
@Composable
fun rememberRevealProgress(key: Any?, duration: Int = Motion.LONG): Float {
    val anim = remember(key) { Animatable(0f) }
    LaunchedEffect(key) { anim.animateTo(1f, tween(duration, easing = Motion.Emphasized)) }
    return anim.value
}

/** Emoji/ikon yang melayang perlahan — dipakai di tampilan kosong. */
@Composable
fun FloatingEmoji(emoji: String, size: TextUnit = 44.sp) {
    val t = rememberInfiniteTransition(label = "float")
    val dy by t.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dy",
    )
    Box(
        Modifier
            .graphicsLayer { translationY = dy.dp.toPx() }
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
            .padding(18.dp),
    ) {
        Text(emoji, fontSize = size)
    }
}

/** Denyut halus untuk ikon penting (mis. layar kunci). */
@Composable
fun pulseScale(): Float {
    val t = rememberInfiniteTransition(label = "pulse")
    val s by t.animateFloat(1f, 1.08f, infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Reverse), label = "s")
    return s
}
