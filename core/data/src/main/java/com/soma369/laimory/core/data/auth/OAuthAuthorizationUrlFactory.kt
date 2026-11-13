package com.soma369.laimory.core.data.auth

import com.soma369.laimory.core.data.di.ApiBaseUrl
import com.soma369.laimory.core.domain.model.auth.SocialLoginProvider
import okhttp3.HttpUrl.Companion.toHttpUrl
import javax.inject.Inject

/** 이 프로세스가 붙는 서버 origin에 제공자별 OAuth 시작 경로와 challenge를 결합한다. */
internal class OAuthAuthorizationUrlFactory
    @Inject
    constructor(
        @ApiBaseUrl private val baseUrl: String,
    ) {
        fun create(
            provider: SocialLoginProvider,
            challenge: String,
        ): String =
            baseUrl.toHttpUrl()
                .newBuilder()
                .addPathSegments("oauth2/authorization")
                .addPathSegment(provider.pathSegment)
                .addQueryParameter("app_challenge", challenge)
                .build()
                .toString()

        private val SocialLoginProvider.pathSegment: String
            get() =
                when (this) {
                    SocialLoginProvider.GOOGLE -> "google"
                    SocialLoginProvider.KAKAO -> "kakao"
                }
    }
