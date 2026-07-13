package com.soma369.laimory.feature.collection.viewmodel.sleep

import com.soma369.laimory.core.domain.usecase.sleep.GetSleepForNightUseCase
import com.soma369.laimory.core.domain.usecase.sleep.ObserveSleepDetectionUseCase
import com.soma369.laimory.core.domain.usecase.sleep.RecordManualSleepUseCase
import com.soma369.laimory.core.domain.usecase.sleep.SetSleepDetectionUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.collection.screen.sleep.SleepInputMath
import com.soma369.laimory.feature.collection.state.sleep.SleepInputUiIntent
import com.soma369.laimory.feature.collection.state.sleep.SleepInputUiSideEffect
import com.soma369.laimory.feature.collection.state.sleep.SleepInputUiState
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
        private val getSleepForNightUseCase: GetSleepForNightUseCase,
        private val observeSleepDetectionUseCase: ObserveSleepDetectionUseCase,
        private val setSleepDetectionUseCase: SetSleepDetectionUseCase,
    ) : BaseMviViewModel<SleepInputUiState, SleepInputUiIntent, SleepInputUiSideEffect>(
            SleepInputUiState(wakeDate = LocalDate.now(ZoneId.systemDefault()).minusDays(1)),
        ) {
        private val zone: ZoneId = ZoneId.systemDefault()

        init {
            refreshHasSleep()
            observeAutoDetection()
        }

        override suspend fun handleIntent(intent: SleepInputUiIntent) {
            when (intent) {
                is SleepInputUiIntent.ShowTimePicker -> updateState { copy(editingField = intent.field) }
                SleepInputUiIntent.DismissTimePicker -> updateState { copy(editingField = null) }
                is SleepInputUiIntent.SetBedTime -> updateState { copy(bedTime = intent.time, editingField = null) }
                is SleepInputUiIntent.SetWakeTime -> updateState { copy(wakeTime = intent.time, editingField = null) }
                is SleepInputUiIntent.SelectDate -> moveToDate(intent.date)
                SleepInputUiIntent.PreviousDay -> moveToDate(state.value.wakeDate.minusDays(1))
                SleepInputUiIntent.NextDay -> moveToDate(state.value.wakeDate.plusDays(1))
                SleepInputUiIntent.ShowDatePicker -> updateState { copy(showDatePicker = true) }
                SleepInputUiIntent.DismissDatePicker -> updateState { copy(showDatePicker = false) }
                is SleepInputUiIntent.SetAutoDetection -> setAutoDetection(intent.enabled)
                SleepInputUiIntent.Save -> save()
            }
        }

        /** 자동 감지 활성 여부를 관찰해 상태에 반영한다(온보딩 카드 토글 소스). */
        private fun observeAutoDetection() =
            safeLaunch {
                observeSleepDetectionUseCase().collect { enabled ->
                    updateState { copy(autoDetectionEnabled = enabled) }
                }
            }

        /** 자동 감지를 켜고/끈다. 권한 확보(활동 인식·HC 쓰기)는 화면이 선행하고, 여기선 의도만 반영한다. */
        private fun setAutoDetection(enabled: Boolean) =
            safeLaunch(onError = ::handleFailure) {
                setSleepDetectionUseCase(enabled)
                sendEffect(
                    SleepInputUiSideEffect.ShowMessage(
                        if (enabled) "수면 자동 감지를 켰어요." else "수면 자동 감지를 껐어요.",
                    ),
                )
            }

        /** 대상 밤을 [date] 로 옮긴다. 미래(오늘 이후)는 오늘로 막고, 이미 기록 여부를 다시 조회한다. */
        private fun moveToDate(date: LocalDate) {
            val clamped = if (date.isAfter(LocalDate.now(zone))) LocalDate.now(zone) else date
            updateState { copy(wakeDate = clamped, showDatePicker = false) }
            refreshHasSleep()
        }

        private fun save() =
            safeLaunch(onError = ::onSaveError) {
                val current = state.value
                // 외부 앱 기록은 HC 상 덮어쓸 수 없어 저장하면 중복 세션이 된다 → 저장을 막는다.
                if (current.hasExternalRecord) {
                    sendEffect(SleepInputUiSideEffect.ShowMessage("다른 앱의 수면 기록이 있어 저장할 수 없어요."))
                    return@safeLaunch
                }
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

        /**
         * 그 밤 수면 기록을 갱신한다. 판정 창 = 기상일 전날 18:00 ~ 기상일 18:00.
         *
         * 기록이 있으면 취침·기상 시간을 그 값으로 프리필하고, 외부 앱 기록인지([SleepNightRecord.isOurs] 반대)를
         * 표시한다. 없으면 기본값(23:00/07:00)을 유지한다.
         */
        private fun refreshHasSleep() =
            safeLaunch {
                val (start, end) = nightWindow(state.value.wakeDate)
                val record = getSleepForNightUseCase(start, end)
                updateState {
                    if (record != null) {
                        copy(
                            alreadyRecorded = true,
                            hasExternalRecord = !record.isOurs,
                            bedTime = record.start.atZone(zone).toLocalTime(),
                            wakeTime = record.end.atZone(zone).toLocalTime(),
                        )
                    } else {
                        copy(alreadyRecorded = false, hasExternalRecord = false)
                    }
                }
            }

        private fun nightWindow(wakeDate: LocalDate): Pair<Instant, Instant> {
            val start = wakeDate.minusDays(1).atTime(18, 0).atZone(zone).toInstant()
            val end = wakeDate.atTime(18, 0).atZone(zone).toInstant()
            return start to end
        }
    }
