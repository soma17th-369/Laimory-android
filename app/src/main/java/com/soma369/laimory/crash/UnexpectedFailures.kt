package com.soma369.laimory.crash

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.HandledException
import kotlin.coroutines.cancellation.CancellationException

/**
 * 원격 non-fatal 로 보고할 만한 실패인지 판정한다. `Logger.isUnexpectedFailure` 에 꽂는다.
 *
 * 기준은 "우리가 몰랐던 일인가" 하나다. 서버가 알려 준 오류나 통신 사정은 이미 화면이 다루고 있고,
 * 그것까지 올리면 노이즈에 묻혀 진짜 신호를 못 본다.
 *
 * 판정을 여기 한 곳에 모은다. `Logger.e` 를 부르는 자리는 화면·워커·푸시 등 여럿이고, 그중에는
 * 실패 종류를 가리지 않고 넘기는 곳도 있다(FID 등록 실패가 네트워크 끊김이어도 `Logger.e` 로 온다).
 * 호출부마다 거르게 하면 새 호출부가 생길 때마다 빠뜨린다.
 */
internal fun isUnexpectedFailure(throwable: Throwable): Boolean =
    when (throwable) {
        // 화면을 떠난 것이지 실패가 아니다.
        is CancellationException -> false
        // UseCase 가 공통 정책으로 이미 다뤘다.
        is HandledException -> false
        /*
         * 서버가 알려 준 오류가 아니라 **우리 쪽에서 뜻을 알 수 없었던** 응답이다. 역직렬화 실패,
         * 성공 응답의 body 누락, 모르는 상태 코드가 전부 여기로 온다(`safeApiCall` 참고). 서버가
         * 계약을 바꿔 앱이 조용히 깨지는 상황이 바로 이 모양이라, 이것만은 반드시 보고한다.
         */
        is ApiException.UnknownException -> true
        // 나머지 ApiException 은 서버가 알려 준 도메인 오류이거나 통신 사정이다.
        is ApiException -> false
        else -> true
    }
