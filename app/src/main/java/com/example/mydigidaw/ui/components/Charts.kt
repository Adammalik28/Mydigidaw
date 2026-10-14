package com.example.mydigidaw.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.mydigidaw.ui.theme.AppColors
import com.example.mydigidaw.util.Money
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

// Spesifikasi visual (sesuai panduan dataviz): batang <= 24dp dengan ujung data membulat 4dp,
// jarak 2dp antar-tanda, garis 2dp, titik >= 8dp bercincin 2dp warna permukaan, grid hairline 1dp.

data class DonutSlice(val label: String, val value: Long, val color: Color, val emoji: String = "")

data class BarGroup(val label: String, val income: Long, val expense: Long)

data class LinePoint(val label: String, val value: Long)

@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    thickness: Dp = 26.dp,
    selected: Int? = null,
    onSelect: (Int?) -> Unit = {},
    center: @Composable () -> Unit = {},
) {
    val total = slices.sumOf { it.value }.coerceAtLeast(1)
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val currentSelected by rememberUpdatedState(selected)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val reveal = rememberRevealProgress(slices.map { it.value })
    val selectedGrow by animateFloatAsState(if (selected != null) 1f else 0f, tween(Motion.SHORT), label = "grow")
    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(slices) {
                    detectTapGestures { pos ->
                        val stroke = thickness.toPx()
                        val radius = (min(size.width, size.height) - stroke * 1.25f) / 2f
                        val dx = pos.x - size.width / 2f
                        val dy = pos.y - size.height / 2f
                        val dist = sqrt(dx * dx + dy * dy)
                        if (dist < radius - stroke || dist > radius + stroke) {
                            currentOnSelect(null)
                            return@detectTapGestures
                        }
                        var angle = atan2(dy, dx) * 180f / PI.toFloat() + 90f
                        if (angle < 0f) angle += 360f
                        var acc = 0f
                        slices.forEachIndexed { i, s ->
                            val sweep = s.value.toFloat() / total * 360f
                            if (angle >= acc && angle < acc + sweep) {
                                currentOnSelect(if (currentSelected == i) null else i)
                                return@detectTapGestures
                            }
                            acc += sweep
                        }
                    }
                },
        ) {
            val stroke = thickness.toPx()
            val diameter = min(size.width, size.height) - stroke * 1.25f
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            if (slices.isEmpty()) {
                drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
                return@Canvas
            }
            val gap = if (slices.size > 1) (2.dp.toPx() / (diameter / 2f)) * (180f / PI.toFloat()) else 0f
            var start = -90f
            slices.forEachIndexed { i, s ->
                val sweep = s.value.toFloat() / total * 360f * reveal
                val dimmed = currentSelected != null && currentSelected != i
                drawArc(
                    color = if (dimmed) s.color.copy(alpha = 0.3f) else s.color,
                    startAngle = start + gap / 2f,
                    sweepAngle = (sweep - gap).coerceAtLeast(0.6f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = if (currentSelected == i) stroke * (1f + 0.2f * selectedGrow) else stroke),
                )
                start += sweep
            }
        }
        center()
    }
}

