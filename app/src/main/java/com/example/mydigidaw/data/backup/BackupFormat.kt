package com.example.mydigidaw.data.backup

import com.example.mydigidaw.data.db.AccountEntity
import com.example.mydigidaw.data.db.BudgetEntity
import com.example.mydigidaw.data.db.CategoryEntity
import com.example.mydigidaw.data.db.DebtEntity
import com.example.mydigidaw.data.db.EventEntity
import com.example.mydigidaw.data.db.FocusSessionEntity
import com.example.mydigidaw.data.db.GoalEntity
import com.example.mydigidaw.data.db.HabitEntity
import com.example.mydigidaw.data.db.HabitLogEntity
import com.example.mydigidaw.data.db.JournalEntity
import com.example.mydigidaw.data.db.NoteEntity
import com.example.mydigidaw.data.db.RecurringEntity
import com.example.mydigidaw.data.db.TaskEntity
import com.example.mydigidaw.data.db.TransactionEntity
import com.example.mydigidaw.data.db.VaultItemEntity
import com.example.mydigidaw.data.prefs.VaultConfig
import com.example.mydigidaw.security.VaultCrypto
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.security.GeneralSecurityException
import javax.crypto.SecretKey

/** Pengaturan yang ikut dicadangkan. Data khusus perangkat (izin folder, ID kalender) sengaja tidak. */
data class BackupSettings(
    val userName: String,
    val themeMode: String,
    val dynamicColor: Boolean,
    val weekStartsMonday: Boolean,
    val appLock: Boolean,
    val lockTimeoutSec: Int,
    val secureScreen: Boolean,
    val hideAmounts: Boolean,
    val vaultAutoLockSec: Int,
)

/** Seluruh isi cadangan dalam bentuk objek, terlepas dari format berkasnya. */
data class BackupData(
    val notes: List<NoteEntity> = emptyList(),
    val vault: List<VaultItemEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val goals: List<GoalEntity> = emptyList(),
    val recurring: List<RecurringEntity> = emptyList(),
    val debts: List<DebtEntity> = emptyList(),
    val events: List<EventEntity> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val habits: List<HabitEntity> = emptyList(),
    val habitLogs: List<HabitLogEntity> = emptyList(),
    val focus: List<FocusSessionEntity> = emptyList(),
    val journal: List<JournalEntity> = emptyList(),
    /** Kunci brankas terbungkus kata sandi utama; kunci biometrik tidak pernah ikut. */
    val vaultConfig: VaultConfig? = null,
    val settings: BackupSettings? = null,
) {
    val count: Int
        get() = notes.size + vault.size + accounts.size + categories.size + transactions.size + budgets.size + goals.size +
            recurring.size + debts.size + events.size + tasks.size + habits.size + habitLogs.size + focus.size + journal.size
}

/** Berkas cadangan terenkripsi butuh kata sandi, atau kata sandinya salah. */
class BackupPasswordException(message: String) : Exception(message)

/**
 * Format berkas cadangan Mydigidaw (JSON), terpisah dari Android agar bisa diuji di JVM.
 *
 * Berkas terenkripsi berupa amplop `{app, version, encrypted, salt, iterations, payload}` dengan
 * payload = AES-256-GCM atas JSON cadangan, kuncinya PBKDF2 dari kata sandi cadangan.
 */
object BackupFormat {
    const val APP_ID = "mydigidaw"
    const val FORMAT_VERSION = 3

    // ---------------- Amplop & enkripsi ----------------

    fun encrypt(json: String, password: CharArray): String {
        val salt = VaultCrypto.randomBytes(16)
        return encrypt(json, VaultCrypto.deriveKey(password, salt, VaultCrypto.ITERATIONS), salt, VaultCrypto.ITERATIONS)
    }

    /** Enkripsi dengan kunci yang sudah diturunkan (dipakai cadangan otomatis agar kata sandi tak perlu disimpan). */
    fun encrypt(json: String, key: SecretKey, salt: ByteArray, iterations: Int): String = JSONObject()
        .put("app", APP_ID)
        .put("version", FORMAT_VERSION)
        .put("encrypted", true)
        .put("salt", VaultCrypto.encode(salt))
        .put("iterations", iterations)
        .put("payload", VaultCrypto.encrypt(key, json.toByteArray(Charsets.UTF_8)))
        .toString()

