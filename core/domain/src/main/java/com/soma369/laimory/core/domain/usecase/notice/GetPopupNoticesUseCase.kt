package com.soma369.laimory.core.domain.usecase.notice

import com.soma369.laimory.core.domain.model.notice.PopupNotice
import com.soma369.laimory.core.domain.repository.PopupNoticeRepository
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 이번에 띄울 팝업 공지, 서버 순서(최신 순) 그대로. 이미 본 것은 뺀다.
 *
 * **무엇이 실패해도 앱 진입을 막지 않는다** — 못 받은 것은 띄우지 않을 뿐이다.
 * - 팝업 목록을 못 받으면 아무것도 띄우지 않는다.
 * - 이미 본 기록을 못 읽으면 아무것도 띄우지 않는다. 띄우면 닫을 때마다 다시 뜨는 팝업이 될 수 있다.
 *
 * 공통 안내를 띄우는 `BaseUseCase` 를 쓰지 않는다. 앱 시작마다 부르는 조회라, 실패 안내가 시작 화면에 뜨면 안 된다.
 */
class GetPopupNoticesUseCase
    @Inject
    constructor(
        private val repository: PopupNoticeRepository,
    ) {
        suspend operator fun invoke(): List<PopupNotice> {
            val notices = orNull { repository.getPopupNotices() } ?: return emptyList()
            if (notices.isEmpty()) return emptyList()
            val seen = orNull { repository.getSeenIds() } ?: return emptyList()
            return notices.distinctBy(PopupNotice::id).filterNot { it.id in seen }
        }

        private suspend fun <T> orNull(block: suspend () -> T): T? =
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
    }
