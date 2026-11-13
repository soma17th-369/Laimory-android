package com.soma369.laimory.feature.home.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.soma369.laimory.core.ui.appicon.rememberAppIcon
import com.soma369.laimory.core.ui.component.LaimoryTopAppBar
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.home.component.DraftConsentLocationMap
import com.soma369.laimory.feature.home.component.InfoTooltipIcon
import com.soma369.laimory.feature.home.component.iconRes
import com.soma369.laimory.feature.home.component.label
import com.soma369.laimory.feature.home.state.ConsentLocationMarker
import com.soma369.laimory.feature.home.state.DraftConsentDeletionUndo
import com.soma369.laimory.feature.home.state.DraftConsentDetailItem
import com.soma369.laimory.feature.home.state.DraftConsentDetailSection
import com.soma369.laimory.feature.home.state.DraftConsentTypeGroup
import com.soma369.laimory.feature.home.state.DraftConsentTypeSummary
import com.soma369.laimory.feature.home.state.DraftConsentUiIntent
import com.soma369.laimory.feature.home.state.DraftConsentUiState
import com.soma369.laimory.feature.home.viewmodel.DraftConsentViewModel
import kotlinx.coroutines.delay
import com.soma369.laimory.core.ui.R as UiR

/**
 * 데이터 유형 1개의 실제 전송 항목을 확인하는 상세 화면.
 *
 * 동의 화면과 같은 activity 범위 ViewModel 을 공유해 동일한 생성 시도 스냅샷을 읽는다.
 * 확인 전용이며 원본 데이터 편집은 제공하지 않는다.
 */
@Composable
fun DraftConsentDetailRoute(
    innerPadding: PaddingValues,
    typeGroup: String?,
    viewModel: DraftConsentViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // 약관 화면에 다녀와 동의하고 돌아오는 경로가 있다. 같은 스냅샷으로 돌아오므로 스냅샷 갱신만
    // 봐서는 재판정 계기가 없어, 복귀를 알려 지도와 주소 해석을 다시 연다.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.sendIntent(DraftConsentUiIntent.Sync)
    }
    // ViewModel 은 Activity 범위라 화면을 떠나도 남는다. 떠나면 삭제 묶음을 끝내 실행 취소를 닫는다.
    DisposableEffect(viewModel) {
        onDispose { viewModel.sendIntent(DraftConsentUiIntent.EndDeletionUndo) }
    }
    val group = typeGroup?.let { name -> DraftConsentTypeGroup.entries.firstOrNull { it.name == name } }
    DraftConsentDetailContent(
        innerPadding = innerPadding,
        state = state,
        group = group,
        onIntent = viewModel::sendIntent,
    )
}

@Composable
private fun DraftConsentDetailContent(
    innerPadding: PaddingValues,
    state: DraftConsentUiState,
    group: DraftConsentTypeGroup?,
    onIntent: (DraftConsentUiIntent) -> Unit,
) {
    // 상세에서의 back 은 준비 상태를 유지한 채 동의 화면으로만 돌아간다.
    BackHandler {
        onIntent(DraftConsentUiIntent.CloseTypeDetail)
    }
    DraftConsentDetailScreen(
        innerPadding = innerPadding,
        summary = group?.let { state.content?.summaryOf(it) },
        title = group?.label() ?: "전송 상세",
        excludedRawIds = state.excludedRawIds,
        locationMarkers = state.content?.locationMarkers.orEmpty(),
        isMapRenderAllowed = state.isMapRenderAllowed,
        isLocationIncluded = state.isLocationIncluded,
        isLocationToggleEnabled = !state.isSubmitting && !state.isSelectionReadOnly,
        isReadOnly = state.isSelectionReadOnly,
        onToggleItem = { itemKey -> onIntent(DraftConsentUiIntent.ToggleItemInclusion(itemKey)) },
        onToggleLocation = { onIntent(DraftConsentUiIntent.ToggleLocationInclusion) },
        onBack = { onIntent(DraftConsentUiIntent.CloseTypeDetail) },
        canDelete = state.canDelete,
        deletionUndo = state.deletionUndo,
        onDeleteItem = { itemKey -> onIntent(DraftConsentUiIntent.DeleteItem(itemKey)) },
        onDeleteNotificationApp = { packageName -> onIntent(DraftConsentUiIntent.DeleteNotificationApp(packageName)) },
        onDeleteAll = { group -> onIntent(DraftConsentUiIntent.DeleteAllOfGroup(group)) },
        onUndoDeletion = { token -> onIntent(DraftConsentUiIntent.UndoDeletion(token)) },
        onExpireDeletionUndo = { token -> onIntent(DraftConsentUiIntent.ExpireDeletionUndo(token)) },
    )
}

