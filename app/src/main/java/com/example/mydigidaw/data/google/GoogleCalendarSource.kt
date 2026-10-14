package com.example.mydigidaw.data.google

import android.Manifest
import android.accounts.Account
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import android.provider.CalendarContract.Instances
import androidx.core.content.ContextCompat
import com.example.mydigidaw.data.db.EventEntity
import com.example.mydigidaw.data.model.Repeat
import com.example.mydigidaw.data.model.enumOr
import com.example.mydigidaw.util.startMillis
import com.example.mydigidaw.util.toLocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneOffset
import java.util.TimeZone

data class DeviceCalendar(
    val id: Long,
    val name: String,
    val account: String,
    val color: Int,
    val writable: Boolean,
    val isGoogle: Boolean,
    val isPrimary: Boolean,
)

data class ExternalEvent(
    val eventId: Long,
    val calendarId: Long,
    val title: String,
    val description: String,
    val location: String,
    val begin: Long,
    val end: Long,
    val allDay: Boolean,
    val color: Int,
    val calendarName: String,
    val isRecurring: Boolean,
)

/**
 * Jembatan ke Google Calendar melalui Calendar Provider Android. Akun Google di HP menyinkronkan
 * perubahan ke server Google secara otomatis, jadi jadwal yang ditulis di sini muncul juga di
 * aplikasi/website Google Calendar (dan sebaliknya).
 */
class GoogleCalendarSource(private val context: Context) {
    private val resolver get() = context.contentResolver

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED

