package com.soma369.laimory.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * M3 [androidx.compose.material3.ColorScheme] 에 없는 확장색 — success/warning/info 와 감정 5색.
 *
 * Figma code syntax 가 `LaimoryColors.*` 로 잡혀 있는 토큰들이다. [LaimoryTheme] 이 라이트/다크에 맞춰
 * [LocalLaimoryColors] 로 내려주며, 화면에선 [MaterialTheme.laimoryColors] 로 접근한다.
 */
@Immutable
data class LaimoryColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    val joy: Color,
    val joyContainer: Color,
    val onJoyContainer: Color,
    val calm: Color,
    val calmContainer: Color,
    val onCalmContainer: Color,
    val mellow: Color,
    val mellowContainer: Color,
    val onMellowContainer: Color,
    val weary: Color,
    val wearyContainer: Color,
    val onWearyContainer: Color,
    val down: Color,
    val downContainer: Color,
    val onDownContainer: Color,
)

internal val LightExtendedColors =
    LaimoryColors(
        success = Success600,
        onSuccess = Neutral0,
        successContainer = Success100,
        onSuccessContainer = Success800,
        warning = Warning600,
        onWarning = Neutral0,
        warningContainer = Warning100,
        onWarningContainer = Warning800,
        info = Info600,
        onInfo = Neutral0,
        infoContainer = Info100,
        onInfoContainer = Info800,
        joy = Joy400,
        joyContainer = Joy50,
        onJoyContainer = Joy800,
        calm = Calm400,
        calmContainer = Calm50,
        onCalmContainer = Calm800,
        mellow = Mellow400,
        mellowContainer = Mellow50,
        onMellowContainer = Mellow800,
        weary = Weary400,
        wearyContainer = Weary50,
        onWearyContainer = Weary800,
        down = Down400,
        downContainer = Down50,
        onDownContainer = Down800,
    )

internal val DarkExtendedColors =
    LaimoryColors(
        success = Success400,
        onSuccess = Success800,
        successContainer = Success800,
        onSuccessContainer = Success100,
        warning = Warning400,
        onWarning = Warning800,
        warningContainer = Warning800,
        onWarningContainer = Warning100,
        info = Info400,
        onInfo = Info800,
        infoContainer = Info800,
        onInfoContainer = Info100,
        joy = Joy400,
        joyContainer = Joy800,
        onJoyContainer = Joy100,
        calm = Calm400,
        calmContainer = Calm800,
        onCalmContainer = Calm100,
        mellow = Mellow400,
        mellowContainer = Mellow800,
        onMellowContainer = Mellow100,
        weary = Weary400,
        wearyContainer = Weary800,
        onWearyContainer = Weary100,
        down = Down400,
        downContainer = Down800,
        onDownContainer = Down100,
    )

/** [LaimoryTheme] 이 모드에 맞는 [LaimoryColors] 를 제공한다. 기본값은 라이트. */
val LocalLaimoryColors = staticCompositionLocalOf { LightExtendedColors }

/** 화면에서 확장색 접근 단축자 — `MaterialTheme.laimoryColors.joy` 처럼 쓴다. */
val MaterialTheme.laimoryColors: LaimoryColors
    @Composable get() = LocalLaimoryColors.current
