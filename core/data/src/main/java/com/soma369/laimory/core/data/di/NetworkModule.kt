package com.soma369.laimory.core.data.di

import com.soma369.laimory.core.data.BuildConfig
import com.soma369.laimory.core.data.network.ApiPrefix
import com.soma369.laimory.core.data.network.api.AuthApi
import com.soma369.laimory.core.data.network.api.Feature1Api
import com.soma369.laimory.core.data.network.api.IntroApi
import com.soma369.laimory.core.data.network.api.OnboardingApi
import com.soma369.laimory.core.data.network.api.PushRegistrationApi
import com.soma369.laimory.core.data.network.api.TimelineDraftApi
import com.soma369.laimory.core.data.network.api.TimelineRecordApi
import com.soma369.laimory.core.data.network.api.UserApi
import com.soma369.laimory.core.data.network.interceptor.AuthTokenAuthenticator
import com.soma369.laimory.core.data.network.interceptor.AuthTokenInterceptor
import com.soma369.laimory.core.data.network.interceptor.MockInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @OptIn(ExperimentalSerializationApi::class)
    @Provides
    @Singleton
    fun provideJson(): Json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            coerceInputValues = true
        }

    @Provides
    @Singleton
    @PublicClient
    fun providePublicOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(MockInterceptor())
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BODY
                        },
                    )
                }
            }
            .build()

    /** 토큰 응답과 verifier가 BODY 로그에 노출되지 않는 인증 세션 전용 클라이언트. */
    @Provides
    @Singleton
    @AuthSessionClient
    fun provideAuthSessionOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .apply {
                if (BuildConfig.DEBUG) addInterceptor(MockInterceptor())
            }
            .build()

    /** Bearer 인증이 필요한 `/a/api` 전용 클라이언트. */
    @Provides
    @Singleton
    @AuthenticatedClient
    internal fun provideAuthenticatedOkHttpClient(
        tokenInterceptor: AuthTokenInterceptor,
        tokenAuthenticator: AuthTokenAuthenticator,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(tokenInterceptor)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(MockInterceptor())
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            redactHeader(AuthTokenInterceptor.AUTHORIZATION)
                            level = HttpLoggingInterceptor.Level.BODY
                        },
                    )
                }
            }
            .authenticator(tokenAuthenticator)
            .build()

    /**
     * 요청이나 응답 BODY 가 민감한 Bearer 인증 API 전용 클라이언트.
     *
     * FID 처럼 보내는 값이 민감한 경우와 닉네임처럼 받는 값이 민감한 경우를 모두 포함한다.
     * debug 에서도 [HttpLoggingInterceptor] 를 붙이지 않아 양쪽 BODY 가 로그에 남지 않는다.
     */
    @Provides
    @Singleton
    @SensitiveAuthenticatedClient
    internal fun provideSensitiveAuthenticatedOkHttpClient(
        tokenInterceptor: AuthTokenInterceptor,
        tokenAuthenticator: AuthTokenAuthenticator,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(tokenInterceptor)
            .apply {
                if (BuildConfig.DEBUG) addInterceptor(MockInterceptor())
            }.authenticator(tokenAuthenticator)
            .build()

    @Provides
    @Singleton
    @PublicRetrofit
    fun providePublicRetrofit(
        @PublicClient okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = buildRetrofit(ApiPrefix.publicBaseUrl(BuildConfig.BASE_URL, BuildConfig.API_APP_VERSION), okHttpClient, json)

    @Provides
    @Singleton
    @AuthSessionRetrofit
    fun provideAuthSessionRetrofit(
        @AuthSessionClient okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = buildRetrofit(ApiPrefix.publicBaseUrl(BuildConfig.BASE_URL, BuildConfig.API_APP_VERSION), okHttpClient, json)

    @Provides
    @Singleton
    @AuthRetrofit
    fun provideAuthRetrofit(
        @AuthenticatedClient okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = buildRetrofit(ApiPrefix.authBaseUrl(BuildConfig.BASE_URL, BuildConfig.API_APP_VERSION), okHttpClient, json)

    @Provides
    @Singleton
    @SensitiveAuthRetrofit
    fun provideSensitiveAuthRetrofit(
        @SensitiveAuthenticatedClient okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = buildRetrofit(ApiPrefix.authBaseUrl(BuildConfig.BASE_URL, BuildConfig.API_APP_VERSION), okHttpClient, json)

    /**
     * S3 presigned 업로드 전용 OkHttpClient.
     *
     * 공용 클라이언트의 debug MockInterceptor 가 S3 요청을 가로채면 안 되고, 우리 서버 인터셉터/인증도
     * 태우면 안 되므로 별도로 둔다. 큰 사진 업로드를 고려해 write/read 타임아웃을 넉넉히 잡는다.
     */
    @Provides
    @Singleton
    @S3Client
    fun provideS3OkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

    private fun buildRetrofit(
        baseUrl: String,
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideFeature1Api(
        @PublicRetrofit retrofit: Retrofit,
    ): Feature1Api = retrofit.create(Feature1Api::class.java)

    @Provides
    @Singleton
    fun provideIntroApi(
        @PublicRetrofit retrofit: Retrofit,
    ): IntroApi = retrofit.create(IntroApi::class.java)

    @Provides
    @Singleton
    fun provideAuthApi(
        @AuthSessionRetrofit retrofit: Retrofit,
    ): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideTimelineDraftApi(
        @AuthRetrofit retrofit: Retrofit,
    ): TimelineDraftApi = retrofit.create(TimelineDraftApi::class.java)

    @Provides
    @Singleton
    fun provideTimelineRecordApi(
        @AuthRetrofit retrofit: Retrofit,
    ): TimelineRecordApi = retrofit.create(TimelineRecordApi::class.java)

    @Provides
    @Singleton
    fun provideUserApi(
        @SensitiveAuthRetrofit retrofit: Retrofit,
    ): UserApi = retrofit.create(UserApi::class.java)

    @Provides
    @Singleton
    fun provideOnboardingApi(
        @AuthRetrofit retrofit: Retrofit,
    ): OnboardingApi = retrofit.create(OnboardingApi::class.java)

    @Provides
    @Singleton
    fun providePushRegistrationApi(
        @SensitiveAuthRetrofit retrofit: Retrofit,
    ): PushRegistrationApi = retrofit.create(PushRegistrationApi::class.java)
}
