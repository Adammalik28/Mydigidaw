@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.example.mydigidaw.ui.notes

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.FormatColorReset
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LocalContentColor
import com.example.mydigidaw.ui.components.SearchPill
import com.example.mydigidaw.ui.components.SegmentedTabs
import com.example.mydigidaw.ui.components.Spacing
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LeadingIconTab
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mydigidaw.data.db.NoteEntity
import com.example.mydigidaw.data.model.NoteCategory
import com.example.mydigidaw.data.model.enumOr
import com.example.mydigidaw.ui.LocalAppContainer
import com.example.mydigidaw.ui.appViewModel
import com.example.mydigidaw.ui.components.BackTopBar
import com.example.mydigidaw.ui.components.ConfirmDialog
import com.example.mydigidaw.ui.components.EmptyState
import com.example.mydigidaw.ui.components.LocalFeedback
import com.example.mydigidaw.ui.components.Motion
import com.example.mydigidaw.ui.components.enterStagger
import com.example.mydigidaw.ui.components.pressScale
import com.example.mydigidaw.ui.navigation.Routes
import com.example.mydigidaw.ui.theme.AppColors
import com.example.mydigidaw.ui.vault.VaultTab
import com.example.mydigidaw.ui.vault.VaultViewModel
import com.example.mydigidaw.util.Dates
import com.example.mydigidaw.util.toLocalDate

@Composable
fun noteContainerColor(index: Int): Color {
    val c = AppColors.current.noteColors.getOrNull(index) ?: Color.Unspecified
    return if (index == 0 || c == Color.Unspecified) MaterialTheme.colorScheme.surfaceContainerLow else c
}

@Composable
fun NotesScreen(onNavigate: (String) -> Unit) {
    val notesVm = appViewModel { NotesViewModel(it.notes) }
    val vaultVm = appViewModel { VaultViewModel(it) }
    val vaultUnlocked by vaultVm.unlocked.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var gridMode by rememberSaveable { mutableStateOf(true) }
    var menuOpen by remember { mutableStateOf(false) }
    var showVaultTypes by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catatan") },
                actions = {
                    if (tab == 0) {
                        IconButton(onClick = { gridMode = !gridMode }) {
                            Icon(
                                if (gridMode) Icons.AutoMirrored.Outlined.ViewList else Icons.Outlined.GridView,
                                contentDescription = "Ubah tampilan",
                            )
                        }
                        Box {
                            IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Menu") }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("Arsip") },
                                    leadingIcon = { Icon(Icons.Outlined.Archive, null) },
                                    onClick = {
                                        menuOpen = false
                                        onNavigate(Routes.noteBin("archive"))
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Sampah") },
                                    leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                                    onClick = {
                                        menuOpen = false
                                        onNavigate(Routes.noteBin("trash"))
                                    },
                                )
                            }
                        }
                    } else if (vaultUnlocked) {
                        TextButton(onClick = vaultVm::lock) {
                            Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Kunci")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (tab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { onNavigate(Routes.noteEdit()) },
                    icon = { Icon(Icons.Filled.Add, null) },
                    text = { Text("Catatan") },
                )
            } else if (vaultUnlocked) {
                ExtendedFloatingActionButton(
                    onClick = { showVaultTypes = true },
                    icon = { Icon(Icons.Filled.Add, null) },
                    text = { Text("Item rahasia") },
                )
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            SegmentedTabs(
                labels = listOf("Catatan", "Brankas"),
                icons = listOf(Icons.AutoMirrored.Outlined.Notes, Icons.Outlined.Lock),
                selectedIndex = tab,
                onSelect = { tab = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (tab == 0) {
                NotesTab(notesVm, gridMode, onOpen = { onNavigate(Routes.noteEdit(it)) })
            } else {
                VaultTab(
                    vm = vaultVm,
                    showTypePicker = showVaultTypes,
                    onDismissTypePicker = { showVaultTypes = false },
                    onNavigate = onNavigate,
                )
            }
        }
    }
}

@Composable
private fun NotesTab(vm: NotesViewModel, gridMode: Boolean, onOpen: (Long) -> Unit) {
    val notes by vm.notes.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val category by vm.category.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        SearchPill(
            query = query,
            onQueryChange = vm::setQuery,
            placeholder = "Cari catatan",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(selected = category == null, onClick = { vm.setCategory(null) }, label = { Text("Semua") })
            NoteCategory.entries.forEach { c ->
                FilterChip(
                    selected = category == c,
                    onClick = { vm.setCategory(if (category == c) null else c) },
                    label = { Text("${c.emoji} ${c.label}") },
                )
            }
        }
        val list = notes ?: return@Column
        if (list.isEmpty()) {
            EmptyState(
                emoji = "📝",
                title = if (query.isBlank() && category == null) "Belum ada catatan" else "Catatan tidak ditemukan",
                message = if (query.isBlank() && category == null) "Ketuk tombol Catatan untuk mulai menulis ide, daftar, atau hal penting." else null,
            )
            return@Column
        }
        val pinned = list.filter { it.isPinned }
        val others = list.filter { !it.isPinned }
        val feedback = LocalFeedback.current
        val actions = NoteActions(
            open = onOpen,
            togglePin = {
                feedback.tick()
                vm.togglePin(it)
            },
            archive = { note ->
                vm.archive(note)
                feedback.undo("Catatan diarsipkan") { vm.unarchive(note) }
            },
            trash = { note ->
                vm.trash(note)
                feedback.undo("Catatan dipindahkan ke sampah") { vm.untrash(note) }
            },
        )
        if (gridMode) {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = Spacing.fabClearance),
                verticalItemSpacing = 10.dp,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (pinned.isNotEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine) { GroupLabel("Disematkan") }
                    items(pinned, key = { it.id }) { NoteCard(it, actions, maxLines = 8, Modifier.animateItem()) }
                    if (others.isNotEmpty()) item(span = StaggeredGridItemSpan.FullLine) { GroupLabel("Lainnya") }
                }
                items(others, key = { it.id }) { NoteCard(it, actions, maxLines = 8, Modifier.animateItem()) }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = Spacing.fabClearance),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (pinned.isNotEmpty()) {
                    item { GroupLabel("Disematkan") }
                    items(pinned, key = { it.id }) { NoteCard(it, actions, maxLines = 3, Modifier.animateItem()) }
                    if (others.isNotEmpty()) item { GroupLabel("Lainnya") }
                }
                items(others, key = { it.id }) { NoteCard(it, actions, maxLines = 3, Modifier.animateItem()) }
            }
        }
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
    )
}

