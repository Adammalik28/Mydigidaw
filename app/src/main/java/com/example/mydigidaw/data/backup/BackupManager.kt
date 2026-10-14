package com.example.mydigidaw.data.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.room.withTransaction
import com.example.mydigidaw.data.db.AppDatabase
import com.example.mydigidaw.data.model.TxType
import com.example.mydigidaw.data.prefs.AutoBackupKey
import com.example.mydigidaw.data.prefs.AutoBackupKind
import com.example.mydigidaw.data.prefs.SettingsRepository
import com.example.mydigidaw.reminders.ReminderScheduler
import com.example.mydigidaw.security.DeviceSecrets
import com.example.mydigidaw.security.VaultCrypto
import com.example.mydigidaw.security.VaultManager
import com.example.mydigidaw.util.toLocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter
import javax.crypto.spec.SecretKeySpec

/**
 * Cadangan lengkap ke berkas JSON pilihan pengguna. Item brankas tetap terenkripsi di dalam berkas
 * dan hanya bisa dibuka dengan kata sandi utama yang sama. Format berkas ada di [BackupFormat].
 */
class BackupManager(
    private val context: Context,
    private val db: AppDatabase,
    private val settings: SettingsRepository,
    private val vault: VaultManager,
    private val reminders: ReminderScheduler,
) {
    /** Menulis cadangan ke [uri]. Bila [password] diisi, seluruh isi berkas dienkripsi AES-256. */
    suspend fun export(uri: Uri, password: CharArray? = null): Int = withContext(Dispatchers.IO) {
        val data = snapshot()
        val json = BackupFormat.toJson(data)
        val text = if (password != null && password.isNotEmpty()) BackupFormat.encrypt(json.toString(), password) else json.toString(2)
        writeText(uri, text)
        data.count
    }

    /**
     * Menjalankan cadangan otomatis ke lokasi yang diatur (Google Drive / folder) dan mencatat hasilnya.
     * Mengembalikan keterangan singkat bila berhasil; galat disimpan di pengaturan lalu dilempar ulang.
     */
    suspend fun runAutoBackup(): String = withContext(Dispatchers.IO) {
        val s = settings.current()
        val uri = Uri.parse(s.autoBackupUri ?: error("Cadangan otomatis belum diatur"))
        try {
            val json = BackupFormat.toJson(snapshot()).toString()
            val text = settings.autoBackupKeyNow()?.let { encryptWith(it, json) } ?: json
            val name = when (s.autoBackupKind) {
                AutoBackupKind.FILE -> {
                    writeText(uri, text)
                    DocumentFile.fromSingleUri(context, uri)?.name ?: "berkas cadangan"
                }
                AutoBackupKind.FOLDER -> writeToFolder(uri, text)
            }
            settings.setAutoBackupResult(System.currentTimeMillis(), null)
            name
        } catch (e: Exception) {
            settings.setAutoBackupResult(System.currentTimeMillis(), e.message ?: e.javaClass.simpleName)
            throw e
        }
    }

    /** Menurunkan kunci dari kata sandi cadangan otomatis; kata sandinya sendiri tidak pernah disimpan. */
    fun createAutoBackupKey(password: CharArray): AutoBackupKey {
        val salt = VaultCrypto.randomBytes(16)
        val key = VaultCrypto.deriveKey(password, salt, VaultCrypto.ITERATIONS)
        return AutoBackupKey(DeviceSecrets.seal(key.encoded), VaultCrypto.encode(salt), VaultCrypto.ITERATIONS)
    }

    private fun encryptWith(stored: AutoBackupKey, json: String): String {
        val keyBytes = try {
            DeviceSecrets.open(stored.sealedKey)
        } catch (e: Exception) {
            error("Kunci cadangan di HP ini tidak berlaku lagi — atur ulang cadangan otomatis")
        }
        return BackupFormat.encrypt(json, SecretKeySpec(keyBytes, "AES"), VaultCrypto.decode(stored.salt), stored.iterations)
    }

    /** Menyimpan maksimal [keep] berkas terbaru di folder pilihan pengguna. */
    private fun writeToFolder(treeUri: Uri, text: String, keep: Int = 5): String {
        val dir = DocumentFile.fromTreeUri(context, treeUri) ?: error("Folder cadangan tidak dapat diakses")
        val name = "mydigidaw-otomatis-${java.time.LocalDate.now()}"
        dir.findFile("$name.json")?.delete()
        val file = dir.createFile("application/json", name) ?: error("Tidak bisa membuat berkas cadangan")
        writeText(file.uri, text)
        dir.listFiles()
            .filter { it.name?.startsWith("mydigidaw-otomatis-") == true }
            .sortedByDescending { it.name }
            .drop(keep)
            .forEach { it.delete() }
        return file.name ?: name
    }

    /**
     * Menimpa isi berkas. Mode "wt" (tulis + potong) dicoba lebih dulu karena sebagian penyedia
     * (termasuk Google Drive) tidak memotong sisa isi lama pada mode "w".
     */
    private fun writeText(uri: Uri, text: String) {
        val resolver = context.contentResolver
        val stream = listOf("wt", "rwt", "w").firstNotNullOfOrNull { mode ->
            runCatching { resolver.openOutputStream(uri, mode) }.getOrNull()
        } ?: error("Tidak bisa menulis berkas cadangan")
        stream.bufferedWriter().use { it.write(text) }
    }

    private suspend fun snapshot(): BackupData = BackupData(
        notes = db.notes().all(),
        vault = db.vault().all(),
        accounts = db.accounts().all(),
        categories = db.categories().all(),
        transactions = db.transactions().all(),
        budgets = db.budgets().all(),
        goals = db.goals().all(),
        recurring = db.recurring().all(),
        debts = db.debts().all(),
        events = db.events().all(),
        tasks = db.tasks().all(),
        habits = db.habits().allHabits(),
        habitLogs = db.habits().allLogs(),
        focus = db.focus().all(),
        journal = db.journal().all(),
        vaultConfig = settings.vaultConfigNow()?.copy(bioWrappedKey = null),
        settings = settings.backupSettings(),
    )

    /** True bila berkas cadangan dienkripsi dan butuh kata sandi. */
    suspend fun isEncrypted(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching { BackupFormat.isEncrypted(readText(uri)) }.getOrDefault(false)
    }

    private fun readText(uri: Uri): String =
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("Tidak bisa membaca berkas")

    suspend fun import(uri: Uri, password: CharArray? = null): Int = withContext(Dispatchers.IO) {
        val data = BackupFormat.parse(BackupFormat.open(readText(uri), password))
        // ID dari cadangan bisa sama dengan ID data lama; alarm lama harus dibatalkan agar tidak
        // memunculkan pengingat palsu untuk item baru yang kebetulan ber-ID sama.
        reminders.cancelAll()
        try {
            replaceAll(data)
        } catch (e: Exception) {
            reminders.rescheduleAll()
            throw e
        }

        vault.reset()
        data.vaultConfig?.let { settings.saveVaultConfig(it) }
        data.settings?.let { settings.applyBackupSettings(it) }
        reminders.rescheduleAll()
        data.count
    }

    private suspend fun replaceAll(data: BackupData) {
        db.withTransaction {
            db.notes().clear(); db.notes().insertAll(data.notes)
            db.vault().clear(); db.vault().insertAll(data.vault)
            db.accounts().clear(); db.accounts().insertAll(data.accounts)
            db.categories().clear(); db.categories().insertAll(data.categories)
            db.transactions().clear(); db.transactions().insertAll(data.transactions)
            db.budgets().clear(); db.budgets().insertAll(data.budgets)
            db.goals().clear(); db.goals().insertAll(data.goals)
            db.recurring().clear(); db.recurring().insertAll(data.recurring)
            db.debts().clear(); db.debts().insertAll(data.debts)
            db.events().clear(); db.events().insertAll(data.events)
            db.tasks().clear(); db.tasks().insertAll(data.tasks)
            db.habits().clearLogs(); db.habits().clearHabits()
            db.habits().insertAllHabits(data.habits); db.habits().insertAllLogs(data.habitLogs)
            db.focus().clear(); db.focus().insertAll(data.focus)
            db.journal().clear(); db.journal().insertAll(data.journal)
        }
    }

    /** Ekspor transaksi ke CSV (bisa dibuka di Excel / Google Sheets). */
    suspend fun exportTransactionsCsv(uri: Uri): Int = withContext(Dispatchers.IO) {
        val accounts = db.accounts().all().associateBy { it.id }
        val categories = db.categories().all().associateBy { it.id }
        val txs = db.transactions().all()
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val stream = context.contentResolver.openOutputStream(uri, "wt") ?: error("Tidak bisa menulis berkas")
        stream.bufferedWriter().use { w ->
            w.write(UTF8_BOM)
            w.write("Tanggal,Jenis,Kategori,Dompet,Ke Dompet,Jumlah,Catatan\n")
            for (tx in txs) {
                val type = TxType.entries.firstOrNull { it.name == tx.type }?.label ?: tx.type
                val signed = when (tx.type) {
                    TxType.EXPENSE.name -> -tx.amount
                    else -> tx.amount
                }
                val row = listOf(
                    fmt.format(tx.date.toLocalDateTime()),
                    type,
                    tx.categoryId?.let { categories[it]?.name } ?: "",
                    accounts[tx.accountId]?.name ?: "",
                    tx.toAccountId?.let { accounts[it]?.name } ?: "",
                    signed.toString(),
                    tx.note,
                )
                w.write(row.joinToString(",") { csv(it) })
                w.write("\n")
            }
        }
        txs.size
    }

    private fun csv(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' }) "\"" + value.replace("\"", "\"\"") + "\"" else value

    private companion object {
        /** Penanda UTF-8 agar Excel membaca huruf non-ASCII dengan benar. */
        const val UTF8_BOM = "\uFEFF"
    }
}
