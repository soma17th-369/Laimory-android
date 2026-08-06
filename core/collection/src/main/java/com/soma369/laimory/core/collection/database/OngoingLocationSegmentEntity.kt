package com.soma369.laimory.core.collection.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 위치 분절기의 단일 진행 상태. 확정된 수집 항목과 분리해 프로세스 재시작 복원에만 사용한다. */
@Entity(tableName = "ongoing_location_segment")
internal data class OngoingLocationSegmentEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val snapshotVersion: Int,
    val snapshotJson: String,
    val updatedAtUtc: Long,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
