package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.model.collection.PhotoCandidate
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import com.soma369.laimory.core.domain.source.PhotoSource
import javax.inject.Inject
import javax.inject.Singleton

/** 사용자 지정 기록 창에 포함되는 MediaStore 사진 후보를 조회한다. */
@Singleton
class GetPhotosInWindowUseCase
    @Inject
    constructor(
        private val photoSource: PhotoSource,
    ) {
        suspend operator fun invoke(window: RecordDateWindow): List<PhotoCandidate> = photoSource.photosIn(window)
    }
