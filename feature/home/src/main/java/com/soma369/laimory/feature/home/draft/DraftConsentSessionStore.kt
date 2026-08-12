package com.soma369.laimory.feature.home.draft

import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelection
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 현재 생성 시도 전용 인메모리 준비 상태.
 *
 * 홈이 스냅샷을 확정해 넣고 동의 화면이 꺼내 쓰는 전달 통로다. 모든 접근은 메인 스레드 계약이며,
 * 프로세스 종료 후에는 복원하지 않는다(동의 화면이 준비물 없음을 안내하고 홈으로 되돌린다).
 * UI 체크 상태나 법적 동의 이력을 저장하는 용도로 확장하지 않는다.
 */
@Singleton
class DraftConsentSessionStore
    @Inject
    constructor() {
        private val mutablePreparation = MutableStateFlow<DraftConsentPreparation?>(null)

        /** 현재 생성 시도의 준비 상태. null 이면 진행 중인 시도가 없다. */
        val preparation: StateFlow<DraftConsentPreparation?> = mutablePreparation.asStateFlow()

        private var nextAttemptId = 1L
        private var hasSubmittedResult = false
        private var needsPhotoReselection = false

        /** 새 생성 시도를 시작한다. 같은 데이터라도 항상 새 attemptId 를 받아 이전 시도와 구분된다. */
        fun prepare(
            recordDate: LocalDate,
            zone: ZoneId,
            window: RecordDateWindow,
            selection: DraftSourceItemSelection,
            discardActiveTask: Boolean,
        ) {
            mutablePreparation.value =
                DraftConsentPreparation(
                    attemptId = nextAttemptId++,
                    recordDate = recordDate,
                    zone = zone,
                    window = window,
                    selection = selection,
                    discardActiveTask = discardActiveTask,
                )
        }

        /** 뒤로가기·제출 완료 시 준비 상태를 명시적으로 폐기한다. */
        fun clearPreparation() {
            mutablePreparation.value = null
        }

        /** 제출 성공을 기록한다. 홈이 복귀 시 [consumeSubmittedResult]로 한 번만 소비한다. */
        fun markSubmitted() {
            hasSubmittedResult = true
        }

        /** 제출 성공 결과를 일회성으로 소비한다. 소비 후에는 false 를 반환한다. */
        fun consumeSubmittedResult(): Boolean {
            val result = hasSubmittedResult
            hasSubmittedResult = false
            return result
        }

        /** 제출 시점 사진 접근 실패를 기록한다. 홈이 복귀 시 [consumePhotoReselectionNeeded]로 한 번만 소비한다. */
        fun markPhotoReselectionNeeded() {
            needsPhotoReselection = true
        }

        /** 사진 재선택 필요 신호를 일회성으로 소비한다. 소비 후에는 false 를 반환한다. */
        fun consumePhotoReselectionNeeded(): Boolean {
            val result = needsPhotoReselection
            needsPhotoReselection = false
            return result
        }

        /**
         * 인증 경계 교체(로그아웃·세션 만료) 시 시도 상태 전체를 초기화한다.
         * 이전 계정의 스냅샷·일회성 결과가 다음 계정으로 이월되지 않게 하는 계정 경계 계약이다.
         */
        fun clearAll() {
            mutablePreparation.value = null
            hasSubmittedResult = false
            needsPhotoReselection = false
        }
    }
