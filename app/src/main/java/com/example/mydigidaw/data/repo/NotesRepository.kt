package com.example.mydigidaw.data.repo

import com.example.mydigidaw.data.db.NoteDao
import com.example.mydigidaw.data.db.NoteEntity
import kotlinx.coroutines.flow.Flow

class NotesRepository(private val dao: NoteDao) {
    val all: Flow<List<NoteEntity>> = dao.observeAll()

    suspend fun get(id: Long): NoteEntity? = dao.get(id)

    suspend fun save(note: NoteEntity): Long =
        if (note.id == 0L) dao.insert(note) else note.id.also { dao.update(note) }

    suspend fun setPinned(id: Long, pinned: Boolean) = dao.setPinned(id, pinned)
    suspend fun setArchived(id: Long, archived: Boolean) = dao.setArchived(id, archived)
    suspend fun setTrashed(id: Long, trashed: Boolean) = dao.setTrashed(id, trashed)
    suspend fun delete(id: Long) = dao.delete(id)
    suspend fun emptyTrash() = dao.emptyTrash()
}
