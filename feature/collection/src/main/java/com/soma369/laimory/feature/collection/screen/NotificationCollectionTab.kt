package com.soma369.laimory.feature.collection.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soma369.laimory.core.domain.model.collection.NotificationFilter
import com.soma369.laimory.core.domain.model.collection.NotificationPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.source.InstalledApp
import com.soma369.laimory.core.ui.LocalSnackbarHostState
import com.soma369.laimory.core.ui.theme.laimoryColors
import com.soma369.laimory.feature.collection.state.NotificationUiIntent
import com.soma369.laimory.feature.collection.state.NotificationUiSideEffect
import com.soma369.laimory.feature.collection.state.NotificationUiState
import com.soma369.laimory.feature.collection.viewmodel.NotificationCollectionViewModel
import kotlinx.coroutines.flow.Flow
import java.time.format.DateTimeFormatter

/**
 * 수집 실험실의 "알림" 탭. NotificationListenerService 로 실시간 캡처된 알림을 확인·설정한다.
 *
 * 과거 백필은 불가하며(서비스가 붙어있는 동안 발생한 알림만), 화면은 ① 알림 접근 권한 → ② 앱/키워드 필터 →
 * ③ 캡처된 목록(수집 이유 컬러 닷) 순으로 구성한다. 화면 인셋은 컨테이너([CollectionLabRoute])가 소유한다.
 */
@Composable
fun NotificationCollectionTab(viewModel: NotificationCollectionViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    NotificationCollectionContent(
        state = state,
        onIntent = viewModel::sendIntent,
        snackbarFlow = viewModel.snackbar,
        sideEffectFlow = viewModel.sideEffect,
    )
}

@Composable
private fun NotificationCollectionContent(
    state: NotificationUiState,
    onIntent: (NotificationUiIntent) -> Unit,
    snackbarFlow: Flow<String>,
    sideEffectFlow: Flow<NotificationUiSideEffect>,
) {
    val snackbarHostState = LocalSnackbarHostState.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        snackbarFlow.collect { message -> snackbarHostState.showSnackbar(message) }
    }
    LaunchedEffect(Unit) {
        sideEffectFlow.collect { effect ->
            when (effect) {
                is NotificationUiSideEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    // 알림 접근은 설정에서 켜므로, 설정에 다녀온 뒤(resume)마다 상태를 다시 읽는다.
    var accessGranted by remember { mutableStateOf(NotificationAccess.isGranted(context)) }
    LifecycleResumeEffect(Unit) {
        accessGranted = NotificationAccess.isGranted(context)
        onPauseOrDispose { }
    }

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            PermissionSection(
                granted = accessGranted,
                onOpenSettings = { context.startActivity(NotificationAccess.settingsIntent()) },
            )
        }

        item {
            CollectionPolicyNotice(
                collectOnClick = state.collectOnClick,
                hasPostFilter =
                    state.useDefaultKeywords || state.keywords.isNotEmpty() || state.allowedPackages.isNotEmpty(),
            )
        }
        item {
            KeywordFilter(
                keywords = state.keywords,
                useDefaultKeywords = state.useDefaultKeywords,
                onToggleDefaults = { onIntent(NotificationUiIntent.ToggleDefaultKeywords) },
                onAdd = { onIntent(NotificationUiIntent.AddKeyword(it)) },
                onRemove = { onIntent(NotificationUiIntent.RemoveKeyword(it)) },
            )
        }
        item {
            AppFilter(
                apps = state.installedApps,
                allowedPackages = state.allowedPackages,
                onToggle = { onIntent(NotificationUiIntent.ToggleApp(it)) },
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "캡처된 알림 (${state.stagedNotifications.size}건)",
                    style = MaterialTheme.typography.titleMedium,
                )
                OutlinedButton(
                    onClick = { onIntent(NotificationUiIntent.ClearStaged) },
                    enabled = state.stagedNotifications.isNotEmpty(),
                ) { Text("일괄 삭제") }
            }
        }

        if (state.stagedNotifications.isEmpty()) {
            item {
                Text(
                    text = "캡처된 알림이 없습니다.\n권한을 켜고 앱이나 키워드를 설정하거나 알림을 클릭해보세요.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                )
            }
        } else {
            items(state.stagedNotifications, key = { it.rawId }) { item -> NotificationRow(item) }
        }
    }
}

@Composable
private fun PermissionSection(
    granted: Boolean,
    onOpenSettings: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (granted) "알림 접근 허용됨" else "알림 접근 권한이 필요합니다",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onOpenSettings) { Text(if (granted) "설정" else "허용") }
        }
    }
}

