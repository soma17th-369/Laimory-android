package com.soma369.laimory.core.domain.model.analytics

/**
 * 튜토리얼의 장. 선언 순서가 튜토리얼 안의 고정 위치([index])다.
 *
 * 그 회차에 실제로 건너뛴 장(기록이 있는 날의 사진 시트 장, 저장한 기록 다시 보기의 저장 장)이 있어도 번호를 당기지 않는다 —
 * 퍼널의 칸이 회차마다 같은 장을 가리켜야 한다.
 */
enum class AnalyticsTutorialStep(
    val tutorial: AnalyticsTutorial,
) {
    /** 홈 추천 날짜 장. 처음 볼 때만 있고, 설정에서 다시 보면 코치마크부터 시작한다. */
    HOME_DATE_RECOMMENDATION(AnalyticsTutorial.HOME),
    HOME_DATE(AnalyticsTutorial.HOME),
    HOME_PHOTO(AnalyticsTutorial.HOME),
    HOME_PHOTO_SHEET_LIST(AnalyticsTutorial.HOME),
    HOME_PHOTO_SHEET_DONE(AnalyticsTutorial.HOME),
    HOME_SOURCES(AnalyticsTutorial.HOME),
    HOME_CREATE(AnalyticsTutorial.HOME),

    TIMELINE_EVENT(AnalyticsTutorial.TIMELINE),
    TIMELINE_MEMO(AnalyticsTutorial.TIMELINE),
    TIMELINE_ADD_EVENT(AnalyticsTutorial.TIMELINE),
    TIMELINE_SAVE(AnalyticsTutorial.TIMELINE),
    ;

    /** 튜토리얼 안에서의 위치. 0부터. */
    val index: Int
        get() = entries.filter { it.tutorial == tutorial }.indexOf(this)
}
