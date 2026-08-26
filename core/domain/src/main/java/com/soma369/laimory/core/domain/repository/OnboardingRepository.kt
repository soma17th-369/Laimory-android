package com.soma369.laimory.core.domain.repository

import com.soma369.laimory.core.domain.model.onboarding.OnboardingState
import kotlinx.coroutines.flow.Flow

/**
 * 설치 단위 온보딩 진행 상태의 로컬 저장 계약.
 *
 * 인증 저장소와 **별도 파일**에 둔다. 세션 정리에 함께 지워지면 로그아웃할 때마다 온보딩이
 * 다시 뜬다.
 */
interface OnboardingRepository {
    fun observe(): Flow<OnboardingState>

    /** 마지막으로 본 페이지를 기록한다. 완료 여부는 건드리지 않는다. */
    suspend fun saveProgress(pageKey: String)

    /** 완료로 확정한다. 이후 자동 노출은 없다. */
    suspend fun complete()

    /** 처음 상태로 되돌린다. QA 반복 확인용이며 release 진입점을 두지 않는다. */
    suspend fun reset()
}