    fun isEncrypted(text: String): Boolean = runCatching { JSONObject(text).optBoolean("encrypted") }.getOrDefault(false)

    /** Membaca isi berkas (mendekripsi bila perlu) menjadi JSON cadangan yang siap di-[parse]. */
    fun open(text: String, password: CharArray?): JSONObject {
        val root = try {
            JSONObject(text)
        } catch (e: JSONException) {
            throw IllegalArgumentException("Berkas ini bukan cadangan Mydigidaw")
        }
        require(root.optString("app") == APP_ID) { "Berkas ini bukan cadangan Mydigidaw" }
        if (!root.optBoolean("encrypted")) return root
        if (password == null || password.isEmpty()) throw BackupPasswordException("Berkas cadangan ini dilindungi kata sandi")
        val key = VaultCrypto.deriveKey(password, VaultCrypto.decode(root.getString("salt")), root.optInt("iterations", VaultCrypto.ITERATIONS))
        val plain = try {
            String(VaultCrypto.decrypt(key, root.getString("payload")), Charsets.UTF_8)
        } catch (e: GeneralSecurityException) {
            throw BackupPasswordException("Kata sandi cadangan salah")
        }
        return JSONObject(plain)
    }

    // ---------------- Objek ↔ JSON ----------------

    fun toJson(data: BackupData, exportedAt: Long = System.currentTimeMillis()): JSONObject {
        val root = JSONObject()
            .put("app", APP_ID)
            .put("version", FORMAT_VERSION)
            .put("exportedAt", exportedAt)
        fun <T> put(key: String, items: List<T>, map: (T) -> JSONObject) {
            root.put(key, JSONArray().apply { items.forEach { put(map(it)) } })
        }
        put("notes", data.notes, ::noteJson)
        put("vault", data.vault, ::vaultJson)
        put("accounts", data.accounts, ::accountJson)
        put("categories", data.categories, ::categoryJson)
        put("transactions", data.transactions, ::transactionJson)
        put("budgets", data.budgets, ::budgetJson)
        put("goals", data.goals, ::goalJson)
        put("recurring", data.recurring, ::recurringJson)
        put("debts", data.debts, ::debtJson)
        put("events", data.events, ::eventJson)
        put("tasks", data.tasks, ::taskJson)
        put("habits", data.habits, ::habitJson)
        put("habitLogs", data.habitLogs, ::habitLogJson)
        put("focus", data.focus, ::focusJson)
        put("journal", data.journal, ::journalJson)
        data.vaultConfig?.let { cfg ->
            root.put("vaultConfig", JSONObject().put("salt", cfg.salt).put("iterations", cfg.iterations).put("wrappedKey", cfg.wrappedKey))
        }
        data.settings?.let { root.put("settings", settingsJson(it)) }
        return root
    }

    /** Kebalikan [toJson]. Kolom yang tidak ada (cadangan versi lama) diisi nilai bawaan. */
    fun parse(root: JSONObject): BackupData {
        fun <T> read(key: String, parse: (JSONObject) -> T): List<T> {
            val arr = root.optJSONArray(key) ?: return emptyList()
            return (0 until arr.length()).map { parse(arr.getJSONObject(it)) }
        }
        return BackupData(
            notes = read("notes", ::parseNote),
            vault = read("vault", ::parseVault),
            accounts = read("accounts", ::parseAccount),
            categories = read("categories", ::parseCategory),
            transactions = read("transactions", ::parseTransaction),
            budgets = read("budgets", ::parseBudget),
            goals = read("goals", ::parseGoal),
            recurring = read("recurring", ::parseRecurring),
            debts = read("debts", ::parseDebt),
            events = read("events", ::parseEvent),
            tasks = read("tasks", ::parseTask),
            habits = read("habits", ::parseHabit),
            habitLogs = read("habitLogs", ::parseHabitLog),
            focus = read("focus", ::parseFocus),
            journal = read("journal", ::parseJournal),
            vaultConfig = root.optJSONObject("vaultConfig")?.let { cfg ->
                VaultConfig(cfg.getString("salt"), cfg.getInt("iterations"), cfg.getString("wrappedKey"), null)
            },
            settings = root.optJSONObject("settings")?.let(::parseSettings),
        )
    }

