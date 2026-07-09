package com.soma369.laimory.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * 앱 공통 테마.
 *
 * [darkTheme] 에 따라 M3 ColorScheme 과 [LaimoryColors] 확장색을 함께 교체한다 — 화면 코드는 모드를
 * 의식하지 않고 토큰만 쓰면 자동 대응된다. Typography/Shape 은 후속(#134 등)에서 채운다.
 */
@Composable
fun LaimoryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) LaimoryDarkColorScheme else LaimoryLightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors
    CompositionLocalProvider(LocalLaimoryColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}
