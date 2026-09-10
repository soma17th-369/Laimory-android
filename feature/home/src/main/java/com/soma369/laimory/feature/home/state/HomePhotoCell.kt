package com.soma369.laimory.feature.home.state

import androidx.compose.runtime.Immutable

/**
 * 사진 카드 격자의 칸 하나.
 *
 * 고른 사진을 **앞세우고 표시한다.** 후보 최신순만 쓰면 여섯 칸에 고른 사진이 한 장도 안 보일 수
 * 있어, 카드가 "무엇을 보내는지" 를 말하지 못한다.
 */
@Immutable
data class HomePhotoCell(
    val uri: String,
    val isSelected: Boolean,
)