    private fun JSONObject.optLongOrNull(key: String): Long? = if (has(key) && !isNull(key)) getLong(key) else null
    private fun JSONObject.optIntOrNull(key: String): Int? = if (has(key) && !isNull(key)) getInt(key) else null
    private fun JSONObject.putNullable(key: String, value: Any?): JSONObject = put(key, value ?: JSONObject.NULL)

    private fun settingsJson(s: BackupSettings) = JSONObject().put("userName", s.userName).put("themeMode", s.themeMode)
        .put("dynamicColor", s.dynamicColor).put("weekStartsMonday", s.weekStartsMonday).put("appLock", s.appLock)
        .put("lockTimeoutSec", s.lockTimeoutSec).put("secureScreen", s.secureScreen).put("hideAmounts", s.hideAmounts)
        .put("vaultAutoLockSec", s.vaultAutoLockSec)

    private fun parseSettings(o: JSONObject) = BackupSettings(
        userName = o.optString("userName"), themeMode = o.optString("themeMode", "SYSTEM"),
        dynamicColor = o.optBoolean("dynamicColor"), weekStartsMonday = o.optBoolean("weekStartsMonday", true),
        appLock = o.optBoolean("appLock"), lockTimeoutSec = o.optInt("lockTimeoutSec"), secureScreen = o.optBoolean("secureScreen"),
        hideAmounts = o.optBoolean("hideAmounts"), vaultAutoLockSec = o.optInt("vaultAutoLockSec", 60),
    )

    private fun noteJson(n: NoteEntity) = JSONObject().put("id", n.id).put("title", n.title).put("content", n.content)
        .put("category", n.category).put("color", n.color).put("isPinned", n.isPinned).put("isArchived", n.isArchived)
        .put("isTrashed", n.isTrashed).put("createdAt", n.createdAt).put("updatedAt", n.updatedAt).put("isChecklist", n.isChecklist)

    private fun parseNote(o: JSONObject) = NoteEntity(
        id = o.getLong("id"), title = o.optString("title"), content = o.optString("content"),
        category = o.optString("category", "UMUM"), color = o.optInt("color"), isPinned = o.optBoolean("isPinned"),
        isArchived = o.optBoolean("isArchived"), isTrashed = o.optBoolean("isTrashed"),
        createdAt = o.optLong("createdAt"), updatedAt = o.optLong("updatedAt"), isChecklist = o.optBoolean("isChecklist"),
    )

    private fun focusJson(f: FocusSessionEntity) = JSONObject().put("id", f.id).put("label", f.label).put("startedAt", f.startedAt)
        .put("plannedMinutes", f.plannedMinutes).put("actualMinutes", f.actualMinutes).put("completed", f.completed)

    private fun parseFocus(o: JSONObject) = FocusSessionEntity(
        id = o.getLong("id"), label = o.optString("label"), startedAt = o.optLong("startedAt"),
        plannedMinutes = o.optInt("plannedMinutes", 25), actualMinutes = o.optInt("actualMinutes"), completed = o.optBoolean("completed"),
    )

    private fun journalJson(j: JournalEntity) = JSONObject().put("day", j.day).put("mood", j.mood).put("note", j.note).put("updatedAt", j.updatedAt)

    private fun parseJournal(o: JSONObject) = JournalEntity(
        day = o.getLong("day"), mood = o.optInt("mood", 3), note = o.optString("note"), updatedAt = o.optLong("updatedAt"),
    )

    private fun vaultJson(v: VaultItemEntity) = JSONObject().put("id", v.id).put("type", v.type).put("payload", v.payload)
        .put("isFavorite", v.isFavorite).put("createdAt", v.createdAt).put("updatedAt", v.updatedAt)

    private fun parseVault(o: JSONObject) = VaultItemEntity(
        id = o.getLong("id"), type = o.optString("type"), payload = o.getString("payload"),
        isFavorite = o.optBoolean("isFavorite"), createdAt = o.optLong("createdAt"), updatedAt = o.optLong("updatedAt"),
    )

