package com.soma369.laimory.update

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.soma369.laimory.core.ui.theme.Spacing

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
 * 강제 업데이트 화면.
 *
 * 닫을 수 없다. 시스템 back 을 [BackHandler] 로 막고, 스토어로 가는 길만 둔다.
 */
@Composable
private fun ForcedUpdateScreen(onUpdateClick: () -> Unit) {
    BackHandler(enabled = true) {
        // 뒤로 가기로 빠져나갈 수 없다. 이 화면은 업데이트 전까지 유일한 화면이다.
    }
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = Spacing.extraLarge2),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "업데이트가 필요해요",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "지금 버전으로는 라이모리를 이용할 수 없어요.\n스토어에서 최신 버전으로 업데이트해주세요.",
            modifier = Modifier.padding(top = Spacing.medium),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onUpdateClick,
            modifier =
                Modifier
                    .padding(top = Spacing.extraLarge3)
                    .fillMaxWidth(),
        ) {
            Text("업데이트하기")
        }
    }
}
