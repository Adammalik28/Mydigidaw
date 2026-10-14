@file:OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)

package com.example.mydigidaw.ui.calendar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mydigidaw.AppContainer
import com.example.mydigidaw.data.db.EventEntity
import com.example.mydigidaw.data.db.TaskEntity
import com.example.mydigidaw.data.google.DeviceCalendar
import com.example.mydigidaw.data.model.Repeat
import com.example.mydigidaw.data.model.enumOr
import com.example.mydigidaw.data.prefs.AppSettings
import com.example.mydigidaw.data.repo.Agenda
import com.example.mydigidaw.data.repo.CalendarRepository
import com.example.mydigidaw.util.endMillis
import com.example.mydigidaw.util.startMillis
import com.example.mydigidaw.util.toLocalDate
import com.example.mydigidaw.util.toLocalDateTime
import com.example.mydigidaw.util.toMillis
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Tanggal pertama di grid bulan (6 minggu) sesuai pengaturan awal minggu. */
fun gridStart(month: YearMonth, mondayFirst: Boolean): LocalDate {
    val first = month.atDay(1)
    val offset = if (mondayFirst) first.dayOfWeek.value - 1 else first.dayOfWeek.value % 7
    return first.minusDays(offset.toLong())
}

class CalendarViewModel(private val c: AppContainer) : ViewModel() {
    val selectedDate = MutableStateFlow(LocalDate.now())
    val visibleMonth = MutableStateFlow(YearMonth.now())
    private val refresh = MutableStateFlow(0)
    val hasPermission = MutableStateFlow(c.google.hasPermission())

