package com.soma369.laimory.core.ui.component.snackbar

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals

/**
 * 시간이 다 되면 스스로 닫히는 스낵바. [LaimorySnackbarHost] 가 남은 시간을 위쪽 막대로 그린다.
 *
 * Material3 는 action 이 있는 스낵바의 기본 지속 시간을 `Indefinite` 로 둔다. action 을 달고 시간을
 * 넘기지 않으면 누르기 전까지 남고, 그동안 뒤이은 스낵바가 전부 줄을 서서 막힌다.
 *
 * `Short`·`Long` 을 쓰지 않는 이유는 둘이 4초·10초로 고정이고, 막대가 끝나는 순간과 닫히는 순간을
 * 한 곳에서 맞춰야 해서다. 그래서 Material3 의 타이머는 끄고(`Indefinite`) 호스트가 막대를 다 줄인 뒤
 * 직접 닫는다.
 *
 * @param durationMillis 기본 표시 시간. 접근성 설정이 권하는 만큼 호스트가 늘린다.
 */
class TimedSnackbarVisuals(
    override val message: String,
    override val actionLabel: String?,
    val durationMillis: Long,
) : SnackbarVisuals {
    override val withDismissAction: Boolean = false
    override val duration: SnackbarDuration = SnackbarDuration.Indefinite
}
