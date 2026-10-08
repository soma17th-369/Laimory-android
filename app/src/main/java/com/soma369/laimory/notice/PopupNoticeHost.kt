package com.soma369.laimory.notice

import androidx.compose.runtime.Composable
import com.soma369.laimory.core.domain.model.notice.Notice
import com.soma369.laimory.core.ui.component.LaimoryDialog
import com.soma369.laimory.core.ui.component.LaimoryDialogButtons

/**
 * 앱 시작 팝업 공지. 시안이 따로 없어 공용 Dialog 를 쓴다 — 제목 `공지사항`, 본문은 공지 제목.
 *
 * 공용 Dialog 자리(`GlobalDialogHost`)를 쓰지 않는다. 그쪽은 한 번에 하나만 보여 줘 다른 Dialog 가 뜨면 밀려
 * 사라지고, 계정 경계에서 통째 비워진다. 그래서 권장 업데이트 안내처럼 따로 두고, 띄울지는 [isVisible] 로 받는다.
 *
 * @param isVisible 홈이 보이고 강제 업데이트·권장 업데이트 안내·다른 전역 Dialog 가 없을 때만 참.
 * @param onClose `닫기` · 바깥 누름 · 뒤로가기.
 * @param onOpen `자세히 보기`. 원문을 열고 닫는 것은 호출부가 한다.
 */
@Composable
fun PopupNoticeHost(
    notice: Notice?,
    isVisible: Boolean,
    onClose: (Notice) -> Unit,
    onOpen: (Notice) -> Unit,
) {
    if (notice == null || !isVisible) return

    LaimoryDialog(
        title = "공지사항",
        body = notice.title,
        buttons =
            LaimoryDialogButtons.Two(
                secondaryLabel = "닫기",
                onSecondaryClick = { onClose(notice) },
                primaryLabel = "자세히 보기",
                onPrimaryClick = { onOpen(notice) },
            ),
        onDismissRequest = { onClose(notice) },
    )
}
