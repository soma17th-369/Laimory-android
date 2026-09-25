package com.soma369.laimory.core.domain.model.analytics

/**
 * 사건 수정에서 바뀐 입력칸. `changed_field_count` 는 이 칸 수로 센다.
 *
 * 칸마다 속한 범위([scope])가 정해져 있다 — 제목과 설명만 바꾸면 2칸이고 범위는 [AnalyticsUpdateScope.CONTENT] 다.
 */
enum class AnalyticsEventField(
    val scope: AnalyticsUpdateScope,
) {
    EVENT_TYPE(AnalyticsUpdateScope.CONTENT),
    TITLE(AnalyticsUpdateScope.CONTENT),
    SUBTITLE(AnalyticsUpdateScope.CONTENT),
    START_AT(AnalyticsUpdateScope.TIME),
    END_AT(AnalyticsUpdateScope.TIME),

    /** 사진을 더하거나 뺐다. 몇 장이든 한 칸이다. */
    PHOTO(AnalyticsUpdateScope.PHOTO),
    MEMO(AnalyticsUpdateScope.MEMO),
}

/** 사건 수정의 범위. 두 범위 이상에 걸치면 [COMBINED] 다. */
enum class AnalyticsUpdateScope {
    CONTENT,
    TIME,
    PHOTO,
    MEMO,
    COMBINED,
    ;

    companion object {
        /** 바뀐 칸이 없으면 null — 보낼 수정이 아니다. */
        fun of(fields: Set<AnalyticsEventField>): AnalyticsUpdateScope? {
            val scopes = fields.mapTo(mutableSetOf()) { it.scope }
            return when (scopes.size) {
                0 -> null
                1 -> scopes.single()
                else -> COMBINED
            }
        }
    }
}
