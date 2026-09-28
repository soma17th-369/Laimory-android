package com.soma369.laimory.feature.settings.viewmodel

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.InquiryAttachmentException
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.message.DialogRequest
import com.soma369.laimory.core.domain.message.DialogResult
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.inquiry.InquirySubmission
import com.soma369.laimory.core.domain.navigation.Page
import com.soma369.laimory.core.domain.repository.InquiryRepository
import com.soma369.laimory.core.domain.usecase.inquiry.SubmitInquiryUseCase
import com.soma369.laimory.feature.settings.state.InquiryUiIntent
import com.soma369.laimory.feature.settings.state.InquiryUiSideEffect
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InquiryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeInquiryRepository()
    private val navigationHelper = RecordingNavigationHelper()
    private val messageHelper = RecordingMessageHelper()

    @Test
    fun `주소와 내용이 있어야 보낼 수 있다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            assertFalse(viewModel.state.value.canSubmit)

            viewModel.fill(email = "user@example.com", body = " ")
            assertFalse(viewModel.state.value.canSubmit)

            viewModel.fill(body = "문의")
            assertTrue(viewModel.state.value.canSubmit)
        }

    @Test
    fun `주소 형식은 보낼 때 보고, 틀리면 알리기만 하고 보내지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // 입력 중에 틀렸다고 띄우면 `user@` 까지 친 사람에게 오류를 보여 주는 셈이다.
            val viewModel = createViewModel()
            viewModel.fill(email = "user@example", body = "문의")
            assertNull(viewModel.state.value.emailError)

            viewModel.sendIntent(InquiryUiIntent.SubmitClicked)
            advanceUntilIdle()

            assertEquals("답변을 받을 수 있는 이메일 주소를 입력해 주세요.", viewModel.state.value.emailError)
            assertTrue(repository.submissions.isEmpty())

            viewModel.fill(email = "user@example.com")
            assertNull(viewModel.state.value.emailError)
        }

    @Test
    fun `보내면 알리고 이전 화면으로 돌아간다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            viewModel.fill(email = " user@example.com ", body = "문의\n내용")
            viewModel.sendIntent(InquiryUiIntent.AttachmentsPicked(listOf("content://a")))

            viewModel.sendIntent(InquiryUiIntent.SubmitClicked)
            advanceUntilIdle()

            assertEquals(listOf(InquirySubmission("user@example.com", "문의\n내용", listOf("content://a"))), repository.submissions)
            assertEquals(listOf<UserMessage>(UserMessage.InquirySubmitted), messageHelper.sent)
            assertEquals(1, navigationHelper.backCount)
        }

    @Test
    fun `보내는 동안에는 다시 보내거나 나갈 수 없다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // 서버는 같은 내용을 새 문의로 또 받는다. 뒤로가기가 쌓였다가 끝난 뒤 처리되면 설정까지 닫힌다.
            val gate = CompletableDeferred<Unit>()
            repository.gate = gate
            val viewModel = createViewModel()
            viewModel.fill(email = "user@example.com", body = "문의")

            viewModel.sendIntent(InquiryUiIntent.SubmitClicked)
            advanceUntilIdle()
            assertTrue(viewModel.state.value.isSubmitting)
            viewModel.sendIntent(InquiryUiIntent.SubmitClicked)
            viewModel.sendIntent(InquiryUiIntent.BackPressed)
            viewModel.sendIntent(InquiryUiIntent.BodyChanged("바뀜"))
            advanceUntilIdle()

            gate.complete(Unit)
            advanceUntilIdle()

            assertEquals(1, repository.submissions.size)
            assertEquals("문의", repository.submissions.single().body)
            assertEquals(1, navigationHelper.backCount)
            assertTrue(messageHelper.dialogs.isEmpty())
        }

    @Test
    fun `보내기에 실패하면 입력을 두고 이유를 알린다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.failure = ApiException.NetworkException()
            val viewModel = createViewModel()
            viewModel.fill(email = "user@example.com", body = "문의")

            viewModel.sendIntent(InquiryUiIntent.SubmitClicked)
            advanceUntilIdle()

            assertFalse(viewModel.state.value.isSubmitting)
            assertEquals("문의", viewModel.state.value.body)
            assertEquals(InquiryUiSideEffect.ShowSnackbar("인터넷 연결을 확인하고 다시 보내 주세요."), viewModel.sideEffect.first())
            assertEquals(0, navigationHelper.backCount)
        }

    @Test
    fun `사진을 읽지 못하면 사진을 다시 고르라고 알린다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.failure = InquiryAttachmentException("decode")
            val viewModel = createViewModel()
            viewModel.fill(email = "user@example.com", body = "문의")

            viewModel.sendIntent(InquiryUiIntent.SubmitClicked)
            advanceUntilIdle()

            assertEquals(
                InquiryUiSideEffect.ShowSnackbar("첨부한 사진을 읽지 못했어요. 사진을 빼고 다시 골라 주세요."),
                viewModel.sideEffect.first(),
            )
        }

    @Test
    fun `내용은 2000자에서 자른다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.fill(body = "가".repeat(2_010))

            assertEquals(2_000, viewModel.state.value.body.length)
        }

    @Test
    fun `사진은 겹치지 않게 3장까지만 받는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // 선택기가 상한을 지키지 않는 기기가 있다.
            val viewModel = createViewModel()
            viewModel.sendIntent(InquiryUiIntent.AttachmentsPicked(listOf("content://a", "content://b")))
            viewModel.sendIntent(InquiryUiIntent.AttachmentsPicked(listOf("content://b", "content://c", "content://d")))
            advanceUntilIdle()

            assertEquals(listOf("content://a", "content://b", "content://c"), viewModel.state.value.attachmentUris)

            viewModel.sendIntent(InquiryUiIntent.AttachmentRemoved("content://b"))
            viewModel.sendIntent(InquiryUiIntent.AddAttachmentClicked)
            advanceUntilIdle()

            assertEquals(listOf("content://a", "content://c"), viewModel.state.value.attachmentUris)
            assertEquals(InquiryUiSideEffect.LaunchPhotoPicker(maxItems = 1), viewModel.sideEffect.first())
        }

    @Test
    fun `입력이 없으면 확인 없이 나간다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.sendIntent(InquiryUiIntent.BackPressed)
            advanceUntilIdle()

            assertEquals(1, navigationHelper.backCount)
            assertTrue(messageHelper.dialogs.isEmpty())
        }

    @Test
    fun `입력이 있으면 확인을 받고, 계속 쓰기를 고르면 남는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            viewModel.fill(body = "쓰던 내용")

            messageHelper.result = DialogResult.Secondary
            viewModel.sendIntent(InquiryUiIntent.BackPressed)
            advanceUntilIdle()
            assertEquals(0, navigationHelper.backCount)

            messageHelper.result = DialogResult.Primary
            viewModel.sendIntent(InquiryUiIntent.BackPressed)
            advanceUntilIdle()
            assertEquals(1, navigationHelper.backCount)
            assertEquals(2, messageHelper.dialogs.size)
        }

    private fun createViewModel() =
        InquiryViewModel(
            submitInquiryUseCase = SubmitInquiryUseCase(repository, messageHelper),
            navigationHelper = navigationHelper,
            messageHelper = messageHelper,
        )

    private suspend fun InquiryViewModel.fill(
        email: String? = null,
        body: String? = null,
    ) {
        email?.let { sendIntent(InquiryUiIntent.EmailChanged(it)) }
        body?.let { sendIntent(InquiryUiIntent.BodyChanged(it)) }
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
    }

    private class FakeInquiryRepository : InquiryRepository {
        val submissions = mutableListOf<InquirySubmission>()
        var failure: Throwable? = null
        var gate: CompletableDeferred<Unit>? = null

        override suspend fun submit(submission: InquirySubmission) {
            submissions += submission
            gate?.await()
            failure?.let { throw it }
        }
    }

    private class RecordingMessageHelper : MessageHelper {
        val sent = mutableListOf<UserMessage>()
        val dialogs = mutableListOf<DialogRequest.TwoButton>()
        var result: DialogResult = DialogResult.Dismissed

        override fun send(message: UserMessage) {
            sent += message
        }

        override suspend fun showTwoButtonDialog(request: DialogRequest.TwoButton): DialogResult {
            dialogs += request
            return result
        }
    }

    private class RecordingNavigationHelper : NavigationHelper {
        var backCount = 0

        override fun navigateTo(page: Page) = Unit

        override fun replaceRoot(page: Page) = Unit

        override fun navigateToBack() {
            backCount++
        }
    }
}
