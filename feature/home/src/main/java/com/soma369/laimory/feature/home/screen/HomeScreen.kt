package com.soma369.laimory.feature.home.screen

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soma369.laimory.core.ui.LocalSnackbarHostState
import com.soma369.laimory.core.ui.appicon.rememberAppIcon
import com.soma369.laimory.core.ui.component.LaimoryDropdownMenu
import com.soma369.laimory.core.ui.component.LaimoryDropdownMenuItem
import com.soma369.laimory.core.ui.component.timepicker.LaimoryTimePickerSheet
import com.soma369.laimory.core.ui.component.timepicker.LaimoryTimePickerValue
import com.soma369.laimory.core.ui.component.timepicker.TimePickerDateOption
import com.soma369.laimory.core.ui.component.timepicker.TimePickerField
import com.soma369.laimory.core.ui.component.timepicker.TimePickerMinuteStep
import com.soma369.laimory.core.ui.greeting.GreetingEmphasis
import com.soma369.laimory.core.ui.greeting.nicknameGreetingSegments
import com.soma369.laimory.core.ui.permission.DataPermission
import com.soma369.laimory.core.ui.permission.DataPermissionState
import com.soma369.laimory.core.ui.permission.DataSourceStatus
import com.soma369.laimory.core.ui.permission.LocationPermissionStep
import com.soma369.laimory.core.ui.permission.rememberDataPermissionState
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.core.util.permission.PhotoPermission
import com.soma369.laimory.feature.home.component.HomeDatePickerDialog
import com.soma369.laimory.feature.home.component.HomePhotoGrid
import com.soma369.laimory.feature.home.component.HomeRotatingContent
import com.soma369.laimory.feature.home.component.HomeSourceCard
import com.soma369.laimory.feature.home.component.HomeTimelineButton
import com.soma369.laimory.feature.home.component.PhotoSelectionSheet
import com.soma369.laimory.feature.home.component.cardBody
import com.soma369.laimory.feature.home.component.cardClick
import com.soma369.laimory.feature.home.component.permissionAction
import com.soma369.laimory.feature.home.component.timeRangeLabel
import com.soma369.laimory.feature.home.state.DraftCreationStatus
import com.soma369.laimory.feature.home.state.DraftEndDay
import com.soma369.laimory.feature.home.state.HomeCalendarItem
import com.soma369.laimory.feature.home.state.HomeNotificationApp
import com.soma369.laimory.feature.home.state.HomeSourceKind
import com.soma369.laimory.feature.home.state.HomeTimeField
import com.soma369.laimory.feature.home.state.HomeTimeSheetState
import com.soma369.laimory.feature.home.state.HomeUiIntent
import com.soma369.laimory.feature.home.state.HomeUiSideEffect
import com.soma369.laimory.feature.home.state.HomeUiState
import com.soma369.laimory.feature.home.state.isDateLocked
import com.soma369.laimory.feature.home.viewmodel.HomeViewModel
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.soma369.laimory.core.ui.R as UiR

