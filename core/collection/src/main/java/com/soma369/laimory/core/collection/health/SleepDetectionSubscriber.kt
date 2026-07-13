package com.soma369.laimory.core.collection.health

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.SleepSegmentRequest
import com.soma369.laimory.core.util.logging.LogDomain
import com.soma369.laimory.core.util.logging.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sleep API 감지 구독 생명주기.
 *
 * 시스템이 구독을 관리하므로 FGS 없이 한 번 [start] 하면 앱이 죽어도 [SleepReceiver] 로 이벤트가 전달된다
 * (segment + classify 를 한 구독으로 함께 받는다). Google Play Services 가 없거나 ACTIVITY_RECOGNITION
 * 권한이 없으면 조용히 비활성(구독하지 않음).
 *
 * 콜드스타트/부팅 시엔 [startIfEnabled] 로 사용자 의도([SleepDetectionPreferences])를 확인해 복원한다.
 */
@Singleton
class SleepDetectionSubscriber
    @Inject
    internal constructor(
        @ApplicationContext private val context: Context,
        private val preferences: SleepDetectionPreferences,
    ) {
        /** 사용자가 자동 감지를 켜둔 상태였을 때만 구독을 복원한다(콜드스타트·부팅용). */
        suspend fun startIfEnabled() {
            if (preferences.isEnabled()) start()
        }

        /** 권한·Play Services 가 갖춰졌으면 감지 구독을 건다(멱등 — 같은 PendingIntent 재요청은 갱신). */
        @SuppressLint("MissingPermission") // 구독 전 hasRecognitionPermission 으로 직접 확인한다.
        fun start() {
            if (!isPlayServicesAvailable() || !hasRecognitionPermission()) {
                Logger.i(LogDomain.COLLECTION, "수면 감지 구독 건너뜀(Play Services/권한 미충족)")
                return
            }
            runCatching {
                ActivityRecognition
                    .getClient(context)
                    .requestSleepSegmentUpdates(
                        pendingIntent(),
                        SleepSegmentRequest(SleepSegmentRequest.SEGMENT_AND_CLASSIFY_EVENTS),
                    )
            }.onFailure { e -> Logger.w(LogDomain.COLLECTION, "수면 감지 구독 실패: ${e.message}") }
        }

        /** 감지 구독을 해제한다. */
        @SuppressLint("MissingPermission")
        fun stop() {
            runCatching {
                ActivityRecognition.getClient(context).removeSleepSegmentUpdates(pendingIntent())
            }
        }

        private fun pendingIntent(): PendingIntent {
            val intent = Intent(context, SleepReceiver::class.java)
            // 시스템이 이벤트를 intent 에 주입하므로 MUTABLE 이 필요하다.
            return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
        }

        private fun isPlayServicesAvailable(): Boolean =
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS

        private fun hasRecognitionPermission(): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                context.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

        private companion object {
            private const val REQUEST_CODE = 0x51E
        }
    }
