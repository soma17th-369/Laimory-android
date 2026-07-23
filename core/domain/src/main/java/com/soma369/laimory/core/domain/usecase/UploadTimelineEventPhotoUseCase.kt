package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.base.BaseUseCase
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.repository.TimelineDraftRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Event에 append할 사진 한 장을 presigned URL로 업로드하고 서버 filename을 반환한다.
 *
 * 한 장 단위로 처리해 여러 장 중 일부 업로드가 성공한 경우 filename을 화면 수명주기 동안 보존하고,
 * 실패한 사진부터 재시도할 수 있게 한다.
 */
@Singleton
class UploadTimelineEventPhotoUseCase
    @Inject
    constructor(
        private val repository: TimelineDraftRepository,
        messageHelper: MessageHelper,
    ) : BaseUseCase(messageHelper) {
        suspend operator fun invoke(clientPhotoUri: String): Result<String> =
            execute {
                repository
                    .uploadPhotos(listOf(clientPhotoUri))
                    .singleOrNull()
                    ?: throw ApiException.UnknownException("업로드된 사진 파일명을 확인할 수 없습니다")
            }
    }
