package com.soma369.laimory.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * 앱 공통 테마.
 *
 * [darkTheme] 에 따라 M3 ColorScheme 과 [LaimoryColors] 확장색을 함께 교체한다 — 화면 코드는 모드를
 * 의식하지 않고 토큰만 쓰면 자동 대응된다. 기본 타이포는 Pretendard([LaimoryTypography]), 강조용
 * 시그니처(고운 바탕)는 [MaterialTheme.laimorySignature] 로 명시 사용한다. Shape 은 후속에서 채운다.
 */
@Composable
fun LaimoryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) LaimoryDarkColorScheme else LaimoryLightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    // edge-to-edge 상태에서 시스템 바 아이콘 명암을 테마에 맞춘다.
    // 라이트 테마 = 밝은 배경이므로 어두운 아이콘(isAppearanceLight* = true), 다크는 반대.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalLaimoryColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LaimoryTypography,
            content = content,
        )
    }
}
