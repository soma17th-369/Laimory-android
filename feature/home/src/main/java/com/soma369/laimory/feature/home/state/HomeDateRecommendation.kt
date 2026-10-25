package com.soma369.laimory.feature.home.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.tutorial.RecommendedRecordDate

/**
 * 홈 튜토리얼의 추천 날짜 시트. 홈에 처음 들어왔을 때 한 번 뜬다.
 *
 * [dates] 가 비어도 띄운다 — 사진 권한이 없거나 최근 한 달에 찍은 사진이 없으면 추천 없이 `다른 날 직접 고르기` 와
 * `건너뛰기` 만 남는다. 추천은 날짜까지만 한다. 사진은 홈 사진 카드에서 사용자가 고른다.
 */
@Immutable
data class HomeDateRecommendation(
    val dates: List<RecommendedRecordDate>,
)
