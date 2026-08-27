package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.model.timeline.request.SaveDailyRecordRequest
import com.soma369.laimory.core.data.model.timeline.request.UpdateDailyRecordEmotionRequest
import com.soma369.laimory.core.data.model.timeline.request.UpdateTimelineEventMemoRequest
import com.soma369.laimory.core.data.model.timeline.request.toRequestJson
import com.soma369.laimory.core.data.model.timeline.response.DailyTimelineListResponse
import com.soma369.laimory.core.data.model.timeline.response.DailyTimelineResponse
import com.soma369.laimory.core.data.model.timeline.response.MonthlyDailyRecordListResponse
import com.soma369.laimory.core.data.model.timeline.response.TimelineEventResponse
import com.soma369.laimory.core.data.network.api.TimelineRecordApi
import com.soma369.laimory.core.data.network.safeApiCall
import com.soma369.laimory.core.data.network.safeApiCallUnit
import com.soma369.laimory.core.domain.model.timeline.CreateTimelineEventCommand
import com.soma369.laimory.core.domain.model.timeline.TimelineEmotion
import kotlinx.serialization.json.JsonObject
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

class TimelineRecordRemoteDataSourceImpl
    @Inject
    constructor(
        private val api: TimelineRecordApi,
    ) : TimelineRecordRemoteDataSource {
        override suspend fun getDailyRecords(): DailyTimelineListResponse = safeApiCall { api.getDailyRecords() }

        override suspend fun getMonthlyDailyRecords(month: YearMonth): MonthlyDailyRecordListResponse =
            safeApiCall { api.getMonthlyDailyRecords(month.year, month.monthValue) }

        override suspend fun getDailyRecord(recordDate: LocalDate): DailyTimelineResponse =
            safeApiCall { api.getDailyRecord(recordDate.toString()) }

        override suspend fun getTimelineEvent(timelineEventId: Long): TimelineEventResponse =
            safeApiCall { api.getTimelineEvent(timelineEventId) }

        override suspend fun updateTimelineEvent(
            timelineEventId: Long,
            request: JsonObject,
        ) {
            safeApiCallUnit { api.updateTimelineEvent(timelineEventId, request) }
        }

        override suspend fun updateTimelineEventMemo(
            timelineEventId: Long,
            request: UpdateTimelineEventMemoRequest,
        ) {
            safeApiCallUnit { api.updateTimelineEventMemo(timelineEventId, request) }
        }

        override suspend fun deleteTimelineEvent(timelineEventId: Long) {
            safeApiCallUnit { api.deleteTimelineEvent(timelineEventId) }
        }

        override suspend fun deleteTimelineEventPhoto(
            timelineEventId: Long,
            timelineItemId: Long,
        ) {
            safeApiCallUnit { api.deleteTimelineEventPhoto(timelineEventId, timelineItemId) }
        }

        override suspend fun deleteDailyRecord(recordDate: LocalDate) {
            safeApiCallUnit { api.deleteDailyRecord(recordDate.toString()) }
        }

        override suspend fun createTimelineEvent(command: CreateTimelineEventCommand): TimelineEventResponse =
            safeApiCall { api.createTimelineEvent(command.recordDate.toString(), command.toRequestJson()) }

        override suspend fun updateDailyRecordEmotion(
            recordDate: LocalDate,
            emotion: TimelineEmotion,
        ) {
            val request = UpdateDailyRecordEmotionRequest(emotionType = emotion.toRequestLiteral())
            safeApiCallUnit { api.updateDailyRecordEmotion(recordDate.toString(), request) }
        }

        override suspend fun saveDailyRecord(
            recordDate: LocalDate,
            emotion: TimelineEmotion,
        ) {
            val request = SaveDailyRecordRequest(emotionType = emotion.toRequestLiteral())
            safeApiCallUnit { api.saveDailyRecord(recordDate.toString(), request) }
        }
    }

/**
 * 도메인 감정을 서버 literal 로 옮긴다. 작성 완료와 감정 교체가 같은 어휘를 쓴다.
 *
 * 이름이 서버 계약과 1:1이지만 [TimelineEmotion.UNKNOWN] 은 조회에서 모르는 값을 수렴시키는 표시 상태라
 * 저장 요청에 실리면 안 된다. `when` 으로 열어 두어 새 감정이 추가되면 여기서 컴파일이 깨지게 한다.
 */
private fun TimelineEmotion.toRequestLiteral(): String =
    when (this) {
        TimelineEmotion.VERY_HAPPY -> "VERY_HAPPY"
        TimelineEmotion.HAPPY -> "HAPPY"
        TimelineEmotion.NEUTRAL -> "NEUTRAL"
        TimelineEmotion.UNHAPPY -> "UNHAPPY"
        TimelineEmotion.VERY_UNHAPPY -> "VERY_UNHAPPY"
        TimelineEmotion.UNKNOWN -> throw IllegalArgumentException("저장할 수 없는 감정입니다: $this")
    }
