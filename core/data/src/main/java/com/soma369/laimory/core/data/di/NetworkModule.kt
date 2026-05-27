package com.soma369.laimory.core.data.di

import com.soma369.laimory.core.data.BuildConfig
import com.soma369.laimory.core.data.network.adapter.ResultCallAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    /**
     * JSON 파서 설정.
     * - [ignoreUnknownKeys]: 서버가 클라이언트 모델에 없는 필드를 내려줘도 무시
     */
    @Provides
    @Singleton
    fun provideJson(): Json =
        Json {
            ignoreUnknownKeys = true
        }

    /**
     * OkHttpClient 제공.
     * - Debug 빌드에서만 요청/응답 로그를 출력 (BODY 레벨)
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BODY
                        },
                    )
                }
            }
            .build()

    /**
     * Retrofit 인스턴스 제공.
     * - [ResultCallAdapterFactory]: API 메서드 반환 타입을 `Result<T>`로 처리
     * - kotlinx.serialization converter: JSON ↔ DTO 변환
     * - BASE_URL은 BuildConfig를 통해 local.properties에서 주입
     */
    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addCallAdapterFactory(ResultCallAdapterFactory(json))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
}
