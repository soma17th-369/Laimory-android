package com.soma369.laimory.core.collection.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.soma369.laimory.core.domain.model.collection.GeoPoint
import com.soma369.laimory.core.domain.model.collection.LocationPayload
import com.soma369.laimory.core.domain.model.collection.LocationTrackingStatus
import com.soma369.laimory.core.domain.model.collection.MovementPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceName
import com.soma369.laimory.core.domain.usecase.AddSourceItemsUseCase
import com.soma369.laimory.core.util.logging.LogDomain
import com.soma369.laimory.core.util.logging.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LocationManager 위치 업데이트를 [LocationSegmenter] 로 분절해 체류(LOCATION)·이동(MOVEMENT)을 저장한다.
 *
 * Phase 1 은 **앱 foreground 한정**이다. 실제 위치 수신을 앱 프로세스 lifecycle([ProcessLifecycleOwner])에 묶어,
 * 백그라운드로 내려가면([onStop]) 중지하고 foreground 복귀 시([onStart]) 재개한다. 그래서 토글을 켠 채 앱을 벗어나도
 * 백그라운드에서는 수집하지 않는다("foreground-only" 계약·프라이버시 기대와 일치). 토글 의도([isTracking])는 세션
 * 상태로 유지되며(콜드 스타트마다 off) 실제 샘플링만 lifecycle 로 게이팅한다. 백그라운드 지속은 Phase 2 몫이다.
 *
 * 권한(ACCESS_FINE_LOCATION) 확인은 UI 책임이며, 권한 없이 샘플링을 시작하면 SecurityException 을 잡아 무시한다
 * (무동작). 저장은 불변 이벤트라 [AddSourceItemsUseCase](addAll) 로 하며 sourceKey 는 시작 시각 기반이라
 * 재저장이 중복되지 않는다.
 */
