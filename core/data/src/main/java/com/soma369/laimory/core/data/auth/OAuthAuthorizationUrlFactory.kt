package com.soma369.laimory.core.data.auth

import com.soma369.laimory.core.data.BuildConfig
import com.soma369.laimory.core.domain.model.auth.SocialLoginProvider
import okhttp3.HttpUrl.Companion.toHttpUrl
import javax.inject.Inject

/** 앱 빌드의 서버 origin에 제공자별 OAuth 시작 경로와 challenge를 결합한다. */
internal class OAuthAuthorizationUrlFactory
    @Inject
    constructor() {
        fun create(
            provider: SocialLoginProvider,
            challenge: String,
        ): String =
            BuildConfig.BASE_URL.toHttpUrl()
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
