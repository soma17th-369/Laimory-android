package com.soma369.laimory.feature.settings.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.ui.base.UiState

@Immutable
data class InquiriesUiState(
    val content: InquiryListContent = InquiryListContent.Loading,
) : UiState
