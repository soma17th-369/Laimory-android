package com.soma369.laimory.feature.timeline.model

import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
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
            val records = listOf(timeline(id = index.toLong(), date = date, emotion = emotion)).toCalendarRecordsByDate()

            assertEquals(uiEmotion, records.getValue(date).emotion)
        }
    }

    @Test
    fun `감정이 없거나 미지 literal 이면 중립으로 내려간다`() {
        val unknownDate = LocalDate.of(2026, 5, 10)
        val missingDate = LocalDate.of(2026, 5, 11)

        val records =
            listOf(
                timeline(id = 1L, date = unknownDate, emotion = TimelineEmotion.UNKNOWN),
                timeline(id = 2L, date = missingDate, emotion = null),
            ).toCalendarRecordsByDate()

        assertNull(records.getValue(unknownDate).emotion)
        assertNull(records.getValue(missingDate).emotion)
        // 감정이 없어도 "기록 없음"과는 다르다 — 키 자체는 존재해야 한다.
        assertTrue(records.containsKey(unknownDate))
        assertTrue(records.containsKey(missingDate))
    }

    @Test
    fun `같은 날짜가 중복되면 서버 정렬상 첫 번째 기록을 대표로 남긴다`() {
        val date = LocalDate.of(2026, 5, 20)

        val records =
            listOf(
                timeline(id = 1L, date = date, emotion = TimelineEmotion.VERY_HAPPY),
                timeline(id = 2L, date = date, emotion = TimelineEmotion.VERY_UNHAPPY),
                timeline(id = 3L, date = date, emotion = TimelineEmotion.NEUTRAL),
            ).toCalendarRecordsByDate()

        assertEquals(1, records.size)
        assertEquals(Emotion.JOY, records.getValue(date).emotion)
    }

    @Test
    fun `빈 목록은 빈 맵이 된다`() {
        assertTrue(emptyList<DailyTimeline>().toCalendarRecordsByDate().isEmpty())
    }

    private fun timeline(
        id: Long,
        date: LocalDate,
        emotion: TimelineEmotion?,
    ) = DailyTimeline(
        dailyRecordId = id,
        recordDate = date,
        emotion = emotion,
        events = emptyList(),
    )
}