    private fun accountJson(a: AccountEntity) = JSONObject().put("id", a.id).put("name", a.name).put("type", a.type)
        .put("initialBalance", a.initialBalance).put("color", a.color).put("includeInTotal", a.includeInTotal)
        .put("sortOrder", a.sortOrder).put("createdAt", a.createdAt)

    private fun parseAccount(o: JSONObject) = AccountEntity(
        id = o.getLong("id"), name = o.optString("name"), type = o.optString("type", "CASH"),
        initialBalance = o.optLong("initialBalance"), color = o.optInt("color"),
        includeInTotal = o.optBoolean("includeInTotal", true), sortOrder = o.optInt("sortOrder"), createdAt = o.optLong("createdAt"),
    )

    private fun categoryJson(c: CategoryEntity) = JSONObject().put("id", c.id).put("name", c.name).put("type", c.type)
        .put("emoji", c.emoji).put("color", c.color).put("sortOrder", c.sortOrder)

    private fun parseCategory(o: JSONObject) = CategoryEntity(
        id = o.getLong("id"), name = o.optString("name"), type = o.optString("type", "EXPENSE"),
        emoji = o.optString("emoji", "📦"), color = o.optInt("color"), sortOrder = o.optInt("sortOrder"),
    )

    private fun transactionJson(t: TransactionEntity) = JSONObject().put("id", t.id).put("type", t.type).put("amount", t.amount)
        .putNullable("categoryId", t.categoryId).put("accountId", t.accountId).putNullable("toAccountId", t.toAccountId)
        .put("date", t.date).put("note", t.note).put("createdAt", t.createdAt)

    private fun parseTransaction(o: JSONObject) = TransactionEntity(
        id = o.getLong("id"), type = o.optString("type", "EXPENSE"), amount = o.optLong("amount"),
        categoryId = o.optLongOrNull("categoryId"), accountId = o.getLong("accountId"), toAccountId = o.optLongOrNull("toAccountId"),
        date = o.optLong("date"), note = o.optString("note"), createdAt = o.optLong("createdAt"),
    )

    private fun budgetJson(b: BudgetEntity) = JSONObject().put("id", b.id).put("categoryId", b.categoryId).put("amount", b.amount)

    private fun parseBudget(o: JSONObject) = BudgetEntity(id = o.getLong("id"), categoryId = o.getLong("categoryId"), amount = o.optLong("amount"))

    private fun goalJson(g: GoalEntity) = JSONObject().put("id", g.id).put("name", g.name).put("emoji", g.emoji)
        .put("targetAmount", g.targetAmount).put("savedAmount", g.savedAmount).putNullable("deadline", g.deadline)
        .put("color", g.color).put("createdAt", g.createdAt)

    private fun parseGoal(o: JSONObject) = GoalEntity(
        id = o.getLong("id"), name = o.optString("name"), emoji = o.optString("emoji", "🎯"),
        targetAmount = o.optLong("targetAmount"), savedAmount = o.optLong("savedAmount"), deadline = o.optLongOrNull("deadline"),
        color = o.optInt("color"), createdAt = o.optLong("createdAt"),
    )

    private fun recurringJson(r: RecurringEntity) = JSONObject().put("id", r.id).put("type", r.type).put("amount", r.amount)
        .putNullable("categoryId", r.categoryId).put("accountId", r.accountId).putNullable("toAccountId", r.toAccountId)
        .put("note", r.note).put("repeat", r.repeat).put("startDate", r.startDate).put("occurrences", r.occurrences)
        .put("nextDate", r.nextDate).put("isActive", r.isActive).put("createdAt", r.createdAt)

    private fun parseRecurring(o: JSONObject) = RecurringEntity(
        id = o.getLong("id"), type = o.optString("type", "EXPENSE"), amount = o.optLong("amount"),
        categoryId = o.optLongOrNull("categoryId"), accountId = o.getLong("accountId"), toAccountId = o.optLongOrNull("toAccountId"),
        note = o.optString("note"), repeat = o.optString("repeat", "MONTHLY"), startDate = o.optLong("startDate"),
        occurrences = o.optInt("occurrences", 1), nextDate = o.optLong("nextDate"), isActive = o.optBoolean("isActive", true),
        createdAt = o.optLong("createdAt"),
    )

