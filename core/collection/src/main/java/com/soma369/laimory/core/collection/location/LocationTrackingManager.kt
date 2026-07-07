package com.soma369.laimory.core.collection.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import com.soma369.laimory.core.domain.model.collection.GeoPoint
import com.soma369.laimory.core.domain.model.collection.LocationPayload
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
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LocationManager 위치 업데이트를 [LocationSegmenter] 로 분절해 체류(LOCATION)·이동(MOVEMENT)을 저장한다.
 *
 * Phase 1 은 foreground 한정 — 앱이 살아있는 동안만 수신한다(백그라운드 지속은 후속). 권한(ACCESS_FINE_LOCATION)
 * 확인은 UI 책임이며, 권한 없이 [start] 하면 SecurityException 을 잡아 무시한다(무동작). 저장은 불변 이벤트라
 * [AddSourceItemsUseCase](addAll) 로 하며 sourceKey 는 시작 시각 기반이라 재저장이 중복되지 않는다.
 */
@Singleton
internal class LocationTrackingManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val addSourceItemsUseCase: AddSourceItemsUseCase,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val locationManager = context.getSystemService(LocationManager::class.java)
        private var segmenter: LocationSegmenter? = null

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

        @SuppressLint("MissingPermission")
        fun start() {
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

        fun stop() {
            val active = segmenter ?: return
            locationManager?.removeUpdates(listener)
            segmenter = null
            val remaining = active.flush()
            if (remaining.isNotEmpty()) saveEvents(remaining)
        }

        private fun onLocation(location: Location) {
            val active = segmenter ?: return
            val events = active.onSample(location.latitude, location.longitude, location.time)
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
            const val MIN_TIME_MS = 15_000L
            const val MIN_DISTANCE_M = 20f
        }
    }
