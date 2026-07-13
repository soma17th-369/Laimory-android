package com.soma369.laimory.core.collection.health.sleep.detection

import javax.inject.Qualifier

/** 수면 신뢰도 표본 버퍼 전용 DataStore 한정자. 다른 Preferences DataStore 바인딩과 구분한다. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class SleepClassifyDataStore
