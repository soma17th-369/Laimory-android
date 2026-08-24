package com.soma369.laimory.feature.home.state

import androidx.compose.runtime.Immutable

/** 유형 상세에서 보여주는 실제 전송 항목 1건의 표시 모델. */
@Immutable
data class DraftConsentDetailItem(
    val key: String,
    val title: String,
    val description: String?,
    val timeText: String,
    /** PHOTO 항목의 썸네일 URI. 다른 유형은 null. */
    val imageUri: String? = null,
    /** 시각 줄 오른쪽 끝에 붙이는 보조 표기. MOVEMENT 의 이동 거리에 쓴다. 다른 유형은 null. */
    val trailingText: String? = null,
)
