package com.soma369.laimory.core.domain.usecase.inquiry

import com.soma369.laimory.core.domain.base.BaseUseCase
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.model.inquiry.InquirySubmission
import com.soma369.laimory.core.domain.repository.InquiryRepository
import javax.inject.Inject

/** 문의를 보낸다. 규칙을 어긴 입력은 서버에 보내지 않는다 — 화면이 이미 막지만 한 번 더 거른다. */
class SubmitInquiryUseCase
    @Inject
    constructor(
        private val repository: InquiryRepository,
        messageHelper: MessageHelper,
    ) : BaseUseCase(messageHelper) {
        suspend operator fun invoke(submission: InquirySubmission): Result<Unit> {
            if (!submission.isValid) return Result.failure(IllegalArgumentException("문의 입력이 규칙에 맞지 않습니다"))
            return execute { repository.submit(submission.copy(email = submission.email.trim())) }
        }
    }
