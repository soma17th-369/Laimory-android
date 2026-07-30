package com.soma369.laimory.core.collection.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
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
 * - 게시([onNotificationPosted]): 키워드 또는 앱이 일치하면 수집.
 * - 제거([onNotificationRemoved]) reason=click: 클릭 수집이 켜져 있으면 키워드·앱 설정과 무관하게 수집.
 *
 * 동일 알림이 게시 이벤트에서 먼저 저장된 뒤 클릭 이벤트로 다시 들어오면 동일한 sourceKey 를 사용한다.
 * 저장소의 insert-or-ignore 정책에 따라 최초 수집 사유(KEYWORD/APP)가 유지된다(first-write-wins).
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
        val reason = reasonFor(notification, clicked = false) ?: return
        capture(notification, reason)
    }

    override fun onNotificationRemoved(
        sbn: StatusBarNotification?,
        rankingMap: RankingMap?,
        reason: Int,
    ) {
        val notification = sbn ?: return
        if (reason == REASON_CLICK) {
            val collectReason = reasonFor(notification, clicked = true) ?: return
            capture(notification, collectReason)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    /** 게시·클릭 이벤트의 수집 사유. 해당 이벤트 경로가 비활성화됐거나 필터와 일치하지 않으면 null. */
    private fun reasonFor(
        sbn: StatusBarNotification,
        clicked: Boolean,
    ): NotificationPayload.CollectReason? {
        val extras = sbn.notification.extras
        return filter.collectReasonFor(
            packageName = sbn.packageName,
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            clicked = clicked,
        )
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
