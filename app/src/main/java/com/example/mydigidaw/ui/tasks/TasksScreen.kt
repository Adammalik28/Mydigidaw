@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.mydigidaw.ui.tasks

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.mydigidaw.AppContainer
import com.example.mydigidaw.data.db.TaskEntity
import com.example.mydigidaw.data.model.Priority
import com.example.mydigidaw.data.model.enumOr
import com.example.mydigidaw.reminders.Notifications
import com.example.mydigidaw.ui.appViewModel
import com.example.mydigidaw.ui.components.SegmentedTabs
import com.example.mydigidaw.ui.components.Spacing
import com.example.mydigidaw.ui.components.BackTopBar
import com.example.mydigidaw.ui.components.ConfirmDialog
import com.example.mydigidaw.ui.components.DatePickerModal
import com.example.mydigidaw.ui.components.EmptyState
import com.example.mydigidaw.ui.components.LocalFeedback
import com.example.mydigidaw.ui.components.Motion
import com.example.mydigidaw.ui.components.SwipeAction
import com.example.mydigidaw.ui.components.SwipeRow
import com.example.mydigidaw.ui.components.TimePickerModal
import com.example.mydigidaw.ui.theme.AppColors
import com.example.mydigidaw.util.Dates
import com.example.mydigidaw.util.startMillis
import com.example.mydigidaw.util.toLocalDate
import com.example.mydigidaw.util.toLocalDateTime
import com.example.mydigidaw.util.toMillis
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TaskFilter(val label: String) {
    ACTIVE("Aktif"),
    TODAY("Hari ini"),
    UPCOMING("Terjadwal"),
    NO_DATE("Tanpa tanggal"),
    DONE("Selesai"),
}

enum class SectionTone { NORMAL, OVERDUE, DONE }

data class TaskSection(val title: String, val tasks: List<TaskEntity>, val tone: SectionTone = SectionTone.NORMAL)

object TaskSorting {
    private val byPriority = compareByDescending<TaskEntity> { enumOr(it.priority, Priority.MEDIUM).ordinal }

    fun sections(all: List<TaskEntity>, filter: TaskFilter, today: LocalDate): List<TaskSection> {
        val open = all.filter { !it.isDone }
        val dated = open.filter { it.dueAt != null }.sortedWith(compareBy<TaskEntity> { it.dueAt }.then(byPriority))
        val overdue = dated.filter { it.dueAt!!.toLocalDate().isBefore(today) }
        val dueToday = dated.filter { it.dueAt!!.toLocalDate() == today }
        val tomorrow = dated.filter { it.dueAt!!.toLocalDate() == today.plusDays(1) }
        val later = dated.filter { it.dueAt!!.toLocalDate().isAfter(today.plusDays(1)) }
        val noDate = open.filter { it.dueAt == null }.sortedWith(byPriority.thenByDescending { it.createdAt })
        val done = all.filter { it.isDone }.sortedByDescending { it.completedAt ?: 0 }
        return when (filter) {
            TaskFilter.ACTIVE -> listOf(
                TaskSection("Terlambat", overdue, SectionTone.OVERDUE),
                TaskSection("Hari ini", dueToday),
                TaskSection("Besok", tomorrow),
                TaskSection("Mendatang", later),
                TaskSection("Tanpa tanggal", noDate),
            )
            TaskFilter.TODAY -> listOf(TaskSection("Terlambat", overdue, SectionTone.OVERDUE), TaskSection("Hari ini", dueToday))
            TaskFilter.UPCOMING -> listOf(TaskSection("Besok", tomorrow), TaskSection("Mendatang", later))
            TaskFilter.NO_DATE -> listOf(TaskSection("Tanpa tanggal", noDate))
            TaskFilter.DONE -> listOf(TaskSection("Selesai", done, SectionTone.DONE))
        }.filter { it.tasks.isNotEmpty() }
    }
}