    suspend fun calendars(): List<DeviceCalendar> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptyList()
        val projection = arrayOf(
            Calendars._ID,
            Calendars.CALENDAR_DISPLAY_NAME,
            Calendars.ACCOUNT_NAME,
            Calendars.ACCOUNT_TYPE,
            Calendars.CALENDAR_COLOR,
            Calendars.CALENDAR_ACCESS_LEVEL,
            Calendars.IS_PRIMARY,
        )
        val list = mutableListOf<DeviceCalendar>()
        runCatching {
            resolver.query(Calendars.CONTENT_URI, projection, "${Calendars.VISIBLE} = 1", null, null)?.use { c ->
                while (c.moveToNext()) {
                    val access = c.getInt(5)
                    list += DeviceCalendar(
                        id = c.getLong(0),
                        name = c.getString(1) ?: "Kalender",
                        account = c.getString(2) ?: "",
                        color = c.getInt(4),
                        writable = access >= Calendars.CAL_ACCESS_CONTRIBUTOR,
                        isGoogle = c.getString(3) == "com.google",
                        isPrimary = c.getInt(6) == 1,
                    )
                }
            }
        }
        list.sortedWith(compareByDescending<DeviceCalendar> { it.isGoogle }.thenByDescending { it.isPrimary }.thenBy { it.name })
    }

    suspend fun instances(start: Long, end: Long, hiddenCalendars: Set<Long>): List<ExternalEvent> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptyList()
        // Acara sepanjang hari disimpan dalam UTC; perlebar jendela agar tidak terpotong zona waktu.
        val builder = Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, start - DAY_MS)
        ContentUris.appendId(builder, end + DAY_MS)
        val projection = arrayOf(
            Instances.EVENT_ID,
            Instances.CALENDAR_ID,
            Instances.TITLE,
            Instances.DESCRIPTION,
            Instances.EVENT_LOCATION,
            Instances.BEGIN,
            Instances.END,
            Instances.ALL_DAY,
            Instances.DISPLAY_COLOR,
            Instances.CALENDAR_DISPLAY_NAME,
            Instances.RRULE,
            Instances.STATUS,
        )
        val list = mutableListOf<ExternalEvent>()
        runCatching {
            resolver.query(builder.build(), projection, "${Instances.VISIBLE} = 1", null, "${Instances.BEGIN} ASC")?.use { c ->
                while (c.moveToNext()) {
                    val calendarId = c.getLong(1)
                    if (calendarId in hiddenCalendars) continue
                    if (!c.isNull(11) && c.getInt(11) == Events.STATUS_CANCELED) continue
                    val allDay = c.getInt(7) == 1
                    var begin = c.getLong(5)
                    var finish = c.getLong(6)
                    if (allDay) {
                        begin = Instant.ofEpochMilli(begin).atZone(ZoneOffset.UTC).toLocalDate().startMillis()
                        finish = Instant.ofEpochMilli(finish).atZone(ZoneOffset.UTC).toLocalDate().startMillis()
                    }
                    if (begin >= end || maxOf(finish, begin + 1) <= start) continue
                    list += ExternalEvent(
                        eventId = c.getLong(0),
                        calendarId = calendarId,
                        title = c.getString(2)?.takeIf { it.isNotBlank() } ?: "(Tanpa judul)",
                        description = c.getString(3) ?: "",
                        location = c.getString(4) ?: "",
                        begin = begin,
                        end = finish,
                        allDay = allDay,
                        color = c.getInt(8),
                        calendarName = c.getString(9) ?: "",
                        isRecurring = !c.getString(10).isNullOrBlank(),
                    )
                }
            }
        }
        list
    }

    /** Mengembalikan ID acara Google yang baru, atau null bila gagal. */
    suspend fun insert(calendarId: Long, event: EventEntity): Long? = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext null
        val values = toValues(event).apply { put(Events.CALENDAR_ID, calendarId) }
        runCatching { resolver.insert(Events.CONTENT_URI, values)?.lastPathSegment?.toLongOrNull() }.getOrNull()
    }

    suspend fun update(eventId: Long, event: EventEntity): Boolean = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext false
        val uri = ContentUris.withAppendedId(Events.CONTENT_URI, eventId)
        runCatching { resolver.update(uri, toValues(event), null, null) > 0 }.getOrDefault(false)
    }

    suspend fun delete(eventId: Long): Boolean = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext false
        val uri = ContentUris.withAppendedId(Events.CONTENT_URI, eventId)
        runCatching { resolver.delete(uri, null, null) > 0 }.getOrDefault(false)
    }

    private fun toValues(event: EventEntity): ContentValues {
        val rule = enumOr(event.repeat, Repeat.NONE)
        return ContentValues().apply {
            put(Events.TITLE, event.title)
            put(Events.DESCRIPTION, event.description)
            put(Events.EVENT_LOCATION, event.location)
            put(Events.HAS_ALARM, 0)
            if (event.allDay) {
                val startDate = event.startAt.toLocalDate()
                val endDate = event.endAt.toLocalDate().let { if (it.isAfter(startDate)) it else startDate.plusDays(1) }
                val startUtc = startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                val endUtc = endDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                put(Events.ALL_DAY, 1)
                put(Events.EVENT_TIMEZONE, "UTC")
                put(Events.DTSTART, startUtc)
                if (rule.rrule == null) {
                    put(Events.DTEND, endUtc)
                    putNull(Events.DURATION)
                } else {
                    putNull(Events.DTEND)
                    put(Events.DURATION, "P${(endUtc - startUtc) / DAY_MS}D")
                }
            } else {
                put(Events.ALL_DAY, 0)
                put(Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                put(Events.DTSTART, event.startAt)
                val end = maxOf(event.endAt, event.startAt)
                if (rule.rrule == null) {
                    put(Events.DTEND, end)
                    putNull(Events.DURATION)
                } else {
                    putNull(Events.DTEND)
                    put(Events.DURATION, "P${(end - event.startAt) / 1000}S")
                }
            }
            if (rule.rrule == null) putNull(Events.RRULE) else put(Events.RRULE, rule.rrule)
        }
    }

    /** Meminta sinkronisasi segera untuk semua akun Google yang punya kalender di perangkat. */
    suspend fun requestSync() = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext
        calendars().filter { it.isGoogle }.map { it.account }.distinct().forEach { name ->
            runCatching {
                ContentResolver.requestSync(
                    Account(name, "com.google"),
                    CalendarContract.AUTHORITY,
                    Bundle().apply {
                        putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
                        putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
                    },
                )
            }
        }
    }

    /** Memancarkan sinyal setiap kali data kalender di perangkat berubah (mis. sinkronisasi Google). */
    fun changes(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        val registered = runCatching {
            resolver.registerContentObserver(CalendarContract.CONTENT_URI, true, observer)
        }.isSuccess
        awaitClose { if (registered) resolver.unregisterContentObserver(observer) }
    }

    companion object {
        const val DAY_MS = 86_400_000L
    }
}
