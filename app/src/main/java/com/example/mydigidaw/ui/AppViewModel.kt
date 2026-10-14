package com.example.mydigidaw.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mydigidaw.AppContainer

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer belum disediakan") }

/** Membuat ViewModel yang terikat ke layar saat ini dengan akses ke [AppContainer]. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    key: String? = null,
    noinline create: (AppContainer) -> VM,
): VM {
    val container = LocalAppContainer.current
    return viewModel(key = key) { create(container) }
}
