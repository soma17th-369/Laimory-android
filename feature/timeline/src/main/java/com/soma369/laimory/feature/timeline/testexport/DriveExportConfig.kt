package com.soma369.laimory.feature.timeline.testexport

import android.content.Context

/**
 * 임시 테스트 전용(삭제 예정) — Drive 내보내기 설정과 자격증명 로더.
 *
 * 자격증명(`drive_oauth.json`)은 debug source set assets 에만 두고 .gitignore 로 커밋 제외한다.
 * release 빌드엔 파일이 없어 [loadBundledOAuthJson] 이 null → exporter 가 자연 비활성된다.
 */
internal object DriveExportConfig {
    private const val OAUTH_ASSET = "drive_oauth.json"

    /** 날짜 폴더의 상위 폴더 이름. 폴더 ID 미지정 시 내 드라이브 루트 아래 이 이름으로 find-or-create. */
    const val PARENT_FOLDER_NAME = "laimory-data"

    /** 특정 폴더 ID 로 고정하려면 채운다. 비면 [PARENT_FOLDER_NAME] 사용. */
    const val ROOT_FOLDER_ID = ""

    /** 사진 다운스케일 적용 여부. 원본 그대로 올리려면 false 로 두면 된다(다운스케일은 별도 메서드). */
    const val DOWNSCALE_PHOTOS = true

    /** 다운스케일 목표 최대 변(px)과 JPEG 품질. */
    const val DOWNSCALE_MAX_DIMENSION = 1600
    const val DOWNSCALE_JPEG_QUALITY = 85

    /**
     * `assets/drive_oauth.json`(client_id/client_secret/refresh_token) 내용을 반환. 없으면 null.
     * 파일은 debug assets 에만 두므로 자격증명은 각자 로컬에서 설정한다.
     */
    fun loadBundledOAuthJson(context: Context): String? =
        runCatching {
            context.assets.open(OAUTH_ASSET).bufferedReader().use { it.readText() }
        }.getOrNull()
}