@Composable
fun HomeRoute(
    innerPadding: PaddingValues,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    // 판정은 설정·온보딩이 쓰는 공용 상태를 그대로 쓴다. 스스로 ON_RESUME 마다 다시 본다.
    val permissionState = rememberDataPermissionState()
    // 그 "다시 봄"은 **다음 재구성**에 반영된다. 같은 ON_RESUME 안에서 읽으면 직전 재구성의
    // 값이라, 설정에서 허용하고 돌아와도 홈에는 이전 상태가 남는다. 값이 바뀔 때 싣는다.
    val sourcePermissions =
        HomeUiIntent.RefreshSourcePermissions(
            photo = permissionState.statusOf(DataPermission.PHOTO),
            calendar = permissionState.statusOf(DataPermission.CALENDAR),
            location = permissionState.locationDotStatus(),
            notification = permissionState.statusOf(DataPermission.NOTIFICATION_LISTENER),
        )
    LaunchedEffect(sourcePermissions) { viewModel.sendIntent(sourcePermissions) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.sendIntent(HomeUiIntent.RefreshProfile)
        viewModel.sendIntent(
            HomeUiIntent.RefreshPhotos(
                hasAccess = PhotoPermission.canRead(context),
                limited = PhotoPermission.isLimited(context),
            ),
        )
        // 약관 화면에 다녀와 동의하고 돌아오는 경로가 있어 복귀마다 다시 판정한다.
        viewModel.sendIntent(HomeUiIntent.RefreshLocationConsent)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeContent(
        innerPadding = innerPadding,
        state = state,
        onIntent = viewModel::sendIntent,
        onRequestPermission = permissionState::act,
        snackbarFlow = viewModel.snackbar,
        sideEffectFlow = viewModel.sideEffect,
    )
}

@Composable
private fun HomeContent(
    innerPadding: PaddingValues,
    state: HomeUiState,
    onIntent: (HomeUiIntent) -> Unit,
    onRequestPermission: (DataPermission) -> Unit,
    snackbarFlow: Flow<String>,
    sideEffectFlow: Flow<HomeUiSideEffect>,
) {
    val snackbarHostState = LocalSnackbarHostState.current
    val context = LocalContext.current
    val photoPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            onIntent(
                HomeUiIntent.ResolvePhotoAccess(
                    granted = PhotoPermission.canRead(context),
                    limited = PhotoPermission.isLimited(context),
                ),
            )
        }

    LaunchedEffect(Unit) {
        snackbarFlow.collect(snackbarHostState::showSnackbar)
    }
    LaunchedEffect(Unit) {
        sideEffectFlow.collect { effect ->
            when (effect) {
                is HomeUiSideEffect.RequestPhotoAccess ->
                    if (!effect.force && PhotoPermission.canRead(context)) {
                        onIntent(
                            HomeUiIntent.ResolvePhotoAccess(
                                granted = true,
                                limited = PhotoPermission.isLimited(context),
                            ),
                        )
                    } else {
                        photoPermissionLauncher.launch(PhotoPermission.required())
                    }

                is HomeUiSideEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    HomeScreen(
        innerPadding = innerPadding,
        state = state,
        onIntent = onIntent,
        onRequestPermission = onRequestPermission,
    )

    if (state.isPhotoSheetVisible) {
        PhotoSelectionSheet(
            state = state,
            onIntent = onIntent,
            onOpenAppSettings = {
                // 앱 상세 설정. 한 번 거부한 뒤에는 시스템이 요청 대화상자를 다시 띄우지 않으므로
                // 여기 말고는 사진 접근을 되살릴 자리가 없다.
                runCatching {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        ),
                    )
                }
            },
        )
    }

    if (state.isDatePickerVisible) {
        HomeDatePickerDialog(
            initialDate = state.selectedDate,
            savedDates = state.savedRecordDates,
            onSelect = { onIntent(HomeUiIntent.SelectDate(it)) },
            onDisplayedMonthChange = { onIntent(HomeUiIntent.LoadMonthlyRecords(it)) },
            onDismiss = { onIntent(HomeUiIntent.DismissDatePicker) },
        )
    }

    state.timeSheet?.let { sheet ->
        LaimoryTimePickerSheet(
            fields = draftTimePickerFields(sheet),
            expandedFieldId = sheet.expandedField?.name,
            onExpandedFieldChange = { id ->
                onIntent(HomeUiIntent.ExpandTimeField(id?.let(HomeTimeField::valueOf)))
            },
            onValueChange = { id, value, _ ->
                onIntent(HomeUiIntent.ChangeSheetTime(HomeTimeField.valueOf(id), value.date, value.time))
            },
            onConfirm = { onIntent(HomeUiIntent.ConfirmTimeSheet) },
            onDismiss = { onIntent(HomeUiIntent.DismissTimePicker) },
            title = "초안 범위 시각",
            confirmEnabled = sheet.isConfirmEnabled,
            supportingText = DRAFT_WINDOW_GUIDE,
        )
    }
}

/**
 * 시작·종료 두 줄을 한 시트에서 다룬다.
 *
 * 시작은 기록 날짜에 고정이라 날짜 열을 두지 않는다. 종료만 당일·익일 두 선택지를 열어, 예전 종료일
 * 칩이 하던 일을 날짜 열이 대신한다.
 */
private fun draftTimePickerFields(sheet: HomeTimeSheetState): List<TimePickerField> =
    listOf(
        TimePickerField(
            id = HomeTimeField.START.name,
            label = "시작 시각",
            value = LaimoryTimePickerValue(date = sheet.recordDate, time = sheet.startTime),
            // 선택지가 하나뿐이라 날짜 롤러는 감춰지고 값에만 날짜가 붙는다 — 시작일 고정 정책을
            // 유지하면서 어느 날의 시각인지 함께 읽힌다.
            dates = listOf(TimePickerDateOption(sheet.recordDate, DraftEndDay.SAME_DAY.label)),
            minuteStep = TimePickerMinuteStep.FIVE,
        ),
        TimePickerField(
            id = HomeTimeField.END.name,
            label = "종료 시각",
            value = LaimoryTimePickerValue.of(sheet.endDateTime),
            dates =
                DraftEndDay.entries.map { endDay ->
                    TimePickerDateOption(
                        date = sheet.recordDate.plusDays(endDay.dayOffset.toLong()),
                        label = endDay.label,
                    )
                },
            minuteStep = TimePickerMinuteStep.FIVE,
            range = sheet.endRange,
        ),
    )

/** 롤러가 이미 범위를 좁히지만, 왜 그 폭인지는 문구로 알려야 알 수 있다. */
private const val DRAFT_WINDOW_GUIDE = "기록 범위는 6시간 이상이어야 하고, 종료는 익일 06:00까지 고를 수 있어요."

@Composable
private fun HomeScreen(
    innerPadding: PaddingValues,
    state: HomeUiState,
    onIntent: (HomeUiIntent) -> Unit,
    onRequestPermission: (DataPermission) -> Unit,
) {
    // CTA 는 **바닥에 고정**하고 카드들만 그 위 영역에 둔다. 사진 칸이 1:1 이라 격자 높이가 화면
    // 폭을 따라가므로, 좁고 긴 화면이나 큰 글꼴에서는 카드가 다 들어가지 않을 수 있다. 그때도
    // CTA 는 화면 밖으로 밀리지 않고 카드 영역만 스크롤된다. 다 들어가면 스크롤이 생기지 않는다.
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(start = Spacing.extraLarge, end = Spacing.extraLarge, bottom = HOME_BOTTOM_PADDING),
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    // 시안은 상태 표시줄 아래 6 을 띄운다.
                    .padding(top = HOME_TOP_PADDING, bottom = Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Spacing.large),
        ) {
            // 인사말과 날짜는 한 덩어리다(시안 간격 2). 카드 사이 간격으로 벌리면 둘이 따로 논다.
            Column(verticalArrangement = Arrangement.spacedBy(HEADER_LINE_GAP)) {
                HomeHeaderRow(
                    nickname = state.nickname,
                    onPastRecordsClick = { onIntent(HomeUiIntent.OpenPastRecords) },
                    // 개발 도구는 버튼을 두지 않고 인사말 뒤에 숨긴다. release 에는 진입점 자체가 없다
                    // (ViewModel 도 호출 경계에서 한 번 더 막는다).
                    onOpenCollectionLab =
                        if (state.isCollectionLabAccessible) {
                            { onIntent(HomeUiIntent.NavigateToCollection) }
                        } else {
                            null
                        },
                    onOpenHealthDetail =
                        if (state.isCollectionLabAccessible) {
                            { onIntent(HomeUiIntent.OpenHealthDetail) }
                        } else {
                            null
                        },
                )
                HomeDateRow(
                    selectedDate = state.selectedDate,
                    windowText = state.timeRangeLabel(),
                    enabled = !state.isDateLocked,
                    onDateClick = { onIntent(HomeUiIntent.ShowDatePicker) },
                    onRangeClick = { onIntent(HomeUiIntent.ShowTimePicker(HomeTimeField.START)) },
                )
            }

            HomeSourceCard(
                kind = HomeSourceKind.PHOTO,
                status = state.permissions.photo,
                body = state.cardBody(HomeSourceKind.PHOTO),
                onClick = state.cardClick(HomeSourceKind.PHOTO, onIntent, onRequestPermission),
                permissionAction = state.permissionAction(HomeSourceKind.PHOTO, onRequestPermission),
            ) {
                HomePhotoGrid(cells = state.summary.photoCells)
            }

            HomeSourceCard(
                kind = HomeSourceKind.CALENDAR,
                status = state.permissions.calendar,
                body = state.cardBody(HomeSourceKind.CALENDAR),
                onClick = state.cardClick(HomeSourceKind.CALENDAR, onIntent, onRequestPermission),
                permissionAction = state.permissionAction(HomeSourceKind.CALENDAR, onRequestPermission),
            ) {
                HomeCalendarSlot(items = state.summary.calendarItems)
            }

            Row(
                // 두 반쪽 카드의 높이를 맞춘다. 큰 글꼴에서 한쪽만 늘면(빈 알림은 한 줄) 테두리가 어긋난다.
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HomeSourceCard(
                    kind = HomeSourceKind.LOCATION,
                    status = state.permissions.location,
                    body = state.cardBody(HomeSourceKind.LOCATION),
                    onClick = state.cardClick(HomeSourceKind.LOCATION, onIntent, onRequestPermission),
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    permissionAction = state.permissionAction(HomeSourceKind.LOCATION, onRequestPermission),
                ) {
                    HomeLabeledSlot(
                        label = "가장 오래 머문 곳",
                        value =
                            when {
                                !state.isLocationConsentGranted -> "동의 후 표시"
                                else -> state.summary.stayPlace?.label ?: "주소 미확인"
                            },
                    )
                }
                HomeSourceCard(
                    kind = HomeSourceKind.NOTIFICATION,
                    status = state.permissions.notification,
                    body = state.cardBody(HomeSourceKind.NOTIFICATION),
                    onClick = state.cardClick(HomeSourceKind.NOTIFICATION, onIntent, onRequestPermission),
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    permissionAction = state.permissionAction(HomeSourceKind.NOTIFICATION, onRequestPermission),
                ) {
                    HomeNotificationSlot(apps = state.summary.notificationApps)
                }
            }
        }

        HomeTimelineButton(
            status = state.draftStatus,
            selectedDate = state.selectedDate,
            today = LocalDate.now(),
            // 제출을 기다리는 동안에는 다시 눌러도 아무 일이 없어야 한다.
            enabled = !state.isSubmitting,
            onClick = {
                onIntent(
                    when (state.draftStatus) {
                        DraftCreationStatus.SUCCESS -> HomeUiIntent.ViewDraft
                        // 생성 중에는 같은 작업의 로딩 화면으로 다시 들어간다.
                        DraftCreationStatus.PROCESSING,
                        DraftCreationStatus.LONG_RUNNING,
                        -> HomeUiIntent.OpenDraftLoading
                        // 사진은 이제 카드에서 고른다. CTA 는 곧장 확인 다이얼로그로 간다.
                        DraftCreationStatus.IDLE, DraftCreationStatus.FAILED -> HomeUiIntent.CreateDraft
                    },
                )
            },
        )
    }
}

