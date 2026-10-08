package com.soma369.laimory.core.domain.usecase.notice

import com.soma369.laimory.core.domain.model.notice.Notice
import com.soma369.laimory.core.domain.repository.NoticeRepository
import com.soma369.laimory.core.domain.repository.PopupNoticeRepository
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 팝업 공지를 닫았다고 남긴다. `닫기` · `자세히 보기` · 뒤로가기 어느 쪽으로 닫아도 다시 띄우지 않는다.
 *
 * [opened] 면(원문을 실제로 열었으면) 설정 공지사항의 읽음에도 남긴다 — 그쪽 읽음 기준이 "원문을 연 것"이라 같은
 * 사건이다. `닫기` 만 눌렀으면 원문을 안 봤으므로 설정의 새 공지 점은 남긴다.
 *
 * 저장 실패는 삼킨다. 다음 콜드 스타트에 한 번 더 뜰 뿐이다.
 */
class MarkPopupNoticeSeenUseCase
    @Inject
    constructor(
        private val popupNoticeRepository: PopupNoticeRepository,
        private val noticeRepository: NoticeRepository,
    ) {
        suspend operator fun invoke(
            notice: Notice,
            opened: Boolean,
        ) {
            swallow { popupNoticeRepository.markSeen(notice.id) }
            if (opened) {
                // 설정 쪽 정리 기준(표시 기간)은 공지 목록을 봐야 알 수 있다. 여기서는 기존 기록을 그대로 두고 하나만
                // 더한다 — 정리는 다음에 설정이 읽음을 남길 때 한다.
                swallow { noticeRepository.markRead(notice.id, keepIds = noticeRepository.getReadNoticeIds()) }
            }
        }

        private suspend fun swallow(block: suspend () -> Unit) {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                Unit
            }
        }
    }
