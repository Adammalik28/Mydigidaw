package com.example.mydigidaw.ui.share

/** Isi yang dibagikan dari aplikasi lain, sudah dipecah untuk catatan, tugas, atau transaksi. */
data class SharedContent(
    val title: String,
    /** Isi lengkap; kosong bila sama persis dengan [title]. */
    val body: String,
    /** Nominal rupiah pertama yang ditemukan (mis. dari SMS bank), atau null. */
    val amount: Long?,
)

/** Mengurai teks bagikan (tautan, SMS, kutipan) — murni Kotlin agar mudah diuji. */
object ShareParser {
    private const val TITLE_MAX = 80
    private val urlRegex = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)
    private val amountRegex = Regex("""(?i)\b(?:rp|idr)\.?\s*([0-9]{1,3}(?:[.,][0-9]{3})+|[0-9]+)""")

    fun parse(text: String?, subject: String? = null): SharedContent? {
        val body = text?.trim().orEmpty()
        val subj = subject?.trim().orEmpty()
        if (body.isEmpty() && subj.isEmpty()) return null
        val firstLine = body.lineSequence().map(String::trim).firstOrNull(String::isNotEmpty).orEmpty()
        val rawTitle = when {
            subj.isNotEmpty() -> subj
            urlRegex.matches(firstLine) -> hostOf(firstLine) ?: firstLine
            else -> firstLine
        }
        val title = ellipsize(rawTitle.replace(Regex("\\s+"), " "))
        return SharedContent(
            title = title,
            body = if (body == title) "" else body,
            amount = findAmount("$subj\n$body"),
        )
    }

    /** "Rp25.000,00" → 25000; "IDR 1,250,000" → 1250000. Desimal sen diabaikan. */
    fun findAmount(text: String): Long? {
        val digits = amountRegex.find(text)?.groupValues?.get(1)?.filter(Char::isDigit) ?: return null
        if (digits.isEmpty() || digits.length > 15) return null
        return digits.toLong().takeIf { it > 0 }
    }

    private fun hostOf(url: String): String? =
        url.substringAfter("://").substringBefore('/').substringBefore('?').removePrefix("www.").ifBlank { null }

    private fun ellipsize(text: String): String =
        if (text.length <= TITLE_MAX) text else text.take(TITLE_MAX - 1).trimEnd() + "…"
}
