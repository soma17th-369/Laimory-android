package com.soma369.laimory.core.domain.usecase.notice

import com.soma369.laimory.core.domain.model.notice.NewNoticePolicy
import com.soma369.laimory.core.domain.repository.NoticeRepository
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 설정의 `공지사항` 줄에 새 공지 표시를 띄울지. 규칙은 [NewNoticePolicy].
 *
 * 실패는 `false` 다 — 표시 하나 때문에 설정 화면에 오류를 띄울 이유가 없다.
 */
class HasNewNoticeUseCase
    @Inject
    constructor(
        private val repository: NoticeRepository,
        private val newNoticePolicy: NewNoticePolicy,
    ) {
        suspend operator fun invoke(): Boolean =
            try {
                newNoticePolicy.newNoticeIds(repository.getNotices(), repository.getReadNoticeIds()).isNotEmpty()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                false
            }
    }
