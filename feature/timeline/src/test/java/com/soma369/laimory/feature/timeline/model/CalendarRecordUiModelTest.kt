package com.soma369.laimory.feature.timeline.model

import com.soma369.laimory.core.domain.model.timeline.MonthlyDailyRecord
import com.soma369.laimory.core.domain.model.timeline.TimelineEmotion
import com.soma369.laimory.core.ui.theme.Emotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CalendarRecordUiModelTest {
    @Test
    fun `감정 literal 5종을 표시 팔레트로 옮긴다`() {
        val expected =
            mapOf(
                TimelineEmotion.VERY_HAPPY to Emotion.JOY,
                TimelineEmotion.HAPPY to Emotion.CALM,
                TimelineEmotion.NEUTRAL to Emotion.MELLOW,
                TimelineEmotion.UNHAPPY to Emotion.WEARY,
                TimelineEmotion.VERY_UNHAPPY to Emotion.DOWN,
            )

        expected.entries.forEachIndexed { index, (emotion, uiEmotion) ->
            val date = LocalDate.of(2026, 5, index + 1)
            val records = listOf(record(date = date, emotion = emotion)).toCalendarRecordsByDate()

            assertEquals(uiEmotion, records.getValue(date).emotion)
        }
    }

    @Test
    fun `감정이 없거나 미지 literal 이면 중립으로 내려간다`() {
        val unknownDate = LocalDate.of(2026, 5, 10)
        val missingDate = LocalDate.of(2026, 5, 11)

        val records =
            listOf(
                record(date = unknownDate, emotion = TimelineEmotion.UNKNOWN),
                record(date = missingDate, emotion = null),
            ).toCalendarRecordsByDate()

        assertNull(records.getValue(unknownDate).emotion)
        assertNull(records.getValue(missingDate).emotion)
        // 감정이 없어도 "기록 없음"과는 다르다 — 키 자체는 존재해야 한다.
        assertTrue(records.containsKey(unknownDate))
        assertTrue(records.containsKey(missingDate))
    }

    @Test
    fun `기록이 없는 월은 빈 맵이 된다`() {
        assertTrue(emptyList<MonthlyDailyRecord>().toCalendarRecordsByDate().isEmpty())
    }

    @Test
    fun `같은 월의 여러 날짜가 각각 키로 남는다`() {
        val records =
            listOf(
                record(date = LocalDate.of(2026, 5, 1), emotion = TimelineEmotion.HAPPY),
                record(date = LocalDate.of(2026, 5, 2), emotion = null),
                record(date = LocalDate.of(2026, 5, 3), emotion = TimelineEmotion.VERY_UNHAPPY),
            ).toCalendarRecordsByDate()

        assertEquals(3, records.size)
    }

    private fun record(
        date: LocalDate,
        emotion: TimelineEmotion?,
    ) = MonthlyDailyRecord(recordDate = date, emotion = emotion)
}
