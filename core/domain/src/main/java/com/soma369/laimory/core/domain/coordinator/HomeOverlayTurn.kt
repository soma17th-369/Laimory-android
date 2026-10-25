package com.soma369.laimory.core.domain.coordinator

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 홈에 들어올 때 뜨는 두 시트 — 앱이 띄우는 팝업 공지와 홈이 띄우는 튜토리얼 — 의 차례를 정한다.
 *
 * 둘 다 홈 첫 진입에 뜨고 서로 다른 계층(앱 루트 · 홈 화면)이 그려서 그냥 두면 겹친다. **먼저 뜬 쪽이 끝날 때까지 다른 쪽이
 * 기다린다.** 공지는 받는 데 시간이 걸려 튜토리얼이 먼저 뜰 수 있고, 그러면 공지는 튜토리얼을 마친 뒤에 뜬다.
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

        fun setPendingNotices(hasPending: Boolean) {
            _hasPendingNotices.value = hasPending
        }

        fun setTutorialActive(isActive: Boolean) {
            _isTutorialActive.value = isActive
        }
    }
