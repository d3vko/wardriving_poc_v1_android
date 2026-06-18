package com.d3vk0.wardriving.rf.village.mx.ui.theme

import android.graphics.Color.parseColor
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.d3vk0.wardriving.rf.village.mx.BuildConfig

@Composable
fun WardrivingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val accent = runCatching { Color(parseColor(BuildConfig.APP_ACCENT_COLOR)) }.getOrDefault(Color(0xFF00A676))
    val scheme = if (darkTheme) darkScheme(accent) else lightScheme(accent)
    MaterialTheme(colorScheme = scheme, content = content)
}

private fun lightScheme(accent: Color): ColorScheme = lightColorScheme(
    primary = accent,
    secondary = Color(0xFF006D77),
    tertiary = Color(0xFF264653),
)

private fun darkScheme(accent: Color): ColorScheme = darkColorScheme(
    primary = accent,
    secondary = Color(0xFF83C5BE),
    tertiary = Color(0xFFA8DADC),
)
