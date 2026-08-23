package com.soma369.laimory.core.collection.notification

import java.time.Duration
import java.time.Instant

/**
 * 같은 알림이 짧은 간격으로 반복 게시될 때 수집을 억제한다.
 *
 * 앱이 내용을 갱신하며 다시 알리면 `postTime` 이 바뀌어 새 `sourceKey` 가 되므로 갱신 주기마다
 * 한 건씩 쌓인다. 실기기에서 카운트다운 알림 하나가 초당 1건씩 6,126건 저장된 사례가 있다.
 *
 * 상주 알림([NotificationSignals.isNonEvent])과 읽을 수 없는 본문 제외가 1차 방어이고, 이 억제는
 * 두 신호가 모두 없는 갱신형 알림에 대한 안전망이다. 정상 앱의 상태 전이 알림(주문 접수 →
 * 조리 시작 → 배달 시작)은 보통 분 단위 이상 간격이라 [MINIMUM_INTERVAL] 을 넘겨 살아남는다.
 *
 * 서비스 수명 동안만 유지되는 메모리 상태다. 폭주를 흡수하는 용도라 영속화하지 않으며,
 * 리스너가 다시 붙으면 초기화된다.
 *
 * 기준 시각은 게시 시각(`postTime`)이 아니라 **수집 시각**이다 — 갱신형 알림은 `postTime` 을
 * 매번 새로 찍으므로 그것으로는 간격을 재도 항상 새 알림처럼 보인다.
 */
internal class NotificationUpdateThrottle(
    private val minimumInterval: Duration = MINIMUM_INTERVAL,
    private val maxEntries: Int = MAX_ENTRIES,
) {
    /** 접근 순서 LRU. 오래된 키부터 밀어내 메모리를 [maxEntries] 로 묶는다. */
    private val lastCollectedAt =
        object : LinkedHashMap<String, Instant>(INITIAL_CAPACITY, LOAD_FACTOR, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Instant>): Boolean = size > maxEntries
        }

    /**
     * 마지막으로 수집한 시각에서 [minimumInterval] 이 지나지 않았는지 본다.
     *
     * 기기 시간이 뒤로 돌아가 경과가 음수면 억제하지 않는다 — 시계 보정 한 번으로 해당 알림이
     * 영영 막히는 편보다 중복 한 건을 받는 편이 낫다.
     */
    @Synchronized
    fun isThrottled(
        key: String,
        at: Instant,
    ): Boolean {
        val last = lastCollectedAt[key] ?: return false
        val elapsed = Duration.between(last, at)
        return !elapsed.isNegative && elapsed < minimumInterval
    }

    /**
     * 실제로 수집한 알림만 기록한다.
     *
     * 게시됐지만 수집되지 않은 알림까지 기록하면, 필터에 걸리지 않던 알림이 뒤늦게 조건을
     * 만족했을 때 억제되어 정상 수집을 잃는다.
     */
    @Synchronized
    fun markCollected(
        key: String,
        at: Instant,
    ) {
        lastCollectedAt[key] = at
    }

    companion object {
        /**
         * 같은 알림을 다시 수집하기까지 필요한 최소 간격.
         *
         * 초 단위 갱신 같은 비정상 빈도만 흡수하는 값이다. 더 길게 잡으면 몇 분 안에 상태가
         * 바뀌는 배달·주문 알림이 함께 잘린다.
         */
        val MINIMUM_INTERVAL: Duration = Duration.ofSeconds(60)

        private const val MAX_ENTRIES = 256
        private const val INITIAL_CAPACITY = 16
        private const val LOAD_FACTOR = 0.75f
    }
}
