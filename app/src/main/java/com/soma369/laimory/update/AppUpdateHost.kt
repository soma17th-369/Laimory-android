package com.soma369.laimory.update

import androidx.compose.runtime.Composable
import com.soma369.laimory.core.ui.component.LaimoryDialog
import com.soma369.laimory.core.ui.component.LaimoryDialogButtons

/**
 * 권장 업데이트 안내.
 *
 * **[MessageHelper] 요청으로 보내지 않는다.** 그쪽은 활성 요청이 하나뿐이라 새 요청을 즉시
 * 물리고, 인증 루트가 교체되면 요청을 취소한다. 앱 정책 상태인 이 안내를 그 채널에 태우면 조용히
 * 유실되거나 루트 전환에 함께 사라진다. 시각 컴포넌트만 [LaimoryDialog] 를 재사용한다.
 *
 * 전역 Dialog 가 떠 있으면 기다린다. 한 화면에 modal 두 개가 겹치지 않아야 하고, 기다린 것은
 * 사용자의 `나중에` 가 아니므로 **보류로 기록하지 않는다** — 그 Dialog 가 닫히면 그대로 뜬다.
 *
 * @param version 안내할 권장 버전. `null` 이면 안내할 것이 없다.
 * @param isGlobalDialogVisible 전역 Dialog 표시 여부.
 */
@Composable
fun AppUpdateHost(
    version: Int?,
    isGlobalDialogVisible: Boolean,
    onLater: (Int) -> Unit,
    onUpdate: (Int) -> Unit,
) {
    if (version == null || isGlobalDialogVisible) return

    LaimoryDialog(
        title = "새 버전이 나왔어요",
        body = "최신 버전으로 업데이트하고 더 나아진 라이모리를 만나보세요.",
        buttons =
            LaimoryDialogButtons.Two(
                secondaryLabel = "나중에",
                onSecondaryClick = { onLater(version) },
                primaryLabel = "업데이트",
                onPrimaryClick = { onUpdate(version) },
            ),
        // 바깥을 누르거나 뒤로 가는 것도 미루는 것으로 본다. 강제와 달리 지금 처리하지 않아도
        // 되는 안내라, 닫는 방법마다 결과가 달라지면 오히려 다시 뜨는 규칙을 알 수 없다.
        onDismissRequest = { onLater(version) },
    )
}
