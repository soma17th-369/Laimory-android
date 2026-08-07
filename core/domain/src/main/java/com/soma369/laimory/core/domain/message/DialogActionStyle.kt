package com.soma369.laimory.core.domain.message

/**
 * Dialog 확인 액션의 중립적인 스타일 표현.
 *
 * 실제 색·컴포넌트 매핑(`LaimoryDialogActionStyle`)은 Root 렌더링 책임이다.
 */
enum class DialogActionStyle {
    PRIMARY,

    /** 삭제·로그아웃처럼 되돌리기 어려운 확인 액션. */
    DESTRUCTIVE,
}
