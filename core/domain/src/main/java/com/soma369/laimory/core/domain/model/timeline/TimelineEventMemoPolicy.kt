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

    /**
     * 입력으로 바뀐 [next] 를 한도에 맞춘다. [previous] 는 입력 직전 글이다.
     *
     * 새 글 전체를 끝에서 자르면 앞·중간에 넣은 글자 대신 **기존 글의 끝**이 사라진다. 그래서 앞뒤로
     * [previous] 와 겹치는 부분은 두고, 가운데 **새로 들어온 구간만** 넘친 만큼 줄인다.
     * 한도를 넘지 않았으면 [next] 를 그대로 돌려준다.
     */
    fun limitInput(
        previous: String,
        next: String,
    ): String {
        val overflow = next.length - allowedLength(previous.length)
        if (overflow <= 0) return next
        val commonLimit = minOf(previous.length, next.length)
        var prefix = 0
        while (prefix < commonLimit && previous[prefix] == next[prefix]) prefix++
        var suffix = 0
        while (suffix < commonLimit - prefix && previous[previous.length - 1 - suffix] == next[next.length - 1 - suffix]) suffix++
        val insertedLength = next.length - prefix - suffix
        // 한도는 이전 길이 이상이라 넘친 양은 늘어난 양보다 클 수 없고, 늘어난 양은 새 구간보다 클 수 없다.
        var kept = insertedLength - overflow
        // 줄이다 서로게이트 쌍의 앞 절반만 남기면 깨진 글자가 된다.
        if (kept > 0 && next[prefix + kept - 1].isHighSurrogate()) kept--
        return next.substring(0, prefix + kept) + next.substring(next.length - suffix)
    }
}
