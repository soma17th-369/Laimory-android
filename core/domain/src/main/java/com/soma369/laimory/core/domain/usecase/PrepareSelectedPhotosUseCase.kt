package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.source.PhotoSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 선택한 MediaStore 사진을 Room 저장 없이 draft 입력용 [SourceItem]으로 변환한다.
 *
 * 조회 중 삭제되거나 접근 권한이 사라진 사진은 [PreparedSelectedPhotos.unavailableIds]로 돌려줘
 * 호출 화면이 사용자에게 선택 변경을 알릴 수 있게 한다.
 */
@Singleton
class PrepareSelectedPhotosUseCase
    @Inject
    constructor(
        private val photoSource: PhotoSource,
    ) {
        suspend operator fun invoke(ids: List<Long>): PreparedSelectedPhotos {
            if (ids.isEmpty()) return PreparedSelectedPhotos()

            val items = photoSource.collect(ids)
            val collectedIds = items.mapNotNullTo(mutableSetOf()) { it.sourceKey.toLongOrNull() }
            return PreparedSelectedPhotos(
                items = items,
                unavailableIds = ids.toSet() - collectedIds,
            )
        }
    }

data class PreparedSelectedPhotos(
    val items: List<SourceItem> = emptyList(),
    val unavailableIds: Set<Long> = emptySet(),
)
