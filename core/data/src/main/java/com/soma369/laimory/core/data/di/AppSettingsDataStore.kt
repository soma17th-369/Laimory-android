package com.soma369.laimory.core.data.di

import javax.inject.Qualifier

/** 계정과 무관한 설치 단위 앱 설정(화면 모드 등) 전용 DataStore 한정자. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class AppSettingsDataStore
