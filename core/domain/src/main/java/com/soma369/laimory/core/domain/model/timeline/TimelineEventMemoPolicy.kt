package com.soma369.laimory.core.domain.model.timeline

/**
 * 이벤트 메모 입력 규칙.
 *
 * [MAX_LENGTH] 는 서버 계약 값을 따라가지 않고 앱이 정한 입력 한도다. 서버 한도(10,000자)보다 훨씬
 * 좁으므로 서버가 값을 바꿔도 앱을 따라 고칠 필요가 없다. 메모는 카드에 자르지 않고 전문을 보여 주기
 * 때문에, 카드를 덮지 않을 길이로 앱이 먼저 건다.
 */
object TimelineEventMemoPolicy {
    const val MAX_LENGTH = 500

    /**
     * 이미 [currentLength] 글자가 든 메모가 받을 수 있는 길이.
     *
     * 한도를 줄이기 전에 저장된 메모는 [MAX_LENGTH] 를 넘을 수 있다. 입력칸을 열자마자 한도로 잘라
     * 버리면 저장하는 순간 뒷부분이 사라지므로, 이미 쓴 글은 그대로 두고 **더 늘리는 입력만** 막는다.
     */
    fun allowedLength(currentLength: Int): Int = maxOf(MAX_LENGTH, currentLength)
}
