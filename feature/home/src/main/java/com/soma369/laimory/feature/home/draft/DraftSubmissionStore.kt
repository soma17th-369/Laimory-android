package com.soma369.laimory.feature.home.draft

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 작업 번호를 받기 전 초안 생성 요청 상태([DraftSubmission])를 홈과 로딩 화면이 함께 보는 인메모리 보관소.
 *
 * 요청 자체는 홈 ViewModel(Activity 수명)이 보낸다. 로딩 화면으로 넘어가도 그 코루틴은 살아 있고, 여기에 결과를
 * 남긴다. 실패는 [consume] 될 때까지 남는다 — 홈이 화면에 다시 보일 때 꺼내 돌아간 뒤의 처리를 한다.
 */
@Singleton
class DraftSubmissionStore
    @Inject
    constructor() {
        private val mutableSubmission = MutableStateFlow<DraftSubmission>(DraftSubmission.Idle)

        val submission: StateFlow<DraftSubmission> = mutableSubmission.asStateFlow()

        fun begin(
            recordDate: LocalDate,
            startedAt: Instant,
        ) {
            mutableSubmission.value = DraftSubmission.Submitting(recordDate, startedAt)
        }

        /** 서버가 작업 번호를 줬다. 이후는 작업 추적이 맡는다. */
        fun succeed() {
            mutableSubmission.value = DraftSubmission.Idle
        }

        fun fail(
            recordDate: LocalDate,
            error: Throwable,
        ) {
            mutableSubmission.value = DraftSubmission.Failed(recordDate, error, DraftSubmitFailureKind.of(error))
        }

        /** 로딩 화면이 실패를 안내했고 사용자가 확인했다. */
        fun markShownOnLoading() {
            mutableSubmission.update { (it as? DraftSubmission.Failed)?.copy(shownOnLoading = true) ?: it }
        }

        /** 남아 있는 실패를 꺼내고 비운다. 실패가 아니면 null 이고 아무것도 바꾸지 않는다. */
        fun consume(): DraftSubmission.Failed? {
            val failed = mutableSubmission.value as? DraftSubmission.Failed ?: return null
            mutableSubmission.value = DraftSubmission.Idle
            return failed
        }

        /** 계정이 바뀌는 등 이전 요청을 잊을 때. */
        fun reset() {
            mutableSubmission.value = DraftSubmission.Idle
        }
    }
