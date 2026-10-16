package com.soma369.laimory.core.domain.usecase.notice

import com.soma369.laimory.core.domain.repository.NoticeRepository
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 팝업 공지 카드를 눌러 원문을 열었다. 설정 공지사항의 읽음에만 남긴다 — 그쪽 읽음 기준이 "원문을 연 것"이라 같은 사건이다.
 *
 * 팝업 기록은 남기지 않는다. 원문을 열었어도 `다시 보지 않기` 를 누르지 않는 이상 `닫기` 와 같아, 다음에 다시 뜬다.
 *
 * 설정 쪽 정리 기준(표시 기간)은 공지 목록을 봐야 알 수 있다. 여기서는 기존 기록을 그대로 두고 하나만 더한다 — 정리는
 * 다음에 설정이 읽음을 남길 때 한다. 저장 실패는 삼킨다.
 */
class MarkPopupNoticeOpenedUseCase
    @Inject
    constructor(
        private val repository: NoticeRepository,
    ) {
        suspend operator fun invoke(noticeId: Long) {
            try {
                repository.markRead(noticeId, keepIds = repository.getReadNoticeIds())
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                Unit
            }
        }
    }
