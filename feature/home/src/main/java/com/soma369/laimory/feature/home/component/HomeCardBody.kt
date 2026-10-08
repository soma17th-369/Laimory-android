package com.soma369.laimory.feature.home.component

/** 홈 원천 카드 본문 한 줄. */
internal sealed interface HomeCardBody {
    /**
     * 모인 것이 있을 때. 화면에는 `N / M`(Figma `Home / SourceCard`) — 보낼 수 N 을 강조하고, 낭독은 [spoken]
     * (`15장 중 5장`)으로 한다. `5 / 15` 를 소리 내어 읽으면 무엇의 몇인지 알 수 없다.
     */
    data class Count(
        val sending: Int,
        val candidate: Int,
        val spoken: String,
    ) : HomeCardBody

    /** 모인 것이 없을 때의 상태 문구(`탭하여 허용`·`아직 모인 것이 없어요` 등). */
    data class Message(
        val text: String,
    ) : HomeCardBody
}
