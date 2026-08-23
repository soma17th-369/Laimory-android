package com.soma369.laimory.core.collection.notification

import java.time.Duration
import java.time.Instant

/**
 * 같은 알림이 반복 게시될 때 수집을 억제한다.
 *
 * 앱이 내용을 갱신하며 다시 알리면 `postTime` 이 바뀌어 새 `sourceKey` 가 되므로 갱신 주기마다
 * 한 건씩 쌓인다. 실기기에서 카운트다운 알림 하나가 초당 1건씩 6,126건 저장된 사례가 있다.
 *
 * 상주 알림 제외(`NotificationSignals.isNonEvent`)와 읽을 수 없는 본문 제외가 1차 방어이고, 이
 * 억제는 두 신호가 모두 없거나 OEM 에서 신호가 누락된 갱신형 알림에 대한 안전망이다.
 *
 * 두 축으로 막는다.
 * - [MINIMUM_INTERVAL]: 같은 알림을 다시 수집하기까지 필요한 최소 간격. 초 단위 갱신을 접는다.
 * - [MAX_COLLECTIONS_PER_EPISODE]: 한 **갱신 에피소드**에서 수집할 수 있는 최대 건수. 간격만으로는
 *   "얼마나 자주" 만 보고 "총 몇 건" 은 보지 않아 갱신이 길어질수록 수집량이 선형으로 늘어난다.
 *   위 사례에 간격만 적용하면 6,126건이 147건으로 줄 뿐이라 초안 알림 상한(100건)을 그대로
 *   잠식한다. 건수 상한까지 걸면 10건에서 멈춘다.
 *
 * 에피소드는 **게시가 [EPISODE_IDLE_GAP] 넘게 끊기면** 끝난다. 갱신형 알림은 억제되는 동안에도
 * 계속 게시되므로 에피소드가 이어져 상한에 묶이고, 같은 알림 id 를 재사용하는 정상 앱의 다음
 * 이벤트는 그 사이 조용한 구간이 있어 새 에피소드로 시작한다. 그래서 게시는 수집 여부와 무관하게
 * 모두 관찰하되, 간격·건수 판정은 실제 수집분만 센다.
 *
 * 알림이 제거되면 [forget] 으로 기록을 버린다 — 앱이 같은 id 를 새 이벤트에 재사용하면 그것은
 * 갱신이 아니라 새 알림이다. 이 PR 이 `sourceKey` 병합을 피한 이유와 같은 경계다.
 *
 * 기준 시각은 게시 시각(`postTime`)이 아니라 **수집·관찰 시각**이다 — 갱신형 알림은 `postTime` 을
 * 매번 새로 찍으므로 그것으로는 간격을 재도 항상 새 알림처럼 보인다.
 *
 * 서비스 수명 동안만 유지되는 메모리 상태다. 폭주를 흡수하는 용도라 영속화하지 않으며,
 * 리스너가 다시 붙으면 초기화된다.
 */
