package com.soma369.laimory.core.collection.notification

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.StatusBarNotification
import com.soma369.laimory.core.domain.model.collection.NotificationContent
import com.soma369.laimory.core.domain.model.collection.NotificationPayload
import com.soma369.laimory.core.domain.model.collection.NotificationSignals
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceName
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

/**
 * 게시된 알림([StatusBarNotification])을 저장용 [SourceItem] 으로 변환한다.
 *
 * 제목·본문은 알림에서 다시 읽지 않고 [content] 로 받는다 — 개인정보 정책을 통과한 정제 결과와
 * 저장 값이 어긋나지 않게 하기 위해서다. 빈 껍데기 알림 제외도 정책이 담당하므로 여기서는
 * 판정하지 않는다.
 *
 * sourceKey 는 `key:postTime:packageName` 으로, 재수신은 무시되고 업데이트 알림(postTime 갱신)은 새 이벤트가 된다.
 */
internal fun StatusBarNotification.toSourceItem(
    content: NotificationContent,
    reason: NotificationPayload.CollectReason,
    packageManager: PackageManager,
    collectedAt: Instant,
    zoneId: ZoneId,
): SourceItem =
    SourceItem(
        rawId = UUID.randomUUID().toString(),
        startAt = Instant.ofEpochMilli(postTime),
        endAt = null,
        timeZoneId = zoneId,
        payload =
            NotificationPayload(
                appName = packageManager.appLabel(packageName),
                packageName = packageName,
                title = content.title,
                text = content.text,
                collectReason = reason,
            ),
        sourceName = SourceName.NOTIFICATION_LISTENER,
        sourceKey = "$key:$postTime:$packageName",
        collectedAt = collectedAt,
    )

/** 알림에서 정제 대상 텍스트를 한 번만 추출한다. */
internal fun Notification.toContent(): NotificationContent =
    NotificationContent(
        title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
        text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
    )

/**
 * 개인정보 정책과 수집 정책이 쓰는 구조 신호만 뽑는다. 프레임워크 타입과 상수는 이 경계 밖으로
 * 넘기지 않고 boolean 으로만 전달한다.
 *
 * MessagingStyle 은 템플릿 이름 또는 메시지 배열 존재로 판정하며, 메시지 원문(`EXTRA_MESSAGES`)은
 * 읽지 않는다.
 */
internal fun Notification.toSignals(): NotificationSignals =
    NotificationSignals(
        isMessage =
            category == Notification.CATEGORY_MESSAGE ||
                extras.getString(Notification.EXTRA_TEMPLATE) == MESSAGING_STYLE_TEMPLATE ||
                extras.containsKey(Notification.EXTRA_MESSAGES),
        isPromotion = category == Notification.CATEGORY_PROMO,
        isOngoing = flags and Notification.FLAG_ONGOING_EVENT != 0,
        isGroupSummary = flags and Notification.FLAG_GROUP_SUMMARY != 0,
        hasProgress =
            extras.containsKey(Notification.EXTRA_PROGRESS) ||
                extras.containsKey(Notification.EXTRA_PROGRESS_INDETERMINATE),
    )

/** 패키지의 표시 이름. 조회 실패 시 패키지명 그대로. */
internal fun PackageManager.appLabel(packageName: String): String =
    runCatching { getApplicationLabel(getApplicationInfo(packageName, 0)).toString() }.getOrDefault(packageName)

private val MESSAGING_STYLE_TEMPLATE: String = Notification.MessagingStyle::class.java.name