class TasksViewModel(private val c: AppContainer) : ViewModel() {
    val filter = MutableStateFlow(TaskFilter.ACTIVE)
    val sections: StateFlow<List<TaskSection>?> = combine(c.tasks.tasks, filter) { list, f ->
        TaskSorting.sections(list, f, LocalDate.now())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun toggle(task: TaskEntity) = viewModelScope.launch { c.tasks.setDone(task.id, !task.isDone) }
    fun save(task: TaskEntity) = viewModelScope.launch { c.tasks.save(task) }
    fun delete(id: Long) = viewModelScope.launch { c.tasks.delete(id) }
    fun restore(task: TaskEntity) = c.appScope.launch { c.tasks.restore(task) }
    fun clearDone() = viewModelScope.launch { c.tasks.deleteDone() }
}

@Composable
fun TasksScreen(openNew: Boolean, onBack: () -> Unit) {
    val vm = appViewModel { TasksViewModel(it) }
    val feedback = LocalFeedback.current
    val sections by vm.sections.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<TaskEntity?>(null) }
    var creating by rememberSaveable { mutableStateOf(openNew) }
    var menu by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            BackTopBar(
                title = "Tugas",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Menu") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Hapus semua tugas selesai") },
                            onClick = {
                                menu = false
                                confirmClear = true
                            },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { creating = true }, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Tugas") })
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TaskFilter.entries.forEach { f ->
                    FilterChip(selected = filter == f, onClick = { vm.filter.value = f }, label = { Text(f.label) })
                }
            }
            val list = sections ?: return@Column
            if (list.isEmpty()) {
                EmptyState(
                    emoji = if (filter == TaskFilter.DONE) "🏁" else "✅",
                    title = if (filter == TaskFilter.DONE) "Belum ada tugas selesai" else "Semua beres!",
                    message = if (filter == TaskFilter.DONE) null else "Tidak ada tugas di sini. Tambahkan tugas baru dengan tombol di bawah.",
                )
                return@Column
            }
            LazyColumn(contentPadding = PaddingValues(bottom = Spacing.fabClearance)) {
                list.forEach { section ->
                    item(key = "h_${section.title}") {
                        Text(
                            "${section.title} · ${section.tasks.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (section.tone == SectionTone.OVERDUE) AppColors.current.criticalText else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
                        )
                    }
                    items(section.tasks, key = { it.id }) { task ->
                        SwipeRow(
                            modifier = Modifier.animateItem(),
                            startAction = SwipeAction(
                                if (task.isDone) Icons.AutoMirrored.Outlined.Undo else Icons.Filled.Check,
                                if (task.isDone) "Belum selesai" else "Selesai",
                                AppColors.current.good,
                            ) { vm.toggle(task) },
                            endAction = SwipeAction(Icons.Outlined.Delete, "Hapus", AppColors.current.critical) {
                                vm.delete(task.id)
                                feedback.undo("Tugas \"${task.title}\" dihapus") { vm.restore(task) }
                            },
                        ) {
                            TaskRow(
                                task,
                                overdue = section.tone == SectionTone.OVERDUE,
                                onToggle = {
                                    feedback.toggle(!task.isDone)
                                    vm.toggle(task)
                                },
                                onClick = { editing = task },
                            )
                        }
                    }
                }
            }
        }
    }

    if (creating || editing != null) {
        TaskSheet(
            initial = editing,
            onDismiss = {
                creating = false
                editing = null
            },
            onSave = { vm.save(it) },
            onDelete = { vm.delete(it) },
        )
    }
    if (confirmClear) {
        ConfirmDialog(
            title = "Hapus tugas selesai?",
            message = "Semua tugas yang sudah dicentang akan dihapus permanen.",
            onConfirm = { vm.clearDone() },
            onDismiss = { confirmClear = false },
        )
    }
}

fun taskDueLabel(task: TaskEntity, today: LocalDate = LocalDate.now()): String? {
    val due = task.dueAt ?: return null
    val day = Dates.relativeDay(due.toLocalDate(), today)
    return if (task.hasTime) "$day, ${Dates.time(due)}" else day
}

