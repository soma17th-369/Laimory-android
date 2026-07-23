package com.soma369.laimory.core.data.di

import javax.inject.Qualifier

/** 활성 타임라인 초안 작업 전용 DataStore 한정자. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class ActiveDraftTaskDataStore
