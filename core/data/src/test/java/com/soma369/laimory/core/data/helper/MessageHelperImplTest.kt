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

            val result = async { helper.showTwoButtonDialog(twoButton(title = "로그아웃할까요?")) }
            runCurrent()

            val active = checkNotNull(helper.activeDialog.value)
            assertEquals("로그아웃할까요?", active.request.title)

            helper.resolveDialog(active.requestId, DialogResult.Primary)
            runCurrent()

            assertEquals(DialogResult.Primary, result.await())
            assertNull(helper.activeDialog.value)
        }

    @Test
    fun `활성 requestId와 다른 결과는 무시하고 같은 id는 한 번만 처리한다`() =
        runTest {
            val helper = MessageHelperImpl()

            val result = async { helper.showOneButtonDialog(oneButton()) }
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
    fun `표시 중 들어온 새 요청은 등록하지 않고 즉시 Dismissed로 응답한다`() =
        runTest {
            val helper = MessageHelperImpl()

            val original = async { helper.showTwoButtonDialog(twoButton(title = "첫 요청")) }
            runCurrent()

            val rejected = async { helper.showTwoButtonDialog(twoButton(title = "두 번째 요청")) }
            runCurrent()

            assertEquals(DialogResult.Dismissed, rejected.await())
            assertEquals("첫 요청", helper.activeDialog.value?.request?.title)

            helper.resolveDialog(checkNotNull(helper.activeDialog.value).requestId, DialogResult.Primary)
            runCurrent()
            assertEquals(DialogResult.Primary, original.await())
        }

    @Test
    fun `호출 coroutine이 취소되면 활성 Dialog를 정리하고 새 요청을 받을 수 있다`() =
        runTest {
            val helper = MessageHelperImpl()

            val cancelled = async { helper.showTwoButtonDialog(twoButton(title = "취소될 요청")) }
            runCurrent()

            cancelled.cancel()
            runCurrent()

            assertNull(helper.activeDialog.value)

            val next = async { helper.showTwoButtonDialog(twoButton(title = "다음 요청")) }
            runCurrent()

            helper.resolveDialog(checkNotNull(helper.activeDialog.value).requestId, DialogResult.Primary)
            runCurrent()
            assertEquals(DialogResult.Primary, next.await())
        }

    @Test
    fun `clearDialogs는 활성 요청을 결과 전달 없이 취소한다`() =
        runTest {
            val helper = MessageHelperImpl()

            val active = async { helper.showTwoButtonDialog(twoButton(title = "정리될 요청")) }
            runCurrent()

            helper.clearDialogs()
            runCurrent()

            assertNull(helper.activeDialog.value)
            assertTrue(active.isCancelled)
        }

    @Test
    fun `전체 정리 뒤 새 요청은 정상 활성화된다`() =
        runTest {
            val helper = MessageHelperImpl()

            val stale = async { helper.showTwoButtonDialog(twoButton(title = "이전 요청")) }
            runCurrent()
            helper.clearDialogs()
            runCurrent()
            assertTrue(stale.isCancelled)

            val fresh = async { helper.showTwoButtonDialog(twoButton(title = "새 요청")) }
            runCurrent()

            helper.resolveDialog(checkNotNull(helper.activeDialog.value).requestId, DialogResult.Primary)
            runCurrent()
            assertEquals(DialogResult.Primary, fresh.await())
        }

    @Test
    fun `확인 체크박스 Dialog도 같은 활성 단일 정책을 따른다`() =
        runTest {
            val helper = MessageHelperImpl()

            val active = async { helper.showConsentDialog(consent()) }
            runCurrent()
            val rejected = async { helper.showTwoButtonDialog(twoButton(title = "겹친 요청")) }
            runCurrent()

            // 표시 중 들어온 요청은 등록되지 않고 즉시 Dismissed 로 응답한다.
            assertEquals(DialogResult.Dismissed, rejected.await())

            val shown = checkNotNull(helper.activeDialog.value)
            assertEquals("계정을 삭제할까요?", shown.request.title)

            helper.resolveDialog(shown.requestId, DialogResult.Primary)
            runCurrent()

            assertEquals(DialogResult.Primary, active.await())
            assertNull(helper.activeDialog.value)
        }

    private fun consent() =
        DialogRequest.Consent(
            title = "계정을 삭제할까요?",
            body = "본문",
            consentLabel = "동의합니다",
            primaryLabel = "계정 삭제",
            secondaryLabel = "취소",
        )

    private fun twoButton(title: String = "제목") =
        DialogRequest.TwoButton(
            title = title,
            body = "본문",
            primaryLabel = "확인",
            secondaryLabel = "취소",
        )

    private fun oneButton() =
        DialogRequest.OneButton(
            title = "제목",
            body = "본문",
            buttonLabel = "확인",
        )
}
