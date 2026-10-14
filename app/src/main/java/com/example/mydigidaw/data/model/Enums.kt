package com.example.mydigidaw.data.model

/** Membaca nama enum yang tersimpan sebagai teks dengan aman. */
inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: default

enum class NoteCategory(val label: String, val emoji: String) {
    UMUM("Umum", "📝"),
    PENTING("Penting", "⭐"),
    PRIBADI("Pribadi", "👤"),
    PEKERJAAN("Pekerjaan", "💼"),
    IDE("Ide", "💡"),
}

enum class TxType(val label: String) {
    EXPENSE("Pengeluaran"),
    INCOME("Pemasukan"),
    TRANSFER("Transfer"),
}

enum class AccountType(val label: String, val emoji: String) {
    CASH("Tunai", "💵"),
    BANK("Bank", "🏦"),
    EWALLET("E-Wallet", "📱"),
    SAVINGS("Tabungan", "🐷"),
    INVESTMENT("Investasi", "📈"),
    OTHER("Lainnya", "👛"),
}

enum class Repeat(val label: String, val rrule: String?) {
    NONE("Tidak berulang", null),
    DAILY("Setiap hari", "FREQ=DAILY"),
    WEEKLY("Setiap minggu", "FREQ=WEEKLY"),
    MONTHLY("Setiap bulan", "FREQ=MONTHLY"),
    YEARLY("Setiap tahun", "FREQ=YEARLY"),
}

enum class Priority(val label: String) {
    LOW("Rendah"),
    MEDIUM("Sedang"),
    HIGH("Tinggi"),
}

enum class DebtType(val label: String, val description: String) {
    PAYABLE("Utang", "Uang yang harus saya bayar"),
    RECEIVABLE("Piutang", "Uang yang dipinjam orang lain"),
}

enum class ThemeMode(val label: String) {
    SYSTEM("Ikuti sistem"),
    LIGHT("Terang"),
    DARK("Gelap"),
}

data class FieldTemplate(val label: String, val secret: Boolean = false)

enum class VaultType(val label: String, val emoji: String, val template: List<FieldTemplate>) {
    LOGIN(
        "Akun & Kata Sandi", "🔑",
        listOf(FieldTemplate("Username / Email"), FieldTemplate("Kata sandi", true), FieldTemplate("Situs / Aplikasi")),
    ),
    CARD(
        "Kartu", "💳",
        listOf(
            FieldTemplate("Nama kartu / Bank"), FieldTemplate("Nomor kartu", true), FieldTemplate("Nama pemilik"),
            FieldTemplate("Berlaku hingga"), FieldTemplate("CVV", true), FieldTemplate("PIN", true),
        ),
    ),
    BANK(
        "Rekening Bank", "🏦",
        listOf(
            FieldTemplate("Nama bank"), FieldTemplate("Nomor rekening"), FieldTemplate("Atas nama"),
            FieldTemplate("PIN ATM", true), FieldTemplate("PIN m-banking", true),
        ),
    ),
    WIFI("Wi-Fi", "📶", listOf(FieldTemplate("Nama jaringan (SSID)"), FieldTemplate("Kata sandi", true))),
    IDENTITY(
        "Identitas", "🪪",
        listOf(FieldTemplate("Jenis dokumen"), FieldTemplate("Nomor", true), FieldTemplate("Nama lengkap"), FieldTemplate("Berlaku hingga")),
    ),
    SECURE_NOTE("Catatan Rahasia", "📄", emptyList()),
    OTHER("Lainnya", "🗂️", listOf(FieldTemplate("Keterangan"), FieldTemplate("Rahasia", true))),
}
