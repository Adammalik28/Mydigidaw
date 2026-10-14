package com.example.mydigidaw.ui.notes

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.mydigidaw.ui.components.LocalFeedback
import com.example.mydigidaw.ui.components.Motion

data class ChecklistItem(val text: String, val checked: Boolean)

/** Format penyimpanan daftar centang: satu item per baris, "[ ] teks" atau "[x] teks". */
object Checklist {
    fun parse(content: String): List<ChecklistItem> {
        if (content.isEmpty()) return emptyList()
        return content.split('\n').map { line ->
            when {
                line.startsWith("[x] ", ignoreCase = true) -> ChecklistItem(line.substring(4), true)
                line.startsWith("[ ] ") -> ChecklistItem(line.substring(4), false)
                else -> ChecklistItem(line.trimStart('-', '•', '*', ' '), false)
            }
        }
    }

    fun serialize(items: List<ChecklistItem>): String =
        items.joinToString("\n") { (if (it.checked) "[x] " else "[ ] ") + it.text }

    fun toPlain(content: String): String = parse(content).filter { it.text.isNotBlank() }.joinToString("\n") { it.text }

    fun fromPlain(text: String): String =
        serialize(text.split('\n').filter { it.isNotBlank() }.map { ChecklistItem(it.trim(), false) }.ifEmpty { listOf(ChecklistItem("", false)) })

    /** Teks yang enak dibaca untuk dibagikan ke aplikasi lain. */
    fun toShareText(content: String): String =
        parse(content).filter { it.text.isNotBlank() }.joinToString("\n") { (if (it.checked) "☑ " else "☐ ") + it.text }
}

fun checklistPreview(content: String, max: Int): String =
    Checklist.parse(content).filter { it.text.isNotBlank() }.take(max).joinToString("  ") { (if (it.checked) "☑ " else "☐ ") + it.text }

@Composable
fun ChecklistEditor(content: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val feedback = LocalFeedback.current
    val items = Checklist.parse(content)
    val open = items.withIndex().filter { !it.value.checked }
    val done = items.withIndex().filter { it.value.checked }
    var focusIndex by remember { mutableStateOf<Int?>(null) }

    fun update(newItems: List<ChecklistItem>) = onChange(Checklist.serialize(newItems))
    fun set(index: Int, item: ChecklistItem) = update(items.toMutableList().also { it[index] = item })
    fun insertAfter(index: Int) {
        update(items.toMutableList().also { it.add(index + 1, ChecklistItem("", false)) })
        focusIndex = index + 1
    }

    LazyColumn(modifier.fillMaxWidth()) {
        items(open, key = { "o${it.index}" }) { (index, item) ->
            ChecklistRow(
                item = item,
                requestFocus = focusIndex == index,
                onFocused = { focusIndex = null },
                onCheck = {
                    feedback.toggle(true)
                    set(index, item.copy(checked = true))
                },
                onText = { set(index, item.copy(text = it)) },
                onNext = { insertAfter(index) },
                onDelete = { update(items.filterIndexed { i, _ -> i != index }) },
            )
        }
        item(key = "add") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        focusIndex = items.size
                        update(items + ChecklistItem("", false))
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(16.dp))
                Text("Tambah item", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
        if (done.isNotEmpty()) {
            item(key = "doneHeader") {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    HorizontalDivider()
                    Text(
                        "Selesai · ${done.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    )
                }
            }
            items(done, key = { "d${it.index}" }) { (index, item) ->
                ChecklistRow(
                    item = item,
                    requestFocus = false,
                    onFocused = {},
                    onCheck = {
                        feedback.toggle(false)
                        set(index, item.copy(checked = false))
                    },
                    onText = { set(index, item.copy(text = it)) },
                    onNext = {},
                    onDelete = { update(items.filterIndexed { i, _ -> i != index }) },
                )
            }
        }
    }
}

@Composable
private fun ChecklistRow(
    item: ChecklistItem,
    requestFocus: Boolean,
    onFocused: () -> Unit,
    onCheck: () -> Unit,
    onText: (String) -> Unit,
    onNext: () -> Unit,
    onDelete: () -> Unit,
) {
    val requester = remember { FocusRequester() }
    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            runCatching { requester.requestFocus() }
            onFocused()
        }
    }
    val textColor by animateColorAsState(
        if (item.checked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
        tween(Motion.MEDIUM),
        label = "checkText",
    )
    Row(Modifier.fillMaxWidth().padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = item.checked, onCheckedChange = { onCheck() })
        BasicTextField(
            value = item.text,
            onValueChange = { onText(it.replace("\n", "")) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = textColor,
                textDecoration = if (item.checked) TextDecoration.LineThrough else null,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { onNext() }),
            decorationBox = { inner ->
                Box {
                    if (item.text.isEmpty()) {
                        Text("Item daftar", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                    }
                    inner()
                }
            },
            modifier = Modifier.weight(1f).focusRequester(requester),
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Close, contentDescription = "Hapus item", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
        }
    }
}
