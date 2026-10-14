package com.example.mydigidaw.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mydigidaw.ui.LocalAppContainer
import com.example.mydigidaw.ui.components.LocalFeedback
import com.example.mydigidaw.ui.components.Motion
import com.example.mydigidaw.ui.theme.AppColors
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private data class OnboardingPage(val emojis: List<String>, val title: String, val body: String)

private val pages = listOf(
    OnboardingPage(
        listOf("🗂️", "✨", "📱"),
        "Selamat datang di Mydigidaw",
        "Catatan, keuangan, kalender, dan produktivitas — tertata rapi dalam satu aplikasi.",
    ),
    OnboardingPage(
        listOf("📝", "🔐", "☑️"),
        "Catatan & Brankas",
        "Tulis ide dan daftar belanja. Simpan kata sandi, PIN, dan rekening dengan enkripsi AES-256.",
    ),
    OnboardingPage(
        listOf("💰", "📊", "🎯"),
        "Keuangan terkendali",
        "Catat arus kas, pantau anggaran, lihat grafik & wawasan, dan wujudkan target tabungan.",
    ),
    OnboardingPage(
        listOf("📅", "🔥", "⏱️"),
        "Hari yang tertata",
        "Jadwal tersinkron Google Calendar, tugas, kebiasaan, timer fokus, dan jurnal harian.",
    ),
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val container = LocalAppContainer.current
    val feedback = LocalFeedback.current
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState { pages.size }
    var name by remember { mutableStateOf("") }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val brand = AppColors.current.brand
    val last = pager.currentPage == pages.lastIndex

    fun finish() {
        scope.launch {
            container.settings.setUserName(name)
            container.settings.setOnboarded()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        onFinished()
    }

    BackHandler(enabled = pager.currentPage > 0) { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(brand))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.End) {
                if (!last) {
                    TextButton(onClick = { scope.launch { pager.animateScrollToPage(pages.lastIndex) } }) {
                        Text("Lewati", color = Color.White.copy(alpha = 0.85f))
                    }
                } else {
                    Spacer(Modifier.height(48.dp))
                }
            }
            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { index ->
                val offset = { (pager.currentPage - index) + pager.currentPageOffsetFraction }
                PageContent(pages[index], offset)
            }
            AnimatedVisibility(
                visible = last,
                enter = fadeIn(tween(Motion.MEDIUM)) + expandVertically(tween(Motion.MEDIUM)),
                exit = fadeOut(tween(Motion.SHORT)) + shrinkVertically(tween(Motion.SHORT)),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(30) },
                    label = { Text("Nama panggilan Anda") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.8f),
                        cursorColor = Color.White,
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                )
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.Center) {
                pages.indices.forEach { i ->
                    val width by animateDpAsState(if (pager.currentPage == i) 26.dp else 8.dp, tween(Motion.MEDIUM), label = "dot")
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = if (pager.currentPage == i) 1f else 0.4f)),
                    )
                }
            }
            Button(
                onClick = {
                    feedback.tick()
                    if (last) finish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = brand.first()),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 20.dp).height(56.dp),
            ) {
                AnimatedContent(
                    last,
                    transitionSpec = { fadeIn(tween(Motion.SHORT)) togetherWith fadeOut(tween(Motion.SHORT)) },
                    label = "cta",
                ) { isLast ->
                    Text(if (isLast) "Mulai sekarang" else "Lanjut", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun PageContent(page: OnboardingPage, offset: () -> Float) {
    val t = rememberInfiniteTransition(label = "float")
    val bob by t.animateFloat(-6f, 6f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    // Di layar pendek (mis. halaman terakhir yang memuat kolom nama) ilustrasi mengecil dan isi bisa
    // digulir, sehingga teks tidak pernah terpotong.
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val compact = maxHeight < 480.dp
    val art = if (compact) 160.dp else 220.dp
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .heightIn(min = maxHeight)
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(art), contentAlignment = Alignment.Center) {
            Box(Modifier.size(art * 0.89f).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)))
            Box(Modifier.size(art * 0.68f).clip(CircleShape).background(Color.White.copy(alpha = 0.14f)))
            Text(
                page.emojis[0],
                fontSize = if (compact) 56.sp else 76.sp,
                modifier = Modifier.graphicsLayer {
                    translationX = offset() * 160f
                    translationY = bob.dp.toPx()
                },
            )
            Text(
                page.emojis[1],
                fontSize = 40.sp,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 18.dp, end = 10.dp).graphicsLayer {
                    translationX = offset() * 320f
                    translationY = -bob.dp.toPx()
                },
            )
            Text(
                page.emojis[2],
                fontSize = 34.sp,
                modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 22.dp, start = 12.dp).graphicsLayer {
                    translationX = offset() * 240f
                    translationY = bob.dp.toPx() * 0.6f
                },
            )
        }
        Spacer(Modifier.height(if (compact) 16.dp else 28.dp))
        Text(
            page.title,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer {
                translationX = offset() * 60f
                alpha = 1f - offset().absoluteValue.coerceAtMost(1f)
            },
        )
        Spacer(Modifier.height(12.dp))
        Text(
            page.body,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer {
                translationX = offset() * 30f
                alpha = 1f - offset().absoluteValue.coerceAtMost(1f)
            },
        )
    }
    }
}
