package com.soma369.laimory.core.data.model.timeline.request

import com.soma369.laimory.core.domain.model.timeline.TimelineEventPhotoAddition
import kotlinx.serialization.Serializable

/**
 * Event 에 붙일 업로드 완료 PHOTO. 생성·수정이 같은 모양을 쓴다.
 *
 * 값이 없는 키는 전역 `explicitNulls = false` 가 지워 준다 — 서버가 `startAt`·`endAt`·좌표를
 * optional 로 받으므로 그 동작이 계약과 맞는다.
 */
@Serializable
data class TimelineEventPhotoRequest(
    val rawId: String,
    val startAt: String? = null,
    val endAt: String? = null,
    val payload: Payload,
) {
    @Serializable
    data class Payload(
        val filename: String,
        val clientPhotoUri: String,
        val latitude: Double? = null,
        val longitude: Double? = null,
    )
}

fun TimelineEventPhotoAddition.toRequest() =
    TimelineEventPhotoRequest(
        rawId = rawId,
        startAt = startAt?.toApiDateTime(),
        endAt = endAt?.toApiDateTime(),
        payload =
            TimelineEventPhotoRequest.Payload(
                filename = filename,
                clientPhotoUri = clientPhotoUri,
                latitude = latitude,
                longitude = longitude,
            ),
    )
