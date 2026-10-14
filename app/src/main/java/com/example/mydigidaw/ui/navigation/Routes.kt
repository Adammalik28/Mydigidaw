package com.example.mydigidaw.ui.navigation

import android.content.Intent
import android.net.Uri
import com.example.mydigidaw.data.model.TxType
import java.time.LocalDate

object Routes {
    const val HOME = "home"
    const val NOTES = "notes"
    const val FINANCE = "finance"
    const val CALENDAR = "calendar"
    const val MORE = "more"

    const val NOTE_EDIT = "note/{id}"
    const val NOTE_BIN = "notes_bin/{mode}"
    const val VAULT_EDIT = "vault/{id}?type={type}"
    const val TX_EDIT = "tx/{id}?type={type}&amount={amount}&note={note}"
    const val ACCOUNTS = "accounts"
    const val CATEGORIES = "categories"
    const val GOALS = "goals"
    const val DEBTS = "debts"
    const val RECURRING = "recurring"
    const val EVENT_EDIT = "event/{id}?date={date}"
    const val TASKS = "tasks?new={new}"
    const val HABITS = "habits"
    const val HABIT_DETAIL = "habit/{id}"
    const val SETTINGS = "settings"
    const val SEARCH = "search"
    const val FOCUS = "focus"
    const val JOURNAL = "journal"

    val topLevel = listOf(HOME, NOTES, FINANCE, CALENDAR, MORE)

    fun noteEdit(id: Long = -1) = "note/$id"
    fun noteBin(mode: String) = "notes_bin/$mode"
    fun vaultEdit(id: Long = -1, type: String? = null) = "vault/$id" + (type?.let { "?type=$it" } ?: "")
    fun txEdit(id: Long = -1, type: TxType = TxType.EXPENSE, amount: Long? = null, note: String? = null) =
        "tx/$id?type=${type.name}" + (amount?.let { "&amount=$it" } ?: "") + (note?.let { "&note=${Uri.encode(it)}" } ?: "")
    fun eventEdit(id: Long = -1, date: LocalDate? = null) = "event/$id" + (date?.let { "?date=${it.toEpochDay()}" } ?: "")
    fun tasks(openNew: Boolean = false) = "tasks?new=$openNew"
    fun habitDetail(id: Long) = "habit/$id"
}

/** Tautan internal mydigidaw://… dari pintasan ikon dan notifikasi. */
object DeepLinks {
    fun routeFor(intent: Intent?): String? {
        val data = intent?.data ?: return null
        if (data.scheme != "mydigidaw") return null
        return when ("${data.host}/${data.path.orEmpty().trim('/')}") {
            "new/transaction" -> Routes.txEdit(
                amount = data.getQueryParameter("amount")?.toLongOrNull(),
                note = data.getQueryParameter("note"),
            )
            "new/note" -> Routes.noteEdit()
            "new/event" -> Routes.eventEdit()
            "new/task" -> Routes.tasks(openNew = true)
            "open/home" -> Routes.HOME
            "open/calendar" -> Routes.CALENDAR
            "open/finance" -> Routes.FINANCE
            "open/tasks" -> Routes.tasks()
            "open/habits" -> Routes.HABITS
            "open/focus" -> Routes.FOCUS
            "open/journal" -> Routes.JOURNAL
            "open/debts" -> Routes.DEBTS
            "open/search" -> Routes.SEARCH
            "open/settings" -> Routes.SETTINGS
            else -> null
        }
    }
}
