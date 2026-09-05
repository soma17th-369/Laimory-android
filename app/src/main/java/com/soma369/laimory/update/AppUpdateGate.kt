package com.soma369.laimory.update

import com.soma369.laimory.core.domain.model.IntroInfo
import com.soma369.laimory.core.domain.model.update.AppUpdateRequirement
import com.soma369.laimory.core.domain.model.update.hides
import com.soma369.laimory.core.domain.repository.AppUpdateRepository
import com.soma369.laimory.core.domain.usecase.GetIntroInfoUseCase
import com.soma369.laimory.core.util.logging.LogDomain
import com.soma369.laimory.core.util.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 서버가 요구하는 하한선을 앱 시작과 포그라운드 복귀에서 확인한다.
 *
 * 판단 근거가 앱 밖(`GET /intro`)에 있어 배포 없이 정책을 바꿀 수 있다. Play In-App Updates 를
 * 쓰지 않는 것도 그 때문이다 — 그쪽은 Play 가 설치 출처일 때만 동작해 로컬 빌드로 검증할 수 없다.
 *
 * **조회 실패는 게이트를 연다.** 서버가 한 번 흔들렸다고 앱 전체가 잠기는 편이 훨씬 나쁘다. 강제는
 * 서버가 살아서 명시적으로 요구할 때만 건다.
 */
@Singleton
class AppUpdateGate
    @Inject
    constructor(
        private val getIntroInfo: GetIntroInfoUseCase,
        private val appUpdateRepository: AppUpdateRepository,
        private val clock: Clock,
        @InstalledVersionCode private val installedVersion: Int,
    ) {
        private val _state = MutableStateFlow(AppUpdateGateState.CHECKING)

        /** 앱을 열어도 되는지. 화면은 이 값만 보고 갈린다. */
        val state: StateFlow<AppUpdateGateState> = _state.asStateFlow()

        private val _recommendation = MutableStateFlow<Int?>(null)

        /** 지금 안내할 권장 버전. 미뤄 뒀거나 요구가 없으면 `null`. */
        val recommendation: StateFlow<Int?> = _recommendation.asStateFlow()

        /** 동시에 들어온 확인을 하나로 합친다. 콜드 스타트와 포그라운드 복귀가 겹칠 수 있다. */
        private val mutex = Mutex()
        private var lastAttemptAt: Instant? = null

        /**
         * 마지막 시도로부터 [REFRESH_INTERVAL] 이 지났으면 다시 확인한다.
         *
         * 콜드 스타트만 확인하면 오래 켜 둔 앱이 하한선 변경을 영영 모른다. 반대로 복귀마다
         * 조회하면 화면을 오갈 때마다 요청이 나간다.
         */
        suspend fun refreshIfStale() {
            mutex.withLock {
                val now = clock.instant()
                val last = lastAttemptAt
                if (last != null && Duration.between(last, now) < REFRESH_INTERVAL) return
                lastAttemptAt = now
                try {
                    refresh(now)
                } catch (e: CancellationException) {
                    // 끝내지 못한 시도는 시도로 치지 않는다. 이 호출은 Activity 생명주기에 매여
                    // 있어 백그라운드로 가면 취소되는데, 그대로 두면 돌아와도 1시간 동안 다시 묻지
                    // 않아 판정 화면에 갇힌다.
                    lastAttemptAt = last
                    throw e
                }
            }
        }

        /**
         * 사용자가 `나중에` 를 누르거나 스토어로 떠난 경우. 그 버전을 24시간 미룬다.
         *
         * **저장에 실패해도 안내는 닫는다.** 저장 공간이 부족해 기록을 남기지 못했다고 사용자가
         * 누른 버튼이 듣지 않으면, 앱을 쓰려고 눌렀는데 아무 일도 일어나지 않는 자리가 된다.
         * 기록이 없으면 다음 실행에서 한 번 더 뜰 뿐이다.
         */
        suspend fun dismissRecommendation(version: Int) {
            try {
                appUpdateRepository.dismissRecommendation(version = version, at = clock.instant())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w(LogDomain.REPOSITORY, "권장 업데이트 보류를 저장하지 못했다(${e::class.simpleName})")
            } finally {
                _recommendation.value = null
            }
        }

        private suspend fun refresh(now: Instant) {
            val info = fetchInfo() ?: return openOnFailure()

            when (val requirement = AppUpdateRequirement.of(info, installedVersion)) {
                AppUpdateRequirement.Forced -> {
                    _state.value = AppUpdateGateState.BLOCKED
                    _recommendation.value = null
                }
                is AppUpdateRequirement.Recommended -> {
                    _state.value = AppUpdateGateState.OPEN
                    val dismissed = appUpdateRepository.dismissedRecommendation()
                    _recommendation.value = requirement.version.takeUnless { dismissed.hides(it, now) }
                }
                AppUpdateRequirement.None -> {
                    _state.value = AppUpdateGateState.OPEN
                    _recommendation.value = null
                }
            }
        }

        /**
         * 서버에 물어본다. 못 물어봤으면 `null`.
         *
         * 공용 클라이언트에는 호출 전체를 덮는 timeout 이 없어, 시작 판정이 무한정 기다리지 않도록
         * 여기서만 총 상한을 건다. 공용 설정은 건드리지 않는다.
         *
         * **예상 못 한 예외도 삼킨다.** 이 판정이 앱 시작을 막고 서 있으므로, 여기서 터지면 사용자는
         * 판정 화면에 갇힌다. 게이트는 서버가 살아서 명시적으로 요구할 때만 걸려야 한다.
         */
        private suspend fun fetchInfo(): IntroInfo? =
            withTimeoutOrNull(FETCH_TIMEOUT_MILLIS) {
                try {
                    getIntroInfo().getOrNull()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logger.w(LogDomain.NETWORK, "업데이트 하한선 조회 실패(${e::class.simpleName}) — 게이트를 연다")
                    null
                }
            }

        /**
         * 조회하지 못했다. 아직 판정 전이면 열고, **이미 막아 둔 상태는 그대로 둔다.**
         *
         * 낮추면 강제 화면을 본 뒤 비행기 모드로 돌아와 우회할 수 있다. 이 기억은 프로세스가 사는
         * 동안만 유지되므로, 서버가 하한선을 되돌리면 다음 조회 성공에서 정상적으로 풀린다.
         */
        private fun openOnFailure() {
            if (_state.value != AppUpdateGateState.BLOCKED) _state.value = AppUpdateGateState.OPEN
        }

        private companion object {
            val REFRESH_INTERVAL: Duration = Duration.ofHours(1)
            const val FETCH_TIMEOUT_MILLIS = 5_000L
        }
    }
