package com.soma369.laimory.core.collection.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.soma369.laimory.core.domain.model.collection.NotificationCollectMode
import com.soma369.laimory.core.domain.model.collection.NotificationFilter
import com.soma369.laimory.core.domain.model.collection.NotificationPayload
import com.soma369.laimory.core.domain.repository.NotificationFilterRepository
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
import javax.inject.Inject

/**
 * 알림 실시간 수집 리스너. 알림 접근 권한이 켜진 동안 시스템이 이 서비스를 바인딩한다.
 *
 * 과거 알림 백필은 불가하며(#95 제약), 서비스가 붙어있는 동안 게시/클릭된 알림만 잡는다.
 * 현재 필터([NotificationFilter])는 DataStore 관찰로 캐시해 두고, 알림 이벤트마다 그 스냅샷으로 판정한다.
 * - 게시([onNotificationPosted]): 모든 모드는 무조건, 선택 모드는 키워드/앱 매칭 시 수집.
 * - 제거([onNotificationRemoved]) reason=click: 선택 모드에서 사용자가 클릭한 알림 수집.
 */
@AndroidEntryPoint
internal class LaimoryNotificationListenerService : NotificationListenerService() {
    @Inject
    lateinit var filterRepository: NotificationFilterRepository

    @Inject
    lateinit var addSourceItemsUseCase: AddSourceItemsUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var filter: NotificationFilter = NotificationFilter()

    override fun onListenerConnected() {
        super.onListenerConnected()
        serviceScope.launch {
            filterRepository.observe().collect { filter = it }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        val reason = reasonForPosted(notification) ?: return
        capture(notification, reason)
    }

    override fun onNotificationRemoved(
        sbn: StatusBarNotification?,
        rankingMap: RankingMap?,
        reason: Int,
    ) {
        val notification = sbn ?: return
        // 모든 모드는 게시 시 이미 저장됐으므로, 클릭 수집은 선택 모드에서만 의미가 있다.
        if (reason == REASON_CLICK && filter.mode == NotificationCollectMode.SELECTIVE) {
            capture(notification, NotificationPayload.CollectReason.CLICK)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    /** 게시된 알림의 수집 사유. 수집 대상이 아니면 null. */
    private fun reasonForPosted(sbn: StatusBarNotification): NotificationPayload.CollectReason? =
        when (filter.mode) {
            NotificationCollectMode.ALL -> NotificationPayload.CollectReason.ALL
            NotificationCollectMode.SELECTIVE -> {
                val extras = sbn.notification.extras
                val haystack =
                    listOfNotNull(
                        extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
                        extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
                    ).joinToString(" ")
                when {
                    filter.keywords.any { it.isNotBlank() && haystack.contains(it, ignoreCase = true) } ->
                        NotificationPayload.CollectReason.KEYWORD

                    sbn.packageName in filter.allowedPackages -> NotificationPayload.CollectReason.APP
                    else -> null
                }
            }
        }

    private fun capture(
        sbn: StatusBarNotification,
        reason: NotificationPayload.CollectReason,
    ) {
        serviceScope.launch {
            runCatching {
                val item = sbn.toSourceItem(reason, packageManager, Instant.now(), ZoneId.systemDefault()) ?: return@launch
                addSourceItemsUseCase(listOf(item))
            }.onFailure { e ->
                Logger.w(LogDomain.COLLECTION, "알림 저장 실패: ${e.message}")
            }
        }
    }
}
