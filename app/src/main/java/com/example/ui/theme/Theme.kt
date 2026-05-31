package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = SecurityBlue,
    secondary = SecurityBlueLight,
    tertiary = Slate400,
    background = SoftDarkCarbon,
    surface = Color(0xFF262930),
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
  )

private val LightColorScheme =
  lightColorScheme(
    primary = SecurityBlue,
    onPrimary = Color.White,
    primaryContainer = SecurityBlueLight,
    onPrimaryContainer = SecurityBlueText,
    secondary = Slate600,
    onSecondary = Color.White,
    background = NaturalBg,
    onBackground = NaturalText,
    surface = Color.White,
    onSurface = NaturalText,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    outline = BorderSlate
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
