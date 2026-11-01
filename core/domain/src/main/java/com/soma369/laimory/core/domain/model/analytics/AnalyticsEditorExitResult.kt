package com.soma369.laimory.core.domain.model.analytics

/** 사건 편집 화면을 어떻게 떠났는지. */
enum class AnalyticsEditorExitResult {
    /** 저장에 성공하고 나갔다. */
    SAVED,

    /** 사건을 지우고 나갔다. */
    DELETED,

    /** 바꾼 것 없이 뒤로 나갔다. */
    UNCHANGED,

    /** 바꾼 뒤 저장하지 않고 나가기를 골랐다. */
    DISCARDED,
}
