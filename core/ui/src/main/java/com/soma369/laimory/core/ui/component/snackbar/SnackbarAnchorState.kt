package com.soma369.laimory.core.ui.component.snackbar

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 스낵바를 화면 하단의 고정 요소 위로 띄우기 위한 높이.
 *
 * 스낵바 호스트는 루트 `Scaffold` 하나라 바텀바 바로 위에 뜨고, 화면마다 하단에 무엇이 있는지 모른다.
 * 홈의 CTA 처럼 그 자리에 고정된 버튼이 있으면 스낵바가 버튼을 가리고, 방금 누른 손가락 위에 뜬다.
 * 그런 화면이 [bottomInset] 을 알리고 떠날 때 0 으로 되돌린다.
 */
@Stable
class SnackbarAnchorState {
    /** 바텀바 위에서부터 비워 둘 높이. */
    var bottomInset: Dp by mutableStateOf(0.dp)
}

/** 제공하지 않은 곳(미리보기 등)에서는 아무도 읽지 않는 빈 상태를 준다. */
val LocalSnackbarAnchor = staticCompositionLocalOf { SnackbarAnchorState() }
