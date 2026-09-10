package com.soma369.laimory.feature.home.draft

import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelection
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 초안 생성 흐름이 나눠 보는 인메모리 상태.
 *
 * **세 가지를 구분해 담는다.** 지금까지는 `attemptId` 하나가 셋을 겸했는데, 카드에서 CTA 전에도
 * 상세를 열게 되면서 겸할 수 없게 됐다.
 *
 * | | 언제 바뀌나 | 무엇을 초기화하나 |
 * | --- | --- | --- |
 * | [selection] | 기록 창 변경·수집 갱신 | 표시 모델만. 선택 상태는 그대로 |
 * | [excludedRawIds]·[isLocationSendEnabled] | 사용자가 고를 때만 | 아무것도 |
 * | [preparation] | CTA 를 눌렀을 때만 | 제출 성공 시 선택 상태 |
 *
 * 모든 접근은 메인 스레드 계약이며, 프로세스 종료 후에는 복원하지 않는다. UI 체크 상태나 법적
 * 동의 이력을 저장하는 용도로 확장하지 않는다.
 */
@Singleton
class DraftConsentSessionStore
    @Inject
    constructor() {
        private val mutablePreparation = MutableStateFlow<DraftConsentPreparation?>(null)

        /** 현재 생성 시도의 준비 상태. null 이면 진행 중인 시도가 없다. */
        val preparation: StateFlow<DraftConsentPreparation?> = mutablePreparation.asStateFlow()

        private val mutableSelection = MutableStateFlow<DraftConsentSelectionSnapshot?>(null)

        /** 홈이 상시로 유지하는 선택 스냅샷. 카드가 상세를 여는 근거다. */
        val selection: StateFlow<DraftConsentSelectionSnapshot?> = mutableSelection.asStateFlow()

        private val mutableExcludedRawIds = MutableStateFlow<Set<String>>(emptySet())

        /** 사용자가 전송에서 뺀 항목. 홈이 본문 건수를 세고 상세가 토글한다. */
        val excludedRawIds: StateFlow<Set<String>> = mutableExcludedRawIds.asStateFlow()

        private val mutableLocationSendEnabled = MutableStateFlow(true)

        /**
         * 위치 전체 전송 여부. **제외 집합과 따로 소유한다.**
         *
         * `그 시점 rawId 를 제외 집합에 넣기` 로 구현하면 그 뒤 수집된 STAY·MOVEMENT 가 제외 집합에
         * 없어, 스위치는 OFF 인데 위치가 나간다.
         */
        val isLocationSendEnabled: StateFlow<Boolean> = mutableLocationSendEnabled.asStateFlow()

        private var nextAttemptId = 1L
        private var nextRevision = 1L
        private var needsPhotoReselection = false

        private val mutableAccountSession = MutableStateFlow(0L)

        /**
         * 계정 경계를 넘을 때마다 오르는 번호.
         *
         * 스토어를 비우는 것만으로는 계정 경계를 넘어 살아남는 화면(Activity 범위 ViewModel)
         * 안에 남은 판정이 그대로다. 그쪽이 이전 계정의 값을 버릴 계기가 필요하다.
         */
        val accountSession: StateFlow<Long> = mutableAccountSession.asStateFlow()

        /**
         * 홈이 새로 만든 선택 스냅샷을 반영한다.
         *
         * 제외 집합은 **사라진 항목만 걷어 낸다.** 갱신마다 비우면 수집이 돌 때마다 사용자가 뺀
         * 것이 되살아난다. 사진 선택이 `intersect` 로 하는 것과 같은 원칙이다.
         */
        fun updateSelection(
            recordDate: LocalDate,
            zone: ZoneId,
            window: RecordDateWindow,
            selection: DraftSourceItemSelection,
        ) {
            // 같은 내용이면 revision 을 올리지 않는다. 홈은 상태를 만들 때마다 이 함수를 부르는데,
            // 내용이 그대로인데도 번호가 오르면 상세가 헛되이 표시 모델을 다시 만든다.
            val current = mutableSelection.value
            if (current != null &&
                current.recordDate == recordDate &&
                current.window == window &&
                current.selection == selection
            ) {
                return
            }
            mutableSelection.value =
                DraftConsentSelectionSnapshot(
                    revision = nextRevision++,
                    recordDate = recordDate,
                    zone = zone,
                    window = window,
                    selection = selection,
                )
            val alive = selection.items.mapTo(mutableSetOf()) { it.rawId }
            mutableExcludedRawIds.update { excluded -> excluded intersect alive }
        }

        /** 창이 무효해 스냅샷을 만들 수 없을 때. 선택 상태는 건드리지 않는다. */
        fun clearSelection() {
            mutableSelection.value = null
        }

        /** 상세에서 항목 하나를 넣고 뺀다. */
        fun toggleExcluded(rawId: String) {
            mutableExcludedRawIds.update { excluded ->
                if (rawId in excluded) excluded - rawId else excluded + rawId
            }
        }

        /**
         * 위치 전송을 한 번에 켜고 끈다.
         *
         * 끌 때 그 시점 위치 항목을 제외 집합에 넣지 않는다 — 그러면 뒤에 수집된 위치가 샌다.
         * 실제로 무엇을 뺄지는 스냅샷을 쓸 때 이 값으로 판단한다.
         */
        fun setLocationSendEnabled(enabled: Boolean) {
            mutableLocationSendEnabled.value = enabled
        }

        /**
         * CTA 로 제출용 스냅샷을 확정한다. 같은 데이터라도 항상 새 attemptId 를 받아 이전 시도와
         * 구분된다.
         *
         * 상시 스냅샷도 같은 내용으로 맞춰 둔다 — 확정 직후 상세를 열면 제출할 것과 같은 것을
         * 봐야 한다.
         */
        fun prepare(
            recordDate: LocalDate,
            zone: ZoneId,
            window: RecordDateWindow,
            selection: DraftSourceItemSelection,
            discardActiveTask: Boolean,
        ) {
            updateSelection(recordDate, zone, window, selection)
            mutablePreparation.value =
                DraftConsentPreparation(
                    attemptId = nextAttemptId++,
                    snapshot = checkNotNull(mutableSelection.value),
                    discardActiveTask = discardActiveTask,
                )
        }

        /**
         * 뒤로가기·취소·제출 실패 시 **제출용 스냅샷만** 폐기한다.
         *
         * 홈 선택 상태(사진·제외·위치 전송)는 남긴다. 취소 한 번에 사용자가 뺀 일정·알림이
         * 되살아나면 다음 시도에서 빼려던 것이 다시 실린다.
         */
        fun clearPreparation() {
            mutablePreparation.value = null
        }

        /** 제출에 성공했다. 이때만 선택 상태를 비운다 — 그 시도의 선택은 이미 서버로 갔다. */
        fun clearAfterSubmission() {
            mutablePreparation.value = null
            mutableExcludedRawIds.value = emptySet()
            mutableLocationSendEnabled.value = true
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
            mutableSelection.value = null
            mutableExcludedRawIds.value = emptySet()
            mutableLocationSendEnabled.value = true
            needsPhotoReselection = false
            mutableAccountSession.value += 1
        }
    }
