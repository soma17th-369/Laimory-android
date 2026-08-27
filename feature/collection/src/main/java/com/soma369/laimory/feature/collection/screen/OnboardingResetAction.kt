package com.soma369.laimory.feature.collection.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.soma369.laimory.feature.collection.viewmodel.OnboardingResetViewModel

/**
 * 온보딩을 다시 보게 만드는 QA 도구.
 *
 * 화면에 `앞으로 표시하지 않기` 체크박스를 두는 대신 이 경로를 뒀다 — 체크박스가 있으면 일반
 * 사용자가 "체크 안 하면 매번 뜨나" 로 읽는다. 수집 실험실은 debug 라우트에만 등록되므로
 * release 에는 이 동작이 노출되지 않는다.
 */
@Composable
internal fun OnboardingResetAction(viewModel: OnboardingResetViewModel = hiltViewModel()) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(onClick = viewModel::reset) {
            Text(text = "온보딩 다시 보기", style = MaterialTheme.typography.labelLarge)
        }
    }
}
