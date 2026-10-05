package com.soma369.laimory.core.domain.repository

import com.soma369.laimory.core.domain.model.settings.DefaultRecordRange
import kotlinx.coroutines.flow.Flow

/** 기기에 저장하는 기본 기록 범위. 서버와 동기화하지 않고, 로그아웃해도 남는다. */
interface DefaultRecordRangeRepository {
    /** 저장한 값. 저장한 적이 없거나 읽지 못하면 [DefaultRecordRange.INITIAL]. */
    val range: Flow<DefaultRecordRange>

    suspend fun setRange(range: DefaultRecordRange)
}
