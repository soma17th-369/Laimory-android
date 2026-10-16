package com.soma369.laimory.feature.settings.credit

import com.soma369.laimory.core.domain.model.credit.TimelineCredit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 마지막으로 받은 크레딧을 앱 메모리에 기억한다. 설정 계정 카드와 크레딧 화면이 같이 읽고 쓴다.
 *
 * 화면에 들어오면 이 값을 바로 보이고, 뒤에서 다시 받아 조용히 바꾼다 — 잔액 조회는 보통 0.1초 안에 끝나, 들어올 때마다
 * 비우고 로딩을 띄우면 한순간 번쩍여 버그처럼 보인다.
 *
 * 디스크에는 두지 않는다. 잔액은 다른 기기 · 결과 저장으로 바뀌는 서버 값이라 기기에 저장하지 않는다 — 프로세스가 끝나면
 * 사라진다. **계정 경계에서 [clear] 한다**(로그아웃 · 다른 계정 로그인). 비우기 전에 보낸 조회의 응답은 이전 계정의 값이라,
 * 조회를 보낼 때 받은 [generation] 이 그사이 바뀌었으면 버린다.
 */
@Singleton
class CreditBalanceStore
    @Inject
    constructor() {
        private val mutableBalance = MutableStateFlow(CreditBalance())
        val balance: StateFlow<CreditBalance> = mutableBalance.asStateFlow()

        /** 계정 경계마다 오르는 번호. 조회를 보낼 때 받아 두고 응답을 넣을 때 함께 넘긴다. */
        var generation: Long = 0L
            private set

        /** 남은 크레딧만 받은 경우(설정 계정 카드). 못 받았으면 `null` — 옛 값을 남기면 줄어든 뒤에도 옛 잔액이 보인다. */
        fun updateRemaining(
            requestedGeneration: Long,
            remaining: Int?,
        ) {
            if (requestedGeneration != generation) return
            mutableBalance.update { it.copy(remaining = remaining) }
        }

        /** 잔액과 비용을 함께 받은 경우(크레딧 화면). 못 받았으면 잔액을 비운다. 비용은 이전 값을 둔다 — 서버 상수라 잘 바뀌지 않는다. */
        fun updateCredit(
            requestedGeneration: Long,
            credit: TimelineCredit?,
        ) {
            if (requestedGeneration != generation) return
            mutableBalance.update { current ->
                if (credit == null) current.copy(remaining = null) else CreditBalance(remaining = credit.remaining, cost = credit.cost)
            }
        }

        fun clear() {
            generation++
            mutableBalance.value = CreditBalance()
        }
    }
