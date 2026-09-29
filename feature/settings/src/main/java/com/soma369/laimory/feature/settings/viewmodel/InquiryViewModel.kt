package com.soma369.laimory.feature.settings.viewmodel

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.HandledException
import com.soma369.laimory.core.domain.exception.InquiryAttachmentException
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.message.DialogRequest
import com.soma369.laimory.core.domain.message.DialogResult
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.inquiry.InquiryInputRules
import com.soma369.laimory.core.domain.model.inquiry.InquirySubmission
import com.soma369.laimory.core.domain.usecase.inquiry.SubmitInquiryUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.settings.state.InquiryUiIntent
import com.soma369.laimory.feature.settings.state.InquiryUiSideEffect
import com.soma369.laimory.feature.settings.state.InquiryUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import javax.inject.Inject

/**
 * 설정 > 지원 > 문의하기.
 *
 * 서버는 같은 내용을 다시 받으면 새 문의로 또 접수한다(멱등 키 없음). 그래서 보내는 동안에는
 * 보내기·입력·뒤로가기를 모두 막는다. 보내기는 따로 띄운다 — 의도 처리를 붙잡고 있으면 그동안 쌓인
 * 뒤로가기가 끝난 뒤 한꺼번에 처리돼 설정 화면까지 닫는다.
 */
@HiltViewModel
class InquiryViewModel
    @Inject
    constructor(
        private val submitInquiryUseCase: SubmitInquiryUseCase,
        private val navigationHelper: NavigationHelper,
        private val messageHelper: MessageHelper,
    ) : BaseMviViewModel<InquiryUiState, InquiryUiIntent, InquiryUiSideEffect>(InquiryUiState()) {
        private var submitJob: Job? = null
        private var leaveConfirmJob: Job? = null

        override suspend fun handleIntent(intent: InquiryUiIntent) {
            if (state.value.isSubmitting) return
            when (intent) {
                InquiryUiIntent.Opened -> updateState { InquiryUiState() }
                is InquiryUiIntent.EmailChanged -> updateState { copy(email = intent.email, emailError = null) }
                is InquiryUiIntent.TitleChanged ->
                    updateState { copy(title = intent.title.take(InquiryInputRules.TITLE_MAX_LENGTH)) }
                is InquiryUiIntent.DescriptionChanged ->
                    updateState { copy(description = intent.description.take(InquiryInputRules.DESCRIPTION_MAX_LENGTH)) }
                InquiryUiIntent.AddAttachmentClicked -> requestPhotoPicker()
                is InquiryUiIntent.AttachmentsPicked -> addAttachments(intent.uris)
                is InquiryUiIntent.AttachmentRemoved ->
                    updateState { copy(attachmentUris = attachmentUris - intent.uri) }
                InquiryUiIntent.SubmitClicked -> submit()
                InquiryUiIntent.BackPressed -> leave()
            }
        }

        private fun requestPhotoPicker() {
            val remaining = state.value.remainingAttachmentSlots
            if (remaining > 0) sendEffect(InquiryUiSideEffect.LaunchPhotoPicker(maxItems = remaining))
        }

        /** 선택기가 상한을 지키지 않는 기기가 있어 여기서도 자른다. */
        private fun addAttachments(uris: List<String>) {
            updateState {
                val added = uris.distinct().filterNot { it in attachmentUris }.take(remainingAttachmentSlots)
                copy(attachmentUris = attachmentUris + added)
            }
        }

        private fun submit() {
            val current = state.value
            if (!current.canSubmit || submitJob?.isActive == true) return
            if (!InquiryInputRules.isValidEmail(current.email)) {
                updateState { copy(emailError = "답변을 받을 수 있는 이메일 주소를 입력해 주세요.") }
                return
            }
            updateState { copy(isSubmitting = true) }
            submitJob =
                safeLaunch(onError = { onSubmitFailed(it) }) {
                    submitInquiryUseCase(
                        InquirySubmission(
                            email = current.email,
                            title = current.title,
                            description = current.description,
                            attachmentUris = current.attachmentUris,
                        ),
                    ).onSuccess {
                        messageHelper.send(UserMessage.InquirySubmitted)
                        closeAndClear()
                    }.onFailure(::onSubmitFailed)
                }
        }

        /** 입력은 그대로 둔다 — 다시 누르기만 하면 되게. */
        private fun onSubmitFailed(error: Throwable) {
            updateState { copy(isSubmitting = false) }
            // 세션 만료·서버 오류는 공용 안내가 이미 떴다.
            if (error is HandledException) return
            val message =
                when (error) {
                    is InquiryAttachmentException -> "첨부한 사진을 읽지 못했어요. 사진을 빼고 다시 골라 주세요."
                    is ApiException.NetworkException -> "인터넷 연결을 확인하고 다시 보내 주세요."
                    else -> "문의를 보내지 못했어요. 잠시 후 다시 시도해 주세요."
                }
            sendEffect(InquiryUiSideEffect.ShowSnackbar(message))
        }

        private fun leave() {
            if (!state.value.hasInput) {
                closeAndClear()
                return
            }
            if (leaveConfirmJob?.isActive == true) return
            leaveConfirmJob =
                safeLaunch {
                    val result =
                        messageHelper.showTwoButtonDialog(
                            DialogRequest.TwoButton(
                                title = "문의 작성을 그만둘까요?",
                                body = "입력한 내용과 고른 사진은 저장되지 않아요.",
                                primaryLabel = "나가기",
                                secondaryLabel = "계속 쓰기",
                            ),
                        )
                    if (result == DialogResult.Primary) closeAndClear()
                }
        }

        /** 나가면서 입력을 비운다. 이메일·본문을 다음 진입까지 메모리에 남겨 둘 이유가 없다. */
        private fun closeAndClear() {
            updateState { InquiryUiState() }
            navigationHelper.navigateToBack()
        }
    }
