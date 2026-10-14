@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.mydigidaw.ui.calendar

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mydigidaw.ui.components.Spacing
import com.example.mydigidaw.ui.components.AppCard
import com.example.mydigidaw.ui.components.IconTile
import com.example.mydigidaw.ui.components.InlineEmpty
import com.example.mydigidaw.ui.components.SectionTitle
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material.icons.outlined.EventAvailable
import com.example.mydigidaw.data.db.TaskEntity
import com.example.mydigidaw.data.repo.Agenda
import com.example.mydigidaw.data.repo.AgendaSource
import com.example.mydigidaw.ui.LocalAppContainer
import com.example.mydigidaw.ui.appViewModel
import com.example.mydigidaw.ui.components.ConfirmDialog
import com.example.mydigidaw.ui.components.EmptyState
import com.example.mydigidaw.ui.components.LocalFeedback
import com.example.mydigidaw.ui.components.Motion
import com.example.mydigidaw.ui.navigation.Routes
import com.example.mydigidaw.ui.theme.AppColors
import com.example.mydigidaw.util.Dates
import com.example.mydigidaw.util.toLocalDate
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch

private const val BASE_PAGE = 1200
private val CELL_HEIGHT = 50.dp

@Composable
fun agendaColor(item: Agenda): Color = item.colorArgb?.let { Color(it) } ?: AppColors.current.seriesColor(item.colorIndex)

fun agendaTimeText(item: Agenda): String {
    val startDate = item.start.toLocalDate()
    return if (item.allDay) {
        val last = (maxOf(item.end, item.start + 1) - 1).toLocalDate()
        if (last == startDate) "Sepanjang hari · ${Dates.full(startDate)}"
        else "${Dates.medium(startDate)} – ${Dates.medium(last)} · sepanjang hari"
    } else {
        val endDate = item.end.toLocalDate()
        if (endDate == startDate) "${Dates.full(startDate)}, ${Dates.time(item.start)}–${Dates.time(item.end)}"
        else "${Dates.dateTime(item.start)} – ${Dates.dateTime(item.end)}"
    }
}

fun openInGoogleCalendar(context: Context, item: Agenda) {
    val id = item.googleEventId ?: return
    val intent = Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id))
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, item.start)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, item.end)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "Aplikasi Google Calendar tidak ditemukan", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun CalendarScreen(onNavigate: (String) -> Unit) {
    val vm = appViewModel { CalendarViewModel(it) }
    val feedback = LocalFeedback.current
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val selected by vm.selectedDate.collectAsStateWithLifecycle()
    val month by vm.visibleMonth.collectAsStateWithLifecycle()
    val agenda by vm.monthAgenda.collectAsStateWithLifecycle()
    val tasks by vm.tasksByDate.collectAsStateWithLifecycle()
    val hasPermission by vm.hasPermission.collectAsStateWithLifecycle()
    var agendaMode by rememberSaveable { mutableStateOf(false) }
    var bannerDismissed by rememberSaveable { mutableStateOf(false) }
    var googleDetail by remember { mutableStateOf<Agenda?>(null) }

    val baseMonth = remember { YearMonth.now() }
    val pagerState = rememberPagerState(initialPage = BASE_PAGE + ChronoUnit.MONTHS.between(baseMonth, month).toInt()) { BASE_PAGE * 2 }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { vm.setMonth(baseMonth.plusMonths((it - BASE_PAGE).toLong())) }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        vm.onPermissionResult(result.values.all { it })
    }
    LifecycleResumeEffect(Unit) {
        vm.refreshNow()
        onPauseOrDispose { }
    }

    val open: (Agenda) -> Unit = { item ->
        val localId = item.localId
        if (item.source == AgendaSource.LOCAL && localId != null) onNavigate(Routes.eventEdit(localId)) else googleDetail = item
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (agendaMode) "Agenda 30 hari" else Dates.monthYear(month)) },
                actions = {
                    if (!agendaMode) {
                        TextButton(onClick = {
                            vm.select(LocalDate.now())
                            scope.launch { pagerState.animateScrollToPage(BASE_PAGE) }
                        }) { Text("Hari ini") }
                    }
                    IconButton(onClick = { agendaMode = !agendaMode }) {
                        Icon(
                            if (agendaMode) Icons.Outlined.CalendarMonth else Icons.AutoMirrored.Outlined.ViewList,
                            contentDescription = if (agendaMode) "Tampilan bulan" else "Tampilan agenda",
                        )
                    }
                    if (hasPermission && settings.gcalShow) {
                        IconButton(onClick = { vm.refreshNow() }) { Icon(Icons.Outlined.Sync, contentDescription = "Muat ulang") }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigate(Routes.eventEdit(date = selected)) }) {
                Icon(Icons.Filled.Add, contentDescription = "Jadwal baru")
            }
        },
    ) { padding ->
        if (agendaMode) {
            AgendaView(vm, Modifier.padding(padding), onOpen = open)
            return@Scaffold
        }
        PullToRefreshBox(
            isRefreshing = vm.refreshing,
            onRefresh = {
                feedback.tick()
                vm.pullRefresh()
            },
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Spacing.fabClearance)) {
            if ((!hasPermission || !settings.gcalShow) && !bannerDismissed) {
                item {
                    GoogleBanner(
                        hasPermission = hasPermission,
                        onConnect = {
                            if (!hasPermission) {
                                permissionLauncher.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR))
                            } else {
                                vm.enableShowGoogle()
                            }
                        },
                        onDismiss = { bannerDismissed = true },
                    )
                }
            }
            item {
                AppCard(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Column(Modifier.padding(vertical = 8.dp)) {
                        WeekdayHeader(settings.weekStartsMonday)
                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().height(CELL_HEIGHT * 6)) { page ->
                            MonthGrid(
                                month = baseMonth.plusMonths((page - BASE_PAGE).toLong()),
                                selected = selected,
                                agenda = agenda,
                                tasks = tasks,
                                mondayFirst = settings.weekStartsMonday,
                                onSelect = vm::select,
                            )
                        }
                    }
                }
            }
            val dayItems = agenda[selected].orEmpty()
            val dayTasks = tasks[selected].orEmpty()
            item {
                SectionTitle(
                    Dates.full(selected),
                    Modifier.padding(top = 0.dp),
                    trailing = {
                        val count = dayItems.size + dayTasks.size
                        if (count > 0) {
                            Text(
                                "$count item",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                        }
                    },
                )
            }
            if (dayItems.isEmpty() && dayTasks.isEmpty()) {
                item {
                    AppCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        InlineEmpty(
                            Icons.Outlined.EventAvailable,
                            "Belum ada jadwal di tanggal ini",
                            actionLabel = "Tambah",
                            onAction = { onNavigate(Routes.eventEdit(date = selected)) },
                        )
                    }
                }
            }
            items(dayItems, key = { it.key }) { item -> Box(Modifier.animateItem()) { AgendaRow(item, onClick = { open(item) }) } }
            if (dayTasks.isNotEmpty()) {
                item {
                    Text(
                        "Tugas",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                    )
                }
                items(dayTasks, key = { "t${it.id}" }) { task ->
                    TaskMiniRow(task, onToggle = { vm.toggleTask(task) }, onClick = { onNavigate(Routes.tasks()) })
                }
            }
        }
        }
    }

    googleDetail?.let { item ->
        GoogleEventSheet(
            item = item,
            onDismiss = { googleDetail = null },
            onOpenInGoogle = {
                container.appLock.allowExternalFlow()
                openInGoogleCalendar(context, item)
            },
            onDelete = {
                item.googleEventId?.let(vm::deleteGoogleEvent)
                googleDetail = null
            },
        )
    }
}