/** Batang berkelompok pemasukan vs pengeluaran per periode, satu sumbu nilai. */
@Composable
fun IncomeExpenseBarChart(
    groups: List<BarGroup>,
    modifier: Modifier = Modifier,
    height: Dp = 190.dp,
    selected: Int? = null,
    onSelect: (Int?) -> Unit = {},
) {
    val colors = AppColors.current
    val measurer = rememberTextMeasurer()
    val axisStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    val maxValue = groups.maxOfOrNull { max(it.income, it.expense) } ?: 0L
    val ticks = remember(maxValue) { niceTicks(0, maxValue) }
    val tickLayouts = remember(ticks, axisStyle) { ticks.map { measurer.measure(Money.compact(it), axisStyle) } }
    val labelWidth = tickLayouts.maxOfOrNull { it.size.width } ?: 0
    val currentSelected by rememberUpdatedState(selected)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val reveal = rememberRevealProgress(groups)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .pointerInput(groups, labelWidth) {
                detectTapGestures { pos ->
                    val plotLeft = labelWidth + 8.dp.toPx()
                    if (groups.isEmpty() || pos.x < plotLeft) {
                        currentOnSelect(null)
                        return@detectTapGestures
                    }
                    val groupW = (size.width - plotLeft) / groups.size
                    val index = ((pos.x - plotLeft) / groupW).toInt().coerceIn(0, groups.lastIndex)
                    currentOnSelect(if (currentSelected == index) null else index)
                }
            },
    ) {
        val plotLeft = labelWidth + 8.dp.toPx()
        val plotTop = (tickLayouts.firstOrNull()?.size?.height ?: 0) / 2f + 2.dp.toPx()
        val plotBottom = size.height - 20.dp.toPx()
        val top = ticks.last().toFloat().coerceAtLeast(1f)
        fun y(v: Long) = plotBottom - (v / top) * (plotBottom - plotTop)

        ticks.forEachIndexed { i, t ->
            val yy = y(t)
            if (i > 0) drawLine(colors.grid, Offset(plotLeft, yy), Offset(size.width, yy), 1.dp.toPx())
            val layout = tickLayouts[i]
            drawText(layout, topLeft = Offset(plotLeft - 8.dp.toPx() - layout.size.width, yy - layout.size.height / 2f))
        }
        if (groups.isEmpty()) return@Canvas
        val groupW = (size.width - plotLeft) / groups.size
        val gap = 2.dp.toPx()
        val barW = min(24.dp.toPx(), (groupW * 0.72f - gap) / 2f).coerceAtLeast(2f)
        var lastLabelRight = Float.NEGATIVE_INFINITY
        groups.forEachIndexed { i, g ->
            val cx = plotLeft + groupW * (i + 0.5f)
            if (currentSelected == i) {
                drawRect(highlight, topLeft = Offset(plotLeft + groupW * i, plotTop), size = Size(groupW, plotBottom - plotTop))
            }
            val stagger = ((reveal * (groups.size + 2) - i) / 3f).coerceIn(0f, 1f)
            drawTopRoundedBar(cx - gap / 2f - barW, barW, plotBottom - (plotBottom - y(g.income)) * stagger, plotBottom, colors.income)
            drawTopRoundedBar(cx + gap / 2f, barW, plotBottom - (plotBottom - y(g.expense)) * stagger, plotBottom, colors.expense)
            val label = measurer.measure(g.label, axisStyle)
            val left = cx - label.size.width / 2f
            if (left > lastLabelRight + 4.dp.toPx()) {
                drawText(label, topLeft = Offset(left, plotBottom + 4.dp.toPx()))
                lastLabelRight = left + label.size.width
            }
        }
        drawLine(colors.axis, Offset(plotLeft, plotBottom), Offset(size.width, plotBottom), 1.dp.toPx())
    }
}

/** Garis tren satu seri dengan area tipis, titik akhir bercincin, dan garis bidik saat disentuh/digeser. */
@Composable
fun TrendLineChart(
    points: List<LinePoint>,
    color: Color,
    ringColor: Color,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    selected: Int? = null,
    onSelect: (Int?) -> Unit = {},
) {
    val colors = AppColors.current
    val measurer = rememberTextMeasurer()
    val axisStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val lo = points.minOfOrNull { it.value } ?: 0L
    val hi = points.maxOfOrNull { it.value } ?: 0L
    val ticks = remember(lo, hi) { niceTicks(lo, hi) }
    val tickLayouts = remember(ticks, axisStyle) { ticks.map { measurer.measure(Money.compact(it), axisStyle) } }
    val labelWidth = tickLayouts.maxOfOrNull { it.size.width } ?: 0
    val currentOnSelect by rememberUpdatedState(onSelect)
    val reveal = rememberRevealProgress(points, duration = 1200)

    fun indexAt(x: Float, plotRight: Float, plotLeft: Float): Int? {
        if (points.isEmpty()) return null
        if (points.size == 1) return 0
        val step = (plotRight - plotLeft) / (points.size - 1)
        return ((x - plotLeft) / step).roundToInt().coerceIn(0, points.lastIndex)
    }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .pointerInput(points, labelWidth) {
                detectTapGestures { pos ->
                    currentOnSelect(indexAt(pos.x, size.width - 8.dp.toPx(), labelWidth + 8.dp.toPx()))
                }
            }
            .pointerInput(points, labelWidth) {
                detectHorizontalDragGestures(
                    onDragEnd = {},
                ) { change, _ ->
                    change.consume()
                    currentOnSelect(indexAt(change.position.x, size.width - 8.dp.toPx(), labelWidth + 8.dp.toPx()))
                }
            },
    ) {
        val plotLeft = labelWidth + 8.dp.toPx()
        val plotRight = size.width - 8.dp.toPx()
        val plotTop = (tickLayouts.firstOrNull()?.size?.height ?: 0) / 2f + 2.dp.toPx()
        val plotBottom = size.height - 20.dp.toPx()
        val minT = ticks.first().toFloat()
        val maxT = ticks.last().toFloat()
        val span = (maxT - minT).takeIf { it > 0f } ?: 1f
        fun y(v: Long) = plotBottom - ((v - minT) / span) * (plotBottom - plotTop)
        fun x(i: Int) = if (points.size <= 1) (plotLeft + plotRight) / 2f
        else plotLeft + i * (plotRight - plotLeft) / (points.size - 1)

        ticks.forEachIndexed { i, t ->
            val yy = y(t)
            drawLine(if (t == 0L) colors.axis else colors.grid, Offset(plotLeft, yy), Offset(size.width, yy), 1.dp.toPx())
            val layout = tickLayouts[i]
            drawText(layout, topLeft = Offset(plotLeft - 8.dp.toPx() - layout.size.width, yy - layout.size.height / 2f))
        }
        if (points.isEmpty()) return@Canvas

        val line = Path()
        val area = Path()
        points.forEachIndexed { i, p ->
            val px = x(i)
            val py = y(p.value)
            if (i == 0) {
                line.moveTo(px, py)
                area.moveTo(px, plotBottom)
                area.lineTo(px, py)
            } else {
                line.lineTo(px, py)
                area.lineTo(px, py)
            }
        }
        area.lineTo(x(points.lastIndex), plotBottom)
        area.close()
        clipRect(right = plotLeft + (size.width - plotLeft) * reveal) {
            drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.22f), color.copy(alpha = 0.0f)), startY = plotTop, endY = plotBottom))
            drawPath(line, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        // Label sumbu-x: awal, tengah, akhir.
        val labelIdx = listOf(0, points.lastIndex / 2, points.lastIndex).distinct()
        labelIdx.forEach { i ->
            val layout = measurer.measure(points[i].label, axisStyle)
            val lx = (x(i) - layout.size.width / 2f).coerceIn(plotLeft, size.width - layout.size.width)
            drawText(layout, topLeft = Offset(lx, plotBottom + 4.dp.toPx()))
        }

        val sel = selected?.takeIf { it in points.indices }
        if (sel != null) {
            drawLine(colors.axis, Offset(x(sel), plotTop), Offset(x(sel), plotBottom), 1.dp.toPx())
        }
        if (reveal > 0.97f || sel != null) {
            val dotIndex = sel ?: points.lastIndex
            val center = Offset(x(dotIndex), y(points[dotIndex].value))
            drawCircle(color.copy(alpha = 0.18f), radius = 11.dp.toPx(), center = center)
            drawCircle(ringColor, radius = 6.dp.toPx(), center = center)
            drawCircle(color, radius = 4.dp.toPx(), center = center)
        }
    }
}

