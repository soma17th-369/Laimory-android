package com.soma369.laimory.feature.home.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HomeTimelineButtonStatusTest {
    @Test
    fun `생성 중이면 서버에 기록이 있어도 제작중이다`() {
        val state = HomeUiState(draftStatus = DraftCreationStatus.PROCESSING, selectedRecord = HomeRecordState.DRAFT)

        assertEquals(DraftCreationStatus.PROCESSING, state.timelineButtonStatus)
    }

    @Test
    fun `저장된 기록과 내용 있는 초안은 확인하기다`() {
        assertEquals(
            DraftCreationStatus.SUCCESS,
            HomeUiState(selectedRecord = HomeRecordState.SAVED).timelineButtonStatus,
        )
        assertEquals(
            DraftCreationStatus.SUCCESS,
            HomeUiState(selectedRecord = HomeRecordState.DRAFT).timelineButtonStatus,
        )
    }

    @Test
    fun `빈 초안과 기록 없음은 만들기다`() {
        assertEquals(
            DraftCreationStatus.IDLE,
            HomeUiState(selectedRecord = HomeRecordState.EMPTY_DRAFT).timelineButtonStatus,
        )
        assertEquals(DraftCreationStatus.IDLE, HomeUiState().timelineButtonStatus)
    }

    @Test
    fun `추적이 실패로 끝나도 열어 볼 기록이 있으면 확인하기다`() {
        // 하루 넘게 꺼져 있다 켜면 끝난 작업이 서버에서 만료돼 추적은 실패로 끝난다. 기록은 멀쩡하다.
        val state = HomeUiState(draftStatus = DraftCreationStatus.FAILED, selectedRecord = HomeRecordState.DRAFT)

        assertEquals(DraftCreationStatus.SUCCESS, state.timelineButtonStatus)
    }

    @Test
    fun `열어 볼 기록이 있는 날은 만들 것이 없어 입력을 잠근다`() {
        assertTrue(HomeUiState(selectedRecord = HomeRecordState.SAVED).isInputLocked)
        assertFalse(HomeUiState(selectedRecord = HomeRecordState.EMPTY_DRAFT).isInputLocked)
    }

    @Test
    fun `보존 기간은 오늘을 포함해 센다`() {
        val today = LocalDate.of(2026, 9, 13)

        assertTrue(isSelectableRecordDate(today, today, retentionDays = 30))
        assertTrue(isSelectableRecordDate(today.minusDays(29), today, retentionDays = 30))
        assertFalse(isSelectableRecordDate(today.minusDays(30), today, retentionDays = 30))
    }

    @Test
    fun `미래는 보존 기간과 상관없이 고를 수 없다`() {
        val today = LocalDate.of(2026, 9, 13)

        assertFalse(isSelectableRecordDate(today.plusDays(1), today, retentionDays = 365))
        assertFalse(isSelectableRecordDate(today.plusDays(1), today, retentionDays = null))
        assertTrue(isSelectableRecordDate(today.minusYears(3), today, retentionDays = null))
    }
}
