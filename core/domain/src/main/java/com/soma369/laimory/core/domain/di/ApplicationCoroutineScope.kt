package com.soma369.laimory.core.domain.di

import javax.inject.Qualifier

/** 앱 프로세스와 수명을 같이하는 coroutine scope 한정자. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationCoroutineScope
