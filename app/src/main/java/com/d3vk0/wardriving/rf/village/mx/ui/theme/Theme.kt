package com.d3vk0.wardriving.rf.village.mx.ui.theme

import android.graphics.Color.parseColor
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.d3vk0.wardriving.rf.village.mx.BuildConfig

@Composable
fun WardrivingTheme(
    content: @Composable () -> Unit,
) {
    val lima = runCatching { Color(parseColor(BuildConfig.APP_ACCENT_COLOR)) }.getOrDefault(Brand.lima)
    MaterialTheme(colorScheme = darkScheme(lima), content = content)
}

private fun darkScheme(lima: Color): ColorScheme = darkColorScheme(
    primary = lima,
    onPrimary = Brand.tinta,
    secondary = Brand.villaVioleta,
    onSecondary = Brand.crema,
    tertiary = Brand.limaBorde,
    background = Brand.campo,
    onBackground = Brand.crema,
    surface = Brand.campo,
    onSurface = Brand.crema,
    surfaceVariant = Brand.tarjeta,
    onSurfaceVariant = Brand.crema,
)
