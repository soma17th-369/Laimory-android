package com.soma369.laimory.feature.settings.screen

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.soma369.laimory.core.domain.model.inquiry.InquiryInputRules
import com.soma369.laimory.core.ui.LocalSnackbarHostState
import com.soma369.laimory.core.ui.component.LaimoryTextField
import com.soma369.laimory.core.ui.component.LaimoryTopAppBar
import com.soma369.laimory.core.ui.component.photo.LaimoryAddPhotoTile
import com.soma369.laimory.core.ui.component.photo.LaimoryPhotoTile
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.settings.state.InquiriesUiIntent
import com.soma369.laimory.feature.settings.state.InquiryUiIntent
import com.soma369.laimory.feature.settings.state.InquiryUiSideEffect
import com.soma369.laimory.feature.settings.state.InquiryUiState
import com.soma369.laimory.feature.settings.viewmodel.InquiriesViewModel
import com.soma369.laimory.feature.settings.viewmodel.InquiryViewModel
import kotlinx.coroutines.flow.Flow
import java.text.NumberFormat

@Composable
fun InquiryRoute(
    innerPadding: PaddingValues,
    viewModel: InquiryViewModel = hiltViewModel(),
    historyViewModel: InquiriesViewModel = hiltViewModel(),
) {
    // 새 진입은 늘 문의하기 탭부터. 저장 상태에 남으므로 회전해도 보던 탭을 지킨다.
    var selectedTab by rememberSaveable { mutableStateOf(InquiryTab.COMPOSE) }
    // 새 진입에서만 한 번. 저장 상태에 남으므로 회전으로는 다시 보내지 않는다.
    var opened by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!opened) {
            opened = true
            viewModel.sendIntent(InquiryUiIntent.Opened)
            // 로그아웃 뒤 다른 계정으로 들어와도 이전 계정의 문의 목록이 보이지 않게 비운다.
            historyViewModel.sendIntent(InquiriesUiIntent.Opened)
        }
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    InquiryContent(
        innerPadding = innerPadding,
        state = state,
        selectedTab = selectedTab,
        onSelectTab = { selectedTab = it },
        onIntent = viewModel::sendIntent,
        sideEffectFlow = viewModel.sideEffect,
        history = { InquiryHistoryTab(viewModel = historyViewModel) },
    )
}

/** 문의 화면의 두 탭. 설정에는 `문의하기` 한 줄만 두고, 보낸 문의는 이 화면 안에서 본다. */
private enum class InquiryTab(
    val label: String,
) {
    COMPOSE("문의하기"),
    HISTORY("문의 내역"),
}

@Composable
private fun InquiryContent(
    innerPadding: PaddingValues,
    state: InquiryUiState,
    selectedTab: InquiryTab,
    onSelectTab: (InquiryTab) -> Unit,
    onIntent: (InquiryUiIntent) -> Unit,
    sideEffectFlow: Flow<InquiryUiSideEffect>,
    history: @Composable () -> Unit,
) {
    val snackbarHostState = LocalSnackbarHostState.current
    // 여러 장을 고르는 선택기는 상한이 2 이상이어야 한다. 한 자리만 남으면 한 장 선택기를 연다.
    val pickMany =
        rememberLauncherForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia(InquiryInputRules.MAX_ATTACHMENTS),
        ) { uris -> onIntent(InquiryUiIntent.AttachmentsPicked(uris.map { it.toString() })) }
    val pickOne =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) onIntent(InquiryUiIntent.AttachmentsPicked(listOf(uri.toString())))
        }
    LaunchedEffect(sideEffectFlow) {
        sideEffectFlow.collect { effect ->
            when (effect) {
                is InquiryUiSideEffect.LaunchPhotoPicker -> {
                    val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    if (effect.maxItems > 1) pickMany.launch(request) else pickOne.launch(request)
                }
                is InquiryUiSideEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                // 보낸 문의가 바로 보이도록 내역으로 넘긴다. 목록은 탭이 보일 때 새로 받는다.
                InquiryUiSideEffect.ShowHistory -> onSelectTab(InquiryTab.HISTORY)
            }
        }
    }
    // 어느 탭에서 나가든 쓰던 문의는 문의하기 탭의 것이다. 나가기 확인은 그쪽이 판단한다.
    BackHandler { onIntent(InquiryUiIntent.BackPressed) }
    InquiryScreen(
        innerPadding = innerPadding,
        state = state,
        selectedTab = selectedTab,
        onSelectTab = onSelectTab,
        onIntent = onIntent,
        history = history,
    )
}

