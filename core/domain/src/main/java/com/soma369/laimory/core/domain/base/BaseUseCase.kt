package com.soma369.laimory.core.domain.base

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.HandledException
import com.soma369.laimory.core.domain.notification.UserNotification
import com.soma369.laimory.core.domain.notification.UserNotifier

/**
 * 공통 정책성 예외를 UseCase에서 한 번만 처리하는 base UseCase.
 *
 * 일회성 UseCase는 [execute]로 repository 호출을 감싸 `Result<T>`를 반환한다.
 * 공통 정책(세션 만료 / 일시 오류)은 [UserNotifier]로 알린 뒤 [HandledException]으로 감싸
 * 반환하므로, ViewModel은 이를 재알림하지 않는다. (중복 처리 방지)
 */
abstract class BaseUseCase(
    private val notifier: UserNotifier,
) {
    /** repository 호출을 감싸 성공/실패를 `Result<T>`로 반환한다. 공통 정책은 여기서 1회 처리된다. */
    protected suspend fun <T> execute(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: ApiException) {
            Result.failure(handlePolicy(e))
        }

    private fun handlePolicy(e: ApiException): Throwable {
        val notification = e.toCommonNotification() ?: return e
        notifier.notify(notification)
        return HandledException(e)
    }

    private fun ApiException.toCommonNotification(): UserNotification? =
        when (this) {
            is ApiException.UnauthorizedException -> UserNotification.SessionExpired
            is ApiException.ServerException -> UserNotification.TemporaryUnavailable
            else -> null
        }
}