@Composable
fun TaskRow(task: TaskEntity, overdue: Boolean, onToggle: () -> Unit, onClick: () -> Unit) {
    val ext = AppColors.current
    val priority = enumOr(task.priority, Priority.MEDIUM)
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 4.dp, end = 16.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = task.isDone, onCheckedChange = { onToggle() })
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(
                task.title,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                color = animateColorAsState(
                    if (task.isDone) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                    tween(Motion.MEDIUM),
                    label = "taskText",
                ).value,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val due = taskDueLabel(task)
            if (due != null || task.notes.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (due != null) {
                        Icon(
                            Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (overdue) ext.criticalText else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            due,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (overdue) ext.criticalText else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (task.remind && task.hasTime) {
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Outlined.NotificationsActive, contentDescription = "Pengingat", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (task.notes.isNotBlank()) Spacer(Modifier.width(8.dp))
                    }
                    if (task.notes.isNotBlank()) {
                        Text(
                            task.notes,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        if (!task.isDone && priority != Priority.LOW) {
            Icon(
                Icons.Filled.Flag,
                contentDescription = "Prioritas ${priority.label}",
                tint = if (priority == Priority.HIGH) ext.critical else ext.warning,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
fun TaskSheet(
    initial: TaskEntity?,
    onDismiss: () -> Unit,
    onSave: (TaskEntity) -> Unit,
    onDelete: (Long) -> Unit,
    defaultDate: LocalDate? = null,
) {
    val context = LocalContext.current
    val initialDue = initial?.dueAt?.toLocalDateTime()
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var date by remember { mutableStateOf(initialDue?.toLocalDate() ?: defaultDate) }
    var time by remember { mutableStateOf(if (initial?.hasTime == true) initialDue?.toLocalTime() else null) }
    var priority by remember { mutableStateOf(enumOr(initial?.priority, Priority.MEDIUM)) }
    var remind by remember { mutableStateOf(initial?.remind ?: true) }
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    LaunchedEffect(Unit) { if (initial == null) runCatching { focus.requestFocus() } }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().imePadding().padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(if (initial == null) "Tugas baru" else "Ubah tugas", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Apa yang perlu dikerjakan?") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Catatan (opsional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                val today = LocalDate.now()
                listOf("Hari ini" to today, "Besok" to today.plusDays(1)).forEach { (label, d) ->
                    FilterChip(selected = date == d, onClick = { date = if (date == d) null else d }, label = { Text(label) })
                }
                if (date != null) {
                    InputChip(
                        selected = true,
                        onClick = { pickDate = true },
                        label = { Text(Dates.weekdayDayMonth(date!!)) },
                        leadingIcon = { Icon(Icons.Outlined.CalendarToday, null, Modifier.size(16.dp)) },
                        trailingIcon = {
                            Text("✕", modifier = Modifier.clickable {
                                date = null
                                time = null
                            })
                        },
                    )
                } else {
                    AssistChip(
                        onClick = { pickDate = true },
                        label = { Text("Pilih tanggal") },
                        leadingIcon = { Icon(Icons.Outlined.CalendarToday, null, Modifier.size(16.dp)) },
                    )
                }
                if (date != null) {
                    if (time != null) {
                        InputChip(
                            selected = true,
                            onClick = { pickTime = true },
                            label = { Text(Dates.time(time!!)) },
                            leadingIcon = { Icon(Icons.Outlined.Schedule, null, Modifier.size(16.dp)) },
                            trailingIcon = { Text("✕", modifier = Modifier.clickable { time = null }) },
                        )
                    } else {
                        AssistChip(
                            onClick = { pickTime = true },
                            label = { Text("Jam") },
                            leadingIcon = { Icon(Icons.Outlined.Schedule, null, Modifier.size(16.dp)) },
                        )
                    }
                }
            }
            Text("Prioritas", style = MaterialTheme.typography.labelLarge)
            SegmentedTabs(
                labels = Priority.entries.map { it.label },
                selectedIndex = Priority.entries.indexOf(priority),
                onSelect = { priority = Priority.entries[it] },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Ingatkan saya", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (time == null) "Atur tanggal & jam untuk mengaktifkan pengingat" else "Notifikasi muncul tepat pada jam tersebut",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = remind && time != null, onCheckedChange = { remind = it }, enabled = time != null)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                if (initial != null) {
                    TextButton(
                        onClick = {
                            onDelete(initial.id)
                            onDismiss()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text("Hapus") }
                    Spacer(Modifier.weight(1f))
                }
                TextButton(onClick = onDismiss) { Text("Batal") }
                Spacer(Modifier.width(8.dp))
                Button(
                    enabled = title.isNotBlank(),
                    onClick = {
                        val d = date
                        val t = time
                        val dueAt = when {
                            d == null -> null
                            t == null -> d.startMillis()
                            else -> d.atTime(t).toMillis()
                        }
                        val wantsReminder = remind && t != null
                        if (wantsReminder && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Notifications.canPost(context)) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        val base = initial ?: TaskEntity(title = "", createdAt = System.currentTimeMillis())
                        onSave(
                            base.copy(
                                title = title.trim(),
                                notes = notes.trim(),
                                priority = priority.name,
                                dueAt = dueAt,
                                hasTime = t != null,
                                remind = wantsReminder,
                            ),
                        )
                        onDismiss()
                    },
                ) { Text("Simpan") }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (pickDate) DatePickerModal(initial = date ?: LocalDate.now(), onDismiss = { pickDate = false }, onPick = { date = it })
    if (pickTime) {
        TimePickerModal(
            initial = time ?: LocalTime.now().plusHours(1).withMinute(0),
            onDismiss = { pickTime = false },
            onPick = {
                time = it
                remind = true
            },
        )
    }
}
