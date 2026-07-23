package com.soma369.laimory.core.domain.model.timeline

/**
 * Event PATCH의 optional 키 상태.
 *
 * [Unchanged]는 JSON 키를 보내지 않고, [Value]는 값 자체를 전송한다. nullable 값을 담은 [Value]는
 * 명시적 null을 의미하므로 memo 제거처럼 서버가 키 존재 여부를 구분하는 필드에 사용한다.
 */
sealed interface TimelineEventUpdateField<out T> {
    data object Unchanged : TimelineEventUpdateField<Nothing>

    data class Value<T>(
        val value: T,
    ) : TimelineEventUpdateField<T>
}
