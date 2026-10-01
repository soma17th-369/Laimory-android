package com.soma369.laimory.core.domain.usecase.notice

import com.soma369.laimory.core.domain.model.notice.NewNoticePolicy
import com.soma369.laimory.core.domain.model.notice.Notice
import com.soma369.laimory.core.domain.repository.NoticeRepository
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 공지를 눌러 열었다고 남긴다.
 *
 * 저장 실패는 삼킨다 — 원문은 이미 열렸고, 다음에 표시가 한 번 더 뜰 뿐이다.
 *
 * @param notices 지금 보이는 목록. 표시 기간이 지난 공지의 기록을 이때 함께 정리한다.
 */
class MarkNoticeReadUseCase
    @Inject
    constructor(
        private val repository: NoticeRepository,
        private val newNoticePolicy: NewNoticePolicy,
    ) {
        suspend operator fun invoke(
            notice: Notice,
            notices: List<Notice>,
        ) {
            try {
                repository.markRead(notice.id, keepIds = newNoticePolicy.trackedIds(notices))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                Unit
            }
        }
    }
