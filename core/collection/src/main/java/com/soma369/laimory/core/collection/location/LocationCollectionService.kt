package com.soma369.laimory.core.collection.location

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import com.soma369.laimory.core.domain.model.collection.GeoPoint
import com.soma369.laimory.core.domain.model.collection.LocationTrackingStatus
import com.soma369.laimory.core.domain.model.collection.MovementPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceName
import com.soma369.laimory.core.domain.model.collection.StayPayload
import com.soma369.laimory.core.domain.usecase.AddSourceItemsUseCase
import com.soma369.laimory.core.util.logging.LogDomain
import com.soma369.laimory.core.util.logging.Logger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

/**
 * 위치 자동 수집 Foreground Service(Phase 2). LocationManager 업데이트를 [LocationSegmenter] 로 분절해
 * 체류(STAY)·이동(MOVEMENT)을 저장하며, 앱이 백그라운드여도 상시 알림과 함께 지속한다.
 *
 * 서비스 인스턴스는 시스템이 생성하므로 라이브 상태는 @Singleton [LocationTrackingState] 에 반영하고, 토글 의도는
 * [LocationTrackingPreferences] 에 영속한다. 알림의 "중지" 또는 샘플링 실패 시 의도를 off 로 내려 토글과 일치시킨다.
 */
@AndroidEntryPoint
internal class LocationCollectionService : Service() {
    @Inject lateinit var addSourceItemsUseCase: AddSourceItemsUseCase

    @Inject lateinit var trackingState: LocationTrackingState

    @Inject lateinit var preferences: LocationTrackingPreferences

