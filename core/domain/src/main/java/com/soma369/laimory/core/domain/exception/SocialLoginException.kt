package com.soma369.laimory.core.domain.exception

/** OAuth callback과 앱에 보관된 PKCE 시도의 불일치를 나타낸다. */
sealed class SocialLoginException(
    message: String,
) : Exception(message) {
    data object InvalidCallback : SocialLoginException("올바르지 않은 로그인 응답입니다. 다시 시도해 주세요.")

    data object MissingAttempt : SocialLoginException("로그인 요청이 만료되었습니다. 다시 시도해 주세요.")

    class ProviderFailure(
        val errorCode: String,
    ) : SocialLoginException("소셜 로그인을 완료하지 못했습니다. 다시 시도해 주세요.")
}
