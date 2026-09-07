package com.soma369.laimory.core.data.model.timeline.response

import com.soma369.laimory.core.domain.model.timeline.DailyRecordStatus
import com.soma369.laimory.core.domain.model.timeline.MonthlyDailyRecord
import com.soma369.laimory.core.domain.model.timeline.TimelineEmotion
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class MonthlyDailyRecordResponseTest {
    @OptIn(ExperimentalSerializationApi::class)
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            coerceInputValues = true
        }

    private fun decode(body: String): MonthlyDailyRecord = json.decodeFromString<MonthlyDailyRecordResponse>(body).toDomain()

    @Test
    fun `status는 DRAFT와 SAVED만 도메인 상태로 매핑한다`() {
        assertEquals(
            DailyRecordStatus.DRAFT,
            decode("""{"recordDate":"2026-05-03","status":"DRAFT","emotionType":"VERY_HAPPY"}""").status,
        )
        assertEquals(
            DailyRecordStatus.SAVED,
            decode("""{"recordDate":"2026-05-03","status":"SAVED"}""").status,
        )
    }

    @Test
    fun `status 가 없거나 모르는 값이면 null 로 수렴한다`() {
        // 서버 배포가 앞서거나 뒤설 때 앱이 터지지 않아야 한다. 화면은 null 을 초안이 아닌 것으로
        // 다뤄, 모르는 값 때문에 이미 있는 기록 위에 새 초안을 만들지 않는다.
        assertNull(decode("""{"recordDate":"2026-05-03"}""").status)
        assertNull(decode("""{"recordDate":"2026-05-03","status":"ARCHIVED"}""").status)
    }

    @Test
    fun `날짜와 감정은 그대로 옮긴다`() {
        val record = decode("""{"recordDate":"2026-05-03","status":"DRAFT","emotionType":"VERY_HAPPY"}""")

        assertEquals(LocalDate.of(2026, 5, 3), record.recordDate)
        assertEquals(TimelineEmotion.VERY_HAPPY, record.emotion)
    }

    @Test
    fun `모르는 감정 literal 은 UNKNOWN 으로 남는다`() {
        assertEquals(
            TimelineEmotion.UNKNOWN,
            decode("""{"recordDate":"2026-05-03","status":"SAVED","emotionType":"ECSTATIC"}""").emotion,
        )
    }
}
