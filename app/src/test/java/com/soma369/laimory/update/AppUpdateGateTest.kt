package com.soma369.laimory.update

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.IntroInfo
import com.soma369.laimory.core.domain.model.update.DismissedRecommendation
import com.soma369.laimory.core.domain.repository.AppUpdateRepository
import com.soma369.laimory.core.domain.repository.IntroRepository
import com.soma369.laimory.core.domain.usecase.GetIntroInfoUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class AppUpdateGateTest {
    private val introRepository = FakeIntroRepository()
    private val updateRepository = FakeAppUpdateRepository()
    private val clock = TestClock(NOW)

    @Test
    fun `하한선 미만이면 막는다`() =
        runTest {
            introRepository.info = IntroInfo(minAppVersion = 5, recommendAppVersion = 5, debugTestMessage = "")

            val gate = createGate(installedVersion = 4)
            gate.refreshIfStale()

            assertEquals(AppUpdateGateState.BLOCKED, gate.state.value)
            assertNull(gate.recommendation.value)
        }

    @Test
    fun `조회에 실패하면 앱을 열어 준다`() =
        runTest {
            // 서버가 한 번 흔들렸다고 앱 전체가 잠기는 편이 훨씬 나쁘다.
            introRepository.failure = ApiException.NetworkException()

            val gate = createGate(installedVersion = 1)
            gate.refreshIfStale()

            assertEquals(AppUpdateGateState.OPEN, gate.state.value)
        }

    @Test
    fun `예상 못 한 예외도 앱을 열어 준다`() =
        runTest {
            // 이 판정이 앱 시작을 막고 서 있다. 여기서 터지면 사용자는 판정 화면에 갇힌다.
            introRepository.failure = IllegalStateException("unexpected")

            val gate = createGate(installedVersion = 1)
            gate.refreshIfStale()

            assertEquals(AppUpdateGateState.OPEN, gate.state.value)
        }

    @Test
    fun `응답이 없으면 5초에 포기하고 열어 준다`() =
        runTest {
            introRepository.gate = CompletableDeferred()

            val gate = createGate(installedVersion = 1)
            launch { gate.refreshIfStale() }
            advanceTimeBy(4_000)
            runCurrent()
            assertEquals(AppUpdateGateState.CHECKING, gate.state.value)

            advanceTimeBy(2_000)
            runCurrent()

            assertEquals(AppUpdateGateState.OPEN, gate.state.value)
        }

    @Test
    fun `한 번 막은 뒤 조회에 실패해도 풀리지 않는다`() =
        runTest {
            // 강제 화면을 본 뒤 비행기 모드로 돌아와 우회하는 길을 막는다.
            introRepository.info = IntroInfo(minAppVersion = 5, recommendAppVersion = 5, debugTestMessage = "")
            val gate = createGate(installedVersion = 4)
            gate.refreshIfStale()

            introRepository.failure = ApiException.NetworkException()
            advanceClock(Duration.ofHours(2))
            gate.refreshIfStale()

            assertEquals(AppUpdateGateState.BLOCKED, gate.state.value)
        }

    @Test
    fun `서버가 하한선을 되돌리면 같은 프로세스에서 풀린다`() =
        runTest {
            introRepository.info = IntroInfo(minAppVersion = 5, recommendAppVersion = 5, debugTestMessage = "")
            val gate = createGate(installedVersion = 4)
            gate.refreshIfStale()
            assertEquals(AppUpdateGateState.BLOCKED, gate.state.value)

            introRepository.info = IntroInfo(minAppVersion = 1, recommendAppVersion = 1, debugTestMessage = "")
            advanceClock(Duration.ofHours(2))
            gate.refreshIfStale()

            assertEquals(AppUpdateGateState.OPEN, gate.state.value)
        }

    @Test
    fun `한 시간 안에는 다시 조회하지 않는다`() =
        runTest {
            introRepository.info = IntroInfo(minAppVersion = 1, recommendAppVersion = 1, debugTestMessage = "")
            val gate = createGate(installedVersion = 1)

            gate.refreshIfStale()
            advanceClock(Duration.ofMinutes(59))
            gate.refreshIfStale()
            assertEquals(1, introRepository.callCount)

            advanceClock(Duration.ofMinutes(2))
            gate.refreshIfStale()

            assertEquals(2, introRepository.callCount)
        }

    @Test
    fun `동시에 들어온 확인은 한 번으로 합친다`() =
        runTest {
            introRepository.info = IntroInfo(minAppVersion = 1, recommendAppVersion = 1, debugTestMessage = "")
            val gate = createGate(installedVersion = 1)

            repeat(3) { launch { gate.refreshIfStale() } }
            advanceUntilIdle()

            assertEquals(1, introRepository.callCount)
        }

    @Test
    fun `응답 전에 취소된 시도는 재조회를 막지 않는다`() =
        runTest {
            // 시작 조회는 Activity 생명주기에 매여 있어 백그라운드로 가면 취소된다. 그 시도를
            // 시도로 세면 돌아와도 1시간 동안 다시 묻지 않아 판정 화면에 갇힌다.
            introRepository.gate = CompletableDeferred()
            val gate = createGate(installedVersion = 1)

            val attempt = launch { gate.refreshIfStale() }
            advanceTimeBy(1_000)
            runCurrent()
            attempt.cancelAndJoin()
            assertEquals(AppUpdateGateState.CHECKING, gate.state.value)

            // 복귀. 시간은 거의 흐르지 않았지만 앞 시도가 끝나지 못했으므로 다시 물어야 한다.
            introRepository.gate = null
            introRepository.info = IntroInfo(minAppVersion = 1, recommendAppVersion = 1, debugTestMessage = "")
            gate.refreshIfStale()

            assertEquals(AppUpdateGateState.OPEN, gate.state.value)
        }

    @Test
    fun `보류를 저장하지 못해도 안내는 닫는다`() =
        runTest {
            // 저장 공간이 부족하다고 누른 버튼이 듣지 않으면, 앱을 쓰려고 눌렀는데 아무 일도
            // 일어나지 않는 자리가 된다. 기록이 없으면 다음 실행에서 한 번 더 뜰 뿐이다.
            introRepository.info = IntroInfo(minAppVersion = 1, recommendAppVersion = 7, debugTestMessage = "")
            val gate = createGate(installedVersion = 6)
            gate.refreshIfStale()
            assertEquals(7, gate.recommendation.value)

            updateRepository.writeFailure = IOException("no space left")
            gate.dismissRecommendation(version = 7)

            assertNull(gate.recommendation.value)
        }

    @Test
    fun `미뤄 둔 권장은 안내하지 않는다`() =
        runTest {
            introRepository.info = IntroInfo(minAppVersion = 1, recommendAppVersion = 7, debugTestMessage = "")
            updateRepository.dismissed = DismissedRecommendation(version = 7, at = NOW.minus(Duration.ofHours(1)))

            val gate = createGate(installedVersion = 6)
            gate.refreshIfStale()

            assertEquals(AppUpdateGateState.OPEN, gate.state.value)
            assertNull(gate.recommendation.value)
        }

    @Test
    fun `미루면 그 버전과 시각을 남긴다`() =
        runTest {
            introRepository.info = IntroInfo(minAppVersion = 1, recommendAppVersion = 7, debugTestMessage = "")
            val gate = createGate(installedVersion = 6)
            gate.refreshIfStale()
            assertEquals(7, gate.recommendation.value)

            gate.dismissRecommendation(version = 7)

            assertNull(gate.recommendation.value)
            assertEquals(DismissedRecommendation(version = 7, at = NOW), updateRepository.dismissed)
        }

    private fun createGate(installedVersion: Int) =
        AppUpdateGate(
            getIntroInfo = GetIntroInfoUseCase(introRepository, NoOpMessageHelper),
            appUpdateRepository = updateRepository,
            clock = clock,
            installedVersion = installedVersion,
        )

    private fun advanceClock(duration: Duration) {
        clock.now = clock.now.plus(duration)
    }

    /** 시각을 움직일 수 있는 시계. 고정 시계로는 재조회 간격을 시험할 수 없다. */
    private class TestClock(
        var now: Instant,
    ) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = this

        override fun instant(): Instant = now
    }

    private class FakeIntroRepository : IntroRepository {
        var info = IntroInfo(minAppVersion = 0, recommendAppVersion = 0, debugTestMessage = "")
        var failure: Throwable? = null
        var gate: CompletableDeferred<Unit>? = null
        var callCount = 0

        override suspend fun getIntroInfo(): IntroInfo {
            callCount++
            gate?.await()
            failure?.let { throw it }
            return info
        }
    }

    private class FakeAppUpdateRepository : AppUpdateRepository {
        var dismissed: DismissedRecommendation? = null
        var writeFailure: Throwable? = null

        override suspend fun dismissedRecommendation(): DismissedRecommendation? = dismissed

        override suspend fun dismissRecommendation(
            version: Int,
            at: Instant,
        ) {
            writeFailure?.let { throw it }
            dismissed = DismissedRecommendation(version = version, at = at)
        }
    }

    private object NoOpMessageHelper : MessageHelper {
        override fun send(message: UserMessage) = Unit
    }

    private companion object {
        val NOW: Instant = Instant.parse("2026-09-05T12:00:00Z")
    }
}
