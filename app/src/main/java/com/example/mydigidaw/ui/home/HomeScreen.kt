@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalCoroutinesApi::class, FlowPreview::class)

package com.example.mydigidaw.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AddTask
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Circle
import com.example.mydigidaw.ui.components.IconTile
import com.example.mydigidaw.ui.components.InlineEmpty
import com.example.mydigidaw.ui.components.SectionTitle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.mydigidaw.AppContainer
import com.example.mydigidaw.data.db.JournalEntity
import com.example.mydigidaw.data.db.NoteEntity
import com.example.mydigidaw.data.db.TaskEntity
import com.example.mydigidaw.data.model.TxType
import com.example.mydigidaw.data.prefs.AppSettings
import com.example.mydigidaw.data.prefs.FocusState
import com.example.mydigidaw.data.repo.Agenda
import com.example.mydigidaw.data.repo.AgendaSource
import com.example.mydigidaw.data.repo.FinanceRepository
import com.example.mydigidaw.data.repo.Moods
import com.example.mydigidaw.data.repo.TxItem
import com.example.mydigidaw.ui.appViewModel
import com.example.mydigidaw.ui.calendar.AgendaRow
import com.example.mydigidaw.ui.components.AnimatedMoney
import com.example.mydigidaw.ui.components.AppCard
import com.example.mydigidaw.ui.components.AutoSizeText
import com.example.mydigidaw.ui.components.EmojiBadge
import com.example.mydigidaw.ui.components.LocalFeedback
import com.example.mydigidaw.ui.components.Meter
import com.example.mydigidaw.ui.components.Motion
import com.example.mydigidaw.ui.components.enterStagger
import com.example.mydigidaw.ui.components.heroBrush
import com.example.mydigidaw.ui.components.heroDecoration
import com.example.mydigidaw.ui.components.pressScale
import com.example.mydigidaw.ui.finance.BudgetStatus
import com.example.mydigidaw.ui.finance.FinanceBase
import com.example.mydigidaw.ui.finance.FinanceCalc
import com.example.mydigidaw.ui.finance.Insight
import com.example.mydigidaw.ui.finance.InsightTone
import com.example.mydigidaw.ui.finance.TxRow
import com.example.mydigidaw.ui.habits.HabitUi
import com.example.mydigidaw.ui.habits.HabitsCalc
import com.example.mydigidaw.ui.journal.JournalSheet
import com.example.mydigidaw.ui.journal.MoodPicker
import com.example.mydigidaw.ui.journal.moodColor
import com.example.mydigidaw.ui.navigation.Routes
import com.example.mydigidaw.ui.notes.noteContainerColor
import com.example.mydigidaw.ui.tasks.TaskRow
import com.example.mydigidaw.ui.theme.AppColors
import com.example.mydigidaw.util.Dates
import com.example.mydigidaw.util.LocaleID
import com.example.mydigidaw.util.Money
import com.example.mydigidaw.util.endMillis
import com.example.mydigidaw.util.startMillis
import com.example.mydigidaw.util.toLocalDate
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeFinance(
    val total: Long,
    val monthIncome: Long,
    val monthExpense: Long,
    val recent: List<TxItem>,
    val budgetAlerts: List<BudgetStatus>,
    val insights: List<Insight>,
)

data class HomeState(
    val settings: AppSettings,
    val finance: HomeFinance,
    val agenda: List<Agenda>,
    val tasks: List<TaskEntity>,
    val openTaskCount: Int,
    val habits: List<HabitUi>,
    val pinnedNotes: List<NoteEntity>,
    val mood: JournalEntity? = null,
    val focus: FocusState = FocusState(),
)

class HomeViewModel(private val c: AppContainer) : ViewModel() {
    private val refresh = MutableStateFlow(0)

    private val finance = combine(
        c.finance.accounts,
        c.finance.categories,
        c.finance.transactions,
        c.finance.budgets,
        c.finance.goals,
    ) { a, cat, t, b, g ->
        val base = FinanceBase(a, cat, t, b, g)
        val month = YearMonth.now()
        val s = month.startMillis()
        val e = month.plusMonths(1).startMillis()
        val (income, expense) = FinanceCalc.sums(t.filter { it.date >= s && it.date < e })
        HomeFinance(
            total = FinanceRepository.totalBalance(a, t),
            monthIncome = income,
            monthExpense = expense,
            recent = t.take(3).map(base::item),
            budgetAlerts = FinanceCalc.budgetStatuses(base, month).filter { it.ratio >= 0.8f },
            insights = FinanceCalc.insights(base, LocalDate.now()),
        )
    }.flowOn(Dispatchers.Default)

    private val agenda = combine(c.calendar.events, c.settings.settings, refresh) { events, s, _ -> events to s }
        .mapLatest { (events, s) ->
            val today = LocalDate.now()
            c.calendar.agenda(events, s, today.startMillis(), today.endMillis())
        }.flowOn(Dispatchers.Default)

    private val tasks = c.tasks.tasks.map { list ->
        val today = LocalDate.now()
        val open = list.filter { !it.isDone }
        open.filter { it.dueAt != null && !it.dueAt.toLocalDate().isAfter(today) }.sortedBy { it.dueAt } to open.size
    }

    private val habits = combine(c.habits.habits, c.habits.logsSince(LocalDate.now().minusDays(400))) { h, l ->
        HabitsCalc.build(h, l, LocalDate.now()).filter { it.scheduledToday }
    }.flowOn(Dispatchers.Default)

    private val notes = c.notes.all.map { list -> list.filter { it.isPinned && !it.isArchived && !it.isTrashed } }

    val state: StateFlow<HomeState?> = combine(
        combine(c.settings.settings, finance) { s, f -> s to f },
        agenda,
        tasks,
        habits,
        notes,
    ) { (s, f), ag, (t, openCount), h, n ->
        HomeState(s, f, ag, t.take(5), openCount, h, n)
    }.combine(combine(c.journal.observeDay(LocalDate.now()), c.focus.state) { m, fs -> m to fs }) { home, (mood, focus) ->
        home.copy(mood = mood, focus = focus)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch { c.google.changes().debounce(1_000).collect { refresh.update { it + 1 } } }
    }

    fun refresh() = refresh.update { it + 1 }
    fun toggleTask(task: TaskEntity) = viewModelScope.launch { c.tasks.setDone(task.id, !task.isDone) }
    fun toggleHabit(ui: HabitUi) = viewModelScope.launch {
        if (ui.habit.targetPerDay <= 1) c.habits.toggle(ui.habit, LocalDate.now()) else c.habits.increment(ui.habit.id, LocalDate.now())
    }
    fun toggleHideAmounts(current: Boolean) = viewModelScope.launch { c.settings.setHideAmounts(!current) }
    fun saveMood(mood: Int, note: String) = viewModelScope.launch { c.journal.save(LocalDate.now(), mood, note) }
    fun deleteMood() = viewModelScope.launch { c.journal.delete(LocalDate.now()) }
}

