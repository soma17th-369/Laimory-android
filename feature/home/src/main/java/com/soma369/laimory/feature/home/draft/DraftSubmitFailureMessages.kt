package com.soma369.laimory.feature.home.draft

/** 초안 생성 요청 실패 안내 문구. 로딩 화면 안내와 홈 스낵바가 같은 말을 쓴다. */
internal object DraftSubmitFailureMessages {
    const val TERMS_REQUIRED = "타임라인을 만들려면 약관 동의가 필요해요."
    const val NO_NEW_ITEMS = "이미 기록에 들어간 것뿐이라 새로 더할 게 없어요."
    const val PHOTO_ACCESS = "고른 사진 중 열 수 없는 사진이 있어요. 사진을 다시 골라 주세요."
    const val PHOTO_LIMIT_SUFFIX = "사진 선택에서 개수를 줄여주세요."
    const val TIMEOUT = "응답이 없어 요청을 멈췄어요. 잠시 후 다시 시도해 주세요."
    const val OTHER = "초안 생성 요청을 보내지 못했어요."
}
