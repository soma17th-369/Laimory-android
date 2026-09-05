package com.soma369.laimory.update

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.soma369.laimory.core.ui.component.LaimoryDialog
import com.soma369.laimory.core.ui.component.LaimoryDialogButtons

/**
 * 앱 전체를 감싸는 업데이트 게이트.
 *
 * 강제 업데이트는 인증·약관·온보딩보다 앞서고 어떤 [NavSignal] 로도 우회되면 안 된다. 그래서
 * NavGraph 안의 화면이 아니라 **NavGraph 자체를 감싸는 자리**에 둔다.
 */
@Composable
fun AppUpdateGateHost(
    state: AppUpdateGateState,
    onUpdateClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    when (state) {
        AppUpdateGateState.CHECKING -> UpdateCheckingScreen()
        AppUpdateGateState.BLOCKED -> ForcedUpdateScreen(onUpdateClick = onUpdateClick)
        AppUpdateGateState.OPEN -> content()
    }
}

/**
 * 판정 화면.
 *
 * OS 스플래시를 조회 동안 붙잡지 않는다 — 네트워크가 느린 기기에서 앱이 안 뜨는 것처럼 보인다.
 * 대신 앱 안에 중립적인 화면을 두고 여기서 기다린다. 무엇을 기다리는지 적지 않는 것은, 대개
 * 한 프레임 스치고 지나가는 자리라 문구가 오히려 깜빡임으로 읽히기 때문이다.
 */
@Composable
private fun UpdateCheckingScreen() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

/**
 * 강제 업데이트 안내.
 *
 * 권장 안내와 같은 [LaimoryDialog] 를 쓴다 — 사용자가 보기에 같은 성격의 알림이라 모양이 다를
 * 이유가 없다. 다른 것은 닫을 수 있느냐뿐이다.
 *
 * **뒤에 앱을 두지 않는다.** 다이얼로그가 터치를 막더라도 NavGraph 를 그리면 그 뒤에서 화면이
 * 뜨고 조회가 돌아, 쓸 수 없다고 말해 놓고 쓰이는 앱이 된다. 빈 배경만 깔고 그 위에 얹는다.
 *
 * `dismissible = false` 가 바깥 터치와 뒤로 가기를 막고, [BackHandler] 가 다이얼로그가 뜨기
 * 전후의 틈을 막는다.
 */
@Composable
private fun ForcedUpdateScreen(onUpdateClick: () -> Unit) {
    BackHandler(enabled = true) {
        // 뒤로 가기로 빠져나갈 수 없다. 업데이트 전까지 이 앱에서 할 수 있는 일은 없다.
    }
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    )
    LaimoryDialog(
        title = "업데이트가 필요해요",
        body = "지금 버전으로는 라이모리를 이용할 수 없어요.\n스토어에서 최신 버전으로 업데이트해주세요.",
        buttons =
            LaimoryDialogButtons.One(
                label = "업데이트하기",
                onClick = onUpdateClick,
            ),
        onDismissRequest = { },
        dismissible = false,
    )
}
