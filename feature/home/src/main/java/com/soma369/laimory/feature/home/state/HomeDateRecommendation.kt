package com.soma369.laimory.feature.home.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.tutorial.RecommendedRecordDate

/**
 * 홈 튜토리얼의 추천 날짜 시트. 홈에 처음 들어왔을 때 한 번 뜬다.
 *
 * [dates] 가 비어도 띄운다 — 최근 한 달에 찍은 사진이 없으면 추천 없이 `다른 날 직접 고르기` 와 `건너뛰기` 만 남는다.
 * 사진을 못 읽어 비었으면([photoAccess]) 허용을 권하고, 허용하고 돌아오면 그 자리에서 다시 계산한다. 추천은 날짜까지만
 * 한다. 사진은 홈 사진 카드에서 사용자가 고른다.
 */
@Immutable
data class HomeDateRecommendation(
    val dates: List<RecommendedRecordDate>,
    val photoAccess: HomeRecommendationPhotoAccess = HomeRecommendationPhotoAccess.FULL,
) {
    /** 추천이 비었는데 사진을 더 읽을 수 있으면 권할 허용. 추천이 있거나 전체를 읽고도 비었으면 없다. */
    val suggestedPhotoAccess: HomeRecommendationPhotoAccess?
        get() = photoAccess.takeIf { dates.isEmpty() && it != HomeRecommendationPhotoAccess.FULL }
}