@Singleton
internal class LocationTrackingManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val addSourceItemsUseCase: AddSourceItemsUseCase,
    ) : DefaultLifecycleObserver {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val locationManager = context.getSystemService(LocationManager::class.java)
        private var segmenter: LocationSegmenter? = null

        // 사용자 토글 의도와 앱 foreground 여부(둘 다 main 스레드에서만 접근). 실제 샘플링 = 의도 AND foreground.
        private var desiredTracking = false
        private var isForeground = false

        private val _isTracking = MutableStateFlow(false)

        /**
         * 추적 토글 의도(세션 한정). @Singleton 이라 프로세스 생명주기와 함께하며, 콜드 스타트마다 false 로 시작한다.
         * 백그라운드로 내려가도 의도는 유지되어(실제 샘플링만 멈춤) 복귀 시 자동 재개된다.
         */
        val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

        private val _status = MutableStateFlow<LocationTrackingStatus?>(null)

        /** 진행 중 세그먼트의 라이브 상태(체류 중/이동 중). 샘플마다 갱신되고, 샘플링 중지 시 null. */
        val status: StateFlow<LocationTrackingStatus?> = _status.asStateFlow()

        init {
            // ProcessLifecycleOwner 등록은 main 스레드 필수. 등록 시 현재 상태가 replay 되어 onStart 로 isForeground 초기화된다.
            Handler(Looper.getMainLooper()).post {
                ProcessLifecycleOwner.get().lifecycle.addObserver(this)
            }
        }

        private val listener =
            object : LocationListener {
                override fun onLocationChanged(location: Location) = onLocation(location)

                override fun onProviderEnabled(provider: String) = Unit

                override fun onProviderDisabled(provider: String) = Unit

                @Deprecated("구버전 호환용 빈 구현")
                override fun onStatusChanged(
                    provider: String?,
                    status: Int,
                    extras: Bundle?,
                ) = Unit
            }

        /** 토글 ON — 추적 의도를 세우고, foreground 면 즉시 샘플링을 시작한다. */
        fun start() {
            if (desiredTracking) return
            desiredTracking = true
            _isTracking.value = true
            if (isForeground) beginSampling()
        }

        /** 토글 OFF — 추적 의도를 내리고 샘플링을 중지한다. */
        fun stop() {
            if (!desiredTracking) return
            desiredTracking = false
            _isTracking.value = false
            endSampling()
        }

        override fun onStart(owner: LifecycleOwner) {
            isForeground = true
            if (desiredTracking) beginSampling()
        }

        override fun onStop(owner: LifecycleOwner) {
            isForeground = false
            endSampling()
        }

        @SuppressLint("MissingPermission")
        private fun beginSampling() {
            if (segmenter != null) return
            val manager = locationManager ?: return
            segmenter = LocationSegmenter()
            val provider =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) LocationManager.FUSED_PROVIDER else LocationManager.GPS_PROVIDER
            runCatching {
                manager.requestLocationUpdates(provider, MIN_TIME_MS, MIN_DISTANCE_M, listener, Looper.getMainLooper())
            }.onFailure { e ->
                // 권한 미허용은 SecurityException — 계약대로 무동작.
                Logger.w(LogDomain.COLLECTION, "위치 업데이트 시작 실패: ${e.message}")
                segmenter = null
            }
        }

        private fun endSampling() {
            val active = segmenter ?: return
            locationManager?.removeUpdates(listener)
            segmenter = null
            _status.value = null
            val remaining = active.flush()
            if (remaining.isNotEmpty()) saveEvents(remaining)
        }

        private fun onLocation(location: Location) {
            val active = segmenter ?: return
            val events = active.onSample(location.latitude, location.longitude, location.time)
            _status.value = active.currentStatus(location.time)
            if (events.isNotEmpty()) saveEvents(events)
        }

        private fun saveEvents(events: List<DetectedEvent>) {
            val collectedAt = Instant.now()
            val zone = ZoneId.systemDefault()
            val items = events.map { it.toSourceItem(collectedAt, zone) }
            scope.launch {
                runCatching { addSourceItemsUseCase(items) }
                    .onFailure { e -> Logger.w(LogDomain.COLLECTION, "위치 이벤트 저장 실패: ${e.message}") }
            }
        }

        private fun DetectedEvent.toSourceItem(
            collectedAt: Instant,
            zone: ZoneId,
        ): SourceItem =
            when (this) {
                is DetectedEvent.Dwell ->
                    SourceItem(
                        rawId = UUID.randomUUID().toString(),
                        startAt = Instant.ofEpochMilli(startMillis),
                        endAt = Instant.ofEpochMilli(endMillis),
                        timeZoneId = zone,
                        payload = LocationPayload(latitude = latitude, longitude = longitude),
                        sourceName = SourceName.LOCATION_PROVIDER,
                        sourceKey = "LOCATION:$startMillis",
                        collectedAt = collectedAt,
                    )

                is DetectedEvent.Move ->
                    SourceItem(
                        rawId = UUID.randomUUID().toString(),
                        startAt = Instant.ofEpochMilli(startMillis),
                        endAt = Instant.ofEpochMilli(endMillis),
                        timeZoneId = zone,
                        payload =
                            MovementPayload(
                                start = GeoPoint(startLatitude, startLongitude),
                                end = GeoPoint(endLatitude, endLongitude),
                                distanceMeters = distanceMeters,
                                transports = transport,
                            ),
                        sourceName = SourceName.LOCATION_PROVIDER,
                        sourceKey = "MOVEMENT:$startMillis",
                        collectedAt = collectedAt,
                    )
            }

        private companion object {
            const val MIN_TIME_MS = 30_000L

            // 0 = 이동 여부와 무관하게 주기적으로 위치를 받는다. 20m 이상 이동해야만 업데이트가 오면
            // 정지 중엔 샘플이 끊겨 체류 시간이 안 쌓인다.
            const val MIN_DISTANCE_M = 0f
        }
    }
