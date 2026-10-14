package com.example.mydigidaw.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.mydigidaw.R
import com.example.mydigidaw.data.model.ThemeMode
import com.example.mydigidaw.security.findActivity

private val LightColors = lightColorScheme(
    primary = Color(0xFF4338CA),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = Color(0xFF5B5F77),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE2E1F5),
    onSecondaryContainer = Color(0xFF181A2C),
    tertiary = Color(0xFF0E7490),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCFFAFE),
    onTertiaryContainer = Color(0xFF083344),
    background = Color(0xFFFAFAFD),
    onBackground = Color(0xFF16161D),
    surface = Color(0xFFFAFAFD),
    onSurface = Color(0xFF16161D),
    surfaceVariant = Color(0xFFE4E3EE),
    onSurfaceVariant = Color(0xFF484759),
    outline = Color(0xFF787789),
    outlineVariant = Color(0xFFD3D2E0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3F2FA),
    surfaceContainer = Color(0xFFEDECF6),
    surfaceContainerHigh = Color(0xFFE7E6F1),
    surfaceContainerHighest = Color(0xFFE1E0EC),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA5B4FC),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFFC5C4DD),
    onSecondary = Color(0xFF2D2E42),
    secondaryContainer = Color(0xFF44455B),
    onSecondaryContainer = Color(0xFFE2E1F5),
    tertiary = Color(0xFF67E8F9),
    onTertiary = Color(0xFF083344),
    tertiaryContainer = Color(0xFF155E75),
    onTertiaryContainer = Color(0xFFCFFAFE),
    background = Color(0xFF0E0E14),
    onBackground = Color(0xFFE5E4EE),
    surface = Color(0xFF0E0E14),
    onSurface = Color(0xFFE5E4EE),
    surfaceVariant = Color(0xFF46455A),
    onSurfaceVariant = Color(0xFFC8C7D6),
    outline = Color(0xFF918FA3),
    outlineVariant = Color(0xFF3A394A),
    surfaceContainerLowest = Color(0xFF09090E),
    surfaceContainerLow = Color(0xFF16161E),
    surfaceContainer = Color(0xFF1B1B24),
    surfaceContainerHigh = Color(0xFF25252F),
    surfaceContainerHighest = Color(0xFF30303A),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** Plus Jakarta Sans — huruf sans modern rancangan Tokotype (Jakarta), lisensi SIL OFL. */
val JakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold),
)

private val AppTypography = Typography().run {
    fun TextStyle.jakarta(weight: FontWeight? = null, tracking: Float? = null) = copy(
        fontFamily = JakartaSans,
        fontWeight = weight ?: fontWeight,
        letterSpacing = tracking?.sp ?: letterSpacing,
    )
    copy(
        displayLarge = displayLarge.jakarta(FontWeight.Bold, -1f),
        displayMedium = displayMedium.jakarta(FontWeight.Bold, -0.8f),
        displaySmall = displaySmall.jakarta(FontWeight.Bold, -0.6f),
        headlineLarge = headlineLarge.jakarta(FontWeight.Bold, -0.5f),
        headlineMedium = headlineMedium.jakarta(FontWeight.Bold, -0.4f),
        headlineSmall = headlineSmall.jakarta(FontWeight.Bold, -0.3f),
        titleLarge = titleLarge.jakarta(FontWeight.Bold, -0.2f),
        titleMedium = titleMedium.jakarta(FontWeight.SemiBold, -0.1f),
        titleSmall = titleSmall.jakarta(FontWeight.SemiBold),
        bodyLarge = bodyLarge.jakarta(),
        bodyMedium = bodyMedium.jakarta(),
        bodySmall = bodySmall.jakarta(),
        labelLarge = labelLarge.jakarta(FontWeight.SemiBold),
        labelMedium = labelMedium.jakarta(FontWeight.Medium),
        labelSmall = labelSmall.jakarta(FontWeight.Medium),
    )
}

