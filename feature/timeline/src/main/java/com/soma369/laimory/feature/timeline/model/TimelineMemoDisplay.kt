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
 * 메모 영역 표시 내용을 고른다. `null` 이면 영역을 그리지 않는다.
 *
 * 읽기 모드([isEditable] = false)에서 메모와 질문이 모두 없으면 기본 문구를 띄우지 않고 감춘다 —
 * 누를 수 없는 빈 입력칸을 남기지 않기 위해서다.
 */
internal fun timelineMemoDisplay(
    memo: String?,
    question: String?,
    isEditable: Boolean,
): TimelineMemoDisplay? =
    memo?.takeIf(String::isNotBlank)?.let(TimelineMemoDisplay::Memo)
        ?: question?.takeIf(String::isNotBlank)?.let(TimelineMemoDisplay::Question)
        ?: TimelineMemoDisplay.Prompt(DEFAULT_MEMO_PROMPT).takeIf { isEditable }

/** 질문이 없을 때 쓰는 기본 안내 문구. 인라인 편집기의 입력 placeholder 로도 쓴다. */
internal const val DEFAULT_MEMO_PROMPT = "이 순간에 대한 메모…"
