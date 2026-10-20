package com.soma369.laimory.notice

import com.soma369.laimory.core.domain.model.notice.PopupNotice
import com.soma369.laimory.core.domain.usecase.notice.GetPopupNoticeContentUrlUseCase
import com.soma369.laimory.core.domain.usecase.notice.GetPopupNoticesUseCase
import com.soma369.laimory.core.domain.usecase.notice.MarkPopupNoticeOpenedUseCase
import com.soma369.laimory.core.domain.usecase.notice.MarkPopupNoticeSeenUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 홈 팝업 공지 대기열. 홈에 들어올 때마다 받아, 시트가 한 장씩 처리할 카드를 들고 있다.
 *
 * 공지는 사용자가 봐야 하는 것이라 `다시 보지 않기` 를 고르기 전까지는 다시 알린다.
 * - `다시 보지 않기`: 그 공지 한 건만 기기에 남겨 다시는 띄우지 않는다.
 * - `닫기` · 원문 열기 · 시트 내리기: 기록하지 않는다. 앱을 새로 켤 때까지([closedIds])만 다시 띄우지 않고, 새로 켜면
 *   또 띄운다. 홈으로 돌아오거나 앱을 잠깐 내렸다 오는 것(사진 선택기 · 원문 Custom Tab 포함)만으로는 같은 공지가 다시
 *   뜨지 않게 하려는 것이다.
 *
 * 앱을 새로 켰는지는 프로세스가 아니라 앱 화면으로 본다([onAppLaunched]). 위치 수집 포그라운드 서비스가 프로세스를 붙잡고
 * 있어, 최근 앱에서 지워도 프로세스는 살아 있기 때문이다.
 */
@Singleton
class PopupNoticeQueue
    @Inject
    constructor(
        private val getPopupNotices: GetPopupNoticesUseCase,
        private val getContentUrl: GetPopupNoticeContentUrlUseCase,
        private val markPopupNoticeSeen: MarkPopupNoticeSeenUseCase,
        private val markPopupNoticeOpened: MarkPopupNoticeOpenedUseCase,
    ) {
        private val _notices = MutableStateFlow<List<PopupNotice>>(emptyList())

        /** 아직 처리하지 않은 팝업 공지, 서버 순서(최신 순). 처리한 카드는 빠진다. 비면 띄우지 않는다. */
        val notices: StateFlow<List<PopupNotice>> = _notices.asStateFlow()

        private val refreshing = Mutex()

        /** `닫기` 한 공지. 앱을 새로 켜면([onAppLaunched]) 비운다. */
        private val closedIds = mutableSetOf<Long>()

        /**
         * 띄울 공지가 있는지 다시 받는다. 홈에 들어올 때와 홈에 있는 채로 앱에 돌아올 때 부른다.
         *
         * 시트가 떠 있거나(처리할 카드가 남음) 이미 받는 중이면 부르지 않는다. 못 받으면 아무것도 띄우지 않을 뿐이고, 다음
         * 홈 진입에서 다시 받는다.
         */
        suspend fun refresh() {
            if (_notices.value.isNotEmpty() || !refreshing.tryLock()) return
            try {
                val loaded = getPopupNotices()
                val closed = synchronized(closedIds) { closedIds.toSet() }
                _notices.value = loaded.filterNot { it.id in closed }
            } finally {
                refreshing.unlock()
            }
        }

        /** [notice] 의 원문 주소. 팝업 응답엔 없어 공지 단건 조회로 받는다. 숨겨졌거나(404) 못 받으면 `null` — 열지 않는다. */
        suspend fun contentUrlOf(notice: PopupNotice): String? = getContentUrl(notice.id)

        /** 카드를 눌러 [notice] 원문을 열었다. 설정 공지 읽음에만 남기고, 카드는 시트에 그대로 둔다. */
        suspend fun markOpened(notice: PopupNotice) {
            markPopupNoticeOpened(notice.id)
        }

        /** `닫기`. [notice] 를 대기열에서 빼고, 앱을 새로 켤 때까지 다시 띄우지 않는다. */
        fun close(notice: PopupNotice) {
            synchronized(closedIds) { closedIds += notice.id }
            _notices.update { notices -> notices.filterNot { it.id == notice.id } }
        }

        /**
         * `다시 보지 않기`. [notice] 를 대기열에서 빼고 다시는 띄우지 않는다고 기기에 남긴다.
         *
         * 저장이 끝나기 전 홈 진입으로 다시 받아도 뜨지 않게 닫은 기록에도 넣는다.
         */
        suspend fun dismissForever(notice: PopupNotice) {
            close(notice)
            markPopupNoticeSeen(notice.id)
        }

        /**
         * 앱 화면을 처음부터 새로 띄웠다(최근 앱에서 지운 뒤 · 강제 중지 · 재부팅 뒤 열기). 닫은 공지를 다시 띄운다.
         *
         * 시트를 띄운 채 앱을 지웠으면 남은 카드도 버려, 홈 진입 확인이 닫은 공지까지 서버 순서대로 다시 받게 한다.
         * 화면 회전처럼 이전 상태를 되살려 다시 만드는 것은 새로 켠 것이 아니라 부르지 않는다.
         */
        fun onAppLaunched() {
            synchronized(closedIds) { closedIds.clear() }
            _notices.value = emptyList()
        }

        /** 시트를 쓸어내리거나 바깥을 누르거나 뒤로 갔다. 남은 카드를 모두 [close] 한 것과 같다. */
        fun closeAll() {
            val remaining = _notices.getAndUpdate { emptyList() }
            synchronized(closedIds) { remaining.mapTo(closedIds, PopupNotice::id) }
        }
    }
