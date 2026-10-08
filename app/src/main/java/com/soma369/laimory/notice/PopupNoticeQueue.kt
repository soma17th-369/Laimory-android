package com.soma369.laimory.notice

import com.soma369.laimory.core.domain.model.notice.Notice
import com.soma369.laimory.core.domain.usecase.notice.GetPopupNoticesUseCase
import com.soma369.laimory.core.domain.usecase.notice.MarkPopupNoticeSeenUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 앱 시작 팝업 공지의 차례. 프로세스마다 한 번 받아, 최신 순으로 하나씩 내놓는다.
 *
 * 차례는 메모리에만 둔다 — 다음 콜드 스타트에 다시 받는다. 기기에 남는 것은 "이미 띄웠다"는 기록뿐이다.
 */
@Singleton
class PopupNoticeQueue
    @Inject
    constructor(
        private val getPopupNotices: GetPopupNoticesUseCase,
        private val markPopupNoticeSeen: MarkPopupNoticeSeenUseCase,
    ) {
        private val _current = MutableStateFlow<Notice?>(null)

        /** 지금 띄울 팝업. 없으면 `null`. */
        val current: StateFlow<Notice?> = _current.asStateFlow()

        private val pending = ArrayDeque<Notice>()
        private val mutex = Mutex()
        private var isLoaded = false

        /**
         * 로그인 상태가 된 뒤 한 번 받는다(앱 초기화 조회가 인증 API 다). 같은 프로세스에서 다시 불러도 받지 않는다 —
         * 계정을 바꿔도 띄운 기록은 기기 단위라 다시 받을 것이 없다.
         */
        suspend fun loadOnce() {
            mutex.withLock {
                if (isLoaded) return
                isLoaded = true
                val notices =
                    try {
                        getPopupNotices()
                    } catch (e: CancellationException) {
                        // 끝내지 못한 시도는 시도로 치지 않는다. Activity 수명에 매여 있어 백그라운드로 가면 취소된다.
                        isLoaded = false
                        throw e
                    }
                pending.addAll(notices)
                if (_current.value == null) _current.value = pending.removeFirstOrNull()
            }
        }

        /**
         * [notice] 를 닫았다. `닫기` · `자세히 보기` · 뒤로가기 모두 같다 — 띄운 기록을 남기고 다음 차례로 넘어간다.
         *
         * @param opened 원문을 실제로 열었는지. 열었으면 설정 공지사항의 읽음에도 남긴다.
         */
        suspend fun close(
            notice: Notice,
            opened: Boolean,
        ) {
            mutex.withLock {
                if (_current.value?.id != notice.id) return
                _current.value = pending.removeFirstOrNull()
            }
            markPopupNoticeSeen(notice, opened)
        }
    }
