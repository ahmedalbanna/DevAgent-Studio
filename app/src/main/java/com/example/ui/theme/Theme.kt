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

private val GeometricColorScheme =
  lightColorScheme(
    primary = GeomPrimary,
    secondary = GeomOnSecondaryContainer,
    secondaryContainer = GeomSecondaryContainer,
    tertiary = GeomGreen,
    background = GeomBg,
    surface = GeomSurfaceWhite,
    surfaceVariant = GeomSurfaceSide,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onSecondaryContainer = GeomOnSecondaryContainer,
    onBackground = GeomText,
    onSurface = GeomText,
    onSurfaceVariant = GeomMutedText,
    outline = GeomBorder,
    outlineVariant = GeomMutedBorder
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = GeometricColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
