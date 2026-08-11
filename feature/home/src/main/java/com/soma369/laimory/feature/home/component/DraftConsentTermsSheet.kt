package com.soma369.laimory.feature.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.home.state.DraftConsentTerm

/** 동의 항목 1건의 상세 내용을 열람하는 시트. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DraftConsentTermsSheet(
    term: DraftConsentTerm,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.extraLarge, vertical = Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            Text(
                text = stringResource(term.titleRes()),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "법무 검토 전 임시 문구예요. 검토 결과에 따라 내용이 변경될 수 있어요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(
                modifier =
                    Modifier
                        .heightIn(max = TERMS_BODY_MAX_HEIGHT)
                        .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = stringResource(term.bodyRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(modifier = Modifier.height(Spacing.large))
        }
    }
}

private val TERMS_BODY_MAX_HEIGHT = 480.dp
