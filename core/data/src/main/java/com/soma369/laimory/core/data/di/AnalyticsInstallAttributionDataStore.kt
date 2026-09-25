package com.soma369.laimory.core.data.di

import javax.inject.Qualifier

/** 설치 유입 귀속 전용 DataStore 한정자. 백업에서 제외되는 파일이다. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class AnalyticsInstallAttributionDataStore
