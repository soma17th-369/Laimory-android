package com.soma369.laimory.notice

import com.soma369.laimory.core.domain.model.notice.PopupNotice
import com.soma369.laimory.core.domain.usecase.notice.GetPopupNoticeContentUrlUseCase
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
 * 앱 시작 팝업 공지 목록. 프로세스마다 한 번 받아, 시트에서 카드로 넘겨 볼 목록으로 들고 있는다.
 *
 * 목록은 메모리에만 둔다 — 다음 콜드 스타트에 다시 받는다. 기기에 남는 것은 "이미 띄웠다"는 기록뿐이다.
 */
@Singleton
class PopupNoticeQueue
    @Inject
    constructor(
        private val getPopupNotices: GetPopupNoticesUseCase,
        private val getContentUrl: GetPopupNoticeContentUrlUseCase,
        private val markPopupNoticeSeen: MarkPopupNoticeSeenUseCase,
    ) {
        private val _notices = MutableStateFlow<List<PopupNotice>>(emptyList())

        /** 지금 띄울 팝업 공지, 서버 순서(최신 순). 비면 띄우지 않는다. */
        val notices: StateFlow<List<PopupNotice>> = _notices.asStateFlow()

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
                val loaded =
                    try {
                        getPopupNotices()
                    } catch (e: CancellationException) {
                        // 끝내지 못한 시도는 시도로 치지 않는다. Activity 수명에 매여 있어 백그라운드로 가면 취소된다.
                        isLoaded = false
                        throw e
                    }
                _notices.value = loaded
            }
        }

        /** [notice] 의 원문 주소. 팝업 응답엔 없어 공지 단건 조회로 받는다. 숨겨졌거나(404) 못 받으면 `null` — 열지 않는다. */
        suspend fun contentUrlOf(notice: PopupNotice): String? = getContentUrl(notice.id)

        /**
         * 카드의 [notice] 원문을 열었다(`자세히 보기`). 시트는 그대로 두고, 그 공지는 곧바로 본 것 · 읽은 것으로 남긴다 —
         * 원문을 보는 사이 프로세스가 죽어도 다시 뜨지 않게.
         */
        suspend fun markOpened(notice: PopupNotice) {
            markPopupNoticeSeen(notice.id, opened = true)
        }

        /**
         * 시트를 닫았다. `모두 닫기` · X · 쓸어내리기 · 뒤로가기 · 바깥 누름 모두 같다.
         *
         * [viewedIds] — 넘겨서 화면에 띄운 카드 — 만 본 것으로 남긴다. 넘기지 않은 카드는 보지 않은 것이라 다음 콜드
         * 스타트에 다시 뜬다. 이번 프로세스에서는 시트를 다시 띄우지 않는다.
         */
        suspend fun close(viewedIds: Set<Long>) {
            val shown =
                mutex.withLock {
                    _notices.value.also { _notices.value = emptyList() }
                }
            shown.filter { it.id in viewedIds }.forEach { markPopupNoticeSeen(it.id, opened = false) }
        }
    }