@Composable
private fun CollectionPolicyNotice(
    collectOnClick: Boolean,
    hasPostFilter: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text =
                when {
                    collectOnClick && !hasPostFilter ->
                        "앱이나 키워드를 선택하지 않아 직접 클릭한 알림만 수집하고 있어요."

                    collectOnClick ->
                        "직접 클릭했거나 선택한 앱·키워드와 일치하는 알림을 수집해요."

                    hasPostFilter ->
                        "선택한 앱·키워드와 일치하는 알림을 수집해요."

                    else ->
                        "현재 활성화된 알림 수집 조건이 없어요."
                },
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KeywordFilter(
    keywords: Set<String>,
    useDefaultKeywords: Boolean,
    onToggleDefaults: () -> Unit,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    var input by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("키워드", style = MaterialTheme.typography.titleSmall)
        DefaultKeywordToggle(enabled = useDefaultKeywords, onToggle = onToggleDefaults)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("제목·본문에 포함될 단어") },
            )
            Button(
                onClick = {
                    onAdd(input)
                    input = ""
                },
                enabled = input.isNotBlank(),
            ) { Text("추가") }
        }
        if (keywords.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                keywords.forEach { keyword ->
                    InputChip(
                        selected = false,
                        onClick = { onRemove(keyword) },
                        label = { Text(keyword) },
                        trailingIcon = { Text("✕", style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
        }
    }
}

/**
 * 기본 키워드 사용 토글.
 *
 * 기본 사전은 DataStore 의 사용자 키워드에 병합 저장하지 않으므로, 껐다 켜도 사용자가 직접
 * 등록한 키워드는 그대로 남는다.
 */
@Composable
private fun DefaultKeywordToggle(
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("기본 키워드 사용", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = enabled, onCheckedChange = { onToggle() })
        }
        Text(
            text = NotificationFilter.DEFAULT_KEYWORDS.joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // 판정에만 있고 사전에는 드러나지 않는 두 조건이라, 목록 밖 앱에서 왜 안 걸리는지 알 수 있게 적어 둔다.
        Text(
            text = "기본 키워드는 지원되는 금융·주문·배송·예약·교통 앱에서만 적용됩니다.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "문자는 [Web발신] 표기가 붙은 기업 발송만 수집하며, 개인 대화는 수집하지 않습니다.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "(광고) 표기가 붙은 알림은 키워드가 맞아도 제외합니다.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AppFilter(
    apps: List<InstalledApp>,
    allowedPackages: Set<String>,
    onToggle: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("앱 (${allowedPackages.size}개 선택)", style = MaterialTheme.typography.titleSmall)
            Text(if (expanded) "접기 ▲" else "펼치기 ▼", style = MaterialTheme.typography.labelMedium)
        }
        if (expanded) {
            // 목록이 길 수 있어 높이를 제한하고 자체 스크롤(부모 LazyColumn 과 중첩되지만 고정 높이라 안전).
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp)) {
                items(apps, key = { it.packageName }) { app ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onToggle(app.packageName) }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Checkbox(checked = app.packageName in allowedPackages, onCheckedChange = { onToggle(app.packageName) })
                        Text(app.label, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(item: SourceItem) {
    val payload = item.payload as? NotificationPayload ?: return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(payload.appName, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                ReasonBadge(payload.collectReason)
            }
            payload.title?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            payload.text?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(item.postedText(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun ReasonBadge(reason: NotificationPayload.CollectReason) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(8.dp).background(reason.color(), CircleShape))
        Text(reason.label(), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun NotificationPayload.CollectReason.color(): Color =
    when (this) {
        NotificationPayload.CollectReason.KEYWORD -> MaterialTheme.laimoryColors.info
        NotificationPayload.CollectReason.APP -> MaterialTheme.laimoryColors.success
        NotificationPayload.CollectReason.CLICK -> MaterialTheme.laimoryColors.warning
        NotificationPayload.CollectReason.ALL -> MaterialTheme.colorScheme.onSurfaceVariant
    }

private fun NotificationPayload.CollectReason.label(): String =
    when (this) {
        NotificationPayload.CollectReason.KEYWORD -> "키워드"
        NotificationPayload.CollectReason.APP -> "앱"
        NotificationPayload.CollectReason.CLICK -> "클릭"
        NotificationPayload.CollectReason.ALL -> "전체"
    }

private val postedFormatter = DateTimeFormatter.ofPattern("M/d(E) HH:mm")

private fun SourceItem.postedText(): String = startAt.atZone(timeZoneId).format(postedFormatter)
