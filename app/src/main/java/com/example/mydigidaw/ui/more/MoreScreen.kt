@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.mydigidaw.ui.more

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.mydigidaw.ui.components.AppCard
import com.example.mydigidaw.ui.components.InsetDivider
import com.example.mydigidaw.ui.components.NavRow
import com.example.mydigidaw.ui.components.SectionTitle
import com.example.mydigidaw.ui.components.enterStagger
import com.example.mydigidaw.ui.navigation.Routes
import com.example.mydigidaw.ui.theme.AppColors

private data class Feature(val icon: ImageVector, val colorIndex: Int, val title: String, val subtitle: String, val route: String)

private data class FeatureGroup(val title: String, val items: List<Feature>)

private val groups = listOf(
    FeatureGroup(
        "Produktivitas",
        listOf(
            Feature(Icons.Outlined.TaskAlt, 6, "Tugas", "To-do dengan tenggat & pengingat", Routes.tasks()),
            Feature(Icons.Outlined.LocalFireDepartment, 1, "Kebiasaan", "Lacak rutinitas & jaga streak", Routes.HABITS),
            Feature(Icons.Outlined.Timer, 2, "Fokus", "Timer Pomodoro & statistik", Routes.FOCUS),
            Feature(Icons.Outlined.AutoStories, 4, "Jurnal & Mood", "Catat suasana hati harian", Routes.JOURNAL),
        ),
    ),
    FeatureGroup(
        "Keuangan",
        listOf(
            Feature(Icons.Outlined.AccountBalanceWallet, 0, "Dompet", "Rekening, e-wallet & transfer", Routes.ACCOUNTS),
            Feature(Icons.Outlined.Savings, 2, "Target Tabungan", "Menabung terarah untuk impian", Routes.GOALS),
            Feature(Icons.Outlined.Handshake, 3, "Utang & Piutang", "Catat pinjaman agar tak lupa", Routes.DEBTS),
            Feature(Icons.Outlined.EventRepeat, 6, "Transaksi Berulang", "Gaji, langganan & cicilan", Routes.RECURRING),
            Feature(Icons.AutoMirrored.Outlined.Label, 1, "Kategori", "Atur kategori transaksi", Routes.CATEGORIES),
        ),
    ),
    FeatureGroup(
        "Aplikasi",
        listOf(
            Feature(Icons.Outlined.Search, 0, "Cari", "Temukan apa saja dengan cepat", Routes.SEARCH),
            Feature(Icons.Outlined.Settings, -1, "Pengaturan", "Tema, keamanan & cadangan data", Routes.SETTINGS),
        ),
    ),
)

@Composable
fun MoreScreen(onNavigate: (String) -> Unit) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val ext = AppColors.current
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { TopAppBar(title = { Text("Lainnya") }, scrollBehavior = scrollBehavior) },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            groups.forEachIndexed { g, group ->
                item(key = group.title) {
                    Column(Modifier.enterStagger(g, 70)) {
                        SectionTitle(group.title, Modifier.padding(top = if (g == 0) 0.dp else 4.dp))
                        AppCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            group.items.forEachIndexed { i, f ->
                                if (i > 0) InsetDivider()
                                NavRow(
                                    icon = f.icon,
                                    tint = if (f.colorIndex < 0) MaterialTheme.colorScheme.onSurfaceVariant else ext.seriesColor(f.colorIndex),
                                    title = f.title,
                                    subtitle = f.subtitle,
                                    onClick = { onNavigate(f.route) },
                                )
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    "Mydigidaw 2.0 · Semua data tersimpan aman di perangkat Anda.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp, start = 24.dp, end = 24.dp),
                )
            }
        }
    }
}
