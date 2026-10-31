package co.luka.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Suppress("UNUSED_PARAMETER")
@Composable
fun LukaTheme(mode: String, content: @Composable () -> Unit) {
    // Claro fijo: se ignora el modo del sistema y el ajuste dark/light.
    val dark = false
    val context = LocalContext.current
    val gms = remember {
        try {
            context.packageManager.getPackageInfo("com.google.android.gms", 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        } catch (_: Exception) {
            false
        }
    }
    val family = remember(gms) { lukaFontFamily(gms) }
    val scheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            val bar = scheme.background.toArgb()
            window.statusBarColor = bar
            window.navigationBarColor = bar
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = lukaTypography(family),
        shapes = LukaShapes,
    ) {
        CompositionLocalProvider(
            LocalLukaColors provides lukaColors(dark, family),
            content = content,
        )
    }
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