/** Gradien merek tetap gelap di kedua mode agar teks putih selalu kontras. */
val BrandGradientLight = listOf(Color(0xFF4338CA), Color(0xFF0E7490))
val BrandGradientDark = listOf(Color(0xFF3730A3), Color(0xFF155E75))

/**
 * Warna tambahan di luar skema Material: palet kategori grafik (urutan tetap, tervalidasi aman
 * untuk buta warna di mode terang & gelap), warna pemasukan/pengeluaran, status, dan warna catatan.
 */
@Immutable
data class ExtendedColors(
    val series: List<Color>,
    val neutralSeries: Color,
    val income: Color,
    val expense: Color,
    val incomeText: Color,
    val expenseText: Color,
    val good: Color,
    val warning: Color,
    val critical: Color,
    val goodText: Color,
    val criticalText: Color,
    val grid: Color,
    val axis: Color,
    val noteColors: List<Color>,
    val brand: List<Color>,
) {
    /** Indeks 0..7 = palet kategori, 8 = abu-abu netral ("Lainnya"). */
    fun seriesColor(index: Int): Color = if (index in series.indices) series[index] else neutralSeries
}

private val LightExtended = ExtendedColors(
    series = listOf(
        Color(0xFF2A78D6), Color(0xFFEB6834), Color(0xFF1BAF7A), Color(0xFFEDA100),
        Color(0xFFE87BA4), Color(0xFF008300), Color(0xFF4A3AA7), Color(0xFFE34948),
    ),
    neutralSeries = Color(0xFFA9A79F),
    income = Color(0xFF2A78D6),
    expense = Color(0xFFE34948),
    incomeText = Color(0xFF1C5CAB),
    expenseText = Color(0xFFBA2B2B),
    good = Color(0xFF0CA30C),
    warning = Color(0xFFFAB219),
    critical = Color(0xFFD03B3B),
    goodText = Color(0xFF006300),
    criticalText = Color(0xFFB3261E),
    grid = Color(0xFFE1E0D9),
    axis = Color(0xFFC3C2B7),
    noteColors = listOf(
        Color.Unspecified, Color(0xFFFAAFA8), Color(0xFFF39F76), Color(0xFFFFF8B8), Color(0xFFE2F6D3),
        Color(0xFFB4DDD3), Color(0xFFD4E4ED), Color(0xFFAECCDC), Color(0xFFD3BFDB), Color(0xFFF6E2DD), Color(0xFFE9E3D4),
    ),
    brand = BrandGradientLight,
)

private val DarkExtended = ExtendedColors(
    series = listOf(
        Color(0xFF3987E5), Color(0xFFD95926), Color(0xFF199E70), Color(0xFFC98500),
        Color(0xFFD55181), Color(0xFF008300), Color(0xFF9085E9), Color(0xFFE66767),
    ),
    neutralSeries = Color(0xFF6B6A64),
    income = Color(0xFF3987E5),
    expense = Color(0xFFE66767),
    incomeText = Color(0xFF86B6EF),
    expenseText = Color(0xFFF28B82),
    good = Color(0xFF0CA30C),
    warning = Color(0xFFFAB219),
    critical = Color(0xFFD03B3B),
    goodText = Color(0xFF3DC23D),
    criticalText = Color(0xFFF28B82),
    grid = Color(0xFF2C2C2A),
    axis = Color(0xFF383835),
    noteColors = listOf(
        Color.Unspecified, Color(0xFF77172E), Color(0xFF692B17), Color(0xFF7C4A03), Color(0xFF264D3B),
        Color(0xFF0C625D), Color(0xFF256377), Color(0xFF284255), Color(0xFF472E5B), Color(0xFF6C394F), Color(0xFF4B443A),
    ),
    brand = BrandGradientDark,
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtended }

object AppColors {
    val current: ExtendedColors
        @Composable get() = LocalExtendedColors.current
}

@Composable
fun MydigidawTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    CompositionLocalProvider(LocalExtendedColors provides if (dark) DarkExtended else LightExtended) {
        MaterialTheme(colorScheme = scheme, shapes = AppShapes, typography = AppTypography, content = content)
    }
}
