package com.soma369.laimory.feature.timeline.model

/**
 * 메모 영역에 무엇을 보여줄지 고른 결과.
 *
 * **질문은 이 자리를 쓰지 않는다.** 예전에는 메모가 비었을 때 질문 문장이 안내 문구 자리에 들어앉아
 * 답을 적기 시작하면 질문이 사라졌다. 지금은 질문이 메모 위 말풍선으로 따로 서므로
 * ([timelineMemoQuestion]), 이 자리는 사용자가 남긴 글이거나 그 자리를 가리키는 안내 문구뿐이다.
 */
internal sealed interface TimelineMemoDisplay {
    val text: String

    /** 사용자가 남긴 메모. */
    data class Memo(
        override val text: String,
    ) : TimelineMemoDisplay

    /** 아직 비어 있어 안내 문구를 띄운 상태. */
    data class Prompt(
        override val text: String,
    ) : TimelineMemoDisplay
}

/**
 * 메모 영역 표시 내용을 고른다. `null` 이면 영역을 그리지 않는다.
 *
 * **읽기 모드([isEditable] = false)에서는 사용자가 남긴 메모만 보여 준다.** 안내 문구는 답을
 * 유도하는 prompt 라 쓸 수 없는 자리에서는 소음이다 — 읽기만 하는 화면에 "무엇을 적어 보라" 는
 * 문장이 남아 있으면 읽을거리로 오인된다. 그래서 메모가 없으면 영역째 감춘다.
 *
 * 그 대가로 메모가 없는 이벤트는 모드에 따라 카드 높이가 달라진다. 편집 아이콘 자리를 비워 두는
 * 것과 방향이 반대이며, 쓸 수 없는 안내가 더 거슬린다는 제품 판단이다.
 */
internal fun timelineMemoDisplay(
    memo: String?,
    isEditable: Boolean,
): TimelineMemoDisplay? {
    val savedMemo = memo?.takeIf(String::isNotBlank)?.let(TimelineMemoDisplay::Memo)
    if (savedMemo != null) return savedMemo
    if (!isEditable) return null
    return TimelineMemoDisplay.Prompt(MEMO_PROMPT)
}

/**
 * 메모 위에 띄울 AI 질문. `null` 이면 말풍선을 그리지 않는다.
 *
 * **읽기 모드에서는 언제나 감춘다** — 저장을 마친 화면에서 답할 수 없는 질문은 읽을거리가 아니다.
 * 메모를 이미 남겼더라도 편집 모드에서는 계속 띄운다: 무엇에 답한 글인지가 그 글의 문맥이다.
 */
internal fun timelineMemoQuestion(
    question: String?,
    isEditable: Boolean,
): String? = question?.takeIf { isEditable && it.isNotBlank() }

/**
 * 빈 메모 자리의 안내 문구. 인라인 편집기의 입력 placeholder 로도 쓴다.
 *
 * 질문이 있든 없든 같다. 질문은 말풍선이 이미 보여 주고 있으므로, 이 자리는 무엇을 하는 자리인지만
 * 말한다 — 문장 앞에 붙는 연필 아이콘이 그 뜻을 함께 진다.
 */
internal const val MEMO_PROMPT = "입력하기"
