package com.soma369.laimory.core.data.network.interceptor

import com.soma369.laimory.core.domain.coordinator.TermsGateSignal
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 약관 미동의 거절을 통신 경계에서 한 번만 알아챈다.
 *
 * 서버는 인증 API 대부분을 이용약관 동의 여부로 막고, 초안 생성·사진 업로드 발급은 단계 동의를
 * 더 요구한다. 거절은 어느 호출에서든 오고 여러 호출에서 동시에 오므로, 화면이나 UseCase 마다
 * 받아 처리하면 반드시 빠지는 곳이 생긴다. 여기서 신호만 올리고 판정과 화면 전환은
 * [com.soma369.laimory.core.domain.coordinator.TermsAgreementCoordinator] 가 맡는다.
 *
 * 응답은 [Response.peekBody] 로 들여다본다 — 본문을 소비하면 실제 호출부가 읽을 것이 없어진다.
 * 그리고 신호만 올릴 뿐 응답을 바꾸지 않는다. 원요청의 성패는 원래대로 호출부가 판단한다.
 */
@Singleton
internal class TermsGateInterceptor
    @Inject
    constructor(
        private val gateSignal: TermsGateSignal,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val response = chain.proceed(chain.request())
            if (response.code != FORBIDDEN) return response
            if (response.errorCode() == TERMS_AGREEMENT_REQUIRED) gateSignal.notifyAgreementRequired()
            return response
        }

        /** 본문이 없거나 형태가 다르면 조용히 지나간다 — 신호를 못 올린 대가는 화면이 늦게 갈리는 것뿐이다. */
        private fun Response.errorCode(): Int? =
            try {
                val raw = peekBody(PEEK_LIMIT).string()
                val header = json.parseToJsonElement(raw).let { it as? JsonObject }?.get("header") as? JsonObject
                header?.get("code")?.jsonPrimitive?.intOrNull
            } catch (_: Exception) {
                null
            }

        private companion object {
            const val FORBIDDEN = 403
            const val TERMS_AGREEMENT_REQUIRED = -3001

            /** 공통 envelope 의 header 만 보면 되므로 본문 전체를 메모리에 올리지 않는다. */
            const val PEEK_LIMIT = 4096L

            val json = Json { ignoreUnknownKeys = true }
        }
    }
