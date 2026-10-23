package com.soma369.laimory.core.collection.collector

import com.soma369.laimory.core.util.logging.LogDomain
import com.soma369.laimory.core.util.logging.Logger

/**
 * 캘린더 조회 결과를 권한 없음과 실제 실패로 가른다.
 *
 * - `SecurityException`: 권한 미허용. 빈 결과(null)로 돌아간다.
 * - `null` 값: `ContentResolver.query` 는 provider 가 null 을 돌려주거나 죽었을 때도 null 을 낸다.
 *   권한 확인을 통과한 뒤의 null 은 **실제 조회 실패**라 예외로 올린다. 빈 목록으로 바꾸면 "일정이 없는 날" 과
 *   구별되지 않아, 실패 안내 없이 일정 없이 초안이 만들어진다.
 * - 그 밖의 예외: 그대로 올린다.
 *
 * 로그에는 일정 제목이 실릴 수 있는 예외 메시지를 남기지 않는다.
 *
 * `Cursor` 를 다루지 않는 순수 판정이라 JVM 테스트로 검증한다 — 커서를 만들려면 계측 환경이 필요하다.
 */
internal fun <T> resolveCalendarQuery(
    label: String,
    result: Result<T?>,
): T? {
    val failure = result.exceptionOrNull()
    if (failure is SecurityException) {
        Logger.w(LogDomain.COLLECTION, "$label 읽기 권한 없음")
        return null
    }
    return result.getOrThrow() ?: error("$label 조회가 결과를 반환하지 않았습니다.")
}
