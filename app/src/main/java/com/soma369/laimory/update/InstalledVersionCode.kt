package com.soma369.laimory.update

import javax.inject.Qualifier

/**
 * 지금 설치된 빌드의 `versionCode`.
 *
 * 판정에 쓰는 값을 주입으로 받는다 — 상수를 직접 읽으면 버전 조합을 테스트할 수 없다.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class InstalledVersionCode
