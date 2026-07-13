package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.repository.SourceItemRepository
import com.soma369.laimory.core.domain.source.PhotoSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 선택한 사진들을 수집해 로컬 저장소에 저장한다.
 *
 * [ids]("그날 전부" 또는 그리드에서 고른 부분집합)를 [PhotoSource.collect] 로 [SourceItem] 화한 뒤
 * [SourceItemRepository.addAll] 로 저장하고, 실제로 새로 저장된 건수를 반환한다(중복은 저장 계층 멱등이 무시).
 * 빈 선택이면 조회 없이 0.
 */
@Singleton
class CollectSelectedPhotosUseCase
    @Inject
    constructor(
        private val photoSource: PhotoSource,
        private val repository: SourceItemRepository,
    ) {
        suspend operator fun invoke(ids: List<Long>): Int {
            if (ids.isEmpty()) return 0
            return repository.addAll(photoSource.collect(ids))
        }
    }
