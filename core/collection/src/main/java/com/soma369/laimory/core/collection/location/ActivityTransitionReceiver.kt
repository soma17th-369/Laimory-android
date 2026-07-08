package com.soma369.laimory.core.collection.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.soma369.laimory.core.domain.model.collection.MovementPayload
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Activity Recognition 전이 결과를 받아 최신 활동(ENTER)을 [DetectedTransportHolder] 에 반영한다.
 *
 * [LocationCollectionService] 가 등록한 PendingIntent 로만 트리거된다. 의존성은 [EntryPointAccessors] 로 얻는다.
 */
internal class ActivityTransitionReceiver : BroadcastReceiver() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    internal interface Entry {
        fun holder(): DetectedTransportHolder
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (!ActivityTransitionResult.hasResult(intent)) return
        val result = ActivityTransitionResult.extractResult(intent) ?: return
        val holder = EntryPointAccessors.fromApplication(context.applicationContext, Entry::class.java).holder()
        // 구간 dominant 집계를 위해 진입(ENTER) 전이를 시각과 함께 모두 쌓는다.
        result.transitionEvents
            .filter { it.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER }
            .forEach { holder.onEnter(it.activityType.toTransport(), it.elapsedRealTimeNanos / 1_000_000L) }
    }

    private fun Int.toTransport(): MovementPayload.Transport =
        when (this) {
            DetectedActivity.IN_VEHICLE -> MovementPayload.Transport.IN_VEHICLE
            DetectedActivity.ON_BICYCLE -> MovementPayload.Transport.ON_BICYCLE
            DetectedActivity.RUNNING -> MovementPayload.Transport.RUNNING
            DetectedActivity.WALKING, DetectedActivity.ON_FOOT -> MovementPayload.Transport.WALKING
            else -> MovementPayload.Transport.UNKNOWN
        }
}
