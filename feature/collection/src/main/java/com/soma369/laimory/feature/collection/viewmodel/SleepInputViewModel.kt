package com.soma369.laimory.feature.collection.viewmodel

import com.soma369.laimory.core.domain.usecase.HasSleepForNightUseCase
import com.soma369.laimory.core.domain.usecase.RecordManualSleepUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.collection.screen.SleepInputMath
import com.soma369.laimory.feature.collection.state.SleepInputUiIntent
import com.soma369.laimory.feature.collection.state.SleepInputUiSideEffect
import com.soma369.laimory.feature.collection.state.SleepInputUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * 불확실한 밤 수면을 사용자 입력으로 받아 Health Connect 에 기록하는 화면(#145).
 *
 * 기본 대상은 어제 밤(기상일=어제). 저장 시 HC 쓰기 권한 확보는 화면(권한 런처)이 선행하고, ViewModel 은
 * 확보된 뒤 [SleepInputUiIntent.Save] 를 받아 기록만 한다.
 */
@HiltViewModel
class SleepInputViewModel
    @Inject
    constructor(
        private val recordManualSleepUseCase: RecordManualSleepUseCase,
        private val hasSleepForNightUseCase: HasSleepForNightUseCase,
    ) : BaseMviViewModel<SleepInputUiState, SleepInputUiIntent, SleepInputUiSideEffect>(
            SleepInputUiState(wakeDate = LocalDate.now(ZoneId.systemDefault()).minusDays(1)),
        ) {
        private val zone: ZoneId = ZoneId.systemDefault()

        init {
            refreshHasSleep()
        }

        override suspend fun handleIntent(intent: SleepInputUiIntent) {
            when (intent) {
                is SleepInputUiIntent.ShowTimePicker -> updateState { copy(editingField = intent.field) }
                SleepInputUiIntent.DismissTimePicker -> updateState { copy(editingField = null) }
                is SleepInputUiIntent.SetBedTime -> updateState { copy(bedTime = intent.time, editingField = null) }
                is SleepInputUiIntent.SetWakeTime -> updateState { copy(wakeTime = intent.time, editingField = null) }
                SleepInputUiIntent.Save -> save()
            }
        }

        private fun save() =
            safeLaunch(onError = ::onSaveError) {
                val current = state.value
                updateState { copy(isSaving = true) }
                val (start, end) =
                    SleepInputMath.sleepInstants(current.wakeDate, current.bedTime, current.wakeTime, zone)
                recordManualSleepUseCase(current.wakeDate, start, end, zone.rules.getOffset(end))
                updateState { copy(isSaving = false) }
                sendEffect(SleepInputUiSideEffect.ShowMessage("수면을 기록했어요."))
                refreshHasSleep()
            }

        private fun onSaveError(e: Throwable) {
            updateState { copy(isSaving = false) }
            handleFailure(e)
        }

        /** 그 밤에 이미 수면이 있는지 갱신한다. 판정 창 = 기상일 전날 18:00 ~ 기상일 18:00. */
        private fun refreshHasSleep() =
            safeLaunch {
                val (start, end) = nightWindow(state.value.wakeDate)
                val exists = hasSleepForNightUseCase(start, end)
                updateState { copy(alreadyRecorded = exists) }
            }

        private fun nightWindow(wakeDate: LocalDate): Pair<Instant, Instant> {
            val start = wakeDate.minusDays(1).atTime(18, 0).atZone(zone).toInstant()
            val end = wakeDate.atTime(18, 0).atZone(zone).toInstant()
            return start to end
        }
    }
