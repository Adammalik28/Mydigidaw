package com.example.mydigidaw.ui.vault

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mydigidaw.AppContainer
import com.example.mydigidaw.data.model.VaultType
import com.example.mydigidaw.data.model.enumOr
import com.example.mydigidaw.data.repo.VaultField
import com.example.mydigidaw.data.repo.VaultItem
import java.security.SecureRandom
import javax.crypto.Cipher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VaultViewModel(private val c: AppContainer) : ViewModel() {
    val configured: StateFlow<Boolean?> = c.vault.configured.map<Boolean, Boolean?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val unlocked: StateFlow<Boolean> = c.vault.unlocked
    val biometricEnabled: StateFlow<Boolean> = c.vault.biometricEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val query = MutableStateFlow("")
    val typeFilter = MutableStateFlow<VaultType?>(null)

    val items: StateFlow<List<VaultItem>> = combine(c.vaultItems.items, query, typeFilter) { list, q, t ->
        list.filter { item ->
            (t == null || item.type == t) &&
                (q.isBlank() || item.title.contains(q, true) || item.subtitle.contains(q, true))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)

    fun setup(password: String, confirm: String) {
        error = when {
            password.length < 6 -> "Kata sandi utama minimal 6 karakter"
            password != confirm -> "Konfirmasi kata sandi tidak cocok"
            else -> null
        }
        if (error != null) return
        busy = true
        viewModelScope.launch {
            c.vault.setup(password.toCharArray())
            busy = false
        }
    }

    fun unlock(password: String) {
        if (password.isEmpty()) return
        busy = true
        error = null
        viewModelScope.launch {
            val ok = c.vault.unlock(password.toCharArray())
            busy = false
            if (!ok) error = "Kata sandi salah"
        }
    }

    fun lock() = c.vault.lock()

    suspend fun biometricCipher(): Cipher? = c.vault.biometricDecryptCipher()

    fun unlockWithCipher(cipher: Cipher) = viewModelScope.launch {
        if (!c.vault.unlockWithCipher(cipher)) error = "Gagal membuka dengan biometrik, gunakan kata sandi"
    }

    fun toggleFavorite(item: VaultItem) = viewModelScope.launch { c.vaultItems.save(item.copy(favorite = !item.favorite)) }
    fun delete(item: VaultItem) = viewModelScope.launch { c.vaultItems.delete(item.id) }
}

class VaultEditorViewModel(
    private val itemId: Long,
    initialType: String?,
    private val c: AppContainer,
) : ViewModel() {
    val isNew = itemId <= 0
    var type by mutableStateOf(enumOr(initialType, VaultType.LOGIN))
        private set
    var title by mutableStateOf("")
    val fields = mutableStateListOf<VaultField>()
    var notes by mutableStateOf("")
    var favorite by mutableStateOf(false)
    var loaded by mutableStateOf(false)
        private set
    var titleError by mutableStateOf(false)
    private var original: VaultItem? = null

    val unlocked: StateFlow<Boolean> = c.vault.unlocked

    init {
        viewModelScope.launch {
            if (!isNew) {
                c.vaultItems.get(itemId)?.let { item ->
                    original = item
                    type = item.type
                    title = item.title
                    fields.addAll(item.fields)
                    notes = item.notes
                    favorite = item.favorite
                }
            } else {
                fields.addAll(type.template.map { VaultField(it.label, "", it.secret) })
            }
            loaded = true
        }
    }

    fun changeType(newType: VaultType) {
        if (!isNew || newType == type) return
        val filled = fields.filter { it.value.isNotBlank() }
        type = newType
        fields.clear()
        fields.addAll(newType.template.map { VaultField(it.label, "", it.secret) })
        filled.forEach { f -> if (fields.none { it.label == f.label }) fields.add(f) }
    }

    fun updateField(index: Int, value: String) {
        fields[index] = fields[index].copy(value = value)
    }

    fun addField(label: String, secret: Boolean) {
        fields.add(VaultField(label.trim().ifBlank { "Kolom" }, "", secret))
    }

    fun removeField(index: Int) {
        fields.removeAt(index)
    }

    suspend fun save(): Boolean {
        titleError = title.isBlank()
        if (titleError) return false
        c.vaultItems.save(
            VaultItem(
                id = original?.id ?: 0,
                type = type,
                title = title.trim(),
                fields = fields.toList(),
                notes = notes,
                favorite = favorite,
                createdAt = original?.createdAt ?: 0,
            ),
        )
        return true
    }

    suspend fun delete() {
        original?.let { c.vaultItems.delete(it.id) }
    }
}

object PasswordGenerator {
    private const val UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ"
    private const val LOWER = "abcdefghijkmnopqrstuvwxyz"
    private const val DIGITS = "23456789"
    private const val SYMBOLS = "!@#\$%^&*()-_=+[]{};:,.?"
    private val random = SecureRandom()

    fun generate(length: Int, upper: Boolean, lower: Boolean, digits: Boolean, symbols: Boolean): String {
        val sets = buildList {
            if (upper) add(UPPER)
            if (lower) add(LOWER)
            if (digits) add(DIGITS)
            if (symbols) add(SYMBOLS)
        }.ifEmpty { listOf(LOWER) }
        val all = sets.joinToString("")
        val chars = mutableListOf<Char>()
        sets.forEach { set -> chars += set[random.nextInt(set.length)] }
        while (chars.size < length) chars += all[random.nextInt(all.length)]
        for (i in chars.indices.reversed()) {
            val j = random.nextInt(i + 1)
            val tmp = chars[i]
            chars[i] = chars[j]
            chars[j] = tmp
        }
        return chars.take(length).joinToString("")
    }

    /** 0 (sangat lemah) .. 4 (sangat kuat). */
    fun strength(password: String): Int {
        if (password.isEmpty()) return 0
        var score = 0
        if (password.length >= 8) score++
        if (password.length >= 12) score++
        if (password.any(Char::isUpperCase) && password.any(Char::isLowerCase)) score++
        if (password.any(Char::isDigit)) score++
        if (password.any { !it.isLetterOrDigit() }) score++
        if (password.length < 6) score = minOf(score, 1)
        return score.coerceAtMost(4)
    }

    val strengthLabels = listOf("Sangat lemah", "Lemah", "Cukup", "Kuat", "Sangat kuat")
}
