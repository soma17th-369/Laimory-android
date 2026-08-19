package com.soma369.laimory.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

/**
 * Semantic 컬러 — Figma Semantic 컬렉션의 Light/Dark 모드를 M3 [androidx.compose.material3.ColorScheme] 역할명에
 * 1:1로 옮긴 것. `text-primary`=onSurface, `text-secondary`=onSurfaceVariant, `border`=outline 로 매핑된다.
 */
internal val LaimoryLightColorScheme =
    lightColorScheme(
        primary = Primary400,
        onPrimary = Neutral0,
        primaryContainer = Primary100,
        onPrimaryContainer = Primary800,
        secondary = Secondary400,
        onSecondary = Neutral0,
        secondaryContainer = Secondary100,
        onSecondaryContainer = Secondary800,
        background = Neutral50,
        onBackground = Neutral900,
        surface = Neutral0,
        onSurface = Neutral900,
        surfaceVariant = Neutral100,
        onSurfaceVariant = Neutral600,
        surfaceContainer = Neutral50,
        surfaceContainerHigh = Neutral100,
        outline = Neutral300,
        outlineVariant = Neutral200,
        error = Error600,
        onError = Neutral0,
        errorContainer = Error100,
        onErrorContainer = Error800,
        scrim = Neutral900,
        inverseSurface = Neutral800,
        inverseOnSurface = Neutral50,
        // 스낵바 액션처럼 뒤집힌 배경 위에 놓이는 강조색. 비워 두면 M3 기본 보라색이 나온다.
        inversePrimary = Primary300,
    )

internal val LaimoryDarkColorScheme =
    darkColorScheme(
        primary = Primary300,
        onPrimary = Primary900,
        primaryContainer = Primary800,
        onPrimaryContainer = Primary100,
        secondary = Secondary300,
        onSecondary = Secondary900,
        secondaryContainer = Secondary800,
        onSecondaryContainer = Secondary100,
        background = Neutral950,
        onBackground = Neutral100,
        surface = Neutral900,
        onSurface = Neutral50,
        surfaceVariant = Neutral800,
        onSurfaceVariant = Neutral400,
        surfaceContainer = Neutral800,
        surfaceContainerHigh = Neutral700,
        outline = Neutral700,
        outlineVariant = Neutral800,
        error = Error400,
        onError = Error800,
        errorContainer = Error800,
        onErrorContainer = Error100,
        scrim = Neutral950,
        inverseSurface = Neutral100,
        inverseOnSurface = Neutral800,
        // 어두운 테마의 뒤집힌 배경은 밝으므로 더 진한 단계를 쓴다.
        inversePrimary = Primary600,
    )
