package com.soma369.laimory.core.collection.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.soma369.laimory.core.domain.model.collection.NotificationContent
import com.soma369.laimory.core.domain.model.collection.NotificationFilter
import com.soma369.laimory.core.domain.model.collection.NotificationPrivacyPolicy
import com.soma369.laimory.core.domain.model.collection.NotificationSignals
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
 * 첫 emission 전에는 판정하지 않고 알림을 버린다 — 기본값으로 사용자 설정을 우회할 수 있기 때문이다.
 * - 게시([onNotificationPosted]): 키워드 또는 앱이 일치하면 수집.
 * - 제거([onNotificationRemoved]) reason=click: 클릭 수집이 켜져 있으면 키워드·앱 설정과 무관하게 수집.
 *
 * 제목·본문은 이벤트당 한 번만 추출해 개인정보 정책·수집 판정·저장이 같은 값을 쓴다.
 * 개인정보 정책([NotificationPrivacyPolicy])이 수집 판정보다 먼저 실행되므로 클릭·앱 allowlist·키워드로
 * 우회할 수 없다. 구조 신호는 한 번만 변환해 개인정보 정책과 수집 판정이 함께 쓴다.
 *
 * 동일 알림이 게시 이벤트에서 먼저 저장된 뒤 클릭 이벤트로 다시 들어오면 동일한 sourceKey 를 사용한다.
 * 저장소의 insert-or-ignore 정책에 따라 최초 수집 사유(KEYWORD/APP)가 유지된다(first-write-wins).
 *
 * 앱이 내용을 갱신하며 다시 알리면 `postTime` 이 바뀌어 새 아이템이 되므로, 게시 경로에는
 * [NotificationUpdateThrottle] 로 같은 알림의 최소 재수집 간격을 둔다. 클릭은 사용자가 그 알림을
 * 직접 지목한 결과라 억제하지 않는다.
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

    /** 같은 알림의 갱신 폭주를 흡수한다. 리스너가 붙어 있는 동안만 유지된다. */
    private val updateThrottle = NotificationUpdateThrottle()

    /** 저장된 설정을 처음 읽기 전에는 null 이다 — 기본값으로 사용자 설정을 우회하지 않기 위해서다. */
    @Volatile
    private var filter: NotificationFilter? = null

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
        // 저장된 설정을 읽기 전 알림은 버린다 — 기본 키워드가 켜진 기본값으로 판정하면
        // useDefaultKeywords 를 꺼 둔 사용자의 설정을 무시하게 된다.
        val filter = filter ?: return
        val collectedAt = Instant.now()
        // 갱신 억제를 정제·판정보다 먼저 본다. 초당 갱신되는 알림에 개인정보 정규식을 매번
        // 돌리지 않기 위해서다.
        if (!clicked && updateThrottle.isThrottled(sbn.throttleKey, collectedAt)) return

        val signals = sbn.notification.toSignals()
        val sanitized = sanitize(sbn.notification.toContent(), signals) ?: return
        val reason =
            filter.collectReasonFor(
                packageName = sbn.packageName,
                title = sanitized.title,
                text = sanitized.text,
                clicked = clicked,
                signals = signals,
            ) ?: return

        if (!clicked) updateThrottle.markCollected(sbn.throttleKey, collectedAt)

        serviceScope.launch {
            runCatching {
                addSourceItemsUseCase(
                    listOf(sbn.toSourceItem(sanitized, reason, packageManager, collectedAt, ZoneId.systemDefault())),
                )
            }.onFailure { e ->
                // 예외 메시지에 알림 원문이 실릴 수 있어 클래스명만 남긴다.
                Logger.w(LogDomain.COLLECTION, "알림 저장 실패: ${e.javaClass.simpleName}")
            }
        }
    }

    private fun sanitize(
        content: NotificationContent,
        signals: NotificationSignals,
    ): NotificationContent? =
        runCatching { privacyPolicy.sanitize(content, signals) }
            .onFailure { e ->
                Logger.w(LogDomain.COLLECTION, "알림 개인정보 정책 실패: ${e.javaClass.simpleName}")
            }.getOrNull()
}

/**
 * 갱신 억제의 단위. `postTime` 을 빼 같은 알림의 재게시를 하나로 본다.
 *
 * `sbn.key` 는 이미 패키지를 포함하지만 그 구성은 플랫폼 내부 규칙이므로 패키지를 함께 붙인다.
 */
private val StatusBarNotification.throttleKey: String get() = "$packageName:$key"
