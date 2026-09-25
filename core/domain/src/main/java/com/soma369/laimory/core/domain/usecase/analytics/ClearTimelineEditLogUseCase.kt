package com.soma369.laimory.core.domain.usecase.analytics

import com.soma369.laimory.core.domain.repository.AnalyticsTimelineEditLogRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 남은 편집 흔적을 모두 비운다.
 *
 * 로그아웃·탈퇴처럼 그 사람의 작성이 끝났을 때 부른다. 흔적은 기록 날짜로만 묶여 있어서, 비우지 않으면
 * 한 기기에서 계정을 바꿨을 때 앞 사람이 고치고 지운 흔적이 다음 사람의 완료 요약에 섞인다.
 */
@Singleton
class ClearTimelineEditLogUseCase
    @Inject
    constructor(
        private val repository: AnalyticsTimelineEditLogRepository,
    ) {
        suspend operator fun invoke() {
            repository.clear()
        }
    }
