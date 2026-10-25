package com.soma369.laimory.feature.collection.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soma369.laimory.core.domain.usecase.tutorial.ResetTutorialsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 튜토리얼을 보지 않은 상태로 되돌린다. 수집 실험실(debug 전용)에서만 쓴다.
 *
 * 홈이 봤다는 기록을 관찰하고 있어 **홈으로 돌아가면 바로 다시 뜬다.** 수집해 둔 데이터는 그대로다.
 */
@HiltViewModel
class TutorialResetViewModel
    @Inject
    constructor(
        private val resetTutorialsUseCase: ResetTutorialsUseCase,
    ) : ViewModel() {
        fun reset() {
            // 실패해도 알릴 대상이 QA 자신이고 다시 누르면 그만이라 삼킨다.
            viewModelScope.launch { runCatching { resetTutorialsUseCase() } }
        }
    }
