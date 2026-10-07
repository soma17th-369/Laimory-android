package com.soma369.laimory.feature.home.draft

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.DraftPhotoAccessException
import com.soma369.laimory.core.domain.exception.HandledException
import com.soma369.laimory.core.domain.model.timeline.DraftPhotoLimitExceededException
import java.net.SocketTimeoutException

/**
 * 초안 생성 요청(작업 번호를 받기 전)이 실패한 종류. 로딩 화면의 안내 문구 · 버튼과 홈으로 돌아간 뒤의 처리를 가른다.
 *
 * 공통 안내를 띄운 실패([HandledException])는 원본을 열어 본다 — 감싼 채로 보면 서버가 준 오류 코드를 놓친다.
 */
enum class DraftSubmitFailureKind {
    /** 서버가 초안 생성 단계 동의를 다시 요구한다(-3001). */
    TERMS_REQUIRED,

    /** 이미 그 날짜 기록에 들어간 항목만 보냈다(-1013). */
    NO_NEW_ITEMS,

    /** 스냅샷 확정 뒤 사진이 지워졌거나 권한이 바뀌었다. */
    PHOTO_ACCESS,

    /** 사진 상한을 넘었다. */
    PHOTO_LIMIT,

    /** 30초 동안 진행되지 않아 끊었다. 서버는 받았을 수 있다. */
    TIMEOUT,

    /** 그 밖(네트워크 · 서버 오류 등). */
    OTHER,
    ;

    companion object {
        const val TERMS_AGREEMENT_REQUIRED = -3001
        const val APPEND_NO_NEW_ITEMS = -1013

        fun of(error: Throwable): DraftSubmitFailureKind {
            val cause = (error as? HandledException)?.cause ?: error
            return when {
                cause is ApiException && cause.errorCode == TERMS_AGREEMENT_REQUIRED -> TERMS_REQUIRED
                cause is ApiException && cause.errorCode == APPEND_NO_NEW_ITEMS -> NO_NEW_ITEMS
                cause is DraftPhotoAccessException -> PHOTO_ACCESS
                cause is DraftPhotoLimitExceededException -> PHOTO_LIMIT
                cause is SocketTimeoutException -> TIMEOUT
                else -> OTHER
            }
        }
    }
}