/** 상태 표시줄 아래 첫 줄까지(시안 6). */
private val HOME_TOP_PADDING = 6.dp

/** 인사말과 날짜 줄 사이(시안 2). */
private val HEADER_LINE_GAP = 2.dp

/**
 * CTA 와 바텀바 사이. 붙어 있으면 시그니처 버튼이 바텀바의 일부처럼 보인다.
 *
 * 최소 기준으로 카드 사이 간격과 같은 값을 둔다. 이 여백만큼 카드 영역이 줄어든다.
 *
 * 시안의 CTA 아래(35)는 위에서부터 쌓고 **남은 자리**라 옮기지 않는다 — 여기서는 CTA 를 바닥에
 * 고정하므로 남는 높이는 카드 영역이 받는다.
 */
private val HOME_BOTTOM_PADDING = Spacing.large

/**
 * 인사말 + `지난 기록`.
 *
 * 개발 도구 진입점이 하나라도 있으면(debug) 인사말을 눌러 **개발 메뉴**를 연다 — 수집 데이터와
 * 건강 상세. 버튼으로 두면 개발 빌드에서만 홈이 한 줄 늘어 release 와 다른 화면을 보게 된다.
 */
@Composable
private fun HomeHeaderRow(
    nickname: String?,
    onPastRecordsClick: () -> Unit,
    onOpenCollectionLab: (() -> Unit)?,
    onOpenHealthDetail: (() -> Unit)?,
) {
    var isDebugMenuExpanded by remember { mutableStateOf(false) }
    val hasDebugMenu = onOpenCollectionLab != null || onOpenHealthDetail != null
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 메뉴는 인사말에 붙여 띄운다. Popup 은 감싼 상자를 기준으로 자리를 잡는다.
        Box {
            HomeGreeting(
                nickname = nickname,
                onClick = if (hasDebugMenu) ({ isDebugMenuExpanded = true }) else null,
            )
            LaimoryDropdownMenu(
                expanded = isDebugMenuExpanded,
                onDismissRequest = { isDebugMenuExpanded = false },
            ) {
                onOpenCollectionLab?.let { open ->
                    LaimoryDropdownMenuItem(
                        label = "수집 데이터 자세히 보기",
                        onClick = {
                            isDebugMenuExpanded = false
                            open()
                        },
                    )
                }
                onOpenHealthDetail?.let { open ->
                    LaimoryDropdownMenuItem(
                        label = "건강 상세",
                        onClick = {
                            isDebugMenuExpanded = false
                            open()
                        },
                    )
                }
            }
        }
        Icon(
            painter = painterResource(UiR.drawable.ico_home_past_records),
            // 아이콘만 서 있어 뜻을 그림으로만 전한다 — 스크린 리더가 읽을 이름을 붙인다.
            contentDescription = "지난 기록",
            modifier =
                Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onPastRecordsClick)
                    .size(32.dp),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** 날짜 + 기록 범위 칩. 날짜를 누르면 피커, 칩을 누르면 타임 피커다. */
@Composable
private fun HomeDateRow(
    selectedDate: LocalDate,
    windowText: String,
    enabled: Boolean,
    onDateClick: () -> Unit,
    onRangeClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = HOME_DATE_FORMAT.format(selectedDate),
            modifier =
                Modifier
                    .clip(RoundedCornerShape(Spacing.small))
                    .clickable(enabled = enabled, onClick = onDateClick)
                    .padding(vertical = Spacing.extraSmall),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = windowText,
            modifier =
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = enabled, onClick = onRangeClick)
                    .padding(horizontal = Spacing.extraSmall, vertical = 2.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/** 일정 카드 내용. 3초마다 한 건씩 넘기고 우측에 순번을 적는다. */
@Composable
private fun HomeCalendarSlot(items: List<HomeCalendarItem>) {
    if (items.isEmpty()) {
        HomeEmptySlot(message = "일정이 없어요", minHeight = CALENDAR_SLOT_HEIGHT)
        return
    }
    HomeRotatingContent(items = items, modifier = Modifier.fillMaxWidth()) { slot ->
        val item = slot.value
        Row(
            // 시안 높이는 최소값이다. 큰 글꼴에서는 시간·제목 두 줄이 다 들어가도록 늘어난다.
            modifier = Modifier.fillMaxWidth().heightIn(min = CALENDAR_SLOT_HEIGHT),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.timeText(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = slot.positionLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 알림 카드 내용. 앱 아이콘 + `토스 4` 를 3초마다 넘긴다. */
@Composable
private fun HomeNotificationSlot(apps: List<HomeNotificationApp>) {
    if (apps.isEmpty()) {
        HomeEmptySlot(message = "알림이 없어요", minHeight = HALF_CARD_SLOT_HEIGHT)
        return
    }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = HALF_CARD_SLOT_HEIGHT)
                .padding(vertical = Spacing.extraSmall),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "앱 별 알림 건수",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HomeRotatingContent(items = apps) { slot ->
            val app = slot.value
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val icon = rememberAppIcon(packageName = app.packageName, size = 24.dp)
                if (icon == null) {
                    // 삭제된 앱은 아이콘을 읽을 수 없다. 자리를 비우면 글자가 흔들린다.
                    Box(modifier = Modifier.size(24.dp))
                } else {
                    Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(24.dp))
                }
                Text(
                    text = "${app.appName} ${app.count}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * 모인 것이 없을 때의 내용 슬롯.
 *
 * 라벨을 두지 않는다 — 분류 행이 이미 `일정`·`알림` 이라 말하고 있고, `오늘의 일정` 처럼 날짜를
 * 붙이면 홈에서 다른 날을 고른 순간 틀린 말이 된다.
 */
@Composable
private fun HomeEmptySlot(
    message: String,
    minHeight: Dp,
) {
    Box(
        modifier = Modifier.fillMaxWidth().heightIn(min = minHeight),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 위 라벨 + 아래 값 두 줄. 위치 카드가 쓴다. */
@Composable
private fun HomeLabeledSlot(
    label: String,
    value: String,
) {
    // 라벨과 값을 위아래로 벌린다. 붙여 두면 두 줄이 한 덩어리로 읽혀 무엇이 제목인지 흐려진다.
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = HALF_CARD_SLOT_HEIGHT)
                .padding(vertical = Spacing.extraSmall),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun HomeCalendarItem.timeText(): String =
    if (allDay) {
        "종일"
    } else {
        val start = SLOT_TIME_FORMAT.format(startAt.atZone(ZoneId.systemDefault()))
        val end = endAt?.let { SLOT_TIME_FORMAT.format(it.atZone(ZoneId.systemDefault())) }
        if (end == null) start else "$start ~ $end"
    }

/**
 * 일정 카드 내용의 **최소** 높이. 카드 126 = 패딩 12 + 분류 24 + 8 + **40** + 8 + 본문 22 + 패딩 12.
 *
 * 고정값으로 두지 않는다. 시간·제목 두 줄은 큰 글꼴(1.3배)에서 45 가까이 되어, 고정하면 제목이 잘린다.
 */
private val CALENDAR_SLOT_HEIGHT = 40.dp

/** 위치·알림 반쪽 카드 내용의 **최소** 높이. 카드 전체는 144 다. 큰 글꼴에서는 내용만큼 늘어난다. */
private val HALF_CARD_SLOT_HEIGHT = 58.dp

private val HOME_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREA)
private val SLOT_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.KOREA)

/**
 * 위치 도트가 보는 상태.
 *
 * 공용 판정([DataPermissionState.statusOf])은 활동 인식까지 있어야 `GRANTED` 라 그대로 쓰면
 * **항상 허용을 다 해 준 사용자에게도 노란 도트**가 뜬다. 활동 인식은 이동수단 추론에만 쓰이고
 * 없으면 평균 속도로 보완하므로 수집 자체는 멀쩡하다 — 도트는 `항상 허용` 까지만 본다.
 */
private fun DataPermissionState.locationDotStatus(): DataSourceStatus =
    when (locationStep) {
        LocationPermissionStep.FOREGROUND -> DataSourceStatus.DENIED
        LocationPermissionStep.BACKGROUND -> DataSourceStatus.LIMITED
        LocationPermissionStep.ACTIVITY, LocationPermissionStep.GRANTED -> DataSourceStatus.GRANTED
    }

/**
 * 홈 인사말.
 *
 * 닉네임 유무와 무관하게 같은 타이포(`titleLarge`)를 쓴다. 조회가 늦게 끝나도 글자 크기가 바뀌지
 * 않아 목록 첫 줄이 튀지 않는다. 강조는 굵기가 아니라 색 대비다(Figma 규격).
 */
@Composable
private fun HomeGreeting(
    nickname: String?,
    onClick: (() -> Unit)?,
) {
    val normalColor = MaterialTheme.colorScheme.onSurfaceVariant
    val nicknameColor = MaterialTheme.colorScheme.onSurface
    // 조각을 나눠 여러 Text 로 두면 접근성 서비스가 따로 읽으므로 한 문장으로 합친다.
    val greeting =
        remember(nickname, normalColor, nicknameColor) {
            buildAnnotatedString {
                nicknameGreetingSegments(nickname).forEach { segment ->
                    val color =
                        when (segment.emphasis) {
                            GreetingEmphasis.NORMAL -> normalColor
                            GreetingEmphasis.NICKNAME -> nicknameColor
                        }
                    withStyle(SpanStyle(color = color)) { append(segment.text) }
                }
            }
        }
    Text(
        text = greeting,
        // 겉모습은 그대로 둔다 — 개발 도구로 가는 숨은 입구라 눌러 보라고 권하지 않는다. 대신
        // 낭독에는 무엇을 여는지 알린다.
        modifier =
            if (onClick != null) {
                Modifier
                    .clip(RoundedCornerShape(Spacing.small))
                    .clickable(onClickLabel = "개발 메뉴 열기", onClick = onClick)
            } else {
                Modifier
            },
        style = MaterialTheme.typography.titleLarge,
    )
}
