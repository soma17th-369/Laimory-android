package com.soma369.laimory.core.domain.message

/**
 * 공통 Dialog에 대한 사용자 선택.
 *
 * 호출자에게 값으로만 반환되며, 비즈니스 실행은 호출 ViewModel이 화면 Intent로 변환해 수행한다.
 */
sealed interface DialogResult {
    /** 확인(One-button의 단일 버튼, Two-button의 primary). */
    data object Primary : DialogResult

    /** Two-button의 secondary(취소). */
    data object Secondary : DialogResult

    /** 뒤로가기·바깥 터치 등 선택 없이 닫힘. */
    data object Dismissed : DialogResult
}
