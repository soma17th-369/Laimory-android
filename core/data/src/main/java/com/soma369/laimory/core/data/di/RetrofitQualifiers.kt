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

/**
 * 게시된 약관 원문의 정본 위치. **열람 링크 전용 임시 값**이다.
 *
 * 빈 문자열이면 대체 조회 자체가 돌지 않는다(운영 빌드). 개발 catalog 에 seed 가 들어가면
 * 이 한정자와 빌드 필드를 함께 지운다.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PublishedTermsBaseUrl

/**
 * 이 프로세스가 붙는 서버의 도메인 루트(`https://host/`). API baseUrl·OAuth 시작 주소·로그인 콜백 host 가
 * 모두 이 값에서 나온다. 기본은 빌드 값이고 debug 만 콜드 스타트에서 고른 값으로 바뀐다([ApiOriginModule]).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApiBaseUrl
