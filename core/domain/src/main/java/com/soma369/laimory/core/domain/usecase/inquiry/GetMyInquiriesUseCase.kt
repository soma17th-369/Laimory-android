package com.soma369.laimory.core.domain.usecase.inquiry

import com.soma369.laimory.core.domain.base.BaseUseCase
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.model.inquiry.InquirySummary
import com.soma369.laimory.core.domain.repository.InquiryRepository
import javax.inject.Inject

/** 내가 보낸 문의를 서버 정렬(최신 순) 그대로 가져온다. */
class GetMyInquiriesUseCase
    @Inject
    constructor(
        private val repository: InquiryRepository,
        messageHelper: MessageHelper,
    ) : BaseUseCase(messageHelper) {
        suspend operator fun invoke(): Result<List<InquirySummary>> = execute { repository.getMyInquiries() }
    }
