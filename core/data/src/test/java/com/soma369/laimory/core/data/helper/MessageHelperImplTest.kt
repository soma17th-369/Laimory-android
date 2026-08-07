package com.soma369.laimory.core.data.helper

import com.soma369.laimory.core.domain.message.DialogRequest
import com.soma369.laimory.core.domain.message.DialogResult
import com.soma369.laimory.core.domain.message.UserMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageHelperImplTest {
    @Test
    fun `수집 전에 보낸 UserMessage도 버퍼로 전달한다`() =
        runTest {
            val helper = MessageHelperImpl()

            helper.send(UserMessage.SessionExpired)

            assertEquals(UserMessage.SessionExpired, helper.messages.first())
        }

    @Test
    fun `Dialog 요청은 활성 상태로 노출되고 결과를 호출자에게 반환한다`() =
        runTest {
            val helper = MessageHelperImpl()

            val result = async { helper.showTwoButtonDialog(twoButton(key = "logout")) }
            runCurrent()

            val active = checkNotNull(helper.activeDialog.value)
            assertEquals("logout", active.request.key)

            helper.resolveDialog(active.requestId, DialogResult.Primary)
            runCurrent()

            assertEquals(DialogResult.Primary, result.await())
            assertNull(helper.activeDialog.value)
        }

    @Test
    fun `활성 requestId와 다른 결과는 무시하고 같은 id는 한 번만 처리한다`() =
        runTest {
            val helper = MessageHelperImpl()

            val result = async { helper.showOneButtonDialog(oneButton(key = "notice")) }
            runCurrent()
            val active = checkNotNull(helper.activeDialog.value)

            helper.resolveDialog(requestId = active.requestId + 100, result = DialogResult.Primary)
            runCurrent()
            assertEquals(active, helper.activeDialog.value)

            helper.resolveDialog(active.requestId, DialogResult.Dismissed)
            helper.resolveDialog(active.requestId, DialogResult.Primary)
            runCurrent()

            assertEquals(DialogResult.Dismissed, result.await())
        }

    @Test
    fun `동시 요청은 FIFO로 대기하고 앞선 응답 뒤에 활성화된다`() =
        runTest {
            val helper = MessageHelperImpl()

            val first = async { helper.showTwoButtonDialog(twoButton(key = "first")) }
            runCurrent()
            val second = async { helper.showTwoButtonDialog(twoButton(key = "second")) }
            runCurrent()

            assertEquals("first", helper.activeDialog.value?.request?.key)

            helper.resolveDialog(checkNotNull(helper.activeDialog.value).requestId, DialogResult.Secondary)
            runCurrent()

            assertEquals(DialogResult.Secondary, first.await())
            assertEquals("second", helper.activeDialog.value?.request?.key)

            helper.resolveDialog(checkNotNull(helper.activeDialog.value).requestId, DialogResult.Primary)
            runCurrent()
            assertEquals(DialogResult.Primary, second.await())
        }

    @Test
    fun `같은 key의 활성·대기 요청은 등록하지 않고 즉시 Dismissed로 응답한다`() =
        runTest {
            val helper = MessageHelperImpl()

            val original = async { helper.showTwoButtonDialog(twoButton(key = "logout")) }
            runCurrent()

            val duplicate = async { helper.showTwoButtonDialog(twoButton(key = "logout")) }
            runCurrent()

            assertEquals(DialogResult.Dismissed, duplicate.await())

            helper.resolveDialog(checkNotNull(helper.activeDialog.value).requestId, DialogResult.Primary)
            runCurrent()
            assertEquals(DialogResult.Primary, original.await())
        }

    @Test
    fun `호출 coroutine이 취소되면 활성 요청을 제거하고 다음 대기를 승격한다`() =
        runTest {
            val helper = MessageHelperImpl()

            val first = async { helper.showTwoButtonDialog(twoButton(key = "first")) }
            runCurrent()
            val second = async { helper.showTwoButtonDialog(twoButton(key = "second")) }
            runCurrent()

            first.cancel()
            runCurrent()

            assertEquals("second", helper.activeDialog.value?.request?.key)

            helper.resolveDialog(checkNotNull(helper.activeDialog.value).requestId, DialogResult.Primary)
            runCurrent()
            assertEquals(DialogResult.Primary, second.await())
        }

    @Test
    fun `대기 중인 요청이 취소되면 대기열에서만 제거한다`() =
        runTest {
            val helper = MessageHelperImpl()

            val first = async { helper.showTwoButtonDialog(twoButton(key = "first")) }
            runCurrent()
            val second = async { helper.showTwoButtonDialog(twoButton(key = "second")) }
            runCurrent()

            second.cancel()
            runCurrent()

            assertEquals("first", helper.activeDialog.value?.request?.key)

            helper.resolveDialog(checkNotNull(helper.activeDialog.value).requestId, DialogResult.Primary)
            runCurrent()

            assertEquals(DialogResult.Primary, first.await())
            assertNull(helper.activeDialog.value)
        }

    @Test
    fun `clearDialogs는 활성과 대기 요청을 결과 전달 없이 취소한다`() =
        runTest {
            val helper = MessageHelperImpl()

            val first = async { helper.showTwoButtonDialog(twoButton(key = "first")) }
            runCurrent()
            val second = async { helper.showTwoButtonDialog(twoButton(key = "second")) }
            runCurrent()

            helper.clearDialogs()
            runCurrent()

            assertNull(helper.activeDialog.value)
            assertTrue(first.isCancelled)
            assertTrue(second.isCancelled)
        }

    @Test
    fun `전체 정리 뒤 새 요청은 정상 활성화된다`() =
        runTest {
            val helper = MessageHelperImpl()

            val stale = async { helper.showTwoButtonDialog(twoButton(key = "logout")) }
            runCurrent()
            helper.clearDialogs()
            runCurrent()
            assertTrue(stale.isCancelled)

            val fresh = async { helper.showTwoButtonDialog(twoButton(key = "logout")) }
            runCurrent()

            helper.resolveDialog(checkNotNull(helper.activeDialog.value).requestId, DialogResult.Primary)
            runCurrent()
            assertEquals(DialogResult.Primary, fresh.await())
        }

    private fun twoButton(key: String) =
        DialogRequest.TwoButton(
            key = key,
            title = "제목",
            body = "본문",
            primaryLabel = "확인",
            secondaryLabel = "취소",
        )

    private fun oneButton(key: String) =
        DialogRequest.OneButton(
            key = key,
            title = "제목",
            body = "본문",
            buttonLabel = "확인",
        )
}
