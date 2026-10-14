@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class, FlowPreview::class)

package com.example.mydigidaw.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.outlined.Search
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.mydigidaw.AppContainer
import com.example.mydigidaw.data.db.EventEntity
import com.example.mydigidaw.data.db.HabitEntity
import com.example.mydigidaw.data.db.JournalEntity
import com.example.mydigidaw.data.db.NoteEntity
import com.example.mydigidaw.data.db.TaskEntity
import com.example.mydigidaw.data.repo.FinanceRepository
import com.example.mydigidaw.data.repo.Moods
import com.example.mydigidaw.data.repo.TxItem
import com.example.mydigidaw.data.repo.VaultItem
import com.example.mydigidaw.ui.appViewModel
import com.example.mydigidaw.ui.components.EmojiBadge
import com.example.mydigidaw.ui.components.EmptyState
import com.example.mydigidaw.ui.components.enterStagger
import com.example.mydigidaw.ui.finance.TxRow
import com.example.mydigidaw.ui.navigation.Routes
import com.example.mydigidaw.ui.notes.checklistPreview
import com.example.mydigidaw.util.Dates
import com.example.mydigidaw.util.toLocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class SearchResults(
    val query: String = "",
    val notes: List<NoteEntity> = emptyList(),
    val transactions: List<TxItem> = emptyList(),
    val events: List<EventEntity> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val habits: List<HabitEntity> = emptyList(),
    val journal: List<JournalEntity> = emptyList(),
    val vault: List<VaultItem> = emptyList(),
) {
    val total: Int get() = notes.size + transactions.size + events.size + tasks.size + habits.size + journal.size + vault.size
}

class SearchViewModel(c: AppContainer) : ViewModel() {
    val query = MutableStateFlow("")

    private val finance = combine(c.finance.transactions, c.finance.categories, c.finance.accounts) { t, cat, a ->
        FinanceRepository.toItems(t, cat.associateBy { it.id }, a.associateBy { it.id })
    }

    private val sources = combine(c.notes.all, finance, c.calendar.events, c.tasks.tasks, c.habits.habits) { n, f, e, t, h ->
        listOf(n, f, e, t, h)
    }

