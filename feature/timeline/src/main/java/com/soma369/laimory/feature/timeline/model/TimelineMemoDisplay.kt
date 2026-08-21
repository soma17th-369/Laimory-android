package com.soma369.laimory.feature.timeline.model

/**
 * 메모 영역에 무엇을 보여줄지 고른 결과.
 *
 * 표시 우선순위는 `메모 → 질문 → 기본 안내 문구`다. 질문은 AI 가 되묻는 문장이고, 메모가 비어 있을 때
 * 안내 문구를 대신하는 prompt 로만 쓴다 — 답을 유도하는 값이지 보존 대상이 아니라 사용자가 메모를
 * 남기면 그 메모가 질문을 가린다.
 */
internal sealed interface TimelineMemoDisplay {
    val text: String

    /** 사용자가 남긴 메모. */
    data class Memo(
        override val text: String,
    ) : TimelineMemoDisplay

    /** AI 질문을 안내 문구 자리에 띄운 상태. */
    data class Question(
        override val text: String,
    ) : TimelineMemoDisplay

    /** 질문이 없어 기본 문구를 띄운 상태. */
    data class Prompt(
        override val text: String,
    ) : TimelineMemoDisplay
}

/**
 * 메모 영역 표시 내용을 고른다.
 *
 * 읽기·편집 모드가 같은 결과를 낸다. 모드에 따라 영역을 감추면 카드 높이가 달라져 모드를 오갈 때
 * 목록이 밀리고, 메모를 남길 수 있다는 사실도 읽기 모드에서 드러나지 않는다.
 * 편집 가능 여부는 표시가 아니라 클릭에서만 가른다.
 */
internal fun timelineMemoDisplay(
    memo: String?,
    question: String?,
): TimelineMemoDisplay =
    memo?.takeIf(String::isNotBlank)?.let(TimelineMemoDisplay::Memo)
        ?: question?.takeIf(String::isNotBlank)?.let(TimelineMemoDisplay::Question)
        ?: TimelineMemoDisplay.Prompt(DEFAULT_MEMO_PROMPT)

/** 질문이 없을 때 쓰는 기본 안내 문구. 인라인 편집기의 입력 placeholder 로도 쓴다. */
internal const val DEFAULT_MEMO_PROMPT = "이 순간에 대한 메모…"
