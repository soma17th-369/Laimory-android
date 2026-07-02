package com.soma369.laimory.core.domain.exception

/**
 * UseCase에서 공통/기능 정책으로 이미 처리(알림·네비게이션)된 실패를 표시하는 마커.
 *
 * ViewModel은 이 예외를 받으면 화면 상태만 복구하고 추가 피드백(스낵바 등)은 하지 않는다.
 * (중복 알림 방지)
 *
 * @property cause 원본 예외(주로 [ApiException]).
 */
class HandledException(
    override val cause: Throwable,
) : Exception(cause)