internal class NotificationUpdateThrottle(
    private val minimumInterval: Duration = MINIMUM_INTERVAL,
    private val episodeIdleGap: Duration = EPISODE_IDLE_GAP,
    private val maxCollectionsPerEpisode: Int = MAX_COLLECTIONS_PER_EPISODE,
    private val maxEntries: Int = MAX_ENTRIES,
) {
    /** 접근 순서 LRU. 오래된 키부터 밀어내 메모리를 [maxEntries] 로 묶는다. */
    private val states =
        object : LinkedHashMap<String, UpdateState>(INITIAL_CAPACITY, LOAD_FACTOR, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, UpdateState>): Boolean = size > maxEntries
        }

    /**
     * 게시를 관찰하고 이 게시를 억제할지 판정한다.
     *
     * 아직 한 번도 수집되지 않은 알림은 억제하지 않는다 — 게시만으로 간격을 재면 필터에 걸리지
     * 않던 알림이 뒤늦게 조건을 만족했을 때 억제되어 정상 수집을 잃는다.
     *
     * 기기 시간이 뒤로 돌아가 경과가 음수면 억제하지 않는다. 시계 보정 한 번으로 해당 알림이
     * 영영 막히는 편보다 중복 한 건을 받는 편이 낫다.
     *
     * @return 이 게시를 수집하지 않고 버려야 하면 true.
     */
    @Synchronized
    fun onPosted(
        key: String,
        at: Instant,
    ): Boolean {
        val previous = states[key]
        val state =
            if (previous != null && previous.continuesAt(at, episodeIdleGap)) {
                previous.copy(lastPostedAt = at)
            } else {
                UpdateState(lastPostedAt = at, lastCollectedAt = null, collectedInEpisode = 0)
            }
        states[key] = state

        val lastCollectedAt = state.lastCollectedAt ?: return false
        val sinceCollected = Duration.between(lastCollectedAt, at)
        if (sinceCollected.isNegative) return false
        return sinceCollected < minimumInterval || state.collectedInEpisode >= maxCollectionsPerEpisode
    }

    /**
     * 실제로 수집한 알림만 기록한다. [onPosted] 가 같은 시각으로 먼저 호출된 뒤에 부른다.
     *
     * 저장 성공이 아니라 수집을 결정한 시점에 기록한다. 저장이 실패하면 남지도 않은 알림 때문에
     * 이후 [minimumInterval] 이 억제되어 한 건을 잃지만, 저장 완료를 기다렸다가 기록하면 그 사이
     * 도착한 갱신이 억제를 빠져나가 초 단위 갱신에 더 약해진다.
     */
    @Synchronized
    fun onCollected(
        key: String,
        at: Instant,
    ) {
        val previous = states[key]
        states[key] =
            UpdateState(
                lastPostedAt = previous?.lastPostedAt ?: at,
                lastCollectedAt = at,
                collectedInEpisode = (previous?.collectedInEpisode ?: 0) + 1,
            )
    }

    /**
     * 알림이 사라지면 기록을 버린다.
     *
     * 앱이 같은 알림 id 를 새 이벤트에 재사용하면("아침 배송 출발 → 저녁 배송 완료") 제거 뒤의
     * 게시는 갱신이 아니라 새 알림이다. 제거 사유를 가리지 않고 버린다 — 사용자가 지웠든 앱이
     * 취소했든 그 알림의 수명이 끝났다는 사실은 같다.
     */
    @Synchronized
    fun forget(key: String) {
        states.remove(key)
    }

    private data class UpdateState(
        val lastPostedAt: Instant,
        val lastCollectedAt: Instant?,
        val collectedInEpisode: Int,
    ) {
        /** 마지막 게시로부터 [idleGap] 안이면 같은 에피소드가 이어진다. */
        fun continuesAt(
            at: Instant,
            idleGap: Duration,
        ): Boolean {
            val elapsed = Duration.between(lastPostedAt, at)
            return !elapsed.isNegative && elapsed < idleGap
        }
    }

    companion object {
        /**
         * 같은 알림을 다시 수집하기까지 필요한 최소 간격.
         *
         * 초 단위 갱신 같은 비정상 빈도를 접는 값이다. 더 길게 잡으면 몇 분 안에 상태가 바뀌는
         * 배달·주문 알림이 함께 잘린다.
         */
        val MINIMUM_INTERVAL: Duration = Duration.ofSeconds(60)

        /**
         * 이 시간만큼 게시가 끊기면 갱신 에피소드가 끝난 것으로 본다.
         *
         * 갱신형 알림은 억제되는 동안에도 계속 게시되므로 이 간격이 비지 않는다. 반대로 같은
         * 알림 id 를 재사용하는 정상 앱은 다음 이벤트까지 조용하므로 새 에피소드를 얻는다.
         */
        val EPISODE_IDLE_GAP: Duration = Duration.ofMinutes(30)

        /**
         * 한 갱신 에피소드에서 수집할 수 있는 최대 건수.
         *
         * 초안 전송의 알림 타입 상한이 100건이라 그 10% 를 한 알림의 몫으로 둔다. 정상 앱의 한
         * 에피소드(주문 접수 → 조리 시작 → 픽업 → 배달 중 → 완료)는 5건 안팎이라 여유가 있다.
         */
        const val MAX_COLLECTIONS_PER_EPISODE = 10

        private const val MAX_ENTRIES = 256
        private const val INITIAL_CAPACITY = 16
        private const val LOAD_FACTOR = 0.75f
    }
}
