package com.soma369.laimory.core.domain.coordinator

import com.soma369.laimory.core.domain.model.collection.AutoCollectionResult

/**
 * 일정·건강 자동 수집을 한 곳에서 소유하는 조율자.
 *
 * 앱 전경 진입, 초안 날짜 확정, 생성 설정 진입, 최종 생성이 모두 같은 작업을 공유한다.
 * 유형별로 마지막 결과 시각을 따로 들고 있어 한쪽이 실패해도 다른 쪽의 캐시가 무효화되지 않는다.
 *
 * 결과를 로그로 남기지 않고 값으로 돌려준다 — 이 모듈은 플랫폼 의존이 없고, 무엇을 어떤 수준으로
 * 기록할지는 호출 계층이 정한다.
 */
interface AutoCollectionCoordinator {
    /**
     * 다시 볼 유형만 수집하고 결과를 돌려준다.
     *
     * 이미 진행 중이면 새 작업을 만들지 않고 그 작업을 기다린다. 모든 유형이 최근에 끝났으면
     * 즉시 빈 결과로 돌아온다. **호출자가 취소돼도 수집 자체는 멈추지 않는다** — 작업을
     * 애플리케이션 scope 가 소유하므로 취소는 기다림만 끊는다.
     *
     * @param timeoutMillis 기다림의 상한. null 이면 끝날 때까지 기다린다. 상한을 넘기면
     *   [AutoCollectionResult.timedOut] 으로 돌아오며 수집은 계속돼 다음 호출이 그 결과를 쓴다.
     */
    suspend fun refresh(timeoutMillis: Long? = null): AutoCollectionResult

    /**
     * 인증 경계가 바뀌면 최신성 상태를 버린다.
     *
     * 이전 계정에서 수집한 시각을 새 계정이 물려받으면, 새 계정의 첫 생성이 "5분 이내라 재사용"
     * 으로 건너뛴다.
     */
    fun discard()
}
