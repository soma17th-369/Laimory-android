package com.soma369.laimory.core.data.model.timeline.request

import kotlinx.serialization.Serializable

/**
 * sparse 갱신에서 **키를 보내되 값은 null 일 수 있는** 자리.
 *
 * PATCH 는 세 상태를 구분한다.
 *
 * | 뜻 | 프로퍼티 | 나가는 모양 |
 * | --- | --- | --- |
 * | 안 바꿈 | `null` | 키 없음 |
 * | 지움 | `PatchField(null)` | `"memo": null` |
 * | 바꿈 | `PatchField("...")` | `"memo": "..."` |
 *
 * `String?` 하나로는 앞 둘이 같은 값이 돼 구분할 수 없다. 이 래퍼가 **키 존재**를, 안쪽 값이
 * **값의 유무**를 나타낸다. value class 라 런타임 객체가 늘지 않고 직렬화도 안쪽 타입 그대로다.
 *
 * 제네릭이 아닌 이유는 컴파일러 문제다 — `@JvmInline value class Foo<T>` 에 직렬화 플러그인을
 * 붙이면 IR lowering 에서 깨진다(`Mismatching type arguments`). 지금 쓰는 곳이 문자열 하나뿐이라
 * 타입을 고정해 둔다.
 */
@JvmInline
@Serializable
value class PatchField(
    val value: String?,
)
