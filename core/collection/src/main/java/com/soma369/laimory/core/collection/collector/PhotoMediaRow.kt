package com.soma369.laimory.core.collection.collector

import com.soma369.laimory.core.domain.model.collection.PhotoPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceName
import java.time.Instant
import java.time.ZoneId

/**
 * MediaStore 사진 한 행의 원시 값. Android `Cursor` 읽기와 [SourceItem] 변환을 분리하기 위한 중간 모델이다.
 *
 * `Cursor` 자체는 Android 의존이라 단위 테스트가 어렵다. 수집기가 `Cursor` → [PhotoMediaRow] 까지만
 * Android 영역에서 처리하고, [toSourceItem] 은 순수 함수로 두어 시각 매핑/skip 규칙을 JVM 테스트로 검증한다.
 *
 * - [clientPhotoUri]: `ContentUris.withAppendedId(...)` 결과 문자열. URI 조립은 Android 영역(수집기)에서 하고
 *   여기서는 완성된 문자열만 받는다.
 * - [dateTakenMillis]: `DATE_TAKEN` (epoch millis). 없거나 0 이면 null 로 담는다.
 * - [dateAddedSeconds]: `DATE_ADDED` (epoch seconds). `DATE_TAKEN` 부재 시 fallback.
 * - [latitude]/[longitude]: EXIF GPS 좌표. 수집기가 원본 파일 EXIF 에서 읽어 담으며, 없거나 읽기 실패 시 null.
 */
internal data class PhotoMediaRow(
    val id: Long,
    val displayName: String,
    val clientPhotoUri: String,
    val dateTakenMillis: Long?,
    val dateAddedSeconds: Long?,
    val latitude: Double?,
    val longitude: Double?,
)

/**
 * MediaStore 행을 저장용 [SourceItem] 으로 변환한다.
 *
 * `startAt` 은 [PhotoMediaRow.dateTakenMillis] 우선, 없으면 [PhotoMediaRow.dateAddedSeconds]*1000 으로 정한다.
 * 둘 다 유효하지 않으면 시각을 신뢰할 수 없으므로 null 을 반환해 저장에서 제외한다.
 *
 * 위치([latitude]/[longitude])는 수집기가 EXIF 에서 읽어 넘긴 값을 그대로 담는다(없으면 null).
 * description 은 필드를 빼는 게 아니라 값만 null 로 채워 넘긴다 — 후처리/분석이 채우는 필드라
 * 수집 시점 값이 없음을 null 로 표현한다.
 */
internal fun PhotoMediaRow.toSourceItem(
    rawId: String,
    collectedAt: Instant,
    zoneId: ZoneId,
): SourceItem? {
    val startAtMillis =
        when {
            dateTakenMillis != null && dateTakenMillis > 0L -> dateTakenMillis
            dateAddedSeconds != null && dateAddedSeconds > 0L -> dateAddedSeconds * MILLIS_PER_SECOND
            else -> return null
        }

    return SourceItem(
        rawId = rawId,
        startAt = Instant.ofEpochMilli(startAtMillis),
        endAt = null,
        timeZoneId = zoneId,
        payload =
            PhotoPayload(
                fileName = displayName,
                clientPhotoUri = clientPhotoUri,
                latitude = latitude,
                longitude = longitude,
                description = null,
            ),
        sourceName = SourceName.MEDIA_STORE,
        sourceKey = id.toString(),
        collectedAt = collectedAt,
    )
}

private const val MILLIS_PER_SECOND = 1_000L
