package com.soma369.laimory.core.domain.coordinator

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 홈에 들어올 때 뜨는 두 시트 — 앱이 띄우는 팝업 공지와 홈이 띄우는 튜토리얼 — 의 차례를 정한다.
 *
 * 둘 다 홈 첫 진입에 뜨고 서로 다른 계층(앱 루트 · 홈 화면)이 그려서 그냥 두면 겹친다. **먼저 뜬 쪽이 끝날 때까지 다른 쪽이
 * 기다린다.** 공지는 받는 데 시간이 걸려 튜토리얼이 먼저 뜰 수 있고, 그러면 공지는 튜토리얼을 마친 뒤에 뜬다.
 *
 * 앱을 새로 켤 때 홈 위를 덮는 스플래시가 있으면 둘 다 스플래시가 사라질 때까지 기다린다. 스플래시는 화면 안에 그리고 두 시트는
 * 따로 창을 띄우므로, 그대로 두면 시트가 스플래시 위로 올라온다.
 *
 * 메모리에만 둔다. 프로세스가 새로 뜨면 둘 다 비어 있는 것에서 시작한다.
 */
@Singleton
class HomeOverlayTurn
    @Inject
    constructor() {
        private val _hasPendingNotices = MutableStateFlow(false)

        /** 처리하지 않은 팝업 공지가 있다. 튜토리얼은 이것이 풀릴 때까지 시작하지 않는다. */
        val hasPendingNotices: StateFlow<Boolean> = _hasPendingNotices.asStateFlow()

        private val _isTutorialActive = MutableStateFlow(false)

        /** 홈 튜토리얼이 떠 있다. 팝업 공지는 이것이 풀릴 때까지 띄우지 않는다. */
        val isTutorialActive: StateFlow<Boolean> = _isTutorialActive.asStateFlow()

        private val _isSplashVisible = MutableStateFlow(false)

        /** 앱 시작 스플래시가 화면을 덮고 있다. 공지도 튜토리얼도 이것이 풀릴 때까지 띄우지 않는다. */
        val isSplashVisible: StateFlow<Boolean> = _isSplashVisible.asStateFlow()

        /** 튜토리얼을 시작하면 안 된다 — 처리하지 않은 공지가 있거나 스플래시가 덮고 있다. */
        val blocksTutorial: Flow<Boolean> =
            combine(hasPendingNotices, isSplashVisible) { hasPending, isSplash -> hasPending || isSplash }.distinctUntilChanged()

        fun setPendingNotices(hasPending: Boolean) {
            _hasPendingNotices.value = hasPending
        }

        fun setTutorialActive(isActive: Boolean) {
            _isTutorialActive.value = isActive
        }

        fun setSplashVisible(isVisible: Boolean) {
            _isSplashVisible.value = isVisible
        }
    }