@Composable
private fun DraftConsentDetailScreen(
    innerPadding: PaddingValues,
    summary: DraftConsentTypeSummary?,
    title: String,
    onBack: () -> Unit,
    excludedRawIds: Set<String> = emptySet(),
    locationMarkers: List<ConsentLocationMarker> = emptyList(),
    isMapRenderAllowed: Boolean = false,
    isLocationIncluded: Boolean = true,
    isLocationToggleEnabled: Boolean = true,
    isReadOnly: Boolean = false,
    onToggleItem: (String) -> Unit = {},
    onToggleLocation: () -> Unit = {},
    canDelete: Boolean = false,
    deletionUndo: DraftConsentDeletionUndo? = null,
    onDeleteItem: (String) -> Unit = {},
    onDeleteNotificationApp: (String) -> Unit = {},
    onDeleteAll: (DraftConsentTypeGroup) -> Unit = {},
    onUndoDeletion: (Long) -> Unit = {},
    onExpireDeletionUndo: (Long) -> Unit = {},
) {
    val isDeletable = canDelete && summary?.group in DELETABLE_GROUPS
    DetailBody(
        innerPadding = innerPadding,
        summary = summary,
        title = title,
        onBack = onBack,
        excludedRawIds = excludedRawIds,
        locationMarkers = locationMarkers,
        isMapRenderAllowed = isMapRenderAllowed,
        isLocationIncluded = isLocationIncluded,
        isLocationToggleEnabled = isLocationToggleEnabled,
        isReadOnly = isReadOnly,
        onToggleItem = onToggleItem,
        onToggleLocation = onToggleLocation,
        onDeleteItem = onDeleteItem.takeIf { isDeletable },
        onDeleteNotificationApp = onDeleteNotificationApp.takeIf { canDelete },
        onDeleteAll =
            summary
                ?.takeIf { isDeletable && it.sections.any { section -> section.items.isNotEmpty() } }
                ?.let { { onDeleteAll(it.group) } },
        deletionUndo = deletionUndo,
        onUndoDeletion = onUndoDeletion,
        onExpireDeletionUndo = onExpireDeletionUndo,
    )
}

