package com.soma369.laimory.feature.timeline.viewmodel

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.HandledException
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import com.soma369.laimory.core.domain.usecase.ObserveSourceItemsUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.timeline.BuildConfig
import com.soma369.laimory.feature.timeline.model.TimelineSourceCategory
import com.soma369.laimory.feature.timeline.state.SourceSummaryRow
import com.soma369.laimory.feature.timeline.state.TimelineUiIntent
import com.soma369.laimory.feature.timeline.state.TimelineUiSideEffect
import com.soma369.laimory.feature.timeline.state.TimelineUiState
import com.soma369.laimory.feature.timeline.state.UploadTarget
import com.soma369.laimory.feature.timeline.testexport.DriveTestExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class TimelineViewModel
    @Inject
    constructor(
        private val observeSourceItemsUseCase: ObserveSourceItemsUseCase,
        private val driveTestExporter: DriveTestExporter,
    ) : BaseMviViewModel<TimelineUiState, TimelineUiIntent, TimelineUiSideEffect>(
            TimelineUiState(selectedDate = LocalDate.now(ZoneId.systemDefault())),
        ) {
        private val zone: ZoneId = ZoneId.systemDefault()

        /** 요약 재계산의 트리거가 되는 선택 날짜. 상태의 selectedDate 는 이 값을 미러링한다. */
        private val selectedDate = MutableStateFlow(state.value.selectedDate)

        init {
            observeSummary()
        }

        /** 수집 아이템과 선택 날짜를 합쳐, 그날 창 안 아이템을 카테고리별로 센다. */
        private fun observeSummary() =
            safeLaunch {
                combine(observeSourceItemsUseCase(), selectedDate) { items, date ->
                    date to summarize(items, date)
                }.collect { (date, rows) ->
                    updateState { copy(selectedDate = date, summaryRows = rows) }
                }
            }

        private fun summarize(
            items: List<SourceItem>,
            date: LocalDate,
        ): List<SourceSummaryRow> {
            val window = RecordDateWindow.ofDate(date, zone)
            val inWindow = items.filter { window.contains(it) }
            return TimelineSourceCategory.entries.map { category ->
                SourceSummaryRow(
                    label = category.label,
                    count = inWindow.count { it.itemType in category.itemTypes },
                )
            }
        }

        override suspend fun handleIntent(intent: TimelineUiIntent) {
            when (intent) {
                is TimelineUiIntent.SelectDate -> {
                    selectedDate.value = intent.date
                    updateState { copy(showDatePicker = false) }
                }
                TimelineUiIntent.ShowDatePicker -> updateState { copy(showDatePicker = true) }
                TimelineUiIntent.DismissDatePicker -> updateState { copy(showDatePicker = false) }
                is TimelineUiIntent.RequestUpload -> updateState { copy(pendingUpload = intent.target) }
                TimelineUiIntent.ConfirmUpload -> confirmUpload()
                TimelineUiIntent.DismissUpload -> updateState { copy(pendingUpload = null) }
            }
        }

        private fun confirmUpload() {
            val target = state.value.pendingUpload ?: return
            val date = state.value.selectedDate
            updateState { copy(pendingUpload = null) }
            when (target) {
                // 임시 테스트 전용(삭제 예정) — 버튼은 debug 로만 노출되지만, 호출부도 debug 로 가드한다.
                UploadTarget.DRIVE_TEST -> if (BuildConfig.DEBUG) exportToDrive(date)
            }
        }

        /**
         * 이 화면은 base 의 snackbar 채널이 아니라 [sendEffect] 로 스낵바를 흘리므로, 공통 실패도
         * [TimelineUiSideEffect.ShowSnackbar] 로 라우팅한다. [HandledException] 은 UseCase 공통 정책에서
         * 이미 알렸으므로 무시한다(중복 방지).
         */
        override fun handleFailure(e: Throwable) {
            if (e is HandledException) return
            val message =
                when (e) {
                    is ApiException.NetworkException -> ApiException.NETWORK_ERROR
                    is ApiException -> e.message
                    else -> ApiException.UNKNOWN_ERROR
                }
            sendEffect(TimelineUiSideEffect.ShowSnackbar(message))
        }

        /**
         * 임시 테스트 전용(삭제 예정) — Drive 내보내기.
         * `testexport` 패키지를 지울 때 이 메서드와 [driveTestExporter] 주입·호출부만 되돌리면 된다.
         */
        private fun exportToDrive(date: LocalDate) =
            safeLaunch {
                sendEffect(TimelineUiSideEffect.ShowSnackbar("Drive로 내보내는 중…"))
                sendEffect(TimelineUiSideEffect.ShowSnackbar(driveTestExporter.export(date)))
            }
    }
