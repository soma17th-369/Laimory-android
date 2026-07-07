package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.model.collection.PhotoCandidate
import com.soma369.laimory.core.domain.source.PhotoSource
import java.time.LocalDate

/**
 * 해당 날짜에 촬영된 사진 후보를 조회한다.
 *
 * 결과는 "그날 전부 수집"의 대상 목록이자, "선택하여 수집" 바텀시트 그리드의 표시 목록이 된다.
 * 권한 미허용/조회 실패 시 빈 목록.
 */
class GetPhotosOnDateUseCase(
    private val photoSource: PhotoSource,
) {
    suspend operator fun invoke(date: LocalDate): List<PhotoCandidate> = photoSource.photosOn(date)
}
