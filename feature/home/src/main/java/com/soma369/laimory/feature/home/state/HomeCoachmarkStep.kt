package com.soma369.laimory.feature.home.state

/**
 * 홈 튜토리얼 코치마크의 단계. 선언 순서대로 짚는다.
 *
 * 추천 날짜 시트에서 날을 고르면(직접 고르기는 피커를 닫은 뒤) 첫 단계부터 시작한다. 마지막 단계는 타임라인 만들기 버튼이다 —
 * 사진을 고르고 나서 누를 곳으로 끝낸다.
 *
 * [PHOTO] 다음에는 실제 사진 시트를 열고, 시트 위에서 사진 목록([PHOTO_SHEET_LIST])과 아래 버튼([PHOTO_SHEET_DONE])을 짚는다.
 * 코치마크 창은 누름을 모두 받아 그동안은 고를 수 없으므로, 두 단계를 마치면 코치마크를 걷고 직접 고르게 한다. 시트를 닫으면
 * [SOURCES] 부터 잇는다. 이미 기록이 있는 날은 사진을 바꿀 수 없어 시트를 열지 않고 시트 단계를 건너뛴다.
 */
enum class HomeCoachmarkStep(
    val message: String,
    /** 이미 기록이 있는 날의 문구. 그 날 CTA 는 만들기가 아니라 기록 열기라 같은 말을 쓸 수 없다. 없으면 [message]. */
    private val recordedDayMessage: String? = null,
) {
    DATE("만들 날짜예요. 눌러서 다른 날을 고르거나 기록 범위를 바꿀 수 있어요."),
    PHOTO(
        message = "이 날 찍은 사진이에요. 타임라인에 담을 사진은 직접 골라요. 다음으로 넘기면 고르는 창을 열어 볼게요.",
        recordedDayMessage = "이 날 찍은 사진이에요. 타임라인을 만들 때 담을 사진을 여기서 직접 골라요.",
    ),
    PHOTO_SHEET_LIST("이 날 찍은 사진이 모두 모여 있어요. 눌러서 타임라인에 담을 사진을 골라요."),
    PHOTO_SHEET_DONE("다 골랐으면 여기를 눌러 홈으로 돌아가요. 고르지 않고 닫아도 괜찮아요. 이제 직접 골라 보세요."),
    SOURCES("일정·위치·알림도 함께 담겨요. 카드를 눌러 무엇이 담기는지 보고 뺄 수 있어요."),
    CREATE(
        message = "다 골랐으면 여기를 눌러 타임라인을 만들어 보세요.",
        recordedDayMessage = "이 날은 이미 타임라인이 있어요. 여기를 눌러 열어 볼 수 있어요.",
    ),
    ;

    /** 사진 시트 위에서 짚는 단계. 홈이 아니라 시트가 그린다 — 시트 창보다 위에 떠야 한다. */
    val isInPhotoSheet: Boolean
        get() = this == PHOTO_SHEET_LIST || this == PHOTO_SHEET_DONE

    /** [hasRecord] 는 지금 날짜에 열어 볼 기록이 있는지(CTA 가 `타임라인 확인하기`). */
    fun messageFor(hasRecord: Boolean): String = recordedDayMessage?.takeIf { hasRecord } ?: message
}