private class NoteActions(
    val open: (Long) -> Unit,
    val togglePin: (NoteEntity) -> Unit,
    val archive: (NoteEntity) -> Unit,
    val trash: (NoteEntity) -> Unit,
)

@Composable
private fun NoteCard(note: NoteEntity, actions: NoteActions, maxLines: Int, modifier: Modifier = Modifier) {
    var menu by remember { mutableStateOf(false) }
    val category = enumOr(note.category, NoteCategory.UMUM)
    val interaction = remember { MutableInteractionSource() }
    Box(modifier.enterStagger()) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = noteContainerColor(note.color)),
            border = if (note.color == 0) BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant) else null,
            modifier = Modifier
                .fillMaxWidth()
                .pressScale(interaction)
                .clip(MaterialTheme.shapes.large)
                .combinedClickable(interactionSource = interaction, indication = ripple(), onClick = { actions.open(note.id) }, onLongClick = { menu = true }),
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        note.title.ifBlank {
                            if (note.isChecklist) "Daftar centang" else note.content.lineSequence().firstOrNull().orEmpty().take(40)
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (note.isPinned) {
                        Icon(Icons.Filled.PushPin, contentDescription = "Disematkan", modifier = Modifier.size(16.dp))
                    }
                }
                if (note.isChecklist) {
                    val items = Checklist.parse(note.content).filter { it.text.isNotBlank() }
                    val shown = items.sortedBy { it.checked }.take(maxLines.coerceAtMost(6))
                    Spacer(Modifier.height(6.dp))
                    shown.forEach { item ->
                        Text(
                            (if (item.checked) "☑ " else "☐ ") + item.text,
                            style = MaterialTheme.typography.bodyMedium,
                            textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                            color = if (item.checked) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (items.size > shown.size) {
                        Text(
                            "+${items.size - shown.size} item lainnya",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else if (note.content.isNotBlank() && note.title.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        note.content,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = maxLines,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val meta = LocalContentColor.current.copy(alpha = 0.65f)
                    Text(
                        "${category.emoji} ${category.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = meta,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                    )
                    Text(Dates.relativeDay(note.updatedAt.toLocalDate()), style = MaterialTheme.typography.labelSmall, color = meta, maxLines = 1)
                }
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text(if (note.isPinned) "Lepas sematan" else "Sematkan") },
                leadingIcon = { Icon(Icons.Outlined.PushPin, null) },
                onClick = {
                    menu = false
                    actions.togglePin(note)
                },
            )
            DropdownMenuItem(
                text = { Text("Arsipkan") },
                leadingIcon = { Icon(Icons.Outlined.Archive, null) },
                onClick = {
                    menu = false
                    actions.archive(note)
                },
            )
            DropdownMenuItem(
                text = { Text("Pindahkan ke sampah") },
                leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                onClick = {
                    menu = false
                    actions.trash(note)
                },
            )
        }
    }
}

@Composable
fun NotesBinScreen(mode: String, onBack: () -> Unit, onOpenNote: (Long) -> Unit) {
    val isTrash = mode == "trash"
    val vm = appViewModel(key = "bin_$mode") { NotesBinViewModel(it.notes, isTrash) }
    val notes by vm.notes.collectAsStateWithLifecycle()
    var confirmEmpty by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<NoteEntity?>(null) }

    Scaffold(
        topBar = {
            BackTopBar(
                title = if (isTrash) "Sampah" else "Arsip",
                onBack = onBack,
                actions = {
                    if (isTrash && !notes.isNullOrEmpty()) {
                        TextButton(onClick = { confirmEmpty = true }) { Text("Kosongkan") }
                    }
                },
            )
        },
    ) { padding ->
        val list = notes ?: return@Scaffold
        if (list.isEmpty()) {
            EmptyState(
                emoji = if (isTrash) "🗑️" else "📦",
                title = if (isTrash) "Sampah kosong" else "Arsip kosong",
                message = if (isTrash) "Catatan yang dihapus akan muncul di sini." else "Catatan yang diarsipkan akan muncul di sini.",
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(padding),
        ) {
            items(list, key = { it.id }) { note ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = noteContainerColor(note.color)),
                    modifier = Modifier.fillMaxWidth().clickable(enabled = !isTrash) { onOpenNote(note.id) },
                ) {
                    Column(Modifier.padding(start = 14.dp, top = 12.dp, end = 4.dp, bottom = 4.dp)) {
                        Text(
                            note.title.ifBlank { "(Tanpa judul)" },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (note.content.isNotBlank()) {
                            Text(note.content, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { vm.restore(note) }) {
                                Icon(if (isTrash) Icons.Outlined.RestoreFromTrash else Icons.Outlined.Unarchive, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (isTrash) "Pulihkan" else "Keluarkan")
                            }
                            if (isTrash) {
                                TextButton(onClick = { confirmDelete = note }) {
                                    Icon(Icons.Outlined.DeleteForever, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Hapus permanen")
                                }
                            } else {
                                TextButton(onClick = { vm.moveToTrash(note) }) { Text("Hapus") }
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmEmpty) {
        ConfirmDialog(
            title = "Kosongkan sampah?",
            message = "Semua catatan di sampah akan dihapus permanen.",
            onConfirm = { vm.emptyTrash() },
            onDismiss = { confirmEmpty = false },
            confirmLabel = "Kosongkan",
        )
    }
    confirmDelete?.let { note ->
        ConfirmDialog(
            title = "Hapus permanen?",
            message = "Catatan ini tidak bisa dipulihkan lagi.",
            onConfirm = { vm.deleteForever(note) },
            onDismiss = { confirmDelete = null },
        )
    }
}

@Composable
fun NoteEditorScreen(noteId: Long, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val vm = appViewModel { NoteEditorViewModel(noteId, it.notes, it.appScope) }
    val feedback = LocalFeedback.current
    val context = LocalContext.current
    var showColors by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val exit = {
        vm.save()
        onBack()
    }
    BackHandler { exit() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.save() }

    val background = if (vm.color == 0) MaterialTheme.colorScheme.surface else noteContainerColor(vm.color)
    val transparentField = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
    )

    Scaffold(
        containerColor = background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = background),
                title = {},
                navigationIcon = {
                    IconButton(onClick = exit) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali") }
                },
                actions = {
                    IconButton(onClick = { vm.pinned = !vm.pinned }) {
                        Icon(
                            if (vm.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (vm.pinned) "Lepas sematan" else "Sematkan",
                        )
                    }
                    IconButton(onClick = { vm.toggleChecklist() }) {
                        Icon(
                            if (vm.isChecklist) Icons.AutoMirrored.Outlined.Notes else Icons.Outlined.Checklist,
                            contentDescription = if (vm.isChecklist) "Ubah ke teks biasa" else "Ubah ke daftar centang",
                        )
                    }
                    IconButton(onClick = { showColors = !showColors }) { Icon(Icons.Outlined.Palette, contentDescription = "Warna") }
                    IconButton(onClick = {
                        val text = vm.shareText
                        if (text.isNotBlank()) {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, vm.title)
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            container.appLock.allowExternalFlow()
                            context.startActivity(Intent.createChooser(send, "Bagikan catatan"))
                        }
                    }) { Icon(Icons.Outlined.Share, contentDescription = "Bagikan") }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Menu") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text(if (vm.archived) "Keluarkan dari arsip" else "Arsipkan") },
                                leadingIcon = { Icon(if (vm.archived) Icons.Outlined.Unarchive else Icons.Outlined.Archive, null) },
                                onClick = {
                                    menu = false
                                    val wasArchived = vm.archived
                                    vm.toggleArchive()
                                    if (!wasArchived) onBack()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Hapus") },
                                leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                                onClick = {
                                    menu = false
                                    confirmDelete = true
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().imePadding()) {
            if (showColors) NoteColorRow(selected = vm.color, onSelect = { vm.color = it })
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NoteCategory.entries.forEach { c ->
                    FilterChip(
                        selected = vm.category == c,
                        onClick = { vm.category = c },
                        label = { Text("${c.emoji} ${c.label}") },
                    )
                }
            }
            TextField(
                value = vm.title,
                onValueChange = { vm.title = it },
                placeholder = { Text("Judul", style = MaterialTheme.typography.headlineSmall) },
                textStyle = MaterialTheme.typography.headlineSmall,
                colors = transparentField,
                modifier = Modifier.fillMaxWidth(),
            )
            AnimatedContent(
                targetState = vm.isChecklist,
                transitionSpec = { fadeIn(tween(Motion.MEDIUM)) togetherWith fadeOut(tween(Motion.SHORT)) },
                label = "noteMode",
                modifier = Modifier.weight(1f),
            ) { checklist ->
                if (checklist) {
                    ChecklistEditor(vm.content, { vm.content = it }, Modifier.fillMaxSize())
                } else {
                    TextField(
                        value = vm.content,
                        onValueChange = { vm.content = it },
                        placeholder = { Text("Tulis catatan…") },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        colors = transparentField,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Text(
                "Diedit ${Dates.dateTime(vm.updatedAt)} · ${vm.summary}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Hapus catatan?",
            message = "Catatan dipindahkan ke sampah dan masih bisa dipulihkan.",
            onConfirm = {
                val saved = vm.hasSavedNote
                vm.moveToTrash()
                onBack()
                if (saved) feedback.undo("Catatan dipindahkan ke sampah") { vm.restoreFromTrash() }
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun NoteColorRow(selected: Int, onSelect: (Int) -> Unit) {
    val colors = AppColors.current.noteColors
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        colors.forEachIndexed { i, c ->
            val fill = if (i == 0) MaterialTheme.colorScheme.surface else c
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(fill)
                    .border(
                        if (i == selected) 2.dp else 1.dp,
                        if (i == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape,
                    )
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                if (i == 0) Icon(Icons.Outlined.FormatColorReset, contentDescription = "Tanpa warna", modifier = Modifier.size(18.dp))
            }
        }
    }
}
