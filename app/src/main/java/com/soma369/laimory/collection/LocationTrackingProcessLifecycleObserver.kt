package com.soma369.laimory.collection

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.soma369.laimory.core.domain.di.ApplicationCoroutineScope
import com.soma369.laimory.core.domain.usecase.ReconcileLocationTrackingUseCase
import com.soma369.laimory.core.util.logging.LogDomain
import com.soma369.laimory.core.util.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 앱이 전경으로 올라올 때 위치 수집 실행 상태를 조건에 맞춘다.
 *
 * 전경 진입인 이유가 둘이다. 하나는 제약 — 백그라운드에서는 FGS 를 마음대로 시작할 수 없다.
 * 다른 하나는 이 시점이 **권한이 바뀌는 경로를 모두 지난다**는 것이다. 백그라운드 위치는
 * Android 11+ 에서 시스템 설정으로 나갔다 돌아와야 켜지므로, 설정에서 나중에 허용한 사용자도
 * 여기서 잡힌다. 온보딩만 보고 있으면 그 경로가 통째로 빠진다.
 *
 * 인증 여부를 보지 않는다 — 수집은 기기 단위이고 저장은 로컬이며, 부팅 복원도 같은 규칙이다.
 * 여기서만 세션을 보면 로그인 전후로 켜졌다 꺼졌다 한다.
 *
 * `AutoCollectionProcessLifecycleObserver` 와 자리는 같지만 분리한다. 그쪽은 계정에 매인 일정·건강
 * 수집이고 이쪽은 기기에 매인 서비스 구동이라, 조건도 실패의 의미도 서로 다르다.
 */
@Singleton
class LocationTrackingProcessLifecycleObserver
    @Inject
    constructor(
        private val reconcileLocationTracking: ReconcileLocationTrackingUseCase,
        @ApplicationCoroutineScope private val applicationScope: CoroutineScope,
    ) : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            applicationScope.launch {
                runCatching { reconcileLocationTracking() }
                    .onFailure { e -> Logger.w(LogDomain.COLLECTION, "위치 수집 상태 맞추기 실패: ${e::class.simpleName}") }
            }
        }
    }
