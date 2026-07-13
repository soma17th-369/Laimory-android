package com.soma369.laimory.core.collection.health

import javax.inject.Qualifier

/** 수면 자동 감지 "원함" 의도 저장 전용 DataStore 한정자. 신뢰도 표본 버퍼와 구분한다. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class SleepDetectionDataStore
