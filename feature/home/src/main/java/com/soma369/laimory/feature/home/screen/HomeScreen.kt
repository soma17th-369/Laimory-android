package com.soma369.laimory.feature.home.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soma369.laimory.core.ui.LocalSnackbarHostState
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.home.component.DateHeaderCard
import com.soma369.laimory.feature.home.component.DraftSettingsSheet
import com.soma369.laimory.feature.home.component.HomeDatePickerDialog
import com.soma369.laimory.feature.home.component.HomeTimePickerDialog
import com.soma369.laimory.feature.home.component.PhotoSelectionSheet
import com.soma369.laimory.feature.home.state.DraftCreationStatus
import com.soma369.laimory.feature.home.state.HomeTimeField
import com.soma369.laimory.feature.home.state.HomeUiIntent
import com.soma369.laimory.feature.home.state.HomeUiSideEffect
import com.soma369.laimory.feature.home.state.HomeUiState
import com.soma369.laimory.feature.home.viewmodel.HomeViewModel
import kotlinx.coroutines.flow.Flow

@Composable
fun HomeRoute(
    innerPadding: PaddingValues,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeContent(
        innerPadding = innerPadding,
        state = state,
        onIntent = viewModel::sendIntent,
        snackbarFlow = viewModel.snackbar,
        sideEffectFlow = viewModel.sideEffect,
    )
}

@Composable
private fun HomeContent(
    innerPadding: PaddingValues,
    state: HomeUiState,
    onIntent: (HomeUiIntent) -> Unit,
    snackbarFlow: Flow<String>,
    sideEffectFlow: Flow<HomeUiSideEffect>,
) {
    val snackbarHostState = LocalSnackbarHostState.current

    LaunchedEffect(Unit) {
        snackbarFlow.collect(snackbarHostState::showSnackbar)
    }
    LaunchedEffect(Unit) {
        sideEffectFlow.collect { effect ->
            when (effect) {
                is HomeUiSideEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    HomeScreen(innerPadding = innerPadding, state = state, onIntent = onIntent)

    if (state.isDraftSheetVisible) {
        DraftSettingsSheet(
            state = state,
            onIntent = onIntent,
        )
    }

    if (state.isPhotoSheetVisible) {
        PhotoSelectionSheet(
            state = state,
            onIntent = onIntent,
        )
    }

    if (state.isDatePickerVisible) {
        HomeDatePickerDialog(
            initialDate = state.selectedDate,
            onSelect = { onIntent(HomeUiIntent.SelectDate(it)) },
            onDismiss = { onIntent(HomeUiIntent.DismissDatePicker) },
        )
    }

    state.editingTimeField?.let { field ->
        HomeTimePickerDialog(
            initial = if (field == HomeTimeField.START) state.startTime else state.endTime,
            onConfirm = { onIntent(HomeUiIntent.SelectTime(field, it)) },
            onDismiss = { onIntent(HomeUiIntent.DismissTimePicker) },
        )
    }
}

@Composable
private fun HomeScreen(
    innerPadding: PaddingValues,
    state: HomeUiState,
    onIntent: (HomeUiIntent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.extraLarge, vertical = Spacing.extraLarge2),
        verticalArrangement = Arrangement.spacedBy(Spacing.extraLarge2),
    ) {
        Text(
            text = "안녕하세요",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        DateHeaderCard(
            state = state,
            onClick = { onIntent(HomeUiIntent.ShowDatePicker) },
            onActionClick = {
                onIntent(
                    if (state.draftStatus == DraftCreationStatus.SUCCESS) {
                        HomeUiIntent.ViewDraft
                    } else {
                        HomeUiIntent.OpenDraftSheet
                    },
                )
            },
            onPhotoClick = { onIntent(HomeUiIntent.OpenPhotoSheet) },
        )

        TextButton(
            onClick = { onIntent(HomeUiIntent.NavigateToCollection) },
            modifier = Modifier.align(Alignment.End),
        ) {
            Text("수집 데이터 자세히 보기")
        }
    }
}
