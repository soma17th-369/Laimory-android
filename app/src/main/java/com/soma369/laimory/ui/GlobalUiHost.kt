package com.soma369.laimory.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soma369.laimory.core.data.helper.MessageHelperImpl
import com.soma369.laimory.core.domain.helper.GlobalLoadingHelper

/**
 * Root(MainActivity)가 헬퍼 상태를 직접 소비해 전역 로딩 오버레이와 공통 Dialog를 렌더링한다.
 *
 * NavGraph를 거치지 않으므로 내비게이션은 전역 메시지 UI의 존재를 모른다.
 * 화면 콘텐츠 위에 겹치도록 Root의 Box 안에서 콘텐츠 다음에 배치한다.
 */
@Composable
internal fun GlobalUiHost(
    messageHelper: MessageHelperImpl,
    loadingHelper: GlobalLoadingHelper,
) {
    val activeDialog by messageHelper.activeDialog.collectAsStateWithLifecycle()
    val loadingKeys by loadingHelper.activeKeys.collectAsStateWithLifecycle()

    GlobalLoadingOverlay(isVisible = loadingKeys.isNotEmpty())
    GlobalDialogHost(activeDialog = activeDialog, onResult = messageHelper::resolveDialog)
}