    @Suppress("UNCHECKED_CAST")
    val results: StateFlow<SearchResults> = combine(query.debounce(180), sources, c.journal.entries, c.vaultItems.items) { q, src, journal, vault ->
        val term = q.trim()
        if (term.length < 2) return@combine SearchResults(term)
        fun String.hit() = contains(term, ignoreCase = true)
        val notes = (src[0] as List<NoteEntity>).filter { !it.isTrashed && (it.title.hit() || it.content.hit()) }
        val tx = (src[1] as List<TxItem>).filter {
            it.tx.note.hit() || it.category?.name?.hit() == true || it.account?.name?.hit() == true ||
                (term.all(Char::isDigit) && it.tx.amount.toString().contains(term))
        }
        val events = (src[2] as List<EventEntity>).filter { it.title.hit() || it.location.hit() || it.description.hit() }
        val tasks = (src[3] as List<TaskEntity>).filter { it.title.hit() || it.notes.hit() }
        val habits = (src[4] as List<HabitEntity>).filter { it.name.hit() }
        SearchResults(
            query = term,
            notes = notes.take(20),
            transactions = tx.take(20),
            events = events.sortedByDescending { it.startAt }.take(15),
            tasks = tasks.take(15),
            habits = habits.take(10),
            journal = journal.filter { it.note.hit() }.take(10),
            vault = vault.filter { it.title.hit() || it.subtitle.hit() }.take(10),
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchResults())
}

@Composable
fun SearchScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val vm = appViewModel { SearchViewModel(it) }
    val query by vm.query.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali") } },
                title = {
                    TextField(
                        value = query,
                        onValueChange = { vm.query.value = it },
                        placeholder = { Text("Cari di semua fitur…") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                        trailingIcon = {
                            if (query.isNotEmpty()) IconButton(onClick = { vm.query.value = "" }) { Icon(Icons.Filled.Close, "Hapus") }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                },
            )
        },
    ) { padding ->
        val r = results
        when {
            r.query.length < 2 -> SearchHints(Modifier.padding(padding)) { vm.query.value = it }
            r.total == 0 -> EmptyState(
                emoji = "🔍",
                title = "Tidak ada hasil untuk \"${r.query}\"",
                message = "Coba kata lain, misalnya nama kategori atau sebagian judul.",
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                Modifier.padding(padding).fillMaxSize().imePadding(),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                section("Catatan", r.notes.size)
                items(r.notes, key = { "n${it.id}" }) { n ->
                    ResultRow(
                        emoji = if (n.isChecklist) "☑️" else "📝",
                        title = n.title.ifBlank { "(Tanpa judul)" },
                        subtitle = if (n.isChecklist) checklistPreview(n.content, 2) else n.content.replace('\n', ' '),
                        onClick = { onNavigate(Routes.noteEdit(n.id)) },
                    )
                }
                section("Transaksi", r.transactions.size)
                items(r.transactions, key = { "x${it.tx.id}" }) { t ->
                    TxRow(t, hidden = false, onClick = { onNavigate(Routes.txEdit(t.tx.id)) }, showDate = true)
                }
                section("Jadwal", r.events.size)
                items(r.events, key = { "e${it.id}" }) { e ->
                    ResultRow(
                        emoji = "📅",
                        title = e.title,
                        subtitle = Dates.weekdayFull(e.startAt.toLocalDate()) + if (e.allDay) "" else ", ${Dates.time(e.startAt)}",
                        onClick = { onNavigate(Routes.eventEdit(e.id)) },
                    )
                }
                section("Tugas", r.tasks.size)
                items(r.tasks, key = { "t${it.id}" }) { t ->
                    ResultRow(
                        emoji = if (t.isDone) "✅" else "⬜",
                        title = t.title,
                        subtitle = t.dueAt?.let { Dates.relativeDay(it.toLocalDate()) } ?: "Tanpa tanggal",
                        onClick = { onNavigate(Routes.tasks()) },
                    )
                }
                section("Kebiasaan", r.habits.size)
                items(r.habits, key = { "h${it.id}" }) { h ->
                    ResultRow(h.emoji, h.name, "Lihat riwayat & statistik", onClick = { onNavigate(Routes.habitDetail(h.id)) })
                }
                section("Jurnal", r.journal.size)
                items(r.journal, key = { "j${it.day}" }) { j ->
                    ResultRow(
                        Moods.emojiOf(j.mood),
                        Dates.full(LocalDate.ofEpochDay(j.day)),
                        j.note.replace('\n', ' '),
                        onClick = { onNavigate(Routes.JOURNAL) },
                    )
                }
                section("Brankas", r.vault.size)
                items(r.vault, key = { "v${it.id}" }) { v ->
                    ResultRow(v.type.emoji, v.title, v.type.label, onClick = { onNavigate(Routes.vaultEdit(v.id)) })
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(title: String, count: Int) {
    if (count == 0) return
    item(key = "s_$title") {
        Text(
            "$title · $count",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 4.dp).enterStagger(),
        )
    }
}

@Composable
private fun ResultRow(emoji: String, title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingContent = { EmojiBadge(emoji, MaterialTheme.colorScheme.primary) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun SearchHints(modifier: Modifier, onPick: (String) -> Unit) {
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Cari apa saja", style = MaterialTheme.typography.titleLarge, modifier = Modifier.enterStagger(0))
        Text(
            "Catatan, transaksi, jadwal, tugas, kebiasaan, jurnal — dan isi brankas bila sedang terbuka.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.enterStagger(1),
        )
        Spacer(Modifier.width(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.enterStagger(2),
        ) {
            listOf("gym", "gaji", "makan", "belanja", "rapat").forEach { term ->
                AssistChip(
                    onClick = { onPick(term) },
                    label = { Text(term, maxLines = 1, softWrap = false) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                )
            }
        }
        Text(
            "Tips: ketik nominal tanpa titik (mis. 50000) untuk mencari transaksi.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.enterStagger(3),
        )
    }
}
