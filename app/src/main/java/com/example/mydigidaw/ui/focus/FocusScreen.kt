@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.mydigidaw.ui.focus

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.mydigidaw.AppContainer
import com.example.mydigidaw.data.db.FocusSessionEntity
import com.example.mydigidaw.data.prefs.FocusState
import com.example.mydigidaw.reminders.Notifications
import com.example.mydigidaw.ui.appViewModel
import com.example.mydigidaw.ui.components.SegmentedTabs
import com.example.mydigidaw.ui.components.AppCard
import com.example.mydigidaw.ui.components.BackTopBar
import com.example.mydigidaw.ui.components.LocalFeedback
import com.example.mydigidaw.ui.components.Motion
import com.example.mydigidaw.ui.components.StatTile
import com.example.mydigidaw.ui.components.enterStagger
import com.example.mydigidaw.ui.theme.AppColors
import com.example.mydigidaw.util.Dates
import com.example.mydigidaw.util.toLocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class FocusMode(val label: String, val minutes: Int) {
    FOCUS("Fokus", 25),
    SHORT_BREAK("Istirahat 5", 5),
    LONG_BREAK("Istirahat 15", 15),
}

class FocusViewModel(private val c: AppContainer) : ViewModel() {
    val state: StateFlow<FocusState> = c.focus.state.stateIn(viewModelScope, SharingStarted.Eagerly, FocusState())
    val sessions: StateFlow<List<FocusSessionEntity>> = c.focus.sessions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var mode by mutableStateOf(FocusMode.FOCUS)
    var minutes by mutableIntStateOf(25)
    var label by mutableStateOf("")
    var celebrate by mutableStateOf<FocusState?>(null)

    fun start() = viewModelScope.launch {
        celebrate = null
        val isBreak = mode != FocusMode.FOCUS
        c.focus.start(if (isBreak) mode.minutes else minutes, if (isBreak) "" else label, isBreak)
    }

    fun pause() = viewModelScope.launch { c.focus.pause() }
    fun resume() = viewModelScope.launch { c.focus.resume() }
    fun stop() = viewModelScope.launch { c.focus.stop() }
    fun complete() = viewModelScope.launch { c.focus.complete()?.let { celebrate = it } }

    fun startBreakAfter(done: FocusState) {
        mode = if (done.isBreak) FocusMode.FOCUS else FocusMode.SHORT_BREAK
        start()
    }
}

private fun mmss(ms: Long): String {
    val total = (ms.coerceAtLeast(0) + 999) / 1000
    return "%02d:%02d".format(total / 60, total % 60)
}

