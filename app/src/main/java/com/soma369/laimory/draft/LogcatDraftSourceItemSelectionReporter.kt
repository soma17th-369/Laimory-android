package com.soma369.laimory.draft

import com.soma369.laimory.BuildConfig
import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.model.collection.NotificationPayload
import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelectionReport
import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelectionReporter
import com.soma369.laimory.core.util.logging.LogDomain
import com.soma369.laimory.core.util.logging.Logger
import javax.inject.Inject

/** debug 빌드에서 원문 없이 초안 원천 데이터 건수와 요청 크기만 Logcat에 기록한다. */
class LogcatDraftSourceItemSelectionReporter
    @Inject
    constructor() : DraftSourceItemSelectionReporter {
        override val isEnabled: Boolean = BuildConfig.DEBUG

        override fun reportSelection(report: DraftSourceItemSelectionReport) {
            if (!isEnabled) return
            val typeCounts =
                ItemType.entries.joinToString(separator = ", ") { itemType ->
                    val original = report.originalCounts.getOrDefault(itemType, 0)
                    val selected = report.selectedCounts.getOrDefault(itemType, 0)
                    "$itemType=$original/$selected(-${original - selected})"
                }
            Logger.d(
                LogDomain.DRAFT_TASK,
                "draft source selection original=${report.originalTotal} selected=${report.selectedTotal} [$typeCounts]",
            )
            // 타입 건수만으로는 클릭 알림이 통째로 잘려도 드러나지 않는다.
            val reasonCounts =
                NotificationPayload.CollectReason.entries.joinToString(separator = ", ") { reason ->
                    val original = report.notificationOriginalCountsByReason.getOrDefault(reason, 0)
                    val selected = report.notificationSelectedCountsByReason.getOrDefault(reason, 0)
                    "$reason=$original/$selected(-${original - selected})"
                }
            Logger.d(LogDomain.DRAFT_TASK, "draft notification by reason [$reasonCounts]")
        }

        override fun reportRequestSize(
            sourceItemCount: Int,
            utf8ByteCount: Int,
        ) {
            if (!isEnabled) return
            Logger.d(
                LogDomain.DRAFT_TASK,
                "draft request sourceItems=$sourceItemCount utf8Bytes=$utf8ByteCount",
            )
        }
    }
