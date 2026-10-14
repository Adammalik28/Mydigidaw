@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.mydigidaw.ui.calendar

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mydigidaw.data.model.Repeat
import com.example.mydigidaw.reminders.Notifications
import com.example.mydigidaw.ui.appViewModel
import com.example.mydigidaw.ui.components.BackTopBar
import com.example.mydigidaw.ui.components.ColorPickerRow
import com.example.mydigidaw.ui.components.ConfirmDialog
import com.example.mydigidaw.ui.components.DatePickerModal
import com.example.mydigidaw.ui.components.DropdownField
import com.example.mydigidaw.ui.components.LocalFeedback
import com.example.mydigidaw.ui.components.TimePickerModal
import com.example.mydigidaw.ui.theme.AppColors
import com.example.mydigidaw.util.Dates
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.launch

private val templates = listOf(
    "🏋️" to "Gym", "🏃" to "Lari pagi", "📚" to "Belajar", "💼" to "Rapat", "🩺" to "Dokter",
    "🎂" to "Ulang tahun", "🛒" to "Belanja", "✈️" to "Perjalanan", "🕌" to "Ibadah", "☕" to "Ketemu teman",
)

private enum class PickTarget { START_DATE, START_TIME, END_DATE, END_TIME }

@Composable
fun EventEditorScreen(eventId: Long, initialDate: LocalDate?, onBack: () -> Unit) {
    val vm = appViewModel { EventEditorViewModel(eventId, initialDate, it) }
    val feedback = LocalFeedback.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val ext = AppColors.current
    var pick by remember { mutableStateOf<PickTarget?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val calendarPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        vm.onPermissionResult(result.values.all { it })
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    val save: () -> Unit = {
        scope.launch {
            when (vm.save()) {
                SaveResult.SAVED -> {
                    feedback.confirm()
                    feedback.message(if (vm.isNew) "Jadwal tersimpan" else "Perubahan jadwal disimpan")
                    onBack()
                }
                SaveResult.SAVED_LOCAL_ONLY -> {
                    Toast.makeText(context, "Tersimpan di aplikasi, tetapi gagal disinkronkan ke Google Calendar", Toast.LENGTH_LONG).show()
                    onBack()
                }
                SaveResult.INVALID -> Unit
            }
        }
    }

    Scaffold(
        topBar = {
            BackTopBar(
                title = if (vm.isNew) "Jadwal baru" else "Edit jadwal",
                onBack = onBack,
                actions = {
                    if (!vm.isNew) {
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, contentDescription = "Hapus") }
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = save,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(16.dp),
                ) { Text("Simpan jadwal") }
            }
        },
    ) { padding ->
        if (!vm.loaded) return@Scaffold
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedTextField(
                value = vm.title,
                onValueChange = {
                    vm.title = it
                    vm.titleError = false
                },
                label = { Text("Judul") },
                placeholder = { Text("mis. Pergi ke gym") },
                isError = vm.titleError,
                supportingText = if (vm.titleError) ({ Text("Judul wajib diisi") }) else null,
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
            )
            if (vm.isNew) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    templates.forEach { (emoji, name) ->
                        AssistChip(onClick = { vm.applyTemplate(emoji, name) }, label = { Text("$emoji $name") })
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Schedule, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("Sepanjang hari", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(checked = vm.allDay, onCheckedChange = { vm.allDay = it })
            }
            DateTimeRow(
                label = "Mulai",
                date = vm.startDate,
                time = vm.startTime,
                allDay = vm.allDay,
                onDate = { pick = PickTarget.START_DATE },
                onTime = { pick = PickTarget.START_TIME },
            )
            DateTimeRow(
                label = "Selesai",
                date = vm.endDate,
                time = vm.endTime,
                allDay = vm.allDay,
                onDate = { pick = PickTarget.END_DATE },
                onTime = { pick = PickTarget.END_TIME },
            )
            DropdownField("Ulangi", Repeat.entries, vm.repeat, { it.label }, { vm.repeat = it })
            DropdownField(
                label = "Pengingat",
                options = reminderOptions,
                selected = reminderOptions.firstOrNull { it.minutes == vm.reminder } ?: reminderOptions.first(),
                optionLabel = { if (vm.allDay) it.allDayLabel else it.label },
                onSelect = {
                    vm.reminder = it.minutes
                    if (it.minutes >= 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Notifications.canPost(context)) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
            )
            OutlinedTextField(
                value = vm.location,
                onValueChange = { vm.location = it },
                label = { Text("Lokasi") },
                leadingIcon = { Icon(Icons.Outlined.Place, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = vm.description,
                onValueChange = { vm.description = it },
                label = { Text("Deskripsi") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Warna", style = MaterialTheme.typography.labelLarge)
            ColorPickerRow(ext.series, vm.color, { vm.color = it })
            GoogleSyncCard(
                vm = vm,
                onConnect = {
                    calendarPermission.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR))
                },
            )
        }
    }

    when (pick) {
        PickTarget.START_DATE -> DatePickerModal(vm.startDate, onDismiss = { pick = null }, onPick = vm::changeStartDate)
        PickTarget.START_TIME -> TimePickerModal(vm.startTime, onDismiss = { pick = null }, onPick = vm::changeStartTime)
        PickTarget.END_DATE -> DatePickerModal(vm.endDate, onDismiss = { pick = null }, onPick = { vm.endDate = it })
        PickTarget.END_TIME -> TimePickerModal(vm.endTime, onDismiss = { pick = null }, onPick = { vm.endTime = it })
        null -> Unit
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = "Hapus jadwal?",
            message = "Jadwal ini akan dihapus" + " (termasuk dari Google Calendar bila tersinkron).",
            onConfirm = {
                scope.launch {
                    vm.delete()
                    onBack()
                }
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun DateTimeRow(
    label: String,
    date: LocalDate,
    time: LocalTime,
    allDay: Boolean,
    onDate: () -> Unit,
    onTime: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(56.dp))
        OutlinedButton(onClick = onDate, modifier = Modifier.weight(1f)) {
            Icon(Icons.Outlined.CalendarToday, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(Dates.weekdayDayMonth(date) + if (date.year != LocalDate.now().year) " ${date.year}" else "", maxLines = 1)
        }
        if (!allDay) {
            OutlinedButton(onClick = onTime) { Text(Dates.time(time)) }
        }
    }
}

@Composable
private fun GoogleSyncCard(vm: EventEditorViewModel, onConnect: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📅", fontSize = 24.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Simpan ke Google Calendar", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        when {
                            !vm.hasPermission -> "Izinkan akses kalender agar jadwal ini juga tersimpan di akun Google Anda."
                            vm.calendars.isEmpty() -> "Tidak ada kalender yang bisa ditulisi. Pastikan akun Google sudah tersinkron di HP."
                            else -> "Jadwal ikut muncul di Google Calendar pada semua perangkat Anda."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (vm.hasPermission && vm.calendars.isNotEmpty()) {
                    Switch(checked = vm.syncGoogle, onCheckedChange = { vm.syncGoogle = it })
                }
            }
            if (!vm.hasPermission) {
                Button(onClick = onConnect) { Text("Hubungkan Google Calendar") }
            } else if (vm.syncGoogle && vm.calendars.isNotEmpty()) {
                DropdownField(
                    label = "Kalender",
                    options = vm.calendars,
                    selected = vm.calendars.firstOrNull { it.id == vm.calendarId },
                    optionLabel = { if (it.account.isNotBlank() && it.account != it.name) "${it.name} · ${it.account}" else it.name },
                    onSelect = { vm.calendarId = it.id },
                )
            }
        }
    }
}
