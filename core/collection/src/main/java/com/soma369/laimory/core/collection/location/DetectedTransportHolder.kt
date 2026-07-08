package com.soma369.laimory.core.collection.location

import com.soma369.laimory.core.domain.model.collection.MovementPayload
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Activity Recognition 이 감지한 현재 이동수단 공유 홀더.
 *
 * [ActivityTransitionReceiver] 가 최신 전이(ENTER)를 반영하고, [LocationCollectionService] 가 이동(MOVEMENT)
 * 저장 시 읽는다. 감지값이 없거나(UNKNOWN) AR 미허용이면 서비스가 속도 추론(LocationSegmenter) 결과로 폴백한다.
 */
@Singleton
internal class DetectedTransportHolder
    @Inject
    constructor() {
        @Volatile
        private var current: MovementPayload.Transport = MovementPayload.Transport.UNKNOWN

        fun update(transport: MovementPayload.Transport) {
            current = transport
        }

        fun current(): MovementPayload.Transport = current

        fun reset() {
            current = MovementPayload.Transport.UNKNOWN
        }
    }
