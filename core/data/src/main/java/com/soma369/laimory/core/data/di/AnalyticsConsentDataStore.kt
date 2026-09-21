package com.soma369.laimory.core.data.di

import javax.inject.Qualifier

/** 제품 분석 수집 동의 전용 DataStore 한정자. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class AnalyticsConsentDataStore
