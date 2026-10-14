package com.example.mydigidaw.data.db

import com.example.mydigidaw.data.model.AccountType
import com.example.mydigidaw.data.model.TxType

/** Indeks warna 8 = abu-abu netral, dipakai untuk kategori "Lainnya". */
const val NEUTRAL_COLOR = 8

object DefaultData {
    private val expense = listOf(
        "Makanan & Minuman" to "🍔",
        "Transportasi" to "🚗",
        "Belanja" to "🛍️",
        "Tagihan" to "🧾",
        "Hiburan" to "🎬",
        "Kesehatan" to "💊",
        "Pendidikan" to "📚",
        "Olahraga" to "🏋️",
        "Rumah Tangga" to "🏠",
        "Hadiah & Donasi" to "🎁",
    )

    private val income = listOf(
        "Gaji" to "💼",
        "Bonus" to "🎉",
        "Usaha" to "🛒",
        "Investasi" to "📈",
        "Hadiah" to "🎁",
    )

    fun categories(): List<CategoryEntity> {
        val list = mutableListOf<CategoryEntity>()
        expense.forEachIndexed { i, (name, emoji) ->
            list += CategoryEntity(name = name, type = TxType.EXPENSE.name, emoji = emoji, color = i % 8, sortOrder = i)
        }
        list += CategoryEntity(name = "Lainnya", type = TxType.EXPENSE.name, emoji = "📦", color = NEUTRAL_COLOR, sortOrder = 99)
        income.forEachIndexed { i, (name, emoji) ->
            list += CategoryEntity(name = name, type = TxType.INCOME.name, emoji = emoji, color = (i * 2) % 8, sortOrder = i)
        }
        list += CategoryEntity(name = "Lainnya", type = TxType.INCOME.name, emoji = "📦", color = NEUTRAL_COLOR, sortOrder = 99)
        return list
    }

    suspend fun seedIfEmpty(db: AppDatabase) {
        if (db.categories().count() == 0) db.categories().insertAll(categories())
        if (db.accounts().count() == 0) {
            db.accounts().insert(
                AccountEntity(
                    name = "Dompet Tunai",
                    type = AccountType.CASH.name,
                    initialBalance = 0,
                    color = 2,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }
}
