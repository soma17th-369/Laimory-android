package com.soma369.laimory.analytics

import com.soma369.laimory.core.domain.di.ApplicationCoroutineScope
import com.soma369.laimory.core.domain.helper.AnalyticsHelper
import com.soma369.laimory.core.domain.model.analytics.AnalyticsDedupeKeys
import com.soma369.laimory.core.domain.model.analytics.AnalyticsEvent
import com.soma369.laimory.core.domain.model.analytics.AnalyticsReadyTrigger
import com.soma369.laimory.core.domain.model.collection.PhotoPayload
import com.soma369.laimory.core.domain.usecase.ObserveSourceItemsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 수집 항목이 처음 저장되면 `data_collection_ready` 를 기록한다. 설치당 1회.
 *
 * 사진은 여기서 보지 않는다 — 저장하지 않고 생성 때 읽으므로, 사진 권한 허용을 권한 기록 쪽에서
 * 같은 판정 키로 따로 잡는다. 두 경로 중 먼저 온 쪽 하나만 나간다.
 *
 * 수집이 꺼져 있어 보내지 못했으면 판정 키도 소비되지 않아(`logOnce` 계약) 다음 방출에서 다시 시도된다.
 * 그래서 첫 방출에서 멈추지 않고 계속 관찰한다.
 */
@Singleton
class DataCollectionReadyReporter
    @Inject
    constructor(
        private val observeSourceItems: ObserveSourceItemsUseCase,
        private val analyticsHelper: AnalyticsHelper,
        @ApplicationCoroutineScope private val applicationScope: CoroutineScope,
    ) {
        private var job: Job? = null

        fun start() {
            if (job?.isActive == true) return
            job =
                applicationScope.launch {
                    observeSourceItems().collect { items ->
                        if (items.any { item -> item.payload !is PhotoPayload }) {
                            analyticsHelper.logOnce(
                                AnalyticsDedupeKeys.DATA_COLLECTION_READY,
                                AnalyticsEvent.DataCollectionReady(AnalyticsReadyTrigger.STORED_ITEM),
                            )
                        }
                    }
                }
        }
    }
