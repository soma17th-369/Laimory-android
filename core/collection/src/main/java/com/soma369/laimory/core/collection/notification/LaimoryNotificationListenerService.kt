package com.soma369.laimory.core.collection.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.soma369.laimory.core.domain.model.collection.NotificationContent
import com.soma369.laimory.core.domain.model.collection.NotificationFilter
import com.soma369.laimory.core.domain.model.collection.NotificationPrivacyPolicy
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
 * 제목·본문은 이벤트당 한 번만 추출해 개인정보 정책·수집 판정·저장이 같은 값을 쓴다.
 * 개인정보 정책([NotificationPrivacyPolicy])이 수집 판정보다 먼저 실행되므로 클릭·앱 allowlist·키워드로
 * 우회할 수 없다.
 *
 * 동일 알림이 게시 이벤트에서 먼저 저장된 뒤 클릭 이벤트로 다시 들어오면 동일한 sourceKey 를 사용한다.
 * 저장소의 insert-or-ignore 정책에 따라 최초 수집 사유(KEYWORD/APP)가 유지된다(first-write-wins).
 */
@AndroidEntryPoint
internal class LaimoryNotificationListenerService : NotificationListenerService() {
    @Inject
    lateinit var filterRepository: NotificationFilterRepository

    @Inject
    lateinit var privacyPolicy: NotificationPrivacyPolicy

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
        capture(sbn ?: return, clicked = false)
    }

    override fun onNotificationRemoved(
        sbn: StatusBarNotification?,
        rankingMap: RankingMap?,
        reason: Int,
    ) {
        if (reason != REASON_CLICK) return
        capture(sbn ?: return, clicked = true)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    /**
     * 알림 한 건을 정제하고 수집 대상이면 저장한다.
     *
     * 개인정보 정책이 전체 제외를 판정하거나 정책 실행이 실패하면 저장하지 않는다(fail-closed).
     */
    private fun capture(
        sbn: StatusBarNotification,
        clicked: Boolean,
    ) {
        val notification = sbn.notification
        val sanitized = sanitize(notification.toContent(), sbn) ?: return
        val reason =
            filter.collectReasonFor(
                packageName = sbn.packageName,
                title = sanitized.title,
                text = sanitized.text,
                clicked = clicked,
            ) ?: return

        serviceScope.launch {
            runCatching {
                addSourceItemsUseCase(
                    listOf(sbn.toSourceItem(sanitized, reason, packageManager, Instant.now(), ZoneId.systemDefault())),
                )
            }.onFailure { e ->
                // 예외 메시지에 알림 원문이 실릴 수 있어 클래스명만 남긴다.
                Logger.w(LogDomain.COLLECTION, "알림 저장 실패: ${e.javaClass.simpleName}")
            }
        }
    }

    private fun sanitize(
        content: NotificationContent,
        sbn: StatusBarNotification,
    ): NotificationContent? =
        runCatching { privacyPolicy.sanitize(content, sbn.notification.toSignals()) }
            .onFailure { e ->
                Logger.w(LogDomain.COLLECTION, "알림 개인정보 정책 실패: ${e.javaClass.simpleName}")
            }.getOrNull()
}
