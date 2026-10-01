package com.soma369.laimory.core.domain.usecase.notice

import com.soma369.laimory.core.domain.base.BaseUseCase
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.model.notice.NewNoticePolicy
import com.soma369.laimory.core.domain.model.notice.NoticeFeed
import com.soma369.laimory.core.domain.repository.NoticeRepository
import javax.inject.Inject

/**
 * 노출 중인 공지를 서버 정렬(최신 순) 그대로, 새 공지 표시와 함께 가져온다. 인증이 필요 없다.
 *
 * 읽음 기록을 읽지 못하면 아무것도 새 공지로 치지 않는다 — 표시 하나 때문에 목록을 잃을 이유가 없다.
 */
class GetNoticesUseCase
    @Inject
    constructor(
        private val repository: NoticeRepository,
        private val newNoticePolicy: NewNoticePolicy,
        messageHelper: MessageHelper,
    ) : BaseUseCase(messageHelper) {
        suspend operator fun invoke(): Result<NoticeFeed> =
            execute {
                val notices = repository.getNotices()
                val readIds = runCatching { repository.getReadNoticeIds() }.getOrNull()
                NoticeFeed(
                    notices = notices,
                    newIds = readIds?.let { newNoticePolicy.newNoticeIds(notices, it) }.orEmpty(),
                )
            }
    }
