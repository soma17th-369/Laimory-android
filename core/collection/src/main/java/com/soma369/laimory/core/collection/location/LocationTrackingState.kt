package com.soma369.laimory.core.collection.location

import com.soma369.laimory.core.domain.model.collection.LocationTrackingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 위치 수집 서비스가 쓰고 UI 가 읽는 공유 라이브 상태 홀더.
 *
 * FGS([LocationCollectionService]) 인스턴스는 시스템이 생성하지만, @Singleton 홀더를 주입받아 진행 중 세그먼트의
 * 라이브 상태(체류 중/이동 중)를 여기 반영한다. 리포지토리는 이 [status] 를 그대로 노출한다.
 */
@Singleton
internal class LocationTrackingState
    @Inject
    constructor() {
        private val _status = MutableStateFlow<LocationTrackingStatus?>(null)

        /** 진행 중 세그먼트의 라이브 상태(체류 중/이동 중). 샘플마다 갱신, 수집 중지 시 null. */
        val status: StateFlow<LocationTrackingStatus?> = _status.asStateFlow()

        fun update(status: LocationTrackingStatus?) {
            _status.value = status
        }
    }
