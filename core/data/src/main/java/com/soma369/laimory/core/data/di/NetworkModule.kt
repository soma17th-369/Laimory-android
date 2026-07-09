package com.soma369.laimory.core.data.di

import com.soma369.laimory.core.data.BuildConfig
import com.soma369.laimory.core.data.network.ApiPrefix
import com.soma369.laimory.core.data.network.api.Feature1Api
import com.soma369.laimory.core.data.network.api.IntroApi
import com.soma369.laimory.core.data.network.api.TimelineDraftApi
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
    fun provideOkHttpClient(): OkHttpClient =
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

    @Provides
    @Singleton
    @PublicRetrofit
    fun providePublicRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = buildRetrofit(ApiPrefix.publicBaseUrl(BuildConfig.BASE_URL, BuildConfig.API_APP_VERSION), okHttpClient, json)

    /** 인증 필요 API 용. 토큰 발급/부착(인터셉터)은 인증 도입 시 추가한다. */
    @Provides
    @Singleton
    @AuthRetrofit
    fun provideAuthRetrofit(
        okHttpClient: OkHttpClient,
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
    fun provideTimelineDraftApi(
        @AuthRetrofit retrofit: Retrofit,
    ): TimelineDraftApi = retrofit.create(TimelineDraftApi::class.java)
}
