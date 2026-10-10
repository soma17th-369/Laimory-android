package com.soma369.laimory.core.domain.usecase.notice

import com.soma369.laimory.core.domain.repository.PopupNoticeRepository
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 팝업 공지의 원문 주소. 팝업 응답엔 원문 주소가 없어 `자세히 보기` 를 누를 때 공지 단건 조회로 받는다.
 *
 * 못 받으면 `null` 이고 원문을 열지 않는다 — 그사이 관리자가 숨겨 404 인 경우 포함(서버 계약).
 */
class GetPopupNoticeContentUrlUseCase
    @Inject
    constructor(
        private val repository: PopupNoticeRepository,
    ) {
        suspend operator fun invoke(noticeId: Long): String? =
            try {
                repository.getNotice(noticeId).contentUrl
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
    }
