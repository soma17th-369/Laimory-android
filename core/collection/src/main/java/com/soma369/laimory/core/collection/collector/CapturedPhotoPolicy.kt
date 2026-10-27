package com.soma369.laimory.core.collection.collector

/**
 * MediaStore 사진 한 장이 기기 카메라로 직접 찍은 것인지 판별하는 규칙. 튜토리얼이 사진을 많이 찍은 날을 고를 때 쓴다.
 *
 * 갤러리에서 앨범으로 옮겨도 판별이 바뀌지 않게, 폴더보다 **파일 안의 촬영 정보**를 기준으로 삼는다. 폴더만 보면 카메라
 * 사진을 `Pictures/<앨범>` 으로 옮길 때 빠지고, 캡처를 `DCIM/<앨범>` 으로 옮길 때 들어온다.
 *
 * 1. 촬영 정보(`ISO` 또는 `EXPOSURE_TIME`)가 있다. MediaProvider 가 스캔할 때 EXIF 에서 채워 둔 컬럼이라 파일을 열지
 *    않는다. 캡처에는 없다(S22 실측: 카메라 38장 모두 있음, 캡처 22장 모두 없음).
 * 2. 캡처 파일명(`Screenshot…`)이 아니다.
 * 3. 캡처 폴더(`Screenshots/`)가 아니다.
 *
 * 촬영 정보 컬럼은 API 30 부터라, 그 전에는 1번 대신 `DCIM/` 아래인지로 본다. 원본 촬영 정보가 남은 저장본(카카오톡 원본
 * 전송 등)은 찍은 사진으로 센다 — 순위에만 쓰는 값이라 감수한다.
 */
internal object CapturedPhotoPolicy {
    /**
     * @param path API 29+ 는 `RELATIVE_PATH`(`DCIM/Camera/`), API 28 은 `DATA`(파일 전체 경로).
     * @param hasCaptureInfo 촬영 정보가 있는지. 컬럼이 없는 API 30 미만은 null.
     */
    fun isCaptured(
        displayName: String?,
        path: String?,
        hasCaptureInfo: Boolean?,
    ): Boolean {
        if (displayName?.startsWith(SCREENSHOT_NAME_PREFIX, ignoreCase = true) == true) return false
        val normalizedPath = "/${path.orEmpty()}"
        if (normalizedPath.contains(SCREENSHOT_FOLDER, ignoreCase = true)) return false
        return hasCaptureInfo ?: normalizedPath.contains(CAMERA_ROOT, ignoreCase = true)
    }

    private const val SCREENSHOT_NAME_PREFIX = "Screenshot"
    private const val SCREENSHOT_FOLDER = "/Screenshots/"
    private const val CAMERA_ROOT = "/DCIM/"
}
