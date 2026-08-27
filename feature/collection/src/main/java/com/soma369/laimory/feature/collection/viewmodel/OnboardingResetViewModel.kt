package com.soma369.laimory.feature.collection.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soma369.laimory.core.domain.usecase.ResetOnboardingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 온보딩 상태를 처음으로 되돌린다. 수집 실험실(debug 전용)에서만 쓴다.
 *
 * 되돌리면 앱 루트가 온보딩 상태를 관찰하고 있어 **재시작 없이 바로 온보딩으로 바뀐다.**
 */
@HiltViewModel
class OnboardingResetViewModel
    @Inject
    constructor(
        private val resetOnboardingUseCase: ResetOnboardingUseCase,
    ) : ViewModel() {
        fun reset() {
            // 실패해도 알릴 대상이 QA 자신이고 다시 누르면 그만이라 삼킨다.
            viewModelScope.launch { runCatching { resetOnboardingUseCase() } }
        }
    }
