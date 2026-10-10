package com.soma369.laimory.core.domain.usecase.credit

import com.soma369.laimory.core.domain.model.credit.TimelineCredit
import com.soma369.laimory.core.domain.repository.CreditRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 만들기 확인 다이얼로그에 보일 크레딧 — 비용과 잔액을 함께 받는다.
 *
 * **어느 쪽이든 못 받으면 `null` 이다(fail-open).** 화면은 크레딧 줄을 숨기고 `만들기` 를 그대로 둔다 — 최종 판정은
 * 서버의 `-1021` 이 한다. 크레딧 조회 실패 때문에 만들기가 막히면 안 된다.
 *
 * [com.soma369.laimory.core.domain.base.BaseUseCase] 를 쓰지 않는다. 그쪽은 404 · 5xx 에 공통 안내를 띄우는데, 운영 서버에
 * 아직 이 API 가 없을 때(404)나 크레딧 행이 없을 때(500) 다이얼로그 뒤로 "지원하지 않는 기능" 같은 안내가 뜨면 안 된다.
 */
class GetTimelineCreditUseCase
    @Inject
    constructor(
        private val repository: CreditRepository,
    ) {
        suspend operator fun invoke(): TimelineCredit? =
            try {
                withTimeoutOrNull(TIMEOUT_MILLIS) {
                    coroutineScope {
                        val costs = async { repository.getCosts() }
                        val remaining = async { repository.getRemainingCredits() }
                        TimelineCredit(cost = costs.await().timelineCreation, remaining = remaining.await())
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }

        private companion object {
            /** 다이얼로그는 이 값을 받은 뒤에 뜬다. 오래 기다리게 할 값이 아니라 넘으면 줄 없이 띄운다. */
            const val TIMEOUT_MILLIS = 3_000L
        }
    }
