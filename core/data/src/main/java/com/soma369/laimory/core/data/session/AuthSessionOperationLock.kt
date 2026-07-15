package com.soma369.laimory.core.data.session

import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject
import javax.inject.Singleton

/** 토큰 발급·회전·로그아웃이 서로 덮어써 세션을 부활시키지 않도록 직렬화한다. */
@Singleton
internal class AuthSessionOperationLock
    @Inject
    constructor() {
        val mutex = Mutex()
    }
