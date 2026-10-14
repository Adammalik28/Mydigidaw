package com.example.mydigidaw.ui.finance

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.mydigidaw.data.db.NEUTRAL_COLOR
import com.example.mydigidaw.data.model.TxType
import com.example.mydigidaw.data.repo.TxItem
import com.example.mydigidaw.ui.components.EmojiBadge
import com.example.mydigidaw.ui.components.MoneyText
import com.example.mydigidaw.ui.theme.AppColors
import com.example.mydigidaw.util.Dates
import com.example.mydigidaw.util.toLocalDate

/** Warna teks nominal: biru = masuk, merah = keluar, netral = transfer (selalu disertai tanda +/−). */
@Composable
fun amountColor(type: TxType): Color = when (type) {
    TxType.INCOME -> AppColors.current.incomeText
    TxType.EXPENSE -> AppColors.current.expenseText
    TxType.TRANSFER -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
fun TxRow(item: TxItem, hidden: Boolean, onClick: () -> Unit, showDate: Boolean = false) {
    val ext = AppColors.current
    val type = item.type
    val emoji: String
    val title: String
    val color: Color
    if (type == TxType.TRANSFER) {
        emoji = "🔁"
        title = "Transfer"
        color = ext.seriesColor(NEUTRAL_COLOR)
    } else {
        emoji = item.category?.emoji ?: "❔"
        title = item.category?.name ?: "Tanpa kategori"
        color = ext.seriesColor(item.category?.color ?: NEUTRAL_COLOR)
    }
    val subtitle = buildList {
        if (type == TxType.TRANSFER) add("${item.account?.name ?: "?"} → ${item.toAccount?.name ?: "?"}")
        else {
            if (item.tx.note.isNotBlank()) add(item.tx.note)
            item.account?.name?.let(::add)
        }
        if (showDate) add(Dates.relativeDay(item.tx.date.toLocalDate()))
    }.joinToString(" · ")
    val signed = when (type) {
        TxType.INCOME -> item.tx.amount
        TxType.EXPENSE -> -item.tx.amount
        TxType.TRANSFER -> item.tx.amount
    }
    ListItem(
        headlineContent = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingContent = { EmojiBadge(emoji, color) },
        trailingContent = {
            MoneyText(
                signed,
                color = amountColor(type),
                fontWeight = FontWeight.SemiBold,
                withSign = type != TxType.TRANSFER,
                hidden = hidden,
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

/** Chip filter yang membuka menu pilihan. */
@Composable
fun FilterPill(label: String, active: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = active,
        onClick = onClick,
        label = { Text(label, maxLines = 1) },
        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier) },
        colors = FilterChipDefaults.filterChipColors(),
    )
}
