package com.soma369.laimory.core.collection.location

import com.soma369.laimory.core.domain.model.collection.MovementPayload
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Activity Recognition 이 감지한 이동수단 전이를 이동 구간 동안 누적하는 공유 홀더.
 *
 * [ActivityTransitionReceiver] 가 활동 진입(ENTER) 이벤트를 [onEnter] 로 쌓고, [LocationCollectionService] 가
 * 이동(MOVEMENT) 저장 시 [dominant] 로 **시간 기준 최다 이동수단**을 고른다(마감 직전 전이 하나로 결정되는 문제 회피).
 * 저장 후 [reset] 으로 다음 구간을 새로 집계한다. 감지값이 없으면 서비스가 속도 추론으로 폴백한다.
 *
 * 리시버(바인더 스레드)와 서비스(IO)가 동시에 접근하므로 [lock] 으로 보호한다.
 */
@Singleton
internal class DetectedTransportHolder
    @Inject
    constructor() {
        private val lock = Any()

        // (transport, enterElapsedRealtimeMillis) 전이 시퀀스.
        private val events = ArrayList<Pair<MovementPayload.Transport, Long>>()

        fun onEnter(
            transport: MovementPayload.Transport,
            atMillis: Long,
        ) {
            synchronized(lock) { events.add(MovementTransportClassifier.normalize(transport) to atMillis) }
        }

        /**
         * 누적 전이에서 시간 기준 dominant 이동수단. 각 전이는 다음 전이(또는 [nowMillis])까지 지속으로 보고,
         * 신규 정책의 이동수단(WALKING/IN_VEHICLE)만 집계한다. 감지값이 없으면 UNKNOWN.
         */
        fun dominant(nowMillis: Long): MovementPayload.Transport =
            synchronized(lock) {
                if (events.isEmpty()) return MovementPayload.Transport.UNKNOWN
                val durations = HashMap<MovementPayload.Transport, Long>()
                events.forEachIndexed { index, (transport, start) ->
                    val end = if (index + 1 < events.size) events[index + 1].second else nowMillis
                    if (transport != MovementPayload.Transport.UNKNOWN) {
                        durations[transport] = (durations[transport] ?: 0L) + (end - start).coerceAtLeast(0L)
                    }
                }
                durations.maxByOrNull { it.value }?.key ?: MovementPayload.Transport.UNKNOWN
            }

        fun reset() {
            synchronized(lock) { events.clear() }
        }
    }
