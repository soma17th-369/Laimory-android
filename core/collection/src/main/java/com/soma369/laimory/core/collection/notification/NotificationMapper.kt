package com.soma369.laimory.core.collection.notification

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.StatusBarNotification
import com.soma369.laimory.core.domain.model.collection.NotificationPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceName
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

/**
 * 게시된 알림([StatusBarNotification])을 저장용 [SourceItem] 으로 변환한다.
 *
 * 수집 사유가 클릭·앱·키워드 중 무엇이든 제목/본문이 모두 없으면(빈 껍데기 알림) null 을 반환해 저장에서 제외한다.
 * sourceKey 는 `key:postTime:packageName` 으로, 재수신은 무시되고 업데이트 알림(postTime 갱신)은 새 이벤트가 된다.
 */
internal fun StatusBarNotification.toSourceItem(
    reason: NotificationPayload.CollectReason,
    packageManager: PackageManager,
    collectedAt: Instant,
    zoneId: ZoneId,
): SourceItem? {
    val extras = notification.extras
    val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
    val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
    if (!hasCollectibleContent(title, text)) return null

    return SourceItem(
        rawId = UUID.randomUUID().toString(),
        startAt = Instant.ofEpochMilli(postTime),
        endAt = null,
        timeZoneId = zoneId,
        payload =
            NotificationPayload(
                appName = packageManager.appLabel(packageName),
                packageName = packageName,
                title = title,
                text = text,
                collectReason = reason,
            ),
        sourceName = SourceName.NOTIFICATION_LISTENER,
        sourceKey = "$key:$postTime:$packageName",
        collectedAt = collectedAt,
    )
}

/** 클릭 알림도 제목 또는 본문 중 하나는 있어야 저장 대상으로 인정한다. */
internal fun hasCollectibleContent(
    title: String?,
    text: String?,
): Boolean = !title.isNullOrBlank() || !text.isNullOrBlank()

/** 패키지의 표시 이름. 조회 실패 시 패키지명 그대로. */
internal fun PackageManager.appLabel(packageName: String): String =
    runCatching { getApplicationLabel(getApplicationInfo(packageName, 0)).toString() }.getOrDefault(packageName)
