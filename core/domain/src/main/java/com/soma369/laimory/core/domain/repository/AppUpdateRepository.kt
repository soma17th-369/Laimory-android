package com.soma369.laimory.core.domain.repository

import com.soma369.laimory.core.domain.model.update.DismissedRecommendation
import java.time.Instant

/**
 * 권장 업데이트 보류 기록.
 *
 * **강제 판정은 여기에 남기지 않는다.** 영속하면 서버가 하한선을 되돌려도 오프라인 사용자가
 * 빠져나올 길이 없다. 강제는 살아 있는 프로세스에서만 기억한다.
 */
interface AppUpdateRepository {
    /** 미뤄 둔 권장 업데이트. 미룬 적이 없거나 읽지 못하면 `null`. */
    suspend fun dismissedRecommendation(): DismissedRecommendation?

    suspend fun dismissRecommendation(
        version: Int,
        at: Instant,
    )
}
