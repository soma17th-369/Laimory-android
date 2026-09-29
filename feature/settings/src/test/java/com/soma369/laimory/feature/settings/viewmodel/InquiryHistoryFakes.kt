package com.soma369.laimory.feature.settings.viewmodel

import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.inquiry.InquiryDetail
import com.soma369.laimory.core.domain.model.inquiry.InquiryStatus
import com.soma369.laimory.core.domain.model.inquiry.InquirySubmission
import com.soma369.laimory.core.domain.model.inquiry.InquirySummary
import com.soma369.laimory.core.domain.navigation.Page
import com.soma369.laimory.core.domain.repository.InquiryRepository
import kotlinx.coroutines.CompletableDeferred
import java.time.LocalDateTime

/** 문의 내역 목록·상세 테스트가 함께 쓰는 가짜. */
internal class FakeInquiryHistoryRepository : InquiryRepository {
    var list: Result<List<InquirySummary>> = Result.success(emptyList())
    val details = mutableMapOf<Long, Result<InquiryDetail>>()

    /** 이 id 의 상세 조회를 끝내지 않고 붙잡아 둔다. 다른 문의로 넘어가는 경우를 만든다. */
    val gates = mutableMapOf<Long, CompletableDeferred<Unit>>()
    val requestedIds = mutableListOf<Long>()

    override suspend fun submit(submission: InquirySubmission) = error("사용하지 않음")

    override suspend fun getMyInquiries(): List<InquirySummary> = list.getOrThrow()

    override suspend fun getInquiry(inquiryId: Long): InquiryDetail {
        requestedIds += inquiryId
        gates[inquiryId]?.await()
        return details.getValue(inquiryId).getOrThrow()
    }
}

internal data object NoOpInquiryMessageHelper : MessageHelper {
    override fun send(message: UserMessage) = Unit
}

internal class RecordingInquiryNavigationHelper : NavigationHelper {
    val navigatedTo = mutableListOf<Page>()
    var backCount = 0

    override fun navigateTo(page: Page) {
        navigatedTo += page
    }

    override fun replaceRoot(page: Page) = Unit

    override fun navigateToBack() {
        backCount++
    }
}

internal fun inquirySummary(
    id: Long,
    status: InquiryStatus = InquiryStatus.RECEIVED,
) = InquirySummary(id, "문의 $id", status, LocalDateTime.of(2026, 9, 29, 10, 0), null)

internal fun inquiryDetail(id: Long) =
    InquiryDetail(
        id = id,
        title = "문의 $id",
        status = InquiryStatus.RECEIVED,
        email = "user@example.com",
        description = "내용 $id",
        attachmentUrls = emptyList(),
        createdAt = LocalDateTime.of(2026, 9, 29, 10, 0),
        answeredAt = null,
    )
