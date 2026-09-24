package com.soma369.laimory.core.domain.usecase.analytics

import com.soma369.laimory.core.domain.helper.AnalyticsHelper
import com.soma369.laimory.core.domain.model.analytics.AnalyticsDedupeKeys
import com.soma369.laimory.core.domain.model.analytics.AnalyticsEvent
import com.soma369.laimory.core.domain.model.analytics.AnalyticsPermissionState
import com.soma369.laimory.core.domain.model.analytics.AnalyticsPermissionType
import com.soma369.laimory.core.domain.model.analytics.AnalyticsPromptContext
import com.soma369.laimory.core.domain.model.analytics.AnalyticsReadyTrigger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 권한 요청과 결과를 기록한다. 홈·온보딩·설정이 같은 규칙을 쓰도록 한 곳에 둔다.
 *
 * 사진 권한은 허용되는 순간 곧 생성할 재료가 생긴 것이다 — 사진은 저장하지 않고 생성 때 읽으므로
 * 저장 항목만 보면 사진만 쓰는 사용자는 끝내 "준비됨"이 되지 않는다. 그래서 결과를 기록하면서
 * `data_collection_ready` 도 함께 판정한다(저장 항목 경로와 같은 키라 먼저 온 쪽 하나만 나간다).
 */
@Singleton
class LogPermissionEventUseCase
    @Inject
    constructor(
        private val analyticsHelper: AnalyticsHelper,
    ) {
        suspend fun requested(
            permission: AnalyticsPermissionType,
            promptContext: AnalyticsPromptContext,
        ) {
            analyticsHelper.log(AnalyticsEvent.PermissionRequestStarted(permission, promptContext))
        }

        suspend fun settled(
            permission: AnalyticsPermissionType,
            state: AnalyticsPermissionState,
            promptContext: AnalyticsPromptContext,
        ) {
            analyticsHelper.log(AnalyticsEvent.PermissionResult(permission, state, promptContext))
            if (permission == AnalyticsPermissionType.PHOTO && state.canRead) {
                analyticsHelper.logOnce(
                    AnalyticsDedupeKeys.DATA_COLLECTION_READY,
                    AnalyticsEvent.DataCollectionReady(AnalyticsReadyTrigger.PHOTO_PERMISSION),
                )
            }
        }

        /** 일부 선택도 고른 사진은 읽을 수 있어 재료가 생긴 것으로 본다. */
        private val AnalyticsPermissionState.canRead: Boolean
            get() = this == AnalyticsPermissionState.GRANTED || this == AnalyticsPermissionState.PARTIAL
    }
