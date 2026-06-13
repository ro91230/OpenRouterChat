package com.personal.chat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ColorScheme = darkColorScheme(
    primary = CyberAccent,
    background = ObsidianDark,
    surface = SlateGray,
    error = CoralAlert,
    onPrimary = ObsidianDark,
    onBackground = SoftSilver,
    onSurface = SoftSilver
)

@Composable
fun PersonalChatTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        content = content
    )
}
