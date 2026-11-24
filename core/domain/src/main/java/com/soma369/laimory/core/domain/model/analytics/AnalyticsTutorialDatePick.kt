package com.soma369.laimory.core.domain.model.analytics

/**
 * 추천한 날을 고른 내용.
 *
 * @property rank 고른 카드의 추천 순위. 1부터.
 */
data class AnalyticsTutorialDatePick(
    val rank: Int,
    val method: AnalyticsTutorialPickMethod,
)
