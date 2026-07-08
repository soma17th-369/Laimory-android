package com.soma369.laimory.feature.timeline.viewmodel

import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import com.soma369.laimory.core.domain.usecase.ObserveSourceItemsUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.timeline.model.TimelineSourceCategory
import com.soma369.laimory.feature.timeline.state.SourceSummaryRow
import com.soma369.laimory.feature.timeline.state.TimelineUiIntent
import com.soma369.laimory.feature.timeline.state.TimelineUiSideEffect
import com.soma369.laimory.feature.timeline.state.TimelineUiState
import com.soma369.laimory.feature.timeline.state.UploadTarget
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

        /** #120/#121 구현 전까지 배선 확인용 placeholder — 이후 실제 업로드 UseCase 호출로 교체한다. */
        private fun confirmUpload() {
            val target = state.value.pendingUpload ?: return
            updateState { copy(pendingUpload = null) }
            val message =
                when (target) {
                    UploadTarget.SERVER_DRAFT -> "서버로 초안 생성은 아직 준비 중이에요."
                    UploadTarget.DRIVE_TEST -> "Drive 테스트 업로드는 아직 준비 중이에요."
                }
            sendEffect(TimelineUiSideEffect.ShowSnackbar(message))
        }
    }
