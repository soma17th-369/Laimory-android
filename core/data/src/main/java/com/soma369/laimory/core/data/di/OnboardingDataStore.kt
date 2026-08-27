package com.soma369.laimory.core.data.di

import javax.inject.Qualifier

/** 설치 단위 온보딩 진행 상태 전용 DataStore 한정자. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class OnboardingDataStore
