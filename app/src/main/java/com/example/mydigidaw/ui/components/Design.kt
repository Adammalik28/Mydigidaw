package com.example.mydigidaw.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.offset

/** Jarak standar agar semua layar punya ritme yang sama. */
object Spacing {
    /** Tepi kiri/kanan konten layar. */
    val screen = 16.dp
    /** Jarak antarbagian. */
    val section = 20.dp
    /** Ruang kosong di bawah daftar agar tidak tertutup tombol tambah (FAB). */
    val fabClearance = 104.dp
}

/** Ikon Material di dalam kotak bersudut lembut berwarna lembut. */
@Composable
fun IconTile(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    container: Color = tint.copy(alpha = 0.13f),
) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(size * 0.32f)).background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.52f))
    }
}

/**
 * Judul bagian yang seragam di seluruh aplikasi: teks tebal di kiri, tautan kecil di kanan.
 * Dipakai di Beranda, Keuangan, dan layar lain agar hierarki terlihat rapi.
 */
@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = Spacing.screen + 4.dp, end = Spacing.screen - 4.dp, top = Spacing.section, bottom = 8.dp)
            .height(28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
        if (action != null && onAction != null) {
            Row(
                Modifier
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onAction)
                    .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(action, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** Keadaan kosong ringkas di dalam kartu: ikon, satu kalimat, dan tombol opsional. */
@Composable
fun InlineEmpty(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon, MaterialTheme.colorScheme.onSurfaceVariant, size = 36.dp, container = MaterialTheme.colorScheme.surfaceContainerHigh)
        Spacer(Modifier.width(12.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(
                onClick = onAction,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp),
                modifier = Modifier.height(34.dp),
            ) { Text(actionLabel, style = MaterialTheme.typography.labelMedium) }
        }
    }
}

/** Baris menu bergaya daftar pengaturan: ikon bertile, judul, keterangan, panah. */
@Composable
fun NavRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon, tint, size = 40.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Garis pemisah tipis di dalam kartu, sejajar dengan teks (bukan ikon). */
@Composable
fun InsetDivider(start: Dp = 70.dp) {
    HorizontalDivider(
        Modifier.padding(start = start, end = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

data class NavBarItem(val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

/**
 * Bilah navigasi bawah yang ramping (64dp) dengan indikator pil beranimasi —
 * lebih hemat ruang daripada NavigationBar bawaan (80dp) di layar kecil.
 */
@Composable
fun AppNavBar(items: List<NavBarItem>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth().background(scheme.surface)) {
        HorizontalDivider(thickness = 0.5.dp, color = scheme.outlineVariant)
        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(64.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                val pillWidth by animateDpAsState(if (selected) 56.dp else 32.dp, spring(dampingRatio = 0.7f, stiffness = 500f), label = "pill")
                val pillColor by animateColorAsState(if (selected) scheme.primaryContainer else Color.Transparent, tween(Motion.SHORT), label = "pillColor")
                val tint by animateColorAsState(if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant, tween(Motion.SHORT), label = "navTint")
                val labelColor by animateColorAsState(if (selected) scheme.onSurface else scheme.onSurfaceVariant, tween(Motion.SHORT), label = "navLabel")
                val interaction = remember { MutableInteractionSource() }
                Column(
                    Modifier
                        .weight(1f)
                        .selectable(selected = selected, interactionSource = interaction, indication = null, role = Role.Tab) { onSelect(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .pressScale(interaction, 0.9f)
                            .width(pillWidth)
                            .height(30.dp)
                            .clip(CircleShape)
                            .background(pillColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(if (selected) item.selectedIcon else item.icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        item.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** Kolom pencarian berbentuk pil yang selalu terlihat (gaya aplikasi catatan modern). */
@Composable
fun SearchPill(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    androidx.compose.foundation.text.BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(scheme.primary),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(scheme.surfaceContainerHigh)
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant, maxLines = 1)
                    }
                    inner()
                }
                if (query.isNotEmpty()) {
                    androidx.compose.material3.IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Hapus pencarian", tint = scheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                } else {
                    Spacer(Modifier.width(12.dp))
                }
            }
        },
    )
}

/**
 * Tab bergaya segmented control: wadah pil dengan indikator yang meluncur halus.
 * Lebih rapi daripada TabRow saat label panjang di layar sempit.
 */
@Composable
fun SegmentedTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    icons: List<ImageVector>? = null,
) {
    val scheme = MaterialTheme.colorScheme
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(CircleShape)
            .background(scheme.surfaceContainerHigh)
            .padding(4.dp),
    ) {
        val segment = maxWidth / labels.size
        val offset by animateDpAsState(segment * selectedIndex, spring(dampingRatio = 0.8f, stiffness = 600f), label = "segOffset")
        Box(
            Modifier
                // offset, bukan padding: pegas sedikit melewati 0 saat kembali ke tab pertama, dan
                // padding negatif membuat Compose melempar IllegalArgumentException (force close).
                .offset { IntOffset(offset.roundToPx(), 0) }
                .width(segment)
                .fillMaxHeight()
                .shadow(1.dp, CircleShape)
                .clip(CircleShape)
                .background(if (scheme.surface.luminance() < 0.5f) scheme.secondaryContainer else scheme.surfaceContainerLowest),
        )
        Row(Modifier.fillMaxSize()) {
            labels.forEachIndexed { i, label ->
                val selected = i == selectedIndex
                val color by animateColorAsState(if (selected) scheme.onSurface else scheme.onSurfaceVariant, tween(Motion.SHORT), label = "segText")
                Row(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(i) }),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    icons?.getOrNull(i)?.let {
                        Icon(it, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        label,
                        style = if (labels.size > 3) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                        color = color,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
