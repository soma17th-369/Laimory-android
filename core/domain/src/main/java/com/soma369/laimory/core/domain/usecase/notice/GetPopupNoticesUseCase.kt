package com.soma369.laimory.core.domain.usecase.notice

import com.soma369.laimory.core.domain.model.notice.Notice
import com.soma369.laimory.core.domain.repository.PopupNoticeRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 이번에 띄울 팝업 공지, 서버 순서(최신 순) 그대로.
 *
 * **무엇이 실패해도 앱 진입을 막지 않는다** — 못 받은 것은 띄우지 않을 뿐이다.
 * - 팝업 id 를 못 받으면 아무것도 띄우지 않는다.
 * - 이미 본 기록을 못 읽으면 아무것도 띄우지 않는다. 띄우면 닫을 때마다 다시 뜨는 팝업이 될 수 있다.
 * - 한 건을 못 받으면(id 를 받은 직후 숨겨져 404 인 경우 포함) 그 건만 건너뛴다.
 *
 * 공통 안내를 띄우는 `BaseUseCase` 를 쓰지 않는다. 앱 시작마다 부르는 조회라, 운영 서버에 아직 단건 API 가 없을 때
 * (404) "지원하지 않는 기능" 안내가 시작 화면에 뜨면 안 된다.
 */
class GetPopupNoticesUseCase
    @Inject
    constructor(
        private val repository: PopupNoticeRepository,
    ) {
        suspend operator fun invoke(): List<Notice> {
            val ids = orNull { repository.getPopupNoticeIds() } ?: return emptyList()
            if (ids.isEmpty()) return emptyList()
            val seen = orNull { repository.getSeenIds() } ?: return emptyList()
            val unseen = ids.distinct().filterNot { it in seen }
            return coroutineScope {
                unseen.map { id -> async { orNull { repository.getNotice(id) } } }.awaitAll().filterNotNull()
            }
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
