package com.soma369.laimory.core.domain.exception

/**
 * 문의가 없거나 내 문의가 아니다. 서버가 둘을 같은 404 로 숨긴다.
 *
 * 공용 404 안내("지원하지 않는 기능")로 흘리지 않고 화면이 직접 안내하도록 따로 세운다.
 */
class InquiryNotFoundException(
    override val cause: ApiException,
) : Exception(cause.message, cause)
