package com.soma369.laimory.core.data.analytics

/**
 * 같은 논리 사건을 한 번만 기록하기 위한 판정 기록.
 *
 * 키는 기기에만 남고 버킷으로 보내지 않는다.
 */
internal interface AnalyticsDedupeStore {
    /** 처음 보는 키면 기록하고 true. 이미 있으면 false. */
    suspend fun markIfFirst(key: String): Boolean

    /** 기록을 지운다. 다음에 같은 키가 오면 다시 "처음" 이다. */
    suspend fun forget(key: String)
}
