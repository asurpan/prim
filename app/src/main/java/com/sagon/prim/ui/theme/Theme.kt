package com.sagon.prim.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldMain,
    secondary = EmeraldVibrant,
    tertiary = GoldSoft,
    background = EmeraldBlack,
    surface = EmeraldSurface,
    onPrimary = EmeraldBlack,
    onSecondary = EmeraldBlack,
    onTertiary = EmeraldBlack,
    onBackground = MintWhite,
    onSurface = MintWhite,
)

@Composable
fun PRIMTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Forzamos tema oscuro para mantener la estética Premium
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