@Composable
private fun InquiryScreen(
    innerPadding: PaddingValues,
    state: InquiryUiState,
    selectedTab: InquiryTab,
    onSelectTab: (InquiryTab) -> Unit,
    onIntent: (InquiryUiIntent) -> Unit,
    history: @Composable () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
    ) {
        LaimoryTopAppBar(
            title = {
                Text(
                    text = "문의",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            onBackClick = { onIntent(InquiryUiIntent.BackPressed) },
        )
        SecondaryTabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            InquiryTab.entries.forEach { tab ->
                Tab(
                    selected = tab == selectedTab,
                    onClick = { onSelectTab(tab) },
                    text = { Text(text = tab.label, style = MaterialTheme.typography.titleSmall) },
                    selectedContentColor = MaterialTheme.colorScheme.onSurface,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        when (selectedTab) {
            InquiryTab.COMPOSE -> InquiryForm(state = state, onIntent = onIntent)
            InquiryTab.HISTORY -> history()
        }
    }
}

@Composable
private fun ColumnScope.InquiryForm(
    state: InquiryUiState,
    onIntent: (InquiryUiIntent) -> Unit,
) {
    val editable = !state.isSubmitting
    Column(
        modifier =
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SCREEN_HORIZONTAL_PADDING, vertical = Spacing.large),
        verticalArrangement = Arrangement.spacedBy(Spacing.extraLarge),
    ) {
        Text(
            text = "궁금한 점이나 불편한 점을 남겨 주세요.\n답변은 입력한 이메일로 보내드려요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LaimoryTextField(
            value = state.email,
            onValueChange = { onIntent(InquiryUiIntent.EmailChanged(it)) },
            label = "답변 받을 이메일",
            placeholder = "example@email.com",
            error = state.emailError,
            enabled = editable,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        )
        LaimoryTextField(
            value = state.title,
            onValueChange = { onIntent(InquiryUiIntent.TitleChanged(it)) },
            label = "제목",
            placeholder = "무엇에 대한 문의인지 짧게 적어 주세요.",
            // 규칙·서버와 같이 앞뒤 공백을 뺀 길이를 보여 준다.
            counterText =
                "${COUNT_FORMAT.format(state.title.trim().length)} / " +
                    COUNT_FORMAT.format(InquiryInputRules.TITLE_MAX_LENGTH),
            enabled = editable,
        )
        LaimoryTextField(
            value = state.description,
            onValueChange = { onIntent(InquiryUiIntent.DescriptionChanged(it)) },
            label = "내용",
            placeholder = "어떤 점이 궁금하거나 불편했는지 적어 주세요.",
            counterText =
                "${COUNT_FORMAT.format(state.description.length)} / " +
                    COUNT_FORMAT.format(InquiryInputRules.DESCRIPTION_MAX_LENGTH),
            enabled = editable,
            singleLine = false,
            fieldHeight = BODY_FIELD_HEIGHT,
        )
        AttachmentSection(
            attachmentUris = state.attachmentUris,
            canAdd = editable && state.remainingAttachmentSlots > 0,
            enabled = editable,
            onAddClick = { onIntent(InquiryUiIntent.AddAttachmentClicked) },
            onRemove = { onIntent(InquiryUiIntent.AttachmentRemoved(it)) },
        )
        Text(
            text = "입력한 이메일과 문의 내용, 첨부한 사진은 답변에만 쓰고 계정을 삭제하면 함께 지워져요.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Button(
        onClick = { onIntent(InquiryUiIntent.SubmitClicked) },
        enabled = state.canSubmit,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = SCREEN_HORIZONTAL_PADDING, vertical = Spacing.medium)
                .height(CTA_HEIGHT),
        shape = MaterialTheme.shapes.medium,
    ) {
        if (state.isSubmitting) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            Text(text = "보내기", style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun AttachmentSection(
    attachmentUris: List<String>,
    canAdd: Boolean,
    enabled: Boolean,
    onAddClick: () -> Unit,
    onRemove: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        Text(
            text = "사진 첨부 (선택 · 최대 ${InquiryInputRules.MAX_ATTACHMENTS}장)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
            val isPreview = LocalInspectionMode.current
            attachmentUris.forEachIndexed { index, uri ->
                val description = "첨부한 사진 ${index + 1}"
                LaimoryPhotoTile(
                    removeContentDescription = "$description 빼기",
                    onRemove = if (enabled) ({ onRemove(uri) }) else null,
                ) {
                    // 미리보기는 실제 사진을 불러올 수 없다.
                    if (!isPreview) {
                        AsyncImage(
                            model = uri,
                            contentDescription = description,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
            if (canAdd) LaimoryAddPhotoTile(onClick = onAddClick)
        }
    }
}

private val COUNT_FORMAT: NumberFormat = NumberFormat.getIntegerInstance()

private val SCREEN_HORIZONTAL_PADDING = 24.dp
private val BODY_FIELD_HEIGHT = 200.dp
private val CTA_HEIGHT = 52.dp

@Preview(name = "문의 - 빈 화면", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun InquiryEmptyPreview() {
    LaimoryTheme {
        InquiryScreen(
            innerPadding = PaddingValues(),
            state = InquiryUiState(),
            selectedTab = InquiryTab.COMPOSE,
            onSelectTab = {},
            onIntent = {},
            history = {},
        )
    }
}

@Preview(name = "문의 - 입력·첨부", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun InquiryFilledPreview() {
    LaimoryTheme(darkTheme = true) {
        InquiryScreen(
            innerPadding = PaddingValues(),
            state =
                InquiryUiState(
                    email = "user@example",
                    title = "사진이 빠져요",
                    description = "타임라인을 만들 때 사진이 빠져요.",
                    attachmentUris = listOf("content://a", "content://b"),
                    emailError = "답변을 받을 수 있는 이메일 주소를 입력해 주세요.",
                ),
            selectedTab = InquiryTab.COMPOSE,
            onSelectTab = {},
            onIntent = {},
            history = {},
        )
    }
}
