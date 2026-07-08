package com.soma369.laimory.core.data.di

import javax.inject.Qualifier

/** Public API(`/api/{applicationVersion}`) 용 Retrofit. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PublicRetrofit

/** 인증 필요 API(`/a/api/{applicationVersion}`) 용 Retrofit. 토큰 인터셉터는 인증 도입 시 부착한다. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthRetrofit
