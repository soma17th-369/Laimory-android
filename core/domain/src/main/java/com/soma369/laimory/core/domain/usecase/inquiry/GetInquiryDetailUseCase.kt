package com.soma369.laimory.core.domain.usecase.inquiry

import com.soma369.laimory.core.domain.base.BaseUseCase
import com.soma369.laimory.core.domain.exception.InquiryNotFoundException
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.model.inquiry.InquiryDetail
import com.soma369.laimory.core.domain.repository.InquiryRepository
import javax.inject.Inject

/**
 * 내 문의 한 건을 가져온다.
 *
 * 없거나 남의 문의면 [InquiryNotFoundException] 으로 실패한다 — 공용 404 안내가 아니라 화면이 안내한다.
 */
class GetInquiryDetailUseCase
    @Inject
    constructor(
        private val repository: InquiryRepository,
        messageHelper: MessageHelper,
    ) : BaseUseCase(messageHelper) {
        suspend operator fun invoke(inquiryId: Long): Result<InquiryDetail> =
            try {
                execute { repository.getInquiry(inquiryId) }
            } catch (e: InquiryNotFoundException) {
                Result.failure(e)
            }
    }
