package com.soma369.laimory.feature.collection.viewmodel.sleep

import com.soma369.laimory.core.domain.model.sleep.SleepNightRecord
import com.soma369.laimory.core.domain.repository.SleepDetectionRepository
import com.soma369.laimory.core.domain.repository.SleepRecordRepository
import com.soma369.laimory.core.domain.usecase.sleep.GetSleepForNightUseCase
import com.soma369.laimory.core.domain.usecase.sleep.ObserveSleepDetectionUseCase
import com.soma369.laimory.core.domain.usecase.sleep.RecordManualSleepUseCase
import com.soma369.laimory.core.domain.usecase.sleep.SetSleepDetectionUseCase
import com.soma369.laimory.feature.collection.state.sleep.SleepInputUiIntent
import com.soma369.laimory.feature.collection.state.sleep.SleepTimeField
import com.soma369.laimory.feature.collection.viewmodel.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class SleepInputTimeSheetTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `시각 줄을 누르면 현재 값으로 시트를 열고 그 줄을 펼친다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()
            val bedTime = viewModel.state.value.bedTime
            val wakeTime = viewModel.state.value.wakeTime

            viewModel.sendIntent(SleepInputUiIntent.ShowTimePicker(SleepTimeField.WAKE))
            advanceUntilIdle()

            val sheet = viewModel.state.value.timeSheet
            assertEquals(bedTime, sheet?.bedTime)
            assertEquals(wakeTime, sheet?.wakeTime)
            assertEquals(SleepTimeField.WAKE, sheet?.expandedField)
        }

    @Test
    fun `확인 전에는 시트 임시 값만 바뀐다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()
            val originalBedTime = viewModel.state.value.bedTime

            viewModel.sendIntent(SleepInputUiIntent.ShowTimePicker(SleepTimeField.BED))
            viewModel.sendIntent(SleepInputUiIntent.ChangeSheetTime(SleepTimeField.BED, LocalTime.of(1, 30)))
            advanceUntilIdle()

            assertEquals(LocalTime.of(1, 30), viewModel.state.value.timeSheet?.bedTime)
            assertEquals(originalBedTime, viewModel.state.value.bedTime)
        }

    @Test
    fun `확인은 취침과 기상을 함께 확정하고 시트를 닫는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.sendIntent(SleepInputUiIntent.ShowTimePicker(SleepTimeField.BED))
            viewModel.sendIntent(SleepInputUiIntent.ChangeSheetTime(SleepTimeField.BED, LocalTime.of(0, 15)))
            viewModel.sendIntent(SleepInputUiIntent.ChangeSheetTime(SleepTimeField.WAKE, LocalTime.of(8, 45)))
            viewModel.sendIntent(SleepInputUiIntent.ConfirmTimeSheet)
            advanceUntilIdle()

            assertEquals(LocalTime.of(0, 15), viewModel.state.value.bedTime)
            assertEquals(LocalTime.of(8, 45), viewModel.state.value.wakeTime)
            assertNull(viewModel.state.value.timeSheet)
        }

    @Test
    fun `취소하면 고르던 값을 버리고 원래 시각을 남긴다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()
            val originalBedTime = viewModel.state.value.bedTime
            val originalWakeTime = viewModel.state.value.wakeTime

            viewModel.sendIntent(SleepInputUiIntent.ShowTimePicker(SleepTimeField.BED))
            viewModel.sendIntent(SleepInputUiIntent.ChangeSheetTime(SleepTimeField.BED, LocalTime.of(3, 0)))
            viewModel.sendIntent(SleepInputUiIntent.DismissTimePicker)
            advanceUntilIdle()

            assertNull(viewModel.state.value.timeSheet)
            assertEquals(originalBedTime, viewModel.state.value.bedTime)
            assertEquals(originalWakeTime, viewModel.state.value.wakeTime)
        }

    @Test
    fun `펼친 줄은 한 번에 하나만 유지한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.sendIntent(SleepInputUiIntent.ShowTimePicker(SleepTimeField.BED))
            viewModel.sendIntent(SleepInputUiIntent.ExpandTimeField(SleepTimeField.WAKE))
            advanceUntilIdle()
            assertEquals(SleepTimeField.WAKE, viewModel.state.value.timeSheet?.expandedField)

            viewModel.sendIntent(SleepInputUiIntent.ExpandTimeField(null))
            advanceUntilIdle()
            assertNull(viewModel.state.value.timeSheet?.expandedField)
        }

    private fun createViewModel() =
        SleepInputViewModel(
            recordManualSleepUseCase = RecordManualSleepUseCase(FakeSleepRecordRepository()),
            getSleepForNightUseCase = GetSleepForNightUseCase(FakeSleepRecordRepository()),
            observeSleepDetectionUseCase = ObserveSleepDetectionUseCase(FakeSleepDetectionRepository()),
            setSleepDetectionUseCase = SetSleepDetectionUseCase(FakeSleepDetectionRepository()),
        )

    private class FakeSleepRecordRepository : SleepRecordRepository {
        override suspend fun sleepForNight(
            start: Instant,
            end: Instant,
        ): SleepNightRecord? = null

        override suspend fun recordManualSleep(
            night: LocalDate,
            start: Instant,
            end: Instant,
            zoneOffset: ZoneOffset?,
        ) = Unit
    }

    private class FakeSleepDetectionRepository : SleepDetectionRepository {
        override fun observeEnabled(): Flow<Boolean> = flowOf(false)

        override suspend fun setEnabled(enabled: Boolean) = Unit
    }
}
