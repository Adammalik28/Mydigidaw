package com.example.mydigidaw

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.animation.AnticipateInterpolator
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mydigidaw.data.model.ThemeMode
import com.example.mydigidaw.security.SecureWindow
import com.example.mydigidaw.ui.AppRoot
import com.example.mydigidaw.ui.LocalAppContainer
import com.example.mydigidaw.ui.navigation.DeepLinks
import com.example.mydigidaw.ui.theme.MydigidawTheme

/** FragmentActivity diperlukan oleh BiometricPrompt. */
class MainActivity : FragmentActivity() {
    private val pendingRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as MydigidawApp).container
        splash.setKeepOnScreenCondition { !container.appLock.ready.value }
        splash.setOnExitAnimationListener { provider ->
            provider.iconView?.animate()
                ?.scaleX(1.25f)?.scaleY(1.25f)?.alpha(0f)
                ?.setDuration(320L)
                ?.setInterpolator(AnticipateInterpolator())
                ?.start()
            provider.view.animate()
                .alpha(0f)
                .setStartDelay(120L)
                .setDuration(300L)
                .withEndAction { provider.remove() }
                .start()
        }
        if (savedInstanceState == null) pendingRoute.value = DeepLinks.routeFor(intent)

        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            val ready by container.appLock.ready.collectAsStateWithLifecycle()
            val current = settings

            LaunchedEffect(current?.secureScreen) {
                SecureWindow.setGlobal(this@MainActivity, current?.secureScreen == true)
            }
            LaunchedEffect(current?.appLock) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    runCatching { setRecentsScreenshotEnabled(current?.appLock != true) }
                }
            }

            MydigidawTheme(
                themeMode = current?.themeMode ?: ThemeMode.SYSTEM,
                dynamicColor = current?.dynamicColor ?: false,
            ) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (current != null && ready) {
                        CompositionLocalProvider(LocalAppContainer provides container) {
                            AppRoot(
                                settings = current,
                                pendingRoute = pendingRoute.value,
                                onRouteConsumed = { pendingRoute.value = null },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        DeepLinks.routeFor(intent)?.let { pendingRoute.value = it }
    }
}
