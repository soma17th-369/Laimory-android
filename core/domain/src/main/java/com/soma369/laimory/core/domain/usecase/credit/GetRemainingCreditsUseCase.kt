package com.soma369.laimory.core.domain.usecase.credit

import com.soma369.laimory.core.domain.repository.CreditRepository
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 계정 카드에 보일 남은 크레딧. **못 받으면 `null`** 이고 화면은 칸을 비운다 — 0 으로 보이면 남은 게 없는 줄 안다.
 *
 * [GetTimelineCreditUseCase] 와 같은 이유로 공통 안내를 띄우지 않는다. 운영 서버에 API 가 없을 때(404)나 크레딧 행이
 * 없을 때(500) 설정에 들어올 때마다 "지원하지 않는 기능" 안내가 뜨면 안 된다.
 */
class GetRemainingCreditsUseCase
    @Inject
    constructor(
        private val repository: CreditRepository,
    ) {
        suspend operator fun invoke(): Int? =
            try {
                repository.getRemainingCredits()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
    }
