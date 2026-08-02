package com.soma369.laimory.core.domain.di

import javax.inject.Qualifier

/** SourceItem 보존 경계를 계산할 때 실행 시점의 기기 시간대를 제공하는 함수 한정자. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RetentionZoneId
