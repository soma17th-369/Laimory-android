package com.soma369.laimory.core.collection.location

import javax.inject.Qualifier

/** 위치 추적 토글 전용 DataStore 한정자. 다른 Preferences DataStore 바인딩과 구분한다. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class LocationTrackingDataStore
