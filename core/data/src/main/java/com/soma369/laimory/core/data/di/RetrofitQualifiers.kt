package com.soma369.laimory.core.data.di

import javax.inject.Qualifier

/** Public API(`/api/{applicationVersion}`) 용 Retrofit. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PublicRetrofit

/** 토큰 발급·갱신·로그아웃용 public prefix Retrofit. 민감 BODY를 로깅하지 않는다. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthSessionRetrofit

/** 인증 필요 API(`/a/api/{applicationVersion}`) 용 Retrofit. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthRetrofit

/** 인증이 필요하면서 요청·응답 BODY 를 모두 로깅하지 않는 API용 Retrofit. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SensitiveAuthRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PublicClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthSessionClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthenticatedClient

/**
 * Bearer 인증은 적용하지만 요청·응답 BODY 는 로깅하지 않는 클라이언트.
 *
 * 보내는 값이 민감한 API(FID 등)와 받는 값이 민감한 API(닉네임 등) 모두 이 클라이언트를 쓴다.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SensitiveAuthenticatedClient

/** S3 presigned 업로드 전용 OkHttpClient. 서버 envelope/인터셉터(특히 debug MockInterceptor)·인증을 타지 않는다. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class S3Client
