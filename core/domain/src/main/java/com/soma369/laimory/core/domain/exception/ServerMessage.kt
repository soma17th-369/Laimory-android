package com.soma369.laimory.core.domain.exception

/**
 * 사용자에게 보일 실패 문구. 서버가 준 안내([ApiException.serverMessage])가 있으면 그것을, 없으면 [fallback] 을 쓴다.
 *
 * 서버가 에러 코드별 문구를 관리하므로 문구를 바꿀 때 앱을 다시 배포하지 않아도 된다. 앱은 코드로 **동작**(화면 전환·
 * 버튼·재시도)만 가르고, 문구는 서버가 정한다. 서버 응답이 없는 실패(네트워크·시간 초과·앱 안의 검증)는 [fallback] 이 된다.
 *
 * 기능 예외([HandledException]·[TimelineEventUpdateException] 등)는 원본 [ApiException] 을 cause 로 감싸므로 원인 사슬을
 * 따라가 찾는다.
 */
fun Throwable.serverMessageOr(fallback: String): String = findServerMessage() ?: fallback

/** 원인 사슬에서 처음 만나는 서버 안내 문구. 없으면 null. */
fun Throwable.findServerMessage(): String? =
    generateSequence(this) { it.cause }
        .take(MAX_CAUSE_DEPTH)
        .filterIsInstance<ApiException>()
        .firstNotNullOfOrNull { it.serverMessage }

/**
 * 화면이 따로 다루지 않는 [ApiException] 을 공통 스낵바로 알릴 문구. 서버 문구가 없으면 종류별 기본 문구를 쓴다.
 *
 * [ApiException.message] 를 그대로 쓰지 않는다 — 응답 해석 실패처럼 앱이 만든 개발용 문장이나 Spring 기본 오류(`"Not Found"`)가
 * 들어 있을 수 있다.
 */
val ApiException.displayMessage: String
    get() =
        serverMessage ?: when (this) {
            is ApiException.NetworkException -> ApiException.NETWORK_ERROR
            is ApiException.UnauthorizedException -> ApiException.UNAUTHORIZED_ERROR
            is ApiException.ServerException -> ApiException.SERVER_ERROR
            is ApiException.ClientException -> ApiException.CLIENT_ERROR
            is ApiException.ConflictException -> ApiException.CONFLICT_ERROR
            else -> ApiException.UNKNOWN_ERROR
        }

/** 원인 사슬이 순환하는 예외를 대비한 상한. 실제 사슬은 두세 겹이다. */
private const val MAX_CAUSE_DEPTH = 8
