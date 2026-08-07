package com.soma369.laimory.core.data.helper

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobalLoadingHelperImplTest {
    @Test
    fun `서로 다른 key가 겹치면 하나가 끝나도 로딩을 유지한다`() =
        runTest {
            val helper = GlobalLoadingHelperImpl()
            val firstGate = CompletableDeferred<Unit>()
            val secondGate = CompletableDeferred<Unit>()

            launch { helper.withLoading("auth") { firstGate.await() } }
            launch { helper.withLoading("account") { secondGate.await() } }
            runCurrent()

            assertEquals(setOf("auth", "account"), helper.activeKeys.value)

            firstGate.complete(Unit)
            runCurrent()
            assertEquals(setOf("account"), helper.activeKeys.value)

            secondGate.complete(Unit)
            runCurrent()
            assertTrue(helper.activeKeys.value.isEmpty())
        }

    @Test
    fun `같은 key의 중첩 실행은 모두 끝나야 해제된다`() =
        runTest {
            val helper = GlobalLoadingHelperImpl()
            val firstGate = CompletableDeferred<Unit>()
            val secondGate = CompletableDeferred<Unit>()

            launch { helper.withLoading("auth") { firstGate.await() } }
            launch { helper.withLoading("auth") { secondGate.await() } }
            runCurrent()

            firstGate.complete(Unit)
            runCurrent()
            assertEquals(setOf("auth"), helper.activeKeys.value)

            secondGate.complete(Unit)
            runCurrent()
            assertTrue(helper.activeKeys.value.isEmpty())
        }

    @Test
    fun `블록이 예외로 끝나도 key를 해제한다`() =
        runTest {
            val helper = GlobalLoadingHelperImpl()

            val failure =
                runCatching {
                    helper.withLoading("auth") { error("작업 실패") }
                }.exceptionOrNull()

            assertTrue(failure is IllegalStateException)
            assertTrue(helper.activeKeys.value.isEmpty())
        }

    @Test
    fun `블록이 취소돼도 key를 해제한다`() =
        runTest {
            val helper = GlobalLoadingHelperImpl()
            val gate = CompletableDeferred<Unit>()

            val job = launch { helper.withLoading("auth") { gate.await() } }
            runCurrent()
            assertEquals(setOf("auth"), helper.activeKeys.value)

            job.cancel()
            runCurrent()

            assertTrue(helper.activeKeys.value.isEmpty())
        }
}