@Composable
private fun GoogleBanner(hasPermission: Boolean, onConnect: () -> Unit, onDismiss: () -> Unit) {
    AppCard(
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Row(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile(Icons.Outlined.Sync, MaterialTheme.colorScheme.primary, size = 36.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Google Calendar", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (!hasPermission) "Sinkronkan jadwal dengan akun Google" else "Tampilkan acara Google di sini",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            FilledTonalButton(
                onClick = onConnect,
                contentPadding = PaddingValues(horizontal = 14.dp),
                modifier = Modifier.height(34.dp),
            ) { Text(if (!hasPermission) "Hubungkan" else "Tampilkan", style = MaterialTheme.typography.labelMedium) }
            IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Tutup", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun WeekdayHeader(mondayFirst: Boolean) {
    val labels = if (mondayFirst) Dates.weekdayShort else listOf(Dates.weekdayShort.last()) + Dates.weekdayShort.dropLast(1)
    val sundayIndex = if (mondayFirst) 6 else 0
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 2.dp, bottom = 6.dp)) {
        labels.forEachIndexed { i, label ->
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (i == sundayIndex) AppColors.current.criticalText else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    selected: LocalDate,
    agenda: Map<LocalDate, List<Agenda>>,
    tasks: Map<LocalDate, List<TaskEntity>>,
    mondayFirst: Boolean,
    onSelect: (LocalDate) -> Unit,
) {
    val start = gridStart(month, mondayFirst)
    val today = LocalDate.now()
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        for (week in 0 until 6) {
            Row(Modifier.fillMaxWidth()) {
                for (day in 0 until 7) {
                    val date = start.plusDays((week * 7 + day).toLong())
                    DayCell(
                        date = date,
                        inMonth = YearMonth.from(date) == month,
                        isToday = date == today,
                        isSelected = date == selected,
                        items = agenda[date].orEmpty(),
                        hasTasks = tasks[date].orEmpty().any { !it.isDone },
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(date) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    items: List<Agenda>,
    hasTasks: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val holiday = date.dayOfWeek == DayOfWeek.SUNDAY || items.any { it.isHoliday }
    val selectedBg by animateColorAsState(if (isSelected) scheme.primary else Color.Transparent, tween(Motion.SHORT), label = "daySel")
    val selectScale by animateFloatAsState(if (isSelected) 1f else 0.85f, spring(dampingRatio = 0.5f), label = "dayScale")
    val numberColor = when {
        isSelected -> scheme.onPrimary
        !inMonth -> scheme.onSurface.copy(alpha = 0.35f)
        holiday -> AppColors.current.criticalText
        else -> scheme.onSurface
    }
    Column(
        modifier
            .height(CELL_HEIGHT)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .graphicsLayer {
                    scaleX = if (isSelected) selectScale else 1f
                    scaleY = if (isSelected) selectScale else 1f
                }
                .clip(CircleShape)
                .background(selectedBg)
                .then(if (isToday && !isSelected) Modifier.border(1.5.dp, scheme.primary, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = numberColor,
            )
        }
        Spacer(Modifier.height(3.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(6.dp)) {
            items.take(3).forEach { a ->
                Box(Modifier.size(5.dp).clip(CircleShape).background(agendaColor(a).copy(alpha = if (inMonth) 1f else 0.4f)))
            }
            if (hasTasks && items.size < 3) {
                Box(Modifier.size(5.dp).border(1.dp, scheme.onSurfaceVariant, CircleShape))
            }
        }
    }
}

@Composable
fun AgendaRow(item: Agenda, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.width(56.dp)) {
            if (item.allDay) {
                Text("Seharian", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(Dates.time(item.start), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Text(Dates.time(item.end), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Box(Modifier.width(4.dp).height(40.dp).clip(RoundedCornerShape(2.dp)).background(agendaColor(item)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val sub = listOfNotNull(
                item.location.takeIf { it.isNotBlank() }?.let { "📍 $it" },
                if (item.source == AgendaSource.GOOGLE) item.calendarName else null,
            ).joinToString(" · ")
            if (sub.isNotBlank()) {
                Text(
                    sub,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        val iconMod = Modifier.padding(start = 6.dp).size(16.dp)
        val tint = MaterialTheme.colorScheme.onSurfaceVariant
        if (item.isRecurring) Icon(Icons.Outlined.Repeat, contentDescription = "Berulang", tint = tint, modifier = iconMod)
        if (item.hasReminder) Icon(Icons.Outlined.NotificationsNone, contentDescription = "Pengingat", tint = tint, modifier = iconMod)
        if (item.synced) Icon(Icons.Outlined.CloudDone, contentDescription = "Google Calendar", tint = tint, modifier = iconMod)
    }
}

@Composable
private fun TaskMiniRow(task: TaskEntity, onToggle: () -> Unit, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = task.isDone, onCheckedChange = { onToggle() })
        Text(
            task.title,
            style = MaterialTheme.typography.bodyLarge,
            textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
            color = if (task.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (task.hasTime && task.dueAt != null) {
            Text(
                Dates.time(task.dueAt),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 16.dp),
            )
        }
    }
}

@Composable
private fun AgendaView(vm: CalendarViewModel, modifier: Modifier, onOpen: (Agenda) -> Unit) {
    val upcoming by vm.upcoming.collectAsStateWithLifecycle()
    if (upcoming.isEmpty()) {
        EmptyState(
            emoji = "🗓️",
            title = "Tidak ada jadwal",
            message = "Belum ada jadwal dalam 30 hari ke depan.",
            modifier = modifier,
        )
        return
    }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Spacing.fabClearance)) {
        upcoming.forEach { (date, items) ->
            item(key = "d$date") {
                Text(
                    if (date == LocalDate.now() || date == LocalDate.now().plusDays(1)) "${Dates.relativeDay(date)} · ${Dates.long(date)}"
                    else Dates.full(date),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 2.dp),
                )
            }
            items(items, key = { "${date}_${it.key}" }) { item -> AgendaRow(item, onClick = { onOpen(item) }) }
        }
    }
}

@Composable
private fun GoogleEventSheet(item: Agenda, onDismiss: () -> Unit, onOpenInGoogle: () -> Unit, onDelete: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).background(agendaColor(item)))
                Spacer(Modifier.width(12.dp))
                Text(item.title, style = MaterialTheme.typography.titleLarge)
            }
            Text(agendaTimeText(item), style = MaterialTheme.typography.bodyMedium)
            item.calendarName?.takeIf { it.isNotBlank() }?.let { Text("📅 $it", style = MaterialTheme.typography.bodyMedium) }
            if (item.location.isNotBlank()) Text("📍 ${item.location}", style = MaterialTheme.typography.bodyMedium)
            if (item.description.isNotBlank()) {
                Text(item.description, style = MaterialTheme.typography.bodySmall, maxLines = 8, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onOpenInGoogle) { Text("Buka di Google Calendar") }
                if (!item.isRecurring) OutlinedButton(onClick = { confirm = true }) { Text("Hapus") }
            }
            if (item.isRecurring) {
                Text(
                    "Acara berulang dapat diubah atau dihapus dari aplikasi Google Calendar.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    if (confirm) {
        ConfirmDialog(
            title = "Hapus acara?",
            message = "\"${item.title}\" akan dihapus dari Google Calendar.",
            onConfirm = onDelete,
            onDismiss = { confirm = false },
        )
    }
}
