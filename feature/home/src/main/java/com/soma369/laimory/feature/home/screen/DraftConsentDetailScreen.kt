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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.soma369.laimory.core.ui.appicon.rememberAppIcon
import com.soma369.laimory.core.ui.component.LaimoryTopAppBar
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.home.component.iconRes
import com.soma369.laimory.feature.home.component.label
import com.soma369.laimory.feature.home.state.DraftConsentDetailItem
import com.soma369.laimory.feature.home.state.DraftConsentDetailSection
import com.soma369.laimory.feature.home.state.DraftConsentTypeGroup
import com.soma369.laimory.feature.home.state.DraftConsentTypeSummary
import com.soma369.laimory.feature.home.state.DraftConsentUiIntent
import com.soma369.laimory.feature.home.state.DraftConsentUiState
import com.soma369.laimory.feature.home.viewmodel.DraftConsentViewModel

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
        onToggleItem = { itemKey -> onIntent(DraftConsentUiIntent.ToggleItemInclusion(itemKey)) },
        onBack = { onIntent(DraftConsentUiIntent.CloseTypeDetail) },
    )
}

@Composable
private fun DraftConsentDetailScreen(
    innerPadding: PaddingValues,
    summary: DraftConsentTypeSummary?,
    title: String,
    onBack: () -> Unit,
    excludedRawIds: Set<String> = emptySet(),
    onToggleItem: (String) -> Unit = {},
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

        LazyColumn(
            modifier = Modifier.weight(1f),
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
                val label = summary.countLabel(includedCount = summary.sentCount - excludedCount)
                Text(
                    text =
                        if (summary.group == DraftConsentTypeGroup.PHOTO) {
                            "$label · 아래 사진이 그대로 서버로 전송돼요. 사진은 홈 사진 선택에서 변경할 수 있어요."
                        } else {
                            "$label · 항목을 누르면 전송에서 제외하거나 다시 포함할 수 있어요. 흐리게 표시된 항목은 전송되지 않아요."
                        },
                    modifier = Modifier.padding(bottom = Spacing.small),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            summary.sections.forEach { section ->
                sectionItems(
                    group = summary.group,
                    section = section,
                    excludedRawIds = excludedRawIds,
                    onToggleItem = onToggleItem,
                )
            }
        }
    }
}

private fun LazyListScope.sectionItems(
    group: DraftConsentTypeGroup,
    section: DraftConsentDetailSection,
    excludedRawIds: Set<String>,
    onToggleItem: (String) -> Unit,
) {
    section.title?.let { sectionTitle ->
        // 알림은 표시명이 같은 서로 다른 앱이 있을 수 있어 패키지명을 key 로 쓴다.
        item(key = "${group.name}-section-${section.iconPackageName ?: sectionTitle}") {
            if (group == DraftConsentTypeGroup.NOTIFICATION) {
                NotificationSectionHeader(
                    appName = sectionTitle,
                    packageName = section.iconPackageName,
                    count = section.items.size,
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
                    onToggle = { onToggleItem(item.key) },
                )
            }

        DraftConsentTypeGroup.LOCATION ->
            items(items = section.items, key = DraftConsentDetailItem::key) { item ->
                val included = item.key !in excludedRawIds
                val onToggle = { onToggleItem(item.key) }
                if (item.title.contains(" → ")) {
                    MovementItemCard(item = item, included = included, onToggle = onToggle)
                } else {
                    StayItemCard(item = item, included = included, onToggle = onToggle)
                }
            }

        DraftConsentTypeGroup.HEALTH ->
            items(items = section.items, key = DraftConsentDetailItem::key) { item ->
                HealthItemCard(
                    item = item,
                    included = item.key !in excludedRawIds,
                    onToggle = { onToggleItem(item.key) },
                )
            }

        DraftConsentTypeGroup.NOTIFICATION ->
            items(items = section.items, key = DraftConsentDetailItem::key) { item ->
                NotificationItemCard(
                    item = item,
                    included = item.key !in excludedRawIds,
                    onToggle = { onToggleItem(item.key) },
                )
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
    onToggle: () -> Unit,
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
private fun StayItemCard(
    item: DraftConsentDetailItem,
    included: Boolean,
    onToggle: () -> Unit,
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
                painter = painterResource(DraftConsentTypeGroup.LOCATION.iconRes()),
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
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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

@Composable
private fun MovementItemCard(
    item: DraftConsentDetailItem,
    included: Boolean,
    onToggle: () -> Unit,
) {
    DetailCard(included = included, onToggle = onToggle) {
        Column(
            modifier = Modifier.width(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(10.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
            )
            Box(
                modifier =
                    Modifier
                        .width(2.dp)
                        .height(18.dp)
                        .alpha(0.35f)
                        .background(MaterialTheme.colorScheme.primary),
            )
            Box(
                modifier =
                    Modifier
                        .size(10.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
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
                    style = MaterialTheme.typography.labelSmall,
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
private fun HealthItemCard(
    item: DraftConsentDetailItem,
    included: Boolean,
    onToggle: () -> Unit,
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
    onToggle: () -> Unit,
) {
    DetailCard(included = included, onToggle = onToggle) {
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
                    maxLines = 2,
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
    onToggle: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    val border =
        if (included) {
            BorderStroke(width = 1.5.dp, color = MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
        }
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .toggleable(
                    value = included,
                    role = Role.Checkbox,
                    onValueChange = { onToggle() },
                )
                .semantics { stateDescription = if (included) "전송 포함" else "전송 제외" },
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

private const val PHOTO_GRID_COLUMNS = 3
private val PHOTO_GRID_GAP = 6.dp

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
