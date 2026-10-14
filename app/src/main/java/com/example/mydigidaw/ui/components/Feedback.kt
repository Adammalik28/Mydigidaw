package com.example.mydigidaw.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Umpan balik global: snackbar (termasuk "Urungkan") dan getaran halus. Disediakan oleh AppRoot
 * sehingga setiap layar bisa memberi konfirmasi yang konsisten.
 */
class AppFeedback(
    val snackbar: SnackbarHostState,
    private val scope: CoroutineScope,
    private val haptic: HapticFeedback,
) {
    fun message(text: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(text, duration = SnackbarDuration.Short)
        }
    }

    /** Pesan dengan tombol "Urungkan"; [onUndo] dipanggil hanya bila tombol ditekan. */
    fun undo(text: String, onUndo: () -> Unit) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(text, actionLabel = "Urungkan", duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) {
                tick()
                onUndo()
            }
        }
    }

    fun tick() = haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
    fun confirm() = haptic.performHapticFeedback(HapticFeedbackType.Confirm)
    fun reject() = haptic.performHapticFeedback(HapticFeedbackType.Reject)
    fun toggle(on: Boolean) = haptic.performHapticFeedback(if (on) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
    fun longPress() = haptic.performHapticFeedback(HapticFeedbackType.LongPress)
}

val LocalFeedback = staticCompositionLocalOf<AppFeedback> { error("AppFeedback belum disediakan") }

data class SwipeAction(
    val icon: ImageVector,
    val label: String,
    val color: Color,
    val onTrigger: () -> Unit,
)

/**
 * Baris yang bisa digeser: ke kanan untuk [startAction], ke kiri untuk [endAction].
 * Isi baris harus punya latar sendiri ([contentColor]) agar latar aksi hanya terlihat saat digeser.
 */
@Composable
fun SwipeRow(
    modifier: Modifier = Modifier,
    startAction: SwipeAction? = null,
    endAction: SwipeAction? = null,
    contentColor: Color = MaterialTheme.colorScheme.background,
    content: @Composable () -> Unit,
) {
    val state = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    val feedback = LocalFeedback.current
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        enableDismissFromStartToEnd = startAction != null,
        enableDismissFromEndToStart = endAction != null,
        onDismiss = { value ->
            feedback.confirm()
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> startAction?.onTrigger?.invoke()
                SwipeToDismissBoxValue.EndToStart -> endAction?.onTrigger?.invoke()
                SwipeToDismissBoxValue.Settled -> Unit
            }
            scope.launch { state.reset() }
        },
        backgroundContent = {
            val direction = state.dismissDirection
            val action = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> startAction
                SwipeToDismissBoxValue.EndToStart -> endAction
                SwipeToDismissBoxValue.Settled -> null
            }
            val armed = state.targetValue != SwipeToDismissBoxValue.Settled
            val scale by animateFloatAsState(if (armed) 1.2f else 0.9f, spring(dampingRatio = 0.5f), label = "swipeIcon")
            if (action != null) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(action.color.copy(alpha = if (armed) 1f else 0.75f))
                        .padding(horizontal = 24.dp),
                    contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(
                            action.icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            },
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(action.label, color = Color.White, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        },
    ) {
        Box(Modifier.background(contentColor)) { content() }
    }
}