@Composable
fun FocusScreen(onBack: () -> Unit) {
    val vm = appViewModel { FocusViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val feedback = LocalFeedback.current
    val context = LocalContext.current
    val ext = AppColors.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    LaunchedEffect(state.running) {
        while (state.running) withFrameMillis { now = System.currentTimeMillis() }
    }
    val finished = state.running && now >= state.endAt
    LaunchedEffect(finished) {
        if (finished) {
            feedback.confirm()
            vm.complete()
        }
    }

    val totalMs = state.plannedMinutes * 60_000L
    val remaining = when {
        state.running -> (state.endAt - now).coerceAtLeast(0)
        state.paused -> state.remainingMs
        else -> (if (vm.mode == FocusMode.FOCUS) vm.minutes else vm.mode.minutes) * 60_000L
    }
    val progress = if (state.active && totalMs > 0) 1f - remaining.toFloat() / totalMs else 0f

    Scaffold(topBar = { BackTopBar(title = "Fokus", onBack = onBack) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AnimatedVisibility(!state.active) {
                SegmentedTabs(
                    labels = FocusMode.entries.map { it.label },
                    selectedIndex = FocusMode.entries.indexOf(vm.mode),
                    onSelect = {
                        feedback.tick()
                        vm.mode = FocusMode.entries[it]
                    },
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            FocusRing(
                progress = progress,
                time = mmss(remaining),
                caption = when {
                    state.active && state.isBreak -> "Istirahat"
                    state.active -> state.label.ifBlank { "Sesi fokus" }
                    vm.mode == FocusMode.FOCUS -> "Siap fokus"
                    else -> "Siap istirahat"
                },
                paused = state.paused,
                breakMode = state.isBreak || (!state.active && vm.mode != FocusMode.FOCUS),
            )

            AnimatedContent(
                targetState = when {
                    state.running -> 1
                    state.paused -> 2
                    else -> 0
                },
                transitionSpec = { (fadeIn(tween(Motion.MEDIUM)) + scaleIn(initialScale = 0.9f)) togetherWith fadeOut(tween(Motion.SHORT)) },
                label = "focusControls",
            ) { phase ->
                when (phase) {
                    0 -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (vm.mode == FocusMode.FOCUS) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                                listOf(15, 25, 45, 60, 90).forEach { m ->
                                    FilterChip(
                                        selected = vm.minutes == m,
                                        onClick = {
                                            feedback.tick()
                                            vm.minutes = m
                                        },
                                        label = { Text("$m mnt") },
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = vm.label,
                                onValueChange = { vm.label = it.take(40) },
                                label = { Text("Sedang mengerjakan apa?") },
                                placeholder = { Text("mis. Belajar, laporan kantor") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Button(
                            onClick = {
                                feedback.confirm()
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Notifications.canPost(context)) {
                                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                vm.start()
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (vm.mode == FocusMode.FOCUS) "Mulai fokus" else "Mulai istirahat", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    else -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                feedback.reject()
                                vm.stop()
                            },
                            modifier = Modifier.weight(1f).height(56.dp),
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Berhenti")
                        }
                        Button(
                            onClick = {
                                feedback.tick()
                                if (phase == 1) vm.pause() else vm.resume()
                            },
                            modifier = Modifier.weight(1f).height(56.dp),
                        ) {
                            Icon(if (phase == 1) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (phase == 1) "Jeda" else "Lanjut")
                        }
                    }
                }
            }

            vm.celebrate?.let { done -> Celebration(done, onNext = { vm.startBreakAfter(done) }, onDismiss = { vm.celebrate = null }) }

            FocusStats(sessions, ext.series[0])
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FocusRing(progress: Float, time: String, caption: String, paused: Boolean, breakMode: Boolean) {
    val ext = AppColors.current
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val colors = if (breakMode) listOf(ext.series[2], ext.series[0], ext.series[2]) else ext.brand + ext.brand.first()
    Box(Modifier.size(280.dp).padding(top = 8.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 16.dp.toPx()
            val d = size.minDimension - stroke
            val topLeft = Offset((size.width - d) / 2f, (size.height - d) / 2f)
            drawArc(track, 0f, 360f, false, topLeft, Size(d, d), style = Stroke(stroke))
            if (progress > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(colors),
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(d, d),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                time,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 60.sp),
                modifier = Modifier.graphicsLayer { alpha = if (paused) 0.55f else 1f },
            )
            Text(
                if (paused) "Dijeda · $caption" else caption,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun Celebration(done: FocusState, onNext: () -> Unit, onDismiss: () -> Unit) {
    val scale = remember { Animatable(0.4f) }
    LaunchedEffect(done) { scale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 300f)) }
    AppCard(containerColor = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (done.isBreak) "☕" else "🎉",
                fontSize = 44.sp,
                modifier = Modifier.graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                },
            )
            Text(
                if (done.isBreak) "Istirahat selesai" else "${done.plannedMinutes} menit fokus tuntas!",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                if (done.isBreak) "Siap kembali ke sesi berikutnya?" else "Kerja bagus. Regangkan badan dan minum air sejenak.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
            )
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) { Text("Nanti") }
                Button(onClick = onNext) { Text(if (done.isBreak) "Fokus lagi" else "Istirahat 5 menit") }
            }
        }
    }
}

@Composable
private fun FocusStats(sessions: List<FocusSessionEntity>, color: androidx.compose.ui.graphics.Color) {
    val today = LocalDate.now()
    val byDay = sessions.groupBy { it.startedAt.toLocalDate() }.mapValues { (_, list) -> list.sumOf { it.actualMinutes } }
    val todayMinutes = byDay[today] ?: 0
    val todaySessions = sessions.count { it.startedAt.toLocalDate() == today && it.completed }
    var streak = 0
    var d = if ((byDay[today] ?: 0) > 0) today else today.minusDays(1)
    while ((byDay[d] ?: 0) > 0) {
        streak++
        d = d.minusDays(1)
    }
    val week = (6 downTo 0).map { today.minusDays(it.toLong()) }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.enterStagger(1)) {
        StatTile("Hari ini", "$todayMinutes menit", Modifier.weight(1f))
        StatTile("Tuntas", "$todaySessions", Modifier.weight(1f))
        StatTile("Beruntun", "$streak hari", Modifier.weight(1f))
    }
    AppCard(modifier = Modifier.fillMaxWidth().enterStagger(2)) {
        Column(Modifier.padding(18.dp)) {
            Text("7 hari terakhir", style = MaterialTheme.typography.titleMedium)
            Text(
                "Total ${week.sumOf { byDay[it] ?: 0 }} menit fokus",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            WeekBars(week.map { (Dates.weekdayShort[it.dayOfWeek.value - 1]) to (byDay[it] ?: 0) }, color)
        }
    }
    if (sessions.isNotEmpty()) {
        AppCard(modifier = Modifier.fillMaxWidth().enterStagger(3)) {
            Column(Modifier.padding(vertical = 10.dp)) {
                Text(
                    "Riwayat sesi",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                )
                sessions.take(6).forEach { s ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (s.completed) "✅" else "⏹️", fontSize = 16.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                            Text(
                                "${Dates.relativeDay(s.startedAt.toLocalDate())}, ${Dates.time(s.startedAt)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text("${s.actualMinutes} mnt", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekBars(data: List<Pair<String, Int>>, color: androidx.compose.ui.graphics.Color) {
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val valueStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
    val grid = AppColors.current.axis
    val reveal = remember(data) { Animatable(0f) }
    LaunchedEffect(data) { reveal.animateTo(1f, tween(Motion.LONG, easing = Motion.Emphasized)) }
    val max = (data.maxOfOrNull { it.second } ?: 0).coerceAtLeast(30)
    Canvas(Modifier.fillMaxWidth().height(140.dp)) {
        val bottom = size.height - 20.dp.toPx()
        val top = 18.dp.toPx()
        val slot = size.width / data.size
        val barW = minOf(24.dp.toPx(), slot * 0.5f)
        drawLine(grid, Offset(0f, bottom), Offset(size.width, bottom), 1.dp.toPx())
        data.forEachIndexed { i, (label, minutes) ->
            val cx = slot * (i + 0.5f)
            val h = (bottom - top) * (minutes.toFloat() / max) * reveal.value
            if (h > 0.5f) {
                val r = minOf(4.dp.toPx(), barW / 2f, h)
                val barColor = if (i == data.lastIndex) color else color.copy(alpha = 0.45f)
                drawRoundRect(barColor, topLeft = Offset(cx - barW / 2f, bottom - h), size = Size(barW, h), cornerRadius = CornerRadius(r))
                if (h > r) drawRect(barColor, topLeft = Offset(cx - barW / 2f, bottom - r), size = Size(barW, r))
            }
            val l = measurer.measure(label, labelStyle)
            drawText(l, topLeft = Offset(cx - l.size.width / 2f, bottom + 4.dp.toPx()))
            if (i == data.lastIndex && minutes > 0) {
                val v = measurer.measure("$minutes", valueStyle)
                drawText(v, topLeft = Offset(cx - v.size.width / 2f, bottom - h - v.size.height - 2.dp.toPx()))
            }
        }
    }
}