    val settings: StateFlow<AppSettings> = c.settings.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val monthAgenda: StateFlow<Map<LocalDate, List<Agenda>>> =
        combine(visibleMonth, c.calendar.events, c.settings.settings, refresh) { month, events, s, _ -> Triple(month, events, s) }
            .mapLatest { (month, events, s) ->
                val from = gridStart(month, s.weekStartsMonday)
                val to = from.plusDays(42)
                val items = c.calendar.agenda(events, s, from.startMillis(), to.startMillis())
                CalendarRepository.groupByDate(items, from, to)
            }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Jadwal 30 hari ke depan untuk tampilan agenda. */
    val upcoming: StateFlow<List<Pair<LocalDate, List<Agenda>>>> =
        combine(c.calendar.events, c.settings.settings, refresh) { events, s, _ -> events to s }
            .mapLatest { (events, s) ->
                val from = LocalDate.now()
                val to = from.plusDays(30)
                val items = c.calendar.agenda(events, s, from.startMillis(), to.startMillis())
                CalendarRepository.groupByDate(items, from, to).toSortedMap().toList()
            }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tasksByDate: StateFlow<Map<LocalDate, List<TaskEntity>>> = c.tasks.tasks
        .map { list -> list.filter { it.dueAt != null }.groupBy { it.dueAt!!.toLocalDate() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    init {
        viewModelScope.launch {
            c.google.changes().debounce(800).collect { refresh.update { it + 1 } }
        }
    }

    fun select(date: LocalDate) {
        selectedDate.value = date
    }

    fun setMonth(month: YearMonth) {
        visibleMonth.value = month
    }

    var refreshing by mutableStateOf(false)
        private set

    /** Tarik-untuk-segarkan: minta akun Google menyinkronkan kalender, lalu muat ulang. */
    fun pullRefresh() = viewModelScope.launch {
        refreshing = true
        c.google.requestSync()
        delay(1_200)
        refreshNow()
        refreshing = false
    }

    fun refreshNow() {
        hasPermission.value = c.google.hasPermission()
        refresh.update { it + 1 }
    }

    /** Setelah izin kalender diberikan: aktifkan tampilan & sinkron ke kalender Google utama. */
    fun onPermissionResult(granted: Boolean) {
        hasPermission.value = granted
        if (!granted) return
        viewModelScope.launch {
            c.settings.setGcalShow(true)
            val s = c.settings.current()
            if (s.gcalTargetId < 0) {
                val target = c.google.calendars().filter { it.writable }
                    .let { list -> list.firstOrNull { it.isGoogle && it.isPrimary } ?: list.firstOrNull { it.isGoogle } ?: list.firstOrNull() }
                if (target != null) {
                    c.settings.setGcalTarget(target.id)
                    c.settings.setGcalSyncNew(true)
                }
            }
            refresh.update { it + 1 }
        }
    }

    fun enableShowGoogle() = viewModelScope.launch {
        c.settings.setGcalShow(true)
        refresh.update { it + 1 }
    }

    fun deleteGoogleEvent(eventId: Long) = viewModelScope.launch {
        c.calendar.deleteGoogleEvent(eventId)
        refresh.update { it + 1 }
    }

    fun toggleTask(task: TaskEntity) = viewModelScope.launch { c.tasks.setDone(task.id, !task.isDone) }
}

data class ReminderOption(val minutes: Int, val label: String, val allDayLabel: String = label)

val reminderOptions = listOf(
    ReminderOption(-1, "Tanpa pengingat"),
    ReminderOption(0, "Saat dimulai", "Pada hari H (07.00)"),
    ReminderOption(5, "5 menit sebelumnya"),
    ReminderOption(10, "10 menit sebelumnya"),
    ReminderOption(15, "15 menit sebelumnya"),
    ReminderOption(30, "30 menit sebelumnya"),
    ReminderOption(60, "1 jam sebelumnya"),
    ReminderOption(120, "2 jam sebelumnya"),
    ReminderOption(1440, "1 hari sebelumnya", "Sehari sebelumnya (07.00)"),
)

enum class SaveResult { SAVED, SAVED_LOCAL_ONLY, INVALID }

class EventEditorViewModel(
    private val eventId: Long,
    initialDate: LocalDate?,
    private val c: AppContainer,
) : ViewModel() {
    val isNew = eventId <= 0
    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var location by mutableStateOf("")
    var allDay by mutableStateOf(false)
    var startDate by mutableStateOf(initialDate ?: LocalDate.now())
        private set
    var startTime by mutableStateOf(defaultStart(initialDate ?: LocalDate.now()))
        private set
    var endDate by mutableStateOf(initialDate ?: LocalDate.now())
    var endTime by mutableStateOf(startTime.plusHours(1))
    var repeat by mutableStateOf(Repeat.NONE)
    var reminder by mutableIntStateOf(10)
    var color by mutableIntStateOf(0)
    var syncGoogle by mutableStateOf(false)
    var calendarId by mutableStateOf<Long?>(null)
    var calendars by mutableStateOf<List<DeviceCalendar>>(emptyList())
        private set
    var hasPermission by mutableStateOf(c.google.hasPermission())
        private set
    var titleError by mutableStateOf(false)
    var loaded by mutableStateOf(false)
        private set
    private var original: EventEntity? = null

    init {
        if (startTime == LocalTime.of(23, 0)) endDate = startDate.plusDays(1)
        viewModelScope.launch {
            loadCalendars()
            val s = c.settings.current()
            if (!isNew) {
                c.calendar.get(eventId)?.let(::load)
            } else {
                syncGoogle = s.gcalSyncNew && hasPermission && calendars.isNotEmpty()
                calendarId = s.gcalTargetId.takeIf { id -> calendars.any { it.id == id } } ?: defaultCalendar()
            }
            loaded = true
        }
    }

    private fun defaultCalendar(): Long? =
        (calendars.firstOrNull { it.isGoogle && it.isPrimary } ?: calendars.firstOrNull { it.isGoogle } ?: calendars.firstOrNull())?.id

    private suspend fun loadCalendars() {
        hasPermission = c.google.hasPermission()
        calendars = c.google.calendars().filter { it.writable }
    }

    fun onPermissionResult(granted: Boolean) {
        if (!granted) return
        viewModelScope.launch {
            loadCalendars()
            if (calendarId == null) calendarId = defaultCalendar()
            syncGoogle = calendars.isNotEmpty()
        }
    }

    private fun load(e: EventEntity) {
        original = e
        title = e.title
        description = e.description
        location = e.location
        allDay = e.allDay
        val start = e.startAt.toLocalDateTime()
        startDate = start.toLocalDate()
        startTime = start.toLocalTime()
        if (e.allDay) {
            endDate = (e.endAt - 1).toLocalDate().let { if (it.isBefore(startDate)) startDate else it }
            endTime = startTime.plusHours(1)
        } else {
            val end = e.endAt.toLocalDateTime()
            endDate = end.toLocalDate()
            endTime = end.toLocalTime()
        }
        repeat = enumOr(e.repeat, Repeat.NONE)
        reminder = e.reminderMinutes
        color = e.color
        syncGoogle = e.googleEventId != null
        calendarId = e.googleCalendarId?.takeIf { id -> calendars.any { it.id == id } } ?: defaultCalendar()
    }

    /** Menggeser tanggal/jam selesai agar durasi tetap sama. */
    fun changeStartDate(date: LocalDate) {
        val days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate)
        startDate = date
        endDate = date.plusDays(days.coerceAtLeast(0))
    }

    fun changeStartTime(time: LocalTime) {
        val start = LocalDateTime.of(startDate, startTime)
        val end = LocalDateTime.of(endDate, endTime)
        val minutes = java.time.Duration.between(start, end).toMinutes().coerceAtLeast(0)
        startTime = time
        val newEnd = LocalDateTime.of(startDate, time).plusMinutes(minutes)
        endDate = newEnd.toLocalDate()
        endTime = newEnd.toLocalTime()
    }

    fun applyTemplate(emoji: String, name: String) {
        title = "$emoji $name"
        titleError = false
    }

    suspend fun save(): SaveResult {
        titleError = title.isBlank()
        if (titleError) return SaveResult.INVALID
        val now = System.currentTimeMillis()
        val startAt: Long
        val endAt: Long
        if (allDay) {
            startAt = startDate.startMillis()
            endAt = maxOf(endDate, startDate).endMillis()
        } else {
            startAt = startDate.atTime(startTime).toMillis()
            endAt = maxOf(endDate.atTime(endTime).toMillis(), startAt)
        }
        val entity = EventEntity(
            id = original?.id ?: 0,
            title = title.trim(),
            description = description.trim(),
            location = location.trim(),
            startAt = startAt,
            endAt = endAt,
            allDay = allDay,
            color = color,
            repeat = repeat.name,
            reminderMinutes = reminder,
            googleEventId = original?.googleEventId,
            googleCalendarId = original?.googleCalendarId,
            createdAt = original?.createdAt ?: now,
            updatedAt = now,
        )
        val wantGoogle = syncGoogle && hasPermission && calendarId != null
        val ok = c.calendar.save(entity, wantGoogle, calendarId)
        return if (wantGoogle && !ok) SaveResult.SAVED_LOCAL_ONLY else SaveResult.SAVED
    }

    suspend fun delete() {
        original?.let { c.calendar.delete(it) }
    }

    private companion object {
        fun defaultStart(date: LocalDate): LocalTime =
            if (date == LocalDate.now()) {
                val next = LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)
                if (LocalTime.now().hour >= 23) LocalTime.of(23, 0) else next
            } else {
                LocalTime.of(9, 0)
            }
    }
}
