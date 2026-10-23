package com.soma369.laimory.core.collection.collector

import java.util.UUID

/**
 * 캘린더 일정 인스턴스의 식별자 규칙.
 *
 * 일정은 저장하지 않고 볼 때마다 읽으므로 `rawId` 를 무작위로 만들면 읽을 때마다 달라진다. 그러면
 * - 상세에서 뺀 항목(rawId 집합)이 다시 읽을 때마다 풀리고,
 * - 서버가 같은 날짜 이어 붙이기에서 기존 rawId 로 중복을 빼는 판정이 무너진다.
 *
 * 그래서 인스턴스 키(`eventId:begin`)로 이름 기반 UUID 를 만들어 항상 같은 값을 쓴다. 서버는 canonical lowercase
 * UUID 면 버전을 따지지 않는다. `begin` 은 캘린더 원본값(종일 일정이면 UTC 자정)이다 — 반복 일정도 회차마다 다르다.
 */
internal object CalendarInstanceIdentity {
    /** 인스턴스 단위 자연키. 반복 일정도 회차별로 유일하다. */
    fun sourceKeyOf(
        eventId: Long,
        begin: Long,
    ): String = "$eventId:$begin"

    /** [sourceKeyOf] 에서 만든 결정적 rawId. 같은 인스턴스면 언제 읽어도 같다. */
    fun rawIdOf(
        eventId: Long,
        begin: Long,
    ): String = UUID.nameUUIDFromBytes("$NAMESPACE:${sourceKeyOf(eventId, begin)}".toByteArray(Charsets.UTF_8)).toString()

    /** 다른 유형의 이름 기반 rawId 와 겹치지 않게 붙이는 접두사. */
    private const val NAMESPACE = "CALENDAR"
}
