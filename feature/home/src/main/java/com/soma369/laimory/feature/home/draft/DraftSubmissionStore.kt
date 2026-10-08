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
 *
 * 로딩 화면이 떠 있는지([isLoadingShown])도 여기서 본다. 로딩으로 넘어가는 전환 중에는 홈도 아직 그려져 있어,
 * 그 사이 실패를 홈이 먼저 꺼내 가면 로딩 화면은 안내할 실패를 잃고 연출에 멈춘다.
 */
@Singleton
class DraftSubmissionStore
    @Inject
    constructor() {
        private val mutableSubmission = MutableStateFlow<DraftSubmission>(DraftSubmission.Idle)

        val submission: StateFlow<DraftSubmission> = mutableSubmission.asStateFlow()

        private val mutableLoadingShown = MutableStateFlow(false)

        /** 로딩 화면이 떠 있다. 그동안 실패는 로딩 화면이 안내하고, 홈은 닫힌 뒤에 꺼낸다. */
        val isLoadingShown: StateFlow<Boolean> = mutableLoadingShown.asStateFlow()

        /**
         * 요청을 시작한다. 로딩 화면으로 넘어가는 중이므로 떠 있는 것으로 먼저 표시한다 — 오프라인이면 실패가 로딩
         * 화면의 첫 프레임보다 먼저 올 수 있다.
         */
        fun begin(
            recordDate: LocalDate,
            startedAt: Instant,
        ) {
            mutableLoadingShown.value = true
            mutableSubmission.value = DraftSubmission.Submitting(recordDate, startedAt)
        }

        /** 로딩 화면이 화면에 들어오고 나갈 때 알린다. */
        fun setLoadingShown(shown: Boolean) {
            mutableLoadingShown.value = shown
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
            mutableLoadingShown.value = false
        }
    }
