package com.example.mydigidaw.data.repo

import com.example.mydigidaw.data.db.AppDatabase
import com.example.mydigidaw.data.db.EventEntity
import com.example.mydigidaw.data.google.GoogleCalendarSource
import com.example.mydigidaw.data.model.Recurrence
import com.example.mydigidaw.data.model.Repeat
import com.example.mydigidaw.data.model.enumOr
import com.example.mydigidaw.data.prefs.AppSettings
import com.example.mydigidaw.reminders.ReminderScheduler
import com.example.mydigidaw.util.toLocalDate
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

enum class AgendaSource { LOCAL, GOOGLE }

/** Satu kejadian jadwal yang tampil di kalender, baik jadwal lokal maupun dari Google Calendar. */
data class Agenda(
    val key: String,
    val source: AgendaSource,
    val localId: Long? = null,
    val googleEventId: Long? = null,
    val title: String,
    val start: Long,
    val end: Long,
    val allDay: Boolean,
    val colorIndex: Int = 0,
    val colorArgb: Int? = null,
    val location: String = "",
    val description: String = "",
    val calendarName: String? = null,
    val synced: Boolean = false,
    val hasReminder: Boolean = false,
    val isHoliday: Boolean = false,
    val isRecurring: Boolean = false,
)

class CalendarRepository(
    private val db: AppDatabase,
    val google: GoogleCalendarSource,
    private val reminders: ReminderScheduler,
) {
    val events: Flow<List<EventEntity>> = db.events().observeAll()

    suspend fun get(id: Long): EventEntity? = db.events().get(id)

    fun expand(events: List<EventEntity>, from: Long, to: Long): List<Agenda> = events.flatMap { e ->
        val rule = enumOr(e.repeat, Repeat.NONE)
        Recurrence.occurrences(e.startAt, e.endAt, rule, from, to).map { (s, end) ->
            Agenda(
                key = "L${e.id}_$s",
                source = AgendaSource.LOCAL,
                localId = e.id,
                googleEventId = e.googleEventId,
                title = e.title,
                start = s,
                end = end,
                allDay = e.allDay,
                colorIndex = e.color,
                location = e.location,
                description = e.description,
                synced = e.googleEventId != null,
                hasReminder = e.reminderMinutes >= 0,
                isRecurring = rule != Repeat.NONE,
            )
        }
    }

    suspend fun googleAgenda(from: Long, to: Long, settings: AppSettings, mirrored: Set<Long>): List<Agenda> {
        if (!settings.gcalShow || !google.hasPermission()) return emptyList()
        return google.instances(from, to, settings.gcalHiddenIds)
            .filter { it.eventId !in mirrored }
            .map { g ->
                val holidayCalendar = g.calendarName.contains("libur", true) || g.calendarName.contains("holiday", true)
                Agenda(
                    key = "G${g.eventId}_${g.begin}",
                    source = AgendaSource.GOOGLE,
                    googleEventId = g.eventId,
                    title = g.title,
                    start = g.begin,
                    end = g.end,
                    allDay = g.allDay,
                    colorArgb = g.color,
                    location = g.location,
                    description = g.description,
                    calendarName = g.calendarName,
                    synced = true,
                    isHoliday = g.allDay && holidayCalendar,
                    isRecurring = g.isRecurring,
                )
            }
    }

    /** Gabungan jadwal lokal + Google untuk rentang waktu [from, to). */
    suspend fun agenda(events: List<EventEntity>, settings: AppSettings, from: Long, to: Long): List<Agenda> {
        val mirrored = events.mapNotNull { it.googleEventId }.toSet()
        return (expand(events, from, to) + googleAgenda(from, to, settings, mirrored)).sortedWith(ORDER)
    }

    /** Menyimpan jadwal lokal dan (opsional) mencerminkannya ke Google Calendar. False bila sinkron gagal. */
    suspend fun save(event: EventEntity, syncGoogle: Boolean, calendarId: Long?): Boolean {
        var e = event.copy(updatedAt = System.currentTimeMillis())
        var googleOk = true
        val existingGoogleId = e.googleEventId
        if (syncGoogle && calendarId != null) {
            if (existingGoogleId != null && e.googleCalendarId == calendarId && google.update(existingGoogleId, e)) {
                // Sudah tersinkron, cukup diperbarui.
            } else {
                if (existingGoogleId != null) google.delete(existingGoogleId)
                val newId = google.insert(calendarId, e)
                googleOk = newId != null
                e = e.copy(googleEventId = newId, googleCalendarId = if (newId != null) calendarId else null)
            }
        } else if (existingGoogleId != null) {
            google.delete(existingGoogleId)
            e = e.copy(googleEventId = null, googleCalendarId = null)
        }
        val id = if (e.id == 0L) db.events().insert(e) else e.id.also { db.events().update(e) }
        reminders.scheduleEvent(e.copy(id = id))
        return googleOk
    }

    suspend fun delete(event: EventEntity) {
        event.googleEventId?.let { google.delete(it) }
        db.events().delete(event.id)
        reminders.cancel(ReminderScheduler.KIND_EVENT, event.id)
    }

    suspend fun deleteGoogleEvent(eventId: Long): Boolean = google.delete(eventId)

    /** Mengirim semua jadwal lokal yang belum tersinkron ke kalender Google [calendarId]. */
    suspend fun syncAllToGoogle(calendarId: Long): Int {
        var count = 0
        for (e in db.events().all()) {
            if (e.googleEventId != null) continue
            val newId = google.insert(calendarId, e) ?: continue
            db.events().update(e.copy(googleEventId = newId, googleCalendarId = calendarId))
            count++
        }
        return count
    }

    companion object {
        val ORDER: Comparator<Agenda> = compareBy<Agenda>({ !it.allDay }, { it.start }, { it.title })

        /** Mengelompokkan per tanggal; acara multi-hari muncul di setiap harinya. */
        fun groupByDate(items: List<Agenda>, from: LocalDate, toExclusive: LocalDate): Map<LocalDate, List<Agenda>> {
            val map = HashMap<LocalDate, MutableList<Agenda>>()
            for (a in items) {
                val startDate = a.start.toLocalDate()
                val lastDate = maxOf(startDate, (maxOf(a.end, a.start + 1) - 1).toLocalDate())
                var d = maxOf(startDate, from)
                while (!d.isAfter(lastDate) && d.isBefore(toExclusive)) {
                    map.getOrPut(d) { mutableListOf() }.add(a)
                    d = d.plusDays(1)
                }
            }
            return map.mapValues { (_, v) -> v.sortedWith(ORDER) }
        }
    }
}
