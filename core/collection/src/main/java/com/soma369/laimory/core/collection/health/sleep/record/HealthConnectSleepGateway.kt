package com.soma369.laimory.core.collection.health.sleep.record

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject

/**
 * [SleepHealthGateway] 의 Health Connect 구현.
 *
 * 감지분은 autoRecorded, 수동 입력분은 manualEntry 로 recordingMethod 를 구분해 쓴다.
 * clientRecordId 를 실어 같은 밤 재기록 시 HC 가 새 세션을 추가하지 않고 기존 것을 교체하게 한다.
 * (권한/설치 판정은 호출 정책과 UI 계층의 책임이며, 여기서는 raw read/write/delete 만 다룬다.)
 */
internal class HealthConnectSleepGateway
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : SleepHealthGateway {
        private fun client(): HealthConnectClient = HealthConnectClient.getOrCreate(context)

        override suspend fun upsert(session: SleepWrite) {
            val device = Device(type = Device.TYPE_PHONE)
            val metadata =
                when (session.recordingMethod) {
                    SleepRecordingMethod.AUTO_DETECTED ->
                        Metadata.autoRecorded(device, session.clientRecordId, session.version)
                    SleepRecordingMethod.MANUAL ->
                        Metadata.manualEntry(session.clientRecordId, session.version, device)
                }
            val record =
                SleepSessionRecord(
                    startTime = session.start,
                    startZoneOffset = session.zoneOffset,
                    endTime = session.end,
                    endZoneOffset = session.zoneOffset,
                    metadata = metadata,
                )
            client().insertRecords(listOf(record))
        }

        override suspend fun deleteByClientRecordId(clientRecordId: String) {
            client().deleteRecords(
                recordType = SleepSessionRecord::class,
                clientRecordIdsList = listOf(clientRecordId),
                recordIdsList = emptyList(),
            )
        }

        override suspend fun read(
            start: Instant,
            end: Instant,
        ): List<StoredSleepSession> =
            client()
                .readRecords(
                    ReadRecordsRequest(
                        recordType = SleepSessionRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(start, end),
                    ),
                ).records
                .map { record ->
                    StoredSleepSession(
                        start = record.startTime,
                        end = record.endTime,
                        clientRecordId = record.metadata.clientRecordId,
                        originPackageName = record.metadata.dataOrigin.packageName,
                    )
                }
    }
