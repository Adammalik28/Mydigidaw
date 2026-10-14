package com.example.mydigidaw.security

import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.PersistableBundle
import android.view.WindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/** FLAG_SECURE berhitung-referensi: aktif selama ada layar sensitif atau pengaturan global menyala. */
object SecureWindow {
    private var holders = 0
    private var global = false

    fun acquire(activity: Activity?) {
        holders++
        apply(activity)
    }

    fun release(activity: Activity?) {
        holders = (holders - 1).coerceAtLeast(0)
        apply(activity)
    }

    fun setGlobal(activity: Activity?, enabled: Boolean) {
        global = enabled
        apply(activity)
    }

    private fun apply(activity: Activity?) {
        val window = activity?.window ?: return
        if (holders > 0 || global) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}

object SecureClipboard {
    private const val CLEAR_AFTER_MS = 45_000L

    fun copy(context: Context, label: String, text: String, sensitive: Boolean, scope: CoroutineScope) {
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
        val clip = ClipData.newPlainText(label, text)
        if (sensitive) {
            clip.description.extras = PersistableBundle().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                } else {
                    putBoolean("android.content.extra.IS_SENSITIVE", true)
                }
            }
        }
        clipboard.setPrimaryClip(clip)
        if (!sensitive) return
        scope.launch {
            delay(CLEAR_AFTER_MS)
            val current = runCatching { clipboard.primaryClip?.getItemAt(0)?.text?.toString() }.getOrNull()
            if (current == text) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) clipboard.clearPrimaryClip()
                else clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        }
    }
}
