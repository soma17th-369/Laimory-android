package com.soma369.laimory.core.domain.repository

import com.soma369.laimory.core.domain.model.analytics.InstallAttribution
import com.soma369.laimory.core.domain.model.analytics.InstallAttributionRecord

/**
 * 설치 귀속을 기기에 남기는 저장소.
 *
 * 설치에 딸린 값이라 로그아웃·계정 전환이 비우지 않고, 재설치 뒤에 되살아나지 않도록 백업하지 않는다.
 */
interface InstallAttributionRepository {
    /** 저장된 상태를 읽는다. 설치 구분 값이 아직 없으면 이때 만든다. */
    suspend fun load(): InstallAttributionRecord

    /** 확정된 결과를 남긴다. 이후로는 다시 조회하지 않는다. */
    suspend fun saveResolved(attribution: InstallAttribution)

    /** 일시 오류로 끝난 실행을 하나 더 세고, 센 뒤의 값을 돌려준다. */
    suspend fun countFailedLaunch(): Int
}