@Composable
private fun DetailBody(
    innerPadding: PaddingValues,
    summary: DraftConsentTypeSummary?,
    title: String,
    onBack: () -> Unit,
    excludedRawIds: Set<String>,
    locationMarkers: List<ConsentLocationMarker>,
    isMapRenderAllowed: Boolean,
    isLocationIncluded: Boolean,
    isLocationToggleEnabled: Boolean,
    isReadOnly: Boolean,
    onToggleItem: (String) -> Unit,
    onToggleLocation: () -> Unit,
    onDeleteItem: ((String) -> Unit)?,
    onDeleteNotificationApp: ((String) -> Unit)?,
    onDeleteAll: (() -> Unit)?,
    deletionUndo: DraftConsentDeletionUndo?,
    onUndoDeletion: (Long) -> Unit,
    onExpireDeletionUndo: (Long) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
    ) {
        LaimoryTopAppBar(
            title = { Text(title) },
            onBackClick = onBack,
        )
        if (summary == null) {
            DetailUnavailableContent(onBack = onBack)
            return
        }

        // 완성된 날은 보기만 한다. 카드에 토글을 두지 않아야 체크박스로 낭독되지 않는다.
        val itemToggle: ((String) -> Unit)? = onToggleItem.takeUnless { isReadOnly }

        // rawId 하나가 마커 여러 개(이동의 시작·도착)를 가질 수 있어 목록으로 모은다.
        val markerOrders =
            remember(locationMarkers) {
                locationMarkers.groupBy { it.sourceRawId }.mapValues { (_, group) -> group.map { it.order } }
            }

        // 지도와 전송 스위치는 목록 **밖** 고정 영역이다. 목록 안에 두면 스크롤로 사라지는 데다,
        // 지도 드래그와 목록 스크롤이 같은 세로 제스처를 두고 다퉈 지도를 움직이기 어렵다.
        if (summary.group == DraftConsentTypeGroup.LOCATION) {
            Column(
                modifier =
                    Modifier.padding(
                        start = Spacing.extraLarge,
                        end = Spacing.extraLarge,
                        top = Spacing.medium,
                    ),
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                DraftConsentLocationMap(
                    markers = locationMarkers,
                    excludedRawIds = excludedRawIds,
                    renderAllowed = isMapRenderAllowed,
                    onToggleMarker = { rawId -> itemToggle?.invoke(rawId) },
                )
                LocationTransferSwitchRow(
                    included = isLocationIncluded,
                    enabled = isLocationToggleEnabled && locationMarkers.isNotEmpty(),
                    onToggle = onToggleLocation,
                )
            }
        }

        // 목록과 스낵바를 한 상자에 둔다. 스낵바가 아래 `모두 지우기` 를 덮지 않고 그 바로 위에 뜬다.
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        start = Spacing.extraLarge,
                        end = Spacing.extraLarge,
                        top = Spacing.medium,
                        bottom = Spacing.extraLarge,
                    ),
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                item(key = "notice") {
                    val excludedCount =
                        summary.sections.sumOf { section -> section.items.count { it.key in excludedRawIds } }
                    // 한 줄에는 건수만 둔다. 조작법·주의는 `i` 를 눌러 본다 — 다 적으면 목록보다 안내가 먼저 읽힌다.
                    Row(
                        modifier = Modifier.padding(bottom = Spacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = summary.countLabel(includedCount = summary.sentCount - excludedCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        InfoTooltipIcon(
                            label = "전송 안내",
                            guide = detailGuide(group = summary.group, isReadOnly = isReadOnly, canDelete = onDeleteItem != null),
                            // 바로 위가 앱 바라 위로 띄우면 상태 표시줄까지 덮는다.
                            showsBelow = true,
                        )
                    }
                }
                summary.sections.forEach { section ->
                    sectionItems(
                        group = summary.group,
                        section = section,
                        excludedRawIds = excludedRawIds,
                        markerOrders = markerOrders,
                        onToggleItem = itemToggle,
                        onDeleteItem = onDeleteItem,
                        onDeleteNotificationApp = onDeleteNotificationApp,
                    )
                }
            }
            // 공용 스낵바 호스트는 쓰지 않는다. 연달아 지우면 한 스낵바의 건수만 바뀌어야 하는데, 호스트는
            // 스낵바를 줄 세워 하나씩 띄운다.
            deletionUndo?.let { undo ->
                DeletionUndoSnackbar(
                    undo = undo,
                    onUndo = onUndoDeletion,
                    onExpire = onExpireDeletionUndo,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
        onDeleteAll?.let { DeleteAllBar(onClick = it) }
    }
}

/**
 * 상세 맨 아래 고정 `모두 지우기`. 기록 범위 안의 이 유형을 한 번에 지운다(확인 다이얼로그를 거친다).
 *
 * 목록 끝이 아니라 화면 아래에 고정한다 — 알림이 많으면 끝까지 내려야 보인다. 노티스타 저장 목록과 같은 자리다.
 */
@Composable
private fun DeleteAllBar(onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        TextButton(
            onClick = onClick,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.extraLarge, vertical = Spacing.small),
        ) {
            Text(text = "모두 지우기", style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * 상세 안내 말풍선의 문구. 한 문장씩 줄을 나눈다.
 *
 * 완성된 날은 지금 기기에 모인 것을 보여 주므로 보낸 것과 다를 수 있다. 그래도 지울 수는 있어서,
 * 이미 만든 기록까지 지워진다고 오해하지 않게 기기에서만 지운다고 밝힌다.
 */
private fun detailGuide(
    group: DraftConsentTypeGroup,
    isReadOnly: Boolean,
    canDelete: Boolean,
): String =
    buildList {
        when {
            isReadOnly -> {
                add("보낸 내용과 다를 수 있어요. 지금 기기에 모인 항목이에요.")
                add("전송 선택은 바꿀 수 없어요. 흐리게 표시된 항목은 전송에서 뺀 항목이에요.")
                if (canDelete) add("왼쪽으로 밀면 지울 수 있어요. $DEVICE_ONLY_NOTICE")
            }

            group == DraftConsentTypeGroup.PHOTO -> {
                add("아래 사진이 그대로 서버로 전송돼요.")
                add("사진은 홈 사진 선택에서 변경할 수 있어요.")
            }

            group == DraftConsentTypeGroup.LOCATION -> {
                add("지도 핀을 누르면 장소와 포함 여부가 보이고, 말풍선을 한 번 더 누르면 바뀌어요.")
                add("핀 번호는 아래 목록과 같아요.")
                if (canDelete) add(SWIPE_TO_DELETE_NOTICE)
            }

            else -> {
                add("항목을 누르면 전송에서 제외하거나 다시 포함할 수 있어요.")
                add("흐리게 표시된 항목은 전송되지 않아요.")
                if (canDelete) add(SWIPE_TO_DELETE_NOTICE)
            }
        }
    }.joinToString(separator = "\n")

/**
 * 지도 핀과 목록을 잇는 번호 배지.
 *
 * 이동은 시작·도착 두 핀이 한 항목에 묶여 `2-3` 처럼 범위로 적는다.
 * 배지 자체는 장식이라 낭독에서 뺀다 — 카드가 이미 장소·시각·포함 여부를 읽어 준다.
 */
@Composable
private fun MarkerOrderBadge(
    orders: List<Int>,
    included: Boolean,
) {
    if (orders.isEmpty()) return
    val label = if (orders.size > 1) "${orders.first()}-${orders.last()}" else orders.first().toString()
    Box(
        modifier =
            Modifier
                .clearAndSetSemantics { }
                .size(ORDER_BADGE_SIZE)
                .background(
                    color =
                        if (included) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (included) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * 위치정보 전송 Switch 한 줄.
 *
 * 행 전체를 하나의 토글로 노출하고 Switch 자체의 클릭은 비운다 — 둘 다 클릭 가능하면 포커스가
 * 나뉘고 스크린 리더가 같은 상태를 두 번 읽는다.
 */
@Composable
private fun LocationTransferSwitchRow(
    included: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .toggleable(
                    value = included,
                    enabled = enabled,
                    role = Role.Switch,
                    onValueChange = { onToggle() },
                ).semantics { stateDescription = if (included) "전송 포함" else "전송 제외" },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.medium, vertical = Spacing.small),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = "위치정보 전송",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Switch(checked = included, onCheckedChange = null, enabled = enabled)
        }
    }
}

private fun LazyListScope.sectionItems(
    group: DraftConsentTypeGroup,
    section: DraftConsentDetailSection,
    excludedRawIds: Set<String>,
    markerOrders: Map<String, List<Int>>,
    onToggleItem: ((String) -> Unit)?,
    onDeleteItem: ((String) -> Unit)?,
    onDeleteNotificationApp: ((String) -> Unit)?,
) {
    section.title?.let { sectionTitle ->
        // 알림은 표시명이 같은 서로 다른 앱이 있을 수 있어 패키지명을 key 로 쓴다.
        item(key = "${group.name}-section-${section.iconPackageName ?: sectionTitle}") {
            if (group == DraftConsentTypeGroup.NOTIFICATION) {
                NotificationSectionHeader(
                    appName = sectionTitle,
                    packageName = section.iconPackageName,
                    count = section.items.size,
                    onDelete =
                        section.iconPackageName?.let { packageName ->
                            onDeleteNotificationApp?.let { delete -> { delete(packageName) } }
                        },
                )
            } else {
                SectionHeader(title = sectionTitle, count = section.items.size.takeIf { group == DraftConsentTypeGroup.LOCATION })
            }
        }
    }
    // 사진은 홈 선택이 정본이라 토글 없이 그대로 보여준다. 나머지 유형은 항목 탭으로 포함↔미포함을 전환한다.
    when (group) {
        DraftConsentTypeGroup.PHOTO ->
            items(
                items = section.items.chunked(PHOTO_GRID_COLUMNS),
                key = { row -> row.first().key },
            ) { row -> PhotoGridRow(row) }

        DraftConsentTypeGroup.CALENDAR ->
            items(items = section.items, key = DraftConsentDetailItem::key) { item ->
                CalendarItemCard(
                    item = item,
                    included = item.key !in excludedRawIds,
                    onToggle = onToggleItem?.let { toggle -> { toggle(item.key) } },
                )
            }

        DraftConsentTypeGroup.LOCATION ->
            items(items = section.items, key = DraftConsentDetailItem::key) { item ->
                val onDelete = onDeleteItem?.let { delete -> { delete(item.key) } }
                SwipeToDeleteBox(onDelete = onDelete) {
                    LocationItemCard(
                        item = item,
                        included = item.key !in excludedRawIds,
                        // 지도 핀과 대조할 수 있게 같은 번호를 붙인다. 이동은 시작·도착 두 핀이라 범위로 적는다.
                        orders = markerOrders[item.key].orEmpty(),
                        onToggle = onToggleItem?.let { toggle -> { toggle(item.key) } },
                        onDelete = onDelete,
                    )
                }
            }

        DraftConsentTypeGroup.HEALTH ->
            items(items = section.items, key = DraftConsentDetailItem::key) { item ->
                HealthItemCard(
                    item = item,
                    included = item.key !in excludedRawIds,
                    onToggle = onToggleItem?.let { toggle -> { toggle(item.key) } },
                )
            }

        DraftConsentTypeGroup.NOTIFICATION ->
            items(items = section.items, key = DraftConsentDetailItem::key) { item ->
                val onDelete = onDeleteItem?.let { delete -> { delete(item.key) } }
                SwipeToDeleteBox(onDelete = onDelete) {
                    NotificationItemCard(
                        item = item,
                        included = item.key !in excludedRawIds,
                        onToggle = onToggleItem?.let { toggle -> { toggle(item.key) } },
                        onDelete = onDelete,
                    )
                }
            }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int? = null,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = Spacing.small, bottom = Spacing.extraSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        count?.let { CountChip(it) }
    }
}

@Composable
private fun NotificationSectionHeader(
    appName: String,
    packageName: String?,
    count: Int,
    onDelete: (() -> Unit)? = null,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = Spacing.small, bottom = Spacing.extraSmall),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NotificationAppIcon(appName = appName, packageName = packageName)
        Text(
            text = appName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "(${count}건)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (onDelete != null) {
            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                onClick = onDelete,
                // 버튼이 묶음마다 하나씩이라 문구만으로는 어느 앱인지 모른다. 낭독에는 앱 이름을 붙인다.
                // `모두 지우기` 는 아래 고정 버튼(유형 전체)이 쓴다.
                modifier = Modifier.semantics { contentDescription = "$appName 알림 지우기" },
            ) {
                Text(text = "지우기", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/**
 * 알림 섹션의 출처 표시. 설치된 앱의 아이콘을 쓰고, 앱 삭제나 패키지 visibility 로 조회에
 * 실패하면 앱 이름 첫 글자로 폴백한다.
 *
 * 수집 당시 표시명을 얻지 못한 알림은 [appName] 이 패키지명으로 채워져 있어(수집기 폴백)
 * 첫 글자가 "c" 처럼 뜻 없는 글자가 된다 — 이 경우에만 공통 알림 아이콘을 쓴다.
 */
@Composable
private fun NotificationAppIcon(
    appName: String,
    packageName: String?,
) {
    val icon = packageName?.let { rememberAppIcon(packageName = it, size = NOTIFICATION_APP_ICON_SIZE) }
    Box(
        modifier =
            Modifier
                .size(NOTIFICATION_APP_ICON_SIZE)
                .clip(RoundedCornerShape(6.dp))
                .then(
                    if (icon == null) {
                        Modifier.background(MaterialTheme.colorScheme.secondaryContainer)
                    } else {
                        Modifier
                    },
                ),
        contentAlignment = Alignment.Center,
    ) {
        when {
            icon != null ->
                Image(
                    bitmap = icon,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )

            appName == packageName ->
                Icon(
                    painter = painterResource(DraftConsentTypeGroup.NOTIFICATION.iconRes()),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )

            else ->
                Text(
                    text = appName.take(1),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
        }
    }
}

@Composable
private fun CountChip(count: Int) {
    Box(
        modifier =
            Modifier
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = "${count}건",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PhotoGridRow(row: List<DraftConsentDetailItem>) {
    Row(horizontalArrangement = Arrangement.spacedBy(PHOTO_GRID_GAP)) {
        row.forEach { item ->
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp)),
            ) {
                AsyncImage(
                    model = item.imageUri,
                    contentDescription = "전송할 사진 ${item.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        repeat(PHOTO_GRID_COLUMNS - row.size) {
            Box(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun CalendarItemCard(
    item: DraftConsentDetailItem,
    included: Boolean,
    onToggle: (() -> Unit)?,
) {
    DetailCard(included = included, onToggle = onToggle) {
        Box(
            modifier =
                Modifier
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            item.description?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = item.timeText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LocationItemCard(
    item: DraftConsentDetailItem,
    included: Boolean,
    orders: List<Int>,
    onToggle: (() -> Unit)?,
    onDelete: (() -> Unit)? = null,
) {
    DetailCard(included = included, onToggle = onToggle, onDelete = onDelete) {
        MarkerOrderBadge(orders = orders, included = included)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            item.description?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = item.timeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // 이동 거리. 시각과 같은 줄 오른쪽 끝에 붙여 줄 수를 늘리지 않는다.
                item.trailingText?.let { trailing ->
                    Text(
                        text = trailing,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthItemCard(
    item: DraftConsentDetailItem,
    included: Boolean,
    onToggle: (() -> Unit)?,
) {
    DetailCard(included = included, onToggle = onToggle) {
        Box(
            modifier =
                Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(DraftConsentTypeGroup.HEALTH.iconRes()),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            item.description?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = item.timeText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NotificationItemCard(
    item: DraftConsentDetailItem,
    included: Boolean,
    onToggle: (() -> Unit)?,
    onDelete: (() -> Unit)? = null,
) {
    DetailCard(included = included, onToggle = onToggle, onDelete = onDelete) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            item.description?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = NOTIFICATION_BODY_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = item.timeText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 전송 항목 카드. Figma 상세 디자인의 상태 표현을 따른다 —
 * 포함이면 primary 강조 테두리에 선명한 내용, 제외면 기본 테두리에 흐린(알파) 내용.
 */
@Composable
private fun DetailCard(
    included: Boolean,
    onToggle: (() -> Unit)?,
    onDelete: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val border =
        if (included) {
            BorderStroke(width = 1.5.dp, color = MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
        }
    // 위치는 카드마다 토글하지 않고 지도 아래 Switch 하나로 제어한다. 그때는 카드에 토글
    // semantics 를 두지 않는다 — 조작할 수 없는 요소가 체크박스로 낭독되면 안 된다.
    val stateModifier =
        if (onToggle == null) {
            Modifier.semantics { stateDescription = if (included) "전송 포함" else "전송 제외" }
        } else {
            Modifier
                .toggleable(
                    value = included,
                    role = Role.Checkbox,
                    onValueChange = { onToggle() },
                ).semantics { stateDescription = if (included) "전송 포함" else "전송 제외" }
        }
    // 밀기는 TalkBack 으로 할 수 없다. 같은 일을 사용자 지정 동작으로 연다.
    val deleteModifier =
        if (onDelete == null) {
            Modifier
        } else {
            Modifier.semantics {
                customActions =
                    listOf(
                        CustomAccessibilityAction(label = "기기에서 지우기") {
                            onDelete()
                            true
                        },
                    )
            }
        }
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .then(stateModifier)
                .then(deleteModifier),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = border,
    ) {
        Row(
            modifier =
                Modifier
                    .padding(Spacing.medium)
                    .alpha(if (included) 1f else EXCLUDED_CONTENT_ALPHA),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
private fun DetailUnavailableContent(onBack: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(Spacing.extraLarge),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "표시할 전송 항목이 없어요",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "동의 화면에서 다시 확인해주세요.",
            modifier = Modifier.padding(top = Spacing.small),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onBack,
            modifier = Modifier.padding(top = Spacing.extraLarge),
        ) {
            Text("돌아가기")
        }
    }
}

private val NOTIFICATION_APP_ICON_SIZE = 24.dp

/** 알림 본문 줄 수. 넘치면 말줄임한다 — 펼치기는 두지 않는다(카드 탭은 전송 토글이다). */
private const val NOTIFICATION_BODY_MAX_LINES = 3

/** 기기에서 지울 수 있는 유형. 원본이 Laimory 로컬 DB 에만 있는 것들이다. */
private val DELETABLE_GROUPS = setOf(DraftConsentTypeGroup.NOTIFICATION, DraftConsentTypeGroup.LOCATION)

private const val SWIPE_TO_DELETE_NOTICE = "왼쪽으로 밀면 기기에서 지워요."
private const val DEVICE_ONLY_NOTICE = "기기에서만 지워요. 이미 만든 기록은 그대로예요."

/** 실행 취소 스낵바 기본 표시 시간. 접근성 설정이 권하는 만큼 늘린다. */
private const val UNDO_SNACKBAR_MILLIS = 4_000L

/**
 * 지우지 못했을 때 카드를 되돌리기까지 기다리는 시간.
 *
 * 지우면 수집 관찰이 목록을 다시 만들며 카드가 사라진다. 그 전에 되돌리면 밀어 낸 카드가 잠깐
 * 돌아왔다 사라지므로 바로 되돌리지 않는다.
 */
private const val SWIPE_RESET_DELAY_MILLIS = 1_000L

private const val PHOTO_GRID_COLUMNS = 3
private val PHOTO_GRID_GAP = 6.dp
private val ORDER_BADGE_SIZE = 24.dp

/** 미포함 항목의 흐림 처리 강도. 내용은 읽히되 포함 항목과 확실히 구분되는 수준. */
private const val EXCLUDED_CONTENT_ALPHA = 0.55f

@Preview(name = "DraftConsentDetail / 위치", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun DraftConsentDetailLocationPreview() {
    LaimoryTheme {
        DraftConsentDetailScreen(
            innerPadding = PaddingValues(),
            summary = locationPreviewSummary(),
            title = "위치",
            onBack = {},
        )
    }
}

@Preview(name = "DraftConsentDetail / 위치 제외 상태", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun DraftConsentDetailLocationExcludedPreview() {
    LaimoryTheme {
        DraftConsentDetailScreen(
            innerPadding = PaddingValues(),
            summary = locationPreviewSummary(),
            title = "위치",
            onBack = {},
            excludedRawIds = setOf("move-1"),
        )
    }
}

private fun locationPreviewSummary(): DraftConsentTypeSummary =
    DraftConsentTypeSummary(
        group = DraftConsentTypeGroup.LOCATION,
        originalCount = 3,
        sentCount = 3,
        sections =
            listOf(
                DraftConsentDetailSection(
                    title = "체류한 장소",
                    items =
                        listOf(
                            DraftConsentDetailItem(
                                key = "stay-1",
                                title = "서울 강남구",
                                description = null,
                                timeText = "8월 11일 14:30 ~ 16:00",
                            ),
                        ),
                ),
                DraftConsentDetailSection(
                    title = "이동 기록",
                    items =
                        listOf(
                            DraftConsentDetailItem(
                                key = "move-1",
                                title = "강남역 → 삼성역",
                                description = "1.2km · 도보",
                                timeText = "8월 11일 16:10",
                            ),
                        ),
                ),
            ),
    )

@Preview(name = "DraftConsentDetail / 알림 Dark", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun DraftConsentDetailNotificationDarkPreview() {
    LaimoryTheme(darkTheme = true) {
        DraftConsentDetailScreen(
            innerPadding = PaddingValues(),
            summary =
                DraftConsentTypeSummary(
                    group = DraftConsentTypeGroup.NOTIFICATION,
                    originalCount = 12,
                    sentCount = 12,
                    sections =
                        listOf(
                            DraftConsentDetailSection(
                                title = "카카오톡",
                                iconPackageName = "com.kakao.talk",
                                items =
                                    listOf(
                                        DraftConsentDetailItem(
                                            key = "noti-1",
                                            title = "민우",
                                            description = "오늘 미팅 시간 변경 가능해?",
                                            timeText = "8월 11일 14:34",
                                        ),
                                    ),
                            ),
                        ),
                ),
            title = "알림",
            onBack = {},
        )
    }
}

/**
 * 왼쪽으로 밀어 지우는 카드 틀. [onDelete] 가 null 이면 밀 수 없는 평범한 카드다.
 *
 * 오른쪽으로 밀기는 막는다 — 방향이 둘이면 어느 쪽이 지우기인지 헷갈린다.
 */
@Composable
private fun SwipeToDeleteBox(
    onDelete: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    if (onDelete == null) {
        content()
        return
    }
    // 저장 가능한 기본 상태(rememberSwipeToDismissBoxState)는 쓰지 않는다. 목록이 항목 키별로 상태를
    // 보관해 두므로, 실행 취소로 같은 키가 돌아오면 밀린 채 되살아나 또 지워진다.
    val positionalThreshold = SwipeToDismissBoxDefaults.positionalThreshold
    val state = remember { SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, positionalThreshold) }
    val currentOnDelete by rememberUpdatedState(onDelete)
    // 지우기는 여기서 한 번만 부른다. SwipeToDismissBox 의 onDismiss 는 콜백이 바뀔 때마다 다시 불려,
    // 밀린 채 다시 그려지면 같은 항목을 또 지운다. 지우면 카드가 목록에서 빠지며 이 효과도 취소된다.
    // 밀린 채 남아 있으면 지우지 못한 것이라 되돌린다.
    LaunchedEffect(state.settledValue) {
        if (state.settledValue != SwipeToDismissBoxValue.EndToStart) return@LaunchedEffect
        currentOnDelete()
        delay(SWIPE_RESET_DELAY_MILLIS)
        state.reset()
    }
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            if (state.dismissDirection == SwipeToDismissBoxValue.EndToStart) DeleteSwipeBackground()
        },
    ) {
        content()
    }
}

@Composable
private fun DeleteSwipeBackground() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.error)
                .padding(horizontal = Spacing.extraLarge),
        contentAlignment = Alignment.CenterEnd,
    ) {
        // 카드가 이미 "기기에서 지우기" 동작을 낭독하므로 아이콘은 장식이다.
        Icon(
            painter = painterResource(UiR.drawable.ico_setting_trash),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onError,
        )
    }
}

/**
 * 삭제 묶음 스낵바. 연달아 지우면 건수만 바뀌고, 지울 때마다 남은 시간을 처음부터 다시 센다.
 *
 * 시간은 화면이 센다 — 접근성 설정이 권하는 표시 시간은 화면에서만 알 수 있다. 다 되면 [onExpire] 로
 * 묶음을 끝낸다.
 */
@Composable
private fun DeletionUndoSnackbar(
    undo: DraftConsentDeletionUndo,
    onUndo: (Long) -> Unit,
    onExpire: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accessibilityManager = LocalAccessibilityManager.current
    val timeoutMillis =
        remember(accessibilityManager) {
            accessibilityManager?.calculateRecommendedTimeoutMillis(
                originalTimeoutMillis = UNDO_SNACKBAR_MILLIS,
                containsIcons = false,
                containsText = true,
                containsControls = true,
            ) ?: UNDO_SNACKBAR_MILLIS
        }
    LaunchedEffect(undo.token) {
        delay(timeoutMillis)
        onExpire(undo.token)
    }
    Snackbar(
        modifier =
            modifier
                .padding(start = Spacing.medium, end = Spacing.medium, bottom = Spacing.large)
                .semantics { liveRegion = LiveRegionMode.Polite },
        action = {
            TextButton(
                onClick = { onUndo(undo.token) },
                colors = ButtonDefaults.textButtonColors(contentColor = SnackbarDefaults.actionColor),
            ) {
                Text("실행 취소")
            }
        },
    ) {
        Text(if (undo.count == 1) "삭제했어요" else "${undo.count}건 삭제했어요")
    }
}
