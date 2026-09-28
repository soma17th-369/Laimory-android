package com.soma369.laimory.feature.settings.screen

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
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
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.settings.state.InquiryUiIntent
import com.soma369.laimory.feature.settings.state.InquiryUiSideEffect
import com.soma369.laimory.feature.settings.state.InquiryUiState
import com.soma369.laimory.feature.settings.viewmodel.InquiryViewModel
import kotlinx.coroutines.flow.Flow
import java.text.NumberFormat
import com.soma369.laimory.core.ui.R as CoreUiR

@Composable
fun InquiryRoute(
    innerPadding: PaddingValues,
    viewModel: InquiryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    InquiryContent(
        innerPadding = innerPadding,
        state = state,
        onIntent = viewModel::sendIntent,
        sideEffectFlow = viewModel.sideEffect,
    )
}

@Composable
private fun InquiryContent(
    innerPadding: PaddingValues,
    state: InquiryUiState,
    onIntent: (InquiryUiIntent) -> Unit,
    sideEffectFlow: Flow<InquiryUiSideEffect>,
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
            }
        }
    }
    BackHandler { onIntent(InquiryUiIntent.BackPressed) }
    InquiryScreen(
        innerPadding = innerPadding,
        state = state,
        onIntent = onIntent,
    )
}

@Composable
private fun InquiryScreen(
    innerPadding: PaddingValues,
    state: InquiryUiState,
    onIntent: (InquiryUiIntent) -> Unit,
) {
    val editable = !state.isSubmitting
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
                    text = "문의하기",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            onBackClick = { onIntent(InquiryUiIntent.BackPressed) },
        )
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
                value = state.body,
                onValueChange = { onIntent(InquiryUiIntent.BodyChanged(it)) },
                label = "문의 내용",
                placeholder = "어떤 점이 궁금하거나 불편했는지 적어 주세요.",
                counterText = "${COUNT_FORMAT.format(state.body.length)} / ${COUNT_FORMAT.format(InquiryInputRules.BODY_MAX_LENGTH)}",
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
            attachmentUris.forEachIndexed { index, uri ->
                AttachmentThumbnail(
                    uri = uri,
                    contentDescription = "첨부한 사진 ${index + 1}",
                    enabled = enabled,
                    onRemove = { onRemove(uri) },
                )
            }
            if (canAdd) AddAttachmentTile(onClick = onAddClick)
        }
    }
}

@Composable
private fun AttachmentThumbnail(
    uri: String,
    contentDescription: String,
    enabled: Boolean,
    onRemove: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(THUMBNAIL_SIZE)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        // 미리보기는 실제 사진을 불러올 수 없다.
        if (!LocalInspectionMode.current) {
            AsyncImage(
                model = uri,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (enabled) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(Spacing.extraSmall)
                        .size(REMOVE_BUTTON_SIZE)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f))
                        .clickable(role = Role.Button, onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(CoreUiR.drawable.ico_default_close),
                    contentDescription = "$contentDescription 빼기",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.surface,
                )
            }
        }
    }
}

@Composable
private fun AddAttachmentTile(onClick: () -> Unit) {
    Box(
        modifier =
            Modifier
                .size(THUMBNAIL_SIZE)
                .clip(MaterialTheme.shapes.medium)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(CoreUiR.drawable.ico_setting_datasource_photo),
            contentDescription = "사진 추가",
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val COUNT_FORMAT: NumberFormat = NumberFormat.getIntegerInstance()

private val SCREEN_HORIZONTAL_PADDING = 24.dp
private val BODY_FIELD_HEIGHT = 200.dp
private val THUMBNAIL_SIZE = 72.dp
private val REMOVE_BUTTON_SIZE = 22.dp
private val CTA_HEIGHT = 52.dp

@Preview(name = "문의 - 빈 화면", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun InquiryEmptyPreview() {
    LaimoryTheme {
        InquiryScreen(innerPadding = PaddingValues(), state = InquiryUiState(), onIntent = {})
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
                    body = "타임라인을 만들 때 사진이 빠져요.",
                    attachmentUris = listOf("content://a", "content://b"),
                    emailError = "답변을 받을 수 있는 이메일 주소를 입력해 주세요.",
                ),
            onIntent = {},
        )
    }
}
