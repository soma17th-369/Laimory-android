package com.soma369.laimory.feature.settings.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.ui.base.UiState

/**
 * @param inquiryId 지금 보여 주는 문의. ViewModel 이 Activity 수명이라, 다른 문의를 열면 이 값으로
 *   알아채 이전 문의 내용을 지운다.
 */
@Immutable
data class InquiryDetailUiState(
    val inquiryId: Long? = null,
    val content: InquiryDetailContent = InquiryDetailContent.Loading,
) : UiState
