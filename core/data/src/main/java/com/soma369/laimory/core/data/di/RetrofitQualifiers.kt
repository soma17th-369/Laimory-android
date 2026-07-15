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

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PublicClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthSessionClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthenticatedClient

/** S3 presigned 업로드 전용 OkHttpClient. 서버 envelope/인터셉터(특히 debug MockInterceptor)·인증을 타지 않는다. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class S3Client
