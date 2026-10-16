package com.soma369.laimory.core.domain.usecase.notice

import com.soma369.laimory.core.domain.repository.PopupNoticeRepository
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 팝업 공지 카드의 `다시 보지 않기`. 그 공지 한 건만 다시는 팝업으로 띄우지 않는다 — 시트의 다른 카드와 무관하다.
 *
 * 설정 공지사항의 읽음은 남기지 않는다. 그쪽 읽음 기준은 "원문을 연 것"이라, 원문을 보지 않았으면 새 공지 점은 남는다.
 *
 * 저장 실패는 삼킨다. 다음에 한 번 더 뜰 뿐이다.
 */
class MarkPopupNoticeSeenUseCase
    @Inject
    constructor(
        private val repository: PopupNoticeRepository,
    ) {
        suspend operator fun invoke(noticeId: Long) {
            try {
                repository.markSeen(noticeId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                Unit
            }
        }
    }