    private fun debtJson(d: DebtEntity) = JSONObject().put("id", d.id).put("type", d.type).put("person", d.person)
        .put("amount", d.amount).put("paidAmount", d.paidAmount).putNullable("dueDate", d.dueDate).put("note", d.note)
        .put("isSettled", d.isSettled).put("createdAt", d.createdAt)

    private fun parseDebt(o: JSONObject) = DebtEntity(
        id = o.getLong("id"), type = o.optString("type", "PAYABLE"), person = o.optString("person"), amount = o.optLong("amount"),
        paidAmount = o.optLong("paidAmount"), dueDate = o.optLongOrNull("dueDate"), note = o.optString("note"),
        isSettled = o.optBoolean("isSettled"), createdAt = o.optLong("createdAt"),
    )

    private fun eventJson(e: EventEntity) = JSONObject().put("id", e.id).put("title", e.title).put("description", e.description)
        .put("location", e.location).put("startAt", e.startAt).put("endAt", e.endAt).put("allDay", e.allDay).put("color", e.color)
        .put("repeat", e.repeat).put("reminderMinutes", e.reminderMinutes).put("createdAt", e.createdAt).put("updatedAt", e.updatedAt)

    // Tautan ke Google tidak dipulihkan: ID acara berbeda di setiap perangkat.
    private fun parseEvent(o: JSONObject) = EventEntity(
        id = o.getLong("id"), title = o.optString("title"), description = o.optString("description"),
        location = o.optString("location"), startAt = o.optLong("startAt"), endAt = o.optLong("endAt"),
        allDay = o.optBoolean("allDay"), color = o.optInt("color"), repeat = o.optString("repeat", "NONE"),
        reminderMinutes = o.optInt("reminderMinutes", -1), createdAt = o.optLong("createdAt"), updatedAt = o.optLong("updatedAt"),
    )

    private fun taskJson(t: TaskEntity) = JSONObject().put("id", t.id).put("title", t.title).put("notes", t.notes)
        .put("priority", t.priority).putNullable("dueAt", t.dueAt).put("hasTime", t.hasTime).put("remind", t.remind)
        .put("isDone", t.isDone).putNullable("completedAt", t.completedAt).put("createdAt", t.createdAt)

    private fun parseTask(o: JSONObject) = TaskEntity(
        id = o.getLong("id"), title = o.optString("title"), notes = o.optString("notes"), priority = o.optString("priority", "MEDIUM"),
        dueAt = o.optLongOrNull("dueAt"), hasTime = o.optBoolean("hasTime"), remind = o.optBoolean("remind"),
        isDone = o.optBoolean("isDone"), completedAt = o.optLongOrNull("completedAt"), createdAt = o.optLong("createdAt"),
    )

    private fun habitJson(h: HabitEntity) = JSONObject().put("id", h.id).put("name", h.name).put("emoji", h.emoji)
        .put("color", h.color).put("daysMask", h.daysMask).put("targetPerDay", h.targetPerDay)
        .putNullable("reminderMinutes", h.reminderMinutes).put("isArchived", h.isArchived).put("createdAt", h.createdAt)

    private fun parseHabit(o: JSONObject) = HabitEntity(
        id = o.getLong("id"), name = o.optString("name"), emoji = o.optString("emoji", "✅"), color = o.optInt("color"),
        daysMask = o.optInt("daysMask", 127), targetPerDay = o.optInt("targetPerDay", 1),
        reminderMinutes = o.optIntOrNull("reminderMinutes"), isArchived = o.optBoolean("isArchived"), createdAt = o.optLong("createdAt"),
    )

    private fun habitLogJson(l: HabitLogEntity) = JSONObject().put("habitId", l.habitId).put("day", l.day).put("count", l.count)

    private fun parseHabitLog(o: JSONObject) = HabitLogEntity(habitId = o.getLong("habitId"), day = o.getLong("day"), count = o.optInt("count", 1))
}
