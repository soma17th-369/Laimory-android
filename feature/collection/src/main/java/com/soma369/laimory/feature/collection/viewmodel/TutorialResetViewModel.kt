package com.soma369.laimory.feature.collection.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.navigation.HomePage
import com.soma369.laimory.core.domain.usecase.tutorial.ResetTutorialsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 튜토리얼을 보지 않은 상태로 되돌리고 **곧장 홈으로 간다.** 수집 실험실(debug 전용)에서만 쓴다.
 *
 * 홈이 봤다는 기록을 관찰하고 있어 도착하면 추천 날짜 시트부터 다시 뜬다. 실험실에 그대로 남으면 눌러도 아무 일이 없는
 * 것처럼 보인다 — 기록은 지워졌는데 화면에는 변화가 없다(실기기 제보). 수집해 둔 데이터는 그대로다.
 */
@HiltViewModel
class TutorialResetViewModel
    @Inject
    constructor(
        private val resetTutorialsUseCase: ResetTutorialsUseCase,
        private val navigationHelper: NavigationHelper,
    ) : ViewModel() {
        fun reset() {
            viewModelScope.launch {
                // 실패해도 알릴 대상이 QA 자신이고 다시 누르면 그만이라 삼킨다. 지우지 못했으면 홈으로 가도 뜰 것이 없다.
                runCatching { resetTutorialsUseCase() }.onSuccess { navigationHelper.replaceRoot(HomePage) }
            }
        }
    }
