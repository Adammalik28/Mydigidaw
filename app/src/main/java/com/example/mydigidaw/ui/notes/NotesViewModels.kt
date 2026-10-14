package com.example.mydigidaw.ui.notes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mydigidaw.data.db.NoteEntity
import com.example.mydigidaw.data.model.NoteCategory
import com.example.mydigidaw.data.model.enumOr
import com.example.mydigidaw.data.repo.NotesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NotesViewModel(private val repo: NotesRepository) : ViewModel() {
    val query = MutableStateFlow("")
    val category = MutableStateFlow<NoteCategory?>(null)

    val notes: StateFlow<List<NoteEntity>?> = combine(repo.all, query, category) { list, q, cat ->
        list.filter { !it.isArchived && !it.isTrashed }
            .filter { cat == null || it.category == cat.name }
            .filter { q.isBlank() || it.title.contains(q, ignoreCase = true) || it.content.contains(q, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setQuery(value: String) {
        query.value = value
    }

    fun setCategory(value: NoteCategory?) {
        category.value = value
    }

    fun togglePin(note: NoteEntity) = viewModelScope.launch { repo.setPinned(note.id, !note.isPinned) }
    fun archive(note: NoteEntity) = viewModelScope.launch { repo.setArchived(note.id, true) }
    fun trash(note: NoteEntity) = viewModelScope.launch { repo.setTrashed(note.id, true) }
    fun untrash(note: NoteEntity) = viewModelScope.launch { repo.setTrashed(note.id, false) }
    fun unarchive(note: NoteEntity) = viewModelScope.launch { repo.setArchived(note.id, false) }
}

class NotesBinViewModel(private val repo: NotesRepository, val isTrash: Boolean) : ViewModel() {
    val notes: StateFlow<List<NoteEntity>?> = repo.all.map { list ->
        if (isTrash) list.filter { it.isTrashed } else list.filter { it.isArchived && !it.isTrashed }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun restore(note: NoteEntity) = viewModelScope.launch {
        if (isTrash) repo.setTrashed(note.id, false) else repo.setArchived(note.id, false)
    }

    fun deleteForever(note: NoteEntity) = viewModelScope.launch { repo.delete(note.id) }
    fun moveToTrash(note: NoteEntity) = viewModelScope.launch { repo.setTrashed(note.id, true) }
    fun emptyTrash() = viewModelScope.launch { repo.emptyTrash() }
}

/**
 * Editor catatan dengan simpan otomatis. Penyimpanan berjalan di scope aplikasi agar tetap selesai
 * walaupun layar sudah ditutup, dan diserialkan dengan mutex supaya catatan baru tidak tersimpan ganda.
 */
class NoteEditorViewModel(
    noteId: Long,
    private val repo: NotesRepository,
    private val appScope: CoroutineScope,
) : ViewModel() {
    var title by mutableStateOf("")
    var content by mutableStateOf("")
    var category by mutableStateOf(NoteCategory.UMUM)
    var color by mutableIntStateOf(0)
    var pinned by mutableStateOf(false)
    var isChecklist by mutableStateOf(false)
        private set
    var archived by mutableStateOf(false)
        private set
    var updatedAt by mutableLongStateOf(System.currentTimeMillis())
        private set
    var loaded by mutableStateOf(noteId <= 0)
        private set

    private var original: NoteEntity? = null
    @Volatile private var discarded = false
    private val mutex = Mutex()

    private data class Snapshot(
        val title: String,
        val content: String,
        val category: String,
        val color: Int,
        val pinned: Boolean,
        val checklist: Boolean,
    )

    init {
        if (noteId > 0) {
            viewModelScope.launch {
                repo.get(noteId)?.let { n ->
                    original = n
                    title = n.title
                    content = n.content
                    category = enumOr(n.category, NoteCategory.UMUM)
                    color = n.color
                    pinned = n.isPinned
                    isChecklist = n.isChecklist
                    archived = n.isArchived
                    updatedAt = n.updatedAt
                }
                loaded = true
            }
        }
    }

    private fun snapshot() = Snapshot(title, content, category.name, color, pinned, isChecklist)

    /** Harus dipanggil di dalam [mutex]. */
    private suspend fun persistLocked(s: Snapshot) {
        if (discarded) return
        val orig = original
        val emptyContent = if (s.checklist) Checklist.toPlain(s.content).isBlank() else s.content.isBlank()
        if (orig == null && s.title.isBlank() && emptyContent) return
        val unchanged = orig != null && orig.title == s.title && orig.content == s.content &&
            orig.category == s.category && orig.color == s.color && orig.isPinned == s.pinned && orig.isChecklist == s.checklist
        if (unchanged) return
        val now = System.currentTimeMillis()
        val entity = (orig ?: NoteEntity(title = "", content = "", category = s.category, createdAt = now, updatedAt = now))
            .copy(
                title = s.title,
                content = s.content,
                category = s.category,
                color = s.color,
                isPinned = s.pinned,
                isChecklist = s.checklist,
                updatedAt = now,
            )
        val id = repo.save(entity)
        original = entity.copy(id = id)
        updatedAt = now
    }

    fun save() {
        if (!loaded) return
        val s = snapshot()
        appScope.launch { mutex.withLock { persistLocked(s) } }
    }

    fun moveToTrash() {
        discarded = true
        appScope.launch { mutex.withLock { original?.let { repo.setTrashed(it.id, true) } } }
    }

    /** Membatalkan [moveToTrash] lewat tombol "Urungkan". */
    fun restoreFromTrash() {
        appScope.launch { mutex.withLock { original?.let { repo.setTrashed(it.id, false) } } }
    }

    val hasSavedNote: Boolean get() = original != null

    fun toggleArchive() {
        if (!loaded) return
        val s = snapshot()
        val target = !archived
        archived = target
        appScope.launch {
            mutex.withLock {
                persistLocked(s)
                original?.let { repo.setArchived(it.id, target) }
            }
        }
    }

    /** Mengubah catatan teks ⇄ daftar centang tanpa kehilangan isi. */
    fun toggleChecklist() {
        content = if (isChecklist) Checklist.toPlain(content) else Checklist.fromPlain(content)
        isChecklist = !isChecklist
    }

    val shareText: String
        get() = listOf(title, if (isChecklist) Checklist.toShareText(content) else content).filter { it.isNotBlank() }.joinToString("\n\n")

    val summary: String
        get() = if (isChecklist) {
            val items = Checklist.parse(content).filter { it.text.isNotBlank() }
            "${items.count { it.checked }}/${items.size} item selesai"
        } else {
            "${content.split(Regex("\\s+")).count { it.isNotBlank() }} kata"
        }
}
