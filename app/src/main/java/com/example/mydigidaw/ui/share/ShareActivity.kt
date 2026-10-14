@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.mydigidaw.ui.share

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.mydigidaw.AppContainer
import com.example.mydigidaw.MainActivity
import com.example.mydigidaw.MydigidawApp
import com.example.mydigidaw.data.db.NoteEntity
import com.example.mydigidaw.data.db.TaskEntity
import com.example.mydigidaw.data.model.NoteCategory
import com.example.mydigidaw.data.model.Priority
import com.example.mydigidaw.data.model.ThemeMode
import com.example.mydigidaw.ui.components.AmountField
import com.example.mydigidaw.ui.components.IconTile
import com.example.mydigidaw.ui.components.SegmentedTabs
import com.example.mydigidaw.ui.components.digitsToLong
import com.example.mydigidaw.ui.theme.MydigidawTheme
import com.example.mydigidaw.util.startMillis
import java.time.LocalDate
import kotlinx.coroutines.launch

/**
 * Lembar "Simpan ke Mydigidaw" yang muncul di atas aplikasi lain saat pengguna membagikan teks
 * atau memilih teks lalu menekan menu Mydigidaw. Catatan & tugas langsung tersimpan tanpa membuka
 * aplikasi; transaksi diteruskan ke editor lengkap karena butuh kategori & dompet.
 */
class ShareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val shared = parseIntent(intent)
        if (shared == null) {
            Toast.makeText(this, "Tidak ada teks untuk disimpan", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val container = (application as MydigidawApp).container
        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            MydigidawTheme(
                themeMode = settings?.themeMode ?: ThemeMode.SYSTEM,
                dynamicColor = settings?.dynamicColor ?: false,
            ) {
                ShareSheet(
                    shared = shared,
                    onSaveNote = { title, body -> save(container, "Tersimpan di Catatan") { saveNote(it, title, body) } },
                    onSaveTask = { title, notes, due -> save(container, "Tersimpan di Tugas") { saveTask(it, title, notes, due) } },
                    onOpenTransaction = { amount, note -> openTransaction(amount, note) },
                    onDismiss = ::finish,
                )
            }
        }
    }

    private fun parseIntent(intent: Intent?): SharedContent? = when (intent?.action) {
        Intent.ACTION_SEND -> ShareParser.parse(
            intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString(),
            intent.getStringExtra(Intent.EXTRA_SUBJECT),
        )
        Intent.ACTION_PROCESS_TEXT -> ShareParser.parse(intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString())
        else -> null
    }

    private fun save(container: AppContainer, message: String, block: suspend (AppContainer) -> Unit) {
        lifecycleScope.launch {
            val result = runCatching { block(container) }
            Toast.makeText(this@ShareActivity, result.fold({ message }, { "Gagal menyimpan: ${it.message}" }), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private suspend fun saveNote(c: AppContainer, title: String, body: String) {
        val now = System.currentTimeMillis()
        c.notes.save(NoteEntity(title = title.trim(), content = body.trim(), category = NoteCategory.UMUM.name, createdAt = now, updatedAt = now))
    }

    private suspend fun saveTask(c: AppContainer, title: String, notes: String, due: LocalDate?) {
        c.tasks.save(
            TaskEntity(
                title = title.trim(),
                notes = notes.trim(),
                priority = Priority.MEDIUM.name,
                dueAt = due?.startMillis(),
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    private fun openTransaction(amount: Long?, note: String) {
        val link = Uri.Builder().scheme("mydigidaw").authority("new").appendPath("transaction").apply {
            if (amount != null && amount > 0) appendQueryParameter("amount", amount.toString())
            if (note.isNotBlank()) appendQueryParameter("note", note.trim())
        }.build()
        startActivity(
            Intent(this, MainActivity::class.java)
                .setAction(Intent.ACTION_VIEW)
                .setData(link)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        )
        finish()
    }
}

private enum class ShareTarget(val label: String) { NOTE("Catatan"), TASK("Tugas"), TRANSACTION("Transaksi") }

private enum class DueChoice(val label: String) { NONE("Tanpa tanggal"), TODAY("Hari ini"), TOMORROW("Besok") }

@Composable
private fun ShareSheet(
    shared: SharedContent,
    onSaveNote: (String, String) -> Unit,
    onSaveTask: (String, String, LocalDate?) -> Unit,
    onOpenTransaction: (Long?, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    // Teks berisi nominal (mis. SMS bank) kemungkinan besar transaksi.
    var target by rememberSaveable { mutableIntStateOf(if (shared.amount != null) ShareTarget.TRANSACTION.ordinal else ShareTarget.NOTE.ordinal) }
    var title by rememberSaveable { mutableStateOf(shared.title) }
    var body by rememberSaveable { mutableStateOf(shared.body) }
    var amount by rememberSaveable { mutableStateOf(shared.amount?.toString().orEmpty()) }
    var due by rememberSaveable { mutableStateOf(DueChoice.NONE) }
    var busy by remember { mutableStateOf(false) }

    fun closeThen(action: () -> Unit) {
        if (busy) return
        busy = true
        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        scope.launch { sheetState.hide() }.invokeOnCompletion { action() }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(Icons.Outlined.Inventory2, MaterialTheme.colorScheme.primary, size = 40.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Simpan ke Mydigidaw", style = MaterialTheme.typography.titleMedium)
                    Text(
                        shared.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            SegmentedTabs(
                labels = ShareTarget.entries.map { it.label },
                icons = listOf(Icons.AutoMirrored.Outlined.Notes, Icons.Outlined.TaskAlt, Icons.AutoMirrored.Outlined.ReceiptLong),
                selectedIndex = target,
                onSelect = { target = it },
            )
            AnimatedContent(
                targetState = ShareTarget.entries[target],
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "shareTarget",
            ) { current ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (current) {
                        ShareTarget.NOTE -> {
                            OutlinedTextField(title, { title = it }, label = { Text("Judul") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(body, { body = it }, label = { Text("Isi catatan") }, minLines = 3, maxLines = 7, modifier = Modifier.fillMaxWidth())
                        }
                        ShareTarget.TASK -> {
                            OutlinedTextField(title, { title = it }, label = { Text("Tugas") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                DueChoice.entries.forEach { choice ->
                                    FilterChip(selected = due == choice, onClick = { due = choice }, label = { Text(choice.label) })
                                }
                            }
                            if (body.isNotBlank()) {
                                OutlinedTextField(body, { body = it }, label = { Text("Catatan tugas") }, minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth())
                            }
                        }
                        ShareTarget.TRANSACTION -> {
                            AmountField(digits = amount, onDigitsChange = { amount = it }, label = "Jumlah")
                            OutlinedTextField(title, { title = it }, label = { Text("Catatan transaksi") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Text(
                                "Kategori, dompet, dan jenis transaksi dipilih di layar berikutnya.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            val current = ShareTarget.entries[target]
            Button(
                onClick = {
                    when (current) {
                        ShareTarget.NOTE -> closeThen { onSaveNote(title, body) }
                        ShareTarget.TASK -> closeThen {
                            val date = when (due) {
                                DueChoice.NONE -> null
                                DueChoice.TODAY -> LocalDate.now()
                                DueChoice.TOMORROW -> LocalDate.now().plusDays(1)
                            }
                            onSaveTask(title, body, date)
                        }
                        ShareTarget.TRANSACTION -> closeThen { onOpenTransaction(amount.digitsToLong().takeIf { it > 0 }, title) }
                    }
                },
                enabled = !busy && when (current) {
                    ShareTarget.NOTE -> title.isNotBlank() || body.isNotBlank()
                    ShareTarget.TASK -> title.isNotBlank()
                    ShareTarget.TRANSACTION -> true
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(if (current == ShareTarget.TRANSACTION) "Lanjutkan di aplikasi" else "Simpan", style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}
