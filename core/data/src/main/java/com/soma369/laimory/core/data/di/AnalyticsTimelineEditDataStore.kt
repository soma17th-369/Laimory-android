package com.soma369.laimory.core.data.di

import javax.inject.Qualifier

/** 완료 전 편집 흔적 전용 DataStore 한정자. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class AnalyticsTimelineEditDataStore
