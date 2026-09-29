package com.soma369.laimory.feature.settings.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.soma369.laimory.core.domain.model.inquiry.InquiryStatus

/** 문의 처리 상태 표시. 답변 완료만 강조색이다 — 사용자가 할 일(메일 확인)이 생긴 상태라서. */
@Composable
fun InquiryStatusChip(
    status: InquiryStatus,
    modifier: Modifier = Modifier,
) {
    val answered = status == InquiryStatus.ANSWERED
    Text(
        text = status.label,
        modifier =
            modifier
                .background(
                    color = if (answered) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape,
                ).padding(horizontal = 8.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelMedium,
        color = if (answered) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

val InquiryStatus.label: String
    get() =
        when (this) {
            InquiryStatus.RECEIVED -> "확인 중"
            InquiryStatus.ANSWERED -> "답변 완료"
        }
