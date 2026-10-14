package com.example.mydigidaw.data.repo

import com.example.mydigidaw.data.db.VaultDao
import com.example.mydigidaw.data.db.VaultItemEntity
import com.example.mydigidaw.data.model.VaultType
import com.example.mydigidaw.data.model.enumOr
import com.example.mydigidaw.security.VaultManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import org.json.JSONArray
import org.json.JSONObject

data class VaultField(val label: String, val value: String, val secret: Boolean)

data class VaultItem(
    val id: Long = 0,
    val type: VaultType,
    val title: String,
    val fields: List<VaultField>,
    val notes: String,
    val favorite: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
) {
    /** Teks ringkas untuk daftar: kolom tidak-rahasia pertama yang terisi. */
    val subtitle: String
        get() = fields.firstOrNull { !it.secret && it.value.isNotBlank() }?.value
            ?: if (type == VaultType.SECURE_NOTE) notes.lineSequence().firstOrNull().orEmpty() else type.label
}

class VaultRepository(private val dao: VaultDao, private val vault: VaultManager) {
    /** Item terdekripsi selama brankas terbuka; kosong ketika terkunci. */
    val items: Flow<List<VaultItem>> = combine(dao.observeAll(), vault.unlocked) { list, unlocked ->
        if (!unlocked) emptyList() else list.mapNotNull(::decode)
    }.flowOn(Dispatchers.Default)

    suspend fun get(id: Long): VaultItem? = dao.get(id)?.let(::decode)

    suspend fun save(item: VaultItem): Long {
        val now = System.currentTimeMillis()
        val entity = VaultItemEntity(
            id = item.id,
            type = item.type.name,
            payload = vault.encrypt(encodePayload(item)),
            isFavorite = item.favorite,
            createdAt = if (item.id == 0L) now else item.createdAt,
            updatedAt = now,
        )
        return if (entity.id == 0L) dao.insert(entity) else entity.id.also { dao.update(entity) }
    }

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun deleteAll() = dao.clear()

    private fun decode(entity: VaultItemEntity): VaultItem? = runCatching {
        val json = JSONObject(vault.decrypt(entity.payload))
        val fieldsJson = json.optJSONArray("fields") ?: JSONArray()
        val fields = (0 until fieldsJson.length()).map { i ->
            val f = fieldsJson.getJSONObject(i)
            VaultField(f.optString("l"), f.optString("v"), f.optBoolean("s"))
        }
        VaultItem(
            id = entity.id,
            type = enumOr(entity.type, VaultType.OTHER),
            title = json.optString("title"),
            fields = fields,
            notes = json.optString("notes"),
            favorite = entity.isFavorite,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }.getOrNull()

    private fun encodePayload(item: VaultItem): String {
        val fields = JSONArray()
        item.fields.forEach { f ->
            fields.put(JSONObject().put("l", f.label).put("v", f.value).put("s", f.secret))
        }
        return JSONObject()
            .put("title", item.title)
            .put("notes", item.notes)
            .put("fields", fields)
            .toString()
    }
}