@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    val vm = appViewModel { HomeViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose { }
    }
    val s = state
    val name = s?.settings?.userName.orEmpty()
    val feedback = LocalFeedback.current
    var editMood by remember { mutableStateOf(false) }
    if (editMood) {
        JournalSheet(
            day = LocalDate.now(),
            initial = s?.mood,
            onDismiss = { editMood = false },
            onSave = { mood, note -> vm.saveMood(mood, note) },
            onDelete = { vm.deleteMood() },
        )
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Column(Modifier.enterStagger()) {
                        Text(
                            Dates.full(LocalDate.now()).uppercase(LocaleID),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.2.sp,
                        )
                        Text(
                            Dates.greeting() + if (name.isNotBlank()) ", $name" else "",
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigate(Routes.SEARCH) }) {
                        Icon(Icons.Outlined.Search, contentDescription = "Cari")
                    }
                    ProfileAvatar(name, onClick = { onNavigate(Routes.SETTINGS) }, modifier = Modifier.padding(start = 4.dp, end = 12.dp))
                },
            )
        },
    ) { padding ->
        AnimatedVisibility(visible = s != null, enter = fadeIn(tween(Motion.MEDIUM))) {
            if (s == null) return@AnimatedVisibility
            val hidden = s.settings.hideAmounts
            LazyColumn(
                Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp),
            ) {
                item(key = "hero") {
                    HeroCard(
                        finance = s.finance,
                        hidden = hidden,
                        onToggleHidden = { vm.toggleHideAmounts(hidden) },
                        onOpen = { onNavigate(Routes.FINANCE) },
                        modifier = Modifier.enterStagger(0),
                    )
                }
                item(key = "quick") { QuickActions(onNavigate, Modifier.enterStagger(1)) }
                if (s.focus.active) {
                    item(key = "focus") { FocusLiveCard(s.focus, onOpen = { onNavigate(Routes.FOCUS) }, modifier = Modifier.animateItem()) }
                }
                item(key = "mood") {
                    MoodCard(
                        entry = s.mood,
                        onPick = { mood ->
                            feedback.confirm()
                            vm.saveMood(mood, s.mood?.note.orEmpty())
                        },
                        onEdit = { editMood = true },
                        modifier = Modifier.animateItem().enterStagger(2),
                    )
                }
                if (!hidden && s.finance.insights.isNotEmpty()) {
                    item(key = "insightHeader") {
                        HomeSection("Wawasan keuangan", "Statistik", Modifier.enterStagger(2)) { onNavigate(Routes.FINANCE) }
                    }
                    item(key = "insights") { InsightRow(s.finance.insights.take(4), Modifier.enterStagger(3)) }
                }
                if (s.finance.budgetAlerts.isNotEmpty()) {
                    item(key = "budget") {
                        BudgetAlert(s.finance.budgetAlerts, onOpen = { onNavigate(Routes.FINANCE) }, modifier = Modifier.animateItem().enterStagger(2))
                    }
                }

                item(key = "agendaHeader") {
                    HomeSection("Jadwal hari ini", "Kalender", Modifier.enterStagger(2)) { onNavigate(Routes.CALENDAR) }
                }
                item(key = "agendaBody") {
                    SectionCard(Modifier.animateItem().enterStagger(3)) {
                        if (s.agenda.isEmpty()) {
                            InlineEmpty(Icons.Outlined.EventAvailable, "Tidak ada jadwal hari ini", actionLabel = "Tambah", onAction = { onNavigate(Routes.eventEdit(date = LocalDate.now())) })
                        } else {
                            s.agenda.forEach { item ->
                                AgendaRow(item, onClick = {
                                    val localId = item.localId
                                    onNavigate(if (item.source == AgendaSource.LOCAL && localId != null) Routes.eventEdit(localId) else Routes.CALENDAR)
                                })
                            }
                        }
                    }
                }

                item(key = "taskHeader") {
                    HomeSection(
                        if (s.openTaskCount > 0) "Tugas · ${s.openTaskCount} aktif" else "Tugas",
                        "Lihat semua",
                        Modifier.enterStagger(4),
                    ) { onNavigate(Routes.tasks()) }
                }
                item(key = "taskBody") {
                    SectionCard(Modifier.animateItem().enterStagger(5)) {
                        if (s.tasks.isEmpty()) {
                            InlineEmpty(
                                Icons.Outlined.TaskAlt,
                                if (s.openTaskCount > 0) "Tidak ada tugas jatuh tempo hari ini" else "Belum ada tugas",
                                actionLabel = "Tambah",
                                onAction = { onNavigate(Routes.tasks(openNew = true)) },
                            )
                        } else {
                            s.tasks.forEach { task ->
                                TaskRow(
                                    task = task,
                                    overdue = task.dueAt?.toLocalDate()?.isBefore(LocalDate.now()) == true,
                                    onToggle = {
                                        feedback.toggle(!task.isDone)
                                        vm.toggleTask(task)
                                    },
                                    onClick = { onNavigate(Routes.tasks()) },
                                )
                            }
                        }
                    }
                }

                item(key = "habitHeader") {
                    HomeSection("Kebiasaan hari ini", "Kelola", Modifier.enterStagger(6)) { onNavigate(Routes.HABITS) }
                }
                item(key = "habitBody") {
                    if (s.habits.isEmpty()) {
                        SectionCard(Modifier.enterStagger(7)) {
                            InlineEmpty(Icons.Outlined.Repeat, "Lacak kebiasaan seperti ke gym", actionLabel = "Mulai", onAction = { onNavigate(Routes.HABITS) })
                        }
                    } else {
                        HabitStrip(
                            s.habits,
                            onToggle = {
                                feedback.toggle(!it.doneToday)
                                vm.toggleHabit(it)
                            },
                            modifier = Modifier.enterStagger(7),
                        )
                    }
                }

                if (s.pinnedNotes.isNotEmpty()) {
                    item(key = "noteHeader") {
                        HomeSection("Catatan disematkan", "Semua", Modifier.enterStagger(8)) { onNavigate(Routes.NOTES) }
                    }
                    item(key = "noteBody") { PinnedNotes(s.pinnedNotes, onOpen = { onNavigate(Routes.noteEdit(it)) }, modifier = Modifier.enterStagger(9)) }
                }

                item(key = "txHeader") {
                    HomeSection("Transaksi terakhir", "Keuangan", Modifier.enterStagger(9)) { onNavigate(Routes.FINANCE) }
                }
                item(key = "txBody") {
                    SectionCard(Modifier.animateItem().enterStagger(10)) {
                        if (s.finance.recent.isEmpty()) {
                            InlineEmpty(Icons.AutoMirrored.Outlined.ReceiptLong, "Belum ada transaksi", actionLabel = "Catat", onAction = { onNavigate(Routes.txEdit()) })
                        } else {
                            s.finance.recent.forEach { item ->
                                TxRow(item, hidden, onClick = { onNavigate(Routes.txEdit(item.tx.id)) }, showDate = true)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    AppCard(modifier.fillMaxWidth().padding(horizontal = 16.dp).animateContentSize(tween(Motion.MEDIUM))) {
        Column(Modifier.padding(vertical = 4.dp)) { content() }
    }
}

@Composable
private fun HeroCard(finance: HomeFinance, hidden: Boolean, onToggleHidden: () -> Unit, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val white = Color.White
    AppCard(
        onClick = onOpen,
        brush = heroBrush(),
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.heroDecoration().padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Total saldo", style = MaterialTheme.typography.labelLarge, color = white.copy(alpha = 0.8f), modifier = Modifier.weight(1f))
                IconButton(onClick = onToggleHidden, modifier = Modifier.size(34.dp)) {
                    AnimatedContent(hidden, transitionSpec = { (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut()) }, label = "eye") { h ->
                        Icon(
                            if (h) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (h) "Tampilkan nominal" else "Sembunyikan nominal",
                            tint = white,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            AnimatedMoney(
                amount = finance.total,
                style = MaterialTheme.typography.displaySmall,
                color = white,
                hidden = hidden,
                autoSize = true,
                minSize = 20.sp,
            )
            Spacer(Modifier.height(16.dp))
            Text("Arus kas bulan ini", style = MaterialTheme.typography.labelMedium, color = white.copy(alpha = 0.75f))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniFlow(Icons.Filled.SouthWest, "Masuk", finance.monthIncome, hidden, Modifier.weight(1f))
                MiniFlow(Icons.Filled.NorthEast, "Keluar", finance.monthExpense, hidden, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MiniFlow(icon: ImageVector, label: String, value: Long, hidden: Boolean, modifier: Modifier) {
    val white = Color.White
    Row(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(white.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(white.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = white, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = white.copy(alpha = 0.8f), maxLines = 1)
            AnimatedMoney(value, MaterialTheme.typography.titleSmall, white, hidden = hidden, autoSize = true, minSize = 10.sp)
        }
    }
}

@Composable
private fun QuickActions(onNavigate: (String) -> Unit, modifier: Modifier = Modifier) {
    val tint = MaterialTheme.colorScheme.primary
    val actions = listOf(
        QuickAction("Transaksi", Icons.Outlined.Payments, tint, Routes.txEdit(type = TxType.EXPENSE)),
        QuickAction("Catatan", Icons.AutoMirrored.Outlined.NoteAdd, tint, Routes.noteEdit()),
        QuickAction("Jadwal", Icons.Outlined.EditCalendar, tint, Routes.eventEdit(date = LocalDate.now())),
        QuickAction("Tugas", Icons.Outlined.AddTask, tint, Routes.tasks(openNew = true)),
    )
    Row(modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        actions.forEachIndexed { i, a ->
            val interaction = remember { MutableInteractionSource() }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .enterStagger(i + 1, 70)
                    .pressScale(interaction, 0.9f)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(interactionSource = interaction, indication = null) { onNavigate(a.route) }
                    .padding(vertical = 6.dp),
            ) {
                IconTile(a.icon, a.color, size = 52.dp, container = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                Spacer(Modifier.height(6.dp))
                Text(a.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private data class QuickAction(val label: String, val icon: ImageVector, val color: Color, val route: String)

@Composable
private fun HomeSection(title: String, action: String, modifier: Modifier = Modifier, onAction: () -> Unit) {
    SectionTitle(title, modifier, action = action, onAction = onAction)
}

/** Avatar inisial nama di pojok kanan atas — membuka Pengaturan. */
@Composable
private fun ProfileAvatar(name: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .pressScale(interaction, 0.9f)
            .size(40.dp)
            .clip(CircleShape)
            .background(heroBrush())
            .clickable(interactionSource = interaction, indication = ripple(), onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val initial = name.trim().firstOrNull()?.uppercaseChar()
        if (initial != null) {
            Text(initial.toString(), style = MaterialTheme.typography.titleMedium, color = Color.White)
        } else {
            Icon(Icons.Outlined.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Box(
            Modifier.align(Alignment.BottomEnd).size(16.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Settings, contentDescription = "Pengaturan", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
private fun BudgetAlert(alerts: List<BudgetStatus>, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val ext = AppColors.current
    AppCard(
        onClick = onOpen,
        containerColor = ext.warning.copy(alpha = 0.12f),
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = ext.warning, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Perhatikan anggaran", style = MaterialTheme.typography.titleSmall)
            }
            alerts.take(2).forEach { st ->
                Spacer(Modifier.height(10.dp))
                Row {
                    Text("${st.emoji} ${st.name}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1)
                    Text(FinanceCalc.percent(st.ratio), style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.height(6.dp))
                Meter(st.ratio, if (st.ratio > 1f) ext.critical else ext.warning, height = 6.dp)
            }
        }
    }
}

@Composable
private fun HabitStrip(habits: List<HabitUi>, onToggle: (HabitUi) -> Unit, modifier: Modifier = Modifier) {
    if (habits.size <= 2) {
        Row(modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            habits.forEach { HabitChip(it, onToggle, Modifier.weight(1f)) }
        }
        return
    }
    LazyRow(modifier, contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(habits, key = { it.habit.id }) { ui -> HabitChip(ui, onToggle, Modifier.width(184.dp).animateItem()) }
    }
}

@Composable
private fun HabitChip(ui: HabitUi, onToggle: (HabitUi) -> Unit, modifier: Modifier) {
    val color = AppColors.current.seriesColor(ui.habit.color)
    val bg by animateColorAsState(
        if (ui.doneToday) color.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerLow,
        tween(Motion.MEDIUM),
        label = "habitBg",
    )
    AppCard(
        onClick = { onToggle(ui) },
        containerColor = bg,
        border = BorderStroke(1.dp, if (ui.doneToday) color.copy(alpha = 0.6f) else Color.Transparent),
        modifier = modifier,
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            EmojiBadge(ui.habit.emoji, color, size = 36.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(ui.habit.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    when {
                        ui.habit.targetPerDay > 1 -> "${ui.todayCount}/${ui.habit.targetPerDay} hari ini"
                        ui.doneToday -> "Selesai"
                        ui.streak > 0 -> "🔥 ${ui.streak} hari"
                        else -> "Ketuk untuk tandai"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            AnimatedContent(
                ui.doneToday,
                transitionSpec = { (scaleIn(spring(dampingRatio = 0.4f)) + fadeIn()) togetherWith fadeOut() },
                label = "habitDone",
            ) { done ->
                Icon(
                    if (done) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = if (done) "Selesai" else "Belum",
                    tint = if (done) color else MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun PinnedNotes(notes: List<NoteEntity>, onOpen: (Long) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(modifier, contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(notes, key = { it.id }) { note ->
            AppCard(
                onClick = { onOpen(note.id) },
                containerColor = noteContainerColor(note.color),
                modifier = Modifier.width(180.dp).height(120.dp).animateItem(),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        note.title.ifBlank { "(Tanpa judul)" },
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(note.content, style = MaterialTheme.typography.bodySmall, maxLines = 4, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun MoodCard(entry: JournalEntity?, onPick: (Int) -> Unit, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        AnimatedContent(
            targetState = entry,
            contentKey = { it == null },
            transitionSpec = { (fadeIn(tween(Motion.MEDIUM)) + scaleIn(initialScale = 0.95f)) togetherWith fadeOut(tween(Motion.SHORT)) },
            label = "moodCard",
        ) { current ->
            if (current == null) {
                Column(Modifier.padding(vertical = 16.dp)) {
                    Text(
                        "Bagaimana harimu?",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 18.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    MoodPicker(selected = null, onPick = onPick, size = 44)
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().clickable(onClick = onEdit).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).background(moodColor(current.mood).copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center,
                    ) { Text(Moods.emojiOf(current.mood), fontSize = 26.sp) }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Hari ini terasa ${Moods.labelOf(current.mood).lowercase()}", style = MaterialTheme.typography.titleSmall)
                        Text(
                            current.note.ifBlank { "Ketuk untuk menulis jurnal singkat" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun FocusLiveCard(state: FocusState, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.endAt, state.remainingMs) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val remaining = if (state.running) (state.endAt - now).coerceAtLeast(0) else state.remainingMs
    val total = state.plannedMinutes * 60_000L
    val progress = if (total > 0) 1f - remaining.toFloat() / total else 0f
    val secs = (remaining + 999) / 1000
    AppCard(
        onClick = onOpen,
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (state.isBreak) "☕" else "🎯", fontSize = 22.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (state.isBreak) "Istirahat" else state.label.ifBlank { "Sesi fokus" },
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Text(
                        if (state.paused) "Dijeda" else "Sedang berjalan",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                    )
                }
                Text(
                    "%02d:%02d".format(secs / 60, secs % 60),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            Spacer(Modifier.height(10.dp))
            Meter(progress, MaterialTheme.colorScheme.tertiary, height = 6.dp)
        }
    }
}

@Composable
fun InsightRow(insights: List<Insight>, modifier: Modifier = Modifier) {
    val ext = AppColors.current
    LazyRow(modifier, contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(insights, key = { it.title }) { insight ->
            val (tint, label) = when (insight.tone) {
                InsightTone.WARN -> ext.warning to "Perhatian"
                InsightTone.GOOD -> ext.good to "Bagus"
                InsightTone.NEUTRAL -> MaterialTheme.colorScheme.primary to "Info"
            }
            AppCard(modifier = Modifier.width(250.dp).height(150.dp).animateItem()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(insight.emoji, fontSize = 22.sp)
                        Spacer(Modifier.weight(1f))
                        Box(Modifier.size(8.dp).clip(CircleShape).background(tint))
                        Spacer(Modifier.width(6.dp))
                        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(insight.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        insight.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