/** Meteran progres: isian membawa tingkat keparahan, lintasan adalah warna yang sama versi pudar. */
@Composable
fun Meter(progress: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 8.dp) {
    val target = progress.coerceIn(0f, 1f)
    val anim = remember { Animatable(0f) }
    LaunchedEffect(target) { anim.animateTo(target, tween(Motion.LONG, easing = Motion.Emphasized)) }
    val animatedColor by animateColorAsState(color, tween(Motion.MEDIUM), label = "meterColor")
    Canvas(modifier.fillMaxWidth().height(height)) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(animatedColor.copy(alpha = 0.18f), cornerRadius = radius)
        val w = size.width * anim.value
        if (w > 0.5f) drawRoundRect(animatedColor, size = Size(max(w, size.height), size.height), cornerRadius = radius)
    }
}

@Composable
fun LegendItem(color: Color, label: String, modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(2.dp)) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(shape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun DrawScope.drawTopRoundedBar(left: Float, width: Float, top: Float, bottom: Float, color: Color) {
    val h = bottom - top
    if (h <= 0.5f) return
    val r = min(4.dp.toPx(), min(width / 2f, h))
    val path = Path().apply {
        addRoundRect(
            RoundRect(
                left = left,
                top = top,
                right = left + width,
                bottom = bottom,
                topLeftCornerRadius = CornerRadius(r),
                topRightCornerRadius = CornerRadius(r),
                bottomRightCornerRadius = CornerRadius.Zero,
                bottomLeftCornerRadius = CornerRadius.Zero,
            ),
        )
    }
    drawPath(path, color)
}

/** Tick sumbu yang "bulat" (0, 500 rb, 1 jt, …) mencakup rentang [lo, hi]. */
fun niceTicks(lo: Long, hi: Long, count: Int = 4): List<Long> {
    var low = min(lo, hi).toDouble()
    var high = max(lo, hi).toDouble()
    if (low > 0 && low < high * 0.35) low = 0.0
    if (high == low) {
        if (high == 0.0) high = 100_000.0 else {
            low -= abs(low) * 0.1 + 1
            high += abs(high) * 0.1 + 1
        }
    }
    val raw = (high - low) / count
    val mag = 10.0.pow(floor(log10(raw)))
    val norm = raw / mag
    val niceNorm = when {
        norm <= 1.0 -> 1.0
        norm <= 2.0 -> 2.0
        norm <= 2.5 -> 2.5
        norm <= 5.0 -> 5.0
        else -> 10.0
    }
    val step = niceNorm * mag
    val start = floor(low / step) * step
    val end = ceil(high / step) * step
    val list = mutableListOf<Long>()
    var v = start
    var guard = 0
    while (v <= end + step / 2 && guard++ < 20) {
        list += v.toLong()
        v += step
    }
    return list
}