    @Inject lateinit var transportHolder: DetectedTransportHolder

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val locationManager by lazy { getSystemService(LocationManager::class.java) }
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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent?.action == ACTION_STOP) {
            disableAndStop()
            return START_NOT_STICKY
        }
        if (!startForegroundInternal()) {
            // FGS 승격 실패(권한/eligible 부족) — startForegroundService 계약 위반 크래시를 피해 즉시 종료.
            disableAndStop()
            return START_NOT_STICKY
        }
        startSampling()
        return START_STICKY
    }

    override fun onDestroy() {
        stopSampling()
        scope.cancel()
        super.onDestroy()
    }

    /** FGS 승격을 시도하고 성공 여부를 반환한다. location FGS 는 백그라운드 위치 권한/eligible 상태가 없으면 실패한다. */
    private fun startForegroundInternal(): Boolean =
        runCatching {
            val notification = buildNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        }.onFailure { e ->
            Logger.w(LogDomain.COLLECTION, "위치 FGS 시작 실패: ${e.message}")
        }.isSuccess

    @SuppressLint("MissingPermission")
    private fun startSampling() {
        if (segmenter != null) return
        val manager = locationManager ?: return stopSelf()
        segmenter = LocationSegmenter()
        val provider =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) LocationManager.FUSED_PROVIDER else LocationManager.GPS_PROVIDER
        runCatching {
            manager.requestLocationUpdates(provider, MIN_TIME_MS, MIN_DISTANCE_M, listener, Looper.getMainLooper())
        }.onSuccess {
            registerActivityUpdates()
        }.onFailure { e ->
            // 권한 미허용은 SecurityException — 의도를 off 로 내려 토글과 일치시키고 종료.
            Logger.w(LogDomain.COLLECTION, "위치 업데이트 시작 실패: ${e.message}")
            segmenter = null
            disableAndStop()
        }
    }

    private fun stopSampling() {
        val active = segmenter ?: return
        locationManager?.removeUpdates(listener)
        unregisterActivityUpdates()
        segmenter = null
        trackingState.update(null)
        val remaining = active.flush()
        if (remaining.isNotEmpty()) saveEvents(remaining)
    }

    @SuppressLint("MissingPermission")
    private fun registerActivityUpdates() {
        val transitions =
            TRANSPORT_ACTIVITIES.map { activity ->
                ActivityTransition.Builder()
                    .setActivityType(activity)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build()
            }
        runCatching {
            ActivityRecognition.getClient(this)
                .requestActivityTransitionUpdates(ActivityTransitionRequest(transitions), activityPendingIntent())
        }.onFailure { e ->
            // ACTIVITY_RECOGNITION 미허용 등 — AR 없이 속도 추론으로 폴백.
            Logger.w(LogDomain.COLLECTION, "이동수단 인식 등록 실패: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun unregisterActivityUpdates() {
        runCatching {
            ActivityRecognition.getClient(this).removeActivityTransitionUpdates(activityPendingIntent())
        }
        transportHolder.reset()
    }

    private fun activityPendingIntent(): PendingIntent {
        val intent = Intent(this, ActivityTransitionReceiver::class.java)
        // AR 결과 주입을 위해 MUTABLE 필요.
        return PendingIntent.getBroadcast(this, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
    }

    /**
     * 토글 의도를 off 로 내리고 서비스를 종료한다(알림 "중지"·샘플링 실패 공통 경로).
     *
     * 영속 저장이 [onDestroy] 의 scope 취소로 유실되지 않도록 [stopSelf] 는 저장 완료 후 코루틴 안에서 호출한다.
     */
    private fun disableAndStop() {
        scope.launch {
            runCatching { preferences.setEnabled(false) }
            stopSelf()
        }
    }

    private fun onLocation(location: Location) {
        val active = segmenter ?: return
        val previousStatus = active.currentStatus(location.time)
        val events = active.onSample(location.latitude, location.longitude, location.time)
        val currentStatus = active.currentStatus(location.time)
        // 체류 중 쌓인 오탐이 새 이동 구간의 이동수단 판정을 지배하지 않도록 이동 시작 경계에서 비운다.
        if (previousStatus !is LocationTrackingStatus.Moving && currentStatus is LocationTrackingStatus.Moving) {
            transportHolder.reset()
        }
        trackingState.update(currentStatus)
        if (events.isNotEmpty()) saveEvents(events)
    }

    private fun saveEvents(events: List<DetectedEvent>) {
        val collectedAt = Instant.now()
        val zone = ZoneId.systemDefault()
        val items = events.map { it.toSourceItem(collectedAt, zone) }
        // 이동을 확정 저장했으면 AR 누적을 비워 다음 구간을 새로 집계한다(dominant 계산 후 호출).
        if (events.any { it is DetectedEvent.Move }) transportHolder.reset()
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
                    payload = StayPayload(latitude = latitude, longitude = longitude),
                    sourceName = SourceName.LOCATION_PROVIDER,
                    sourceKey = "STAY:$startMillis",
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
                            // 구간 dominant AR 이동수단 우선, 없으면(UNKNOWN) 도보/차량 속도 추론으로 폴백.
                            transports =
                                transportHolder.dominant(SystemClock.elapsedRealtime())
                                    .takeIf { it != MovementPayload.Transport.UNKNOWN } ?: transport,
                        ),
                    sourceName = SourceName.LOCATION_PROVIDER,
                    sourceKey = "MOVEMENT:$startMillis",
                    collectedAt = collectedAt,
                )
        }

    private fun buildNotification(): Notification {
        ensureChannel()
        val contentIntent =
            packageManager.getLaunchIntentForPackage(packageName)?.let { launch ->
                PendingIntent.getActivity(this, 0, launch, PendingIntent.FLAG_IMMUTABLE)
            }
        val stopIntent =
            PendingIntent.getService(
                this,
                1,
                Intent(this, LocationCollectionService::class.java).setAction(ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE,
            )
        val stopAction =
            Notification.Action
                .Builder(Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel), "중지", stopIntent)
                .build()
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("위치 수집 중")
            .setContentText("체류·이동을 자동으로 기록하고 있어요")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .apply { contentIntent?.let { setContentIntent(it) } }
            .addAction(stopAction)
            .build()
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel =
                NotificationChannel(CHANNEL_ID, "위치 수집", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "위치 자동 수집 중임을 알리는 상시 알림"
                    setShowBadge(false)
                }
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val ACTION_STOP = "com.soma369.laimory.core.collection.location.action.STOP"
        private const val NOTIFICATION_ID = 0x10C
        private const val CHANNEL_ID = "location_collection"
        private const val MIN_TIME_MS = 30_000L

        // 0 = 이동 여부와 무관하게 주기적으로 위치를 받는다(정지 중 샘플이 끊기지 않도록).
        private const val MIN_DISTANCE_M = 0f

        // 과거 세부 활동도 구독하되 신규 기록에는 도보/차량으로 정규화한다. STILL 은 속도 폴백을 유도한다.
        private val TRANSPORT_ACTIVITIES =
            listOf(
                DetectedActivity.IN_VEHICLE,
                DetectedActivity.ON_BICYCLE,
                DetectedActivity.WALKING,
                DetectedActivity.RUNNING,
                DetectedActivity.STILL,
            )
    }
}
