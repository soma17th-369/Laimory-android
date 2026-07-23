package com.soma369.laimory.core.domain.model.timeline

import java.time.LocalDateTime

/** Event PATCH로 append할 업로드 완료 PHOTO 정보. */
data class TimelineEventPhotoAddition(
    val rawId: String,
    val startAt: LocalDateTime?,
    val endAt: LocalDateTime?,
    val filename: String,
    val clientPhotoUri: String,
    val latitude: Double?,
    val longitude: Double?,
)
