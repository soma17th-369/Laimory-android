package com.soma369.laimory.feature.timeline.testexport

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 임시 테스트 전용(삭제 예정) — 구글 드라이브 v3 REST 업로드(외부 의존성 없이 HttpURLConnection).
 *
 * 인증은 호출자가 넘긴 OAuth 액세스 토큰(Bearer). 흐름: 상위 폴더 아래 날짜 폴더를 find-or-create →
 * 그 아래 제출 폴더를 만들고 JSON + `photos/` 를 올린다. 필요한 스코프: `drive.file`.
 */
internal object DriveUploader {
    private const val FILES = "https://www.googleapis.com/drive/v3/files"

    // supportsAllDrives: 개인 드라이브엔 무해하고 공유 드라이브까지 지원.
    private const val UPLOAD =
        "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&supportsAllDrives=true&fields=id,name,webViewLink"
    private const val FOLDER_MIME = "application/vnd.google-apps.folder"
    private const val BOUNDARY = "----LaimoryDriveBoundary7f3aK9zQ"

    /** 업로드할 이미지 한 장. */
    data class PhotoUpload(
        val fileName: String,
        val bytes: ByteArray,
    )

    /**
     * 하루치 묶음 업로드: `<parent>/<dateFolderName>/<submissionFolderName>/` 아래에
     * JSON([jsonFileName]) 과 (사진이 있으면) `photos/` 폴더에 이미지들을 올린다.
     * 상위 폴더 결정은 [rootFolderId](지정 시) 또는 [parentFolderName] find-or-create.
     */
    suspend fun uploadDayBundle(
        token: String,
        rootFolderId: String,
        parentFolderName: String,
        dateFolderName: String,
        submissionFolderName: String,
        jsonFileName: String,
        json: String,
        photos: List<PhotoUpload>,
    ): Result<String> =
        withContext(Dispatchers.IO) {
            if (token.isBlank()) return@withContext Result.failure(IllegalStateException("액세스 토큰이 비어 있습니다"))
            runCatching {
                val base = resolveBase(token, rootFolderId, parentFolderName)
                val dateFolder = findOrCreateFolder(token, base, dateFolderName)
                val submission = findOrCreateFolder(token, dateFolder, submissionFolderName)
                uploadMultipart(token, submission, jsonFileName, json.toByteArray(Charsets.UTF_8), "application/json; charset=UTF-8")
                var photoOk = 0
                if (photos.isNotEmpty()) {
                    val photosFolder = findOrCreateFolder(token, submission, "photos")
                    photos.forEach { p ->
                        runCatching {
                            uploadMultipart(token, photosFolder, p.fileName, p.bytes, "image/jpeg")
                            photoOk++
                        }
                    }
                }
                "json + 사진 $photoOk/${photos.size}장"
            }
        }

    // --- 폴더 ---

    private fun resolveBase(
        token: String,
        rootFolderId: String,
        parentFolderName: String,
    ): String =
        when {
            rootFolderId.isNotBlank() -> rootFolderId
            parentFolderName.isNotBlank() -> findOrCreateFolder(token, "root", parentFolderName)
            else -> "root"
        }

    private fun findOrCreateFolder(
        token: String,
        parent: String,
        name: String,
    ): String = findFolder(token, parent, name) ?: createFolder(token, parent, name)

    private fun findFolder(
        token: String,
        parent: String,
        name: String,
    ): String? {
        val q = "name = '${escapeQ(name)}' and mimeType = '$FOLDER_MIME' and '$parent' in parents and trashed = false"
        val url =
            "$FILES?q=${URLEncoder.encode(q, "UTF-8")}&fields=files(id,name)" +
                "&supportsAllDrives=true&includeItemsFromAllDrives=true"
        val (code, text) = request(url, "GET", token)
        if (code !in 200..299) error("폴더 조회 실패 ($code): ${text.take(300)}")
        val files = JSONObject(text).optJSONArray("files") ?: return null
        return if (files.length() > 0) files.getJSONObject(0).getString("id") else null
    }

    private fun createFolder(
        token: String,
        parent: String,
        name: String,
    ): String {
        val meta =
            JSONObject().apply {
                put("name", name)
                put("mimeType", FOLDER_MIME)
                put("parents", JSONArray().put(parent))
            }
        val (code, text) =
            request(
                "$FILES?fields=id&supportsAllDrives=true",
                "POST",
                token,
                "application/json; charset=UTF-8",
                meta.toString().toByteArray(Charsets.UTF_8),
            )
        if (code !in 200..299) error("폴더 생성 실패 ($code): ${text.take(300)}")
        return JSONObject(text).getString("id")
    }

    // --- 파일 ---

    /** metadata + 본문(텍스트/바이너리 공용)을 multipart/related 로 업로드. */
    private fun uploadMultipart(
        token: String,
        folderId: String,
        fileName: String,
        content: ByteArray,
        contentType: String,
    ): String {
        val meta =
            JSONObject().apply {
                put("name", fileName)
                put("parents", JSONArray().put(folderId))
            }
        val pre =
            (
                "--$BOUNDARY\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n" +
                    meta.toString() + "\r\n" +
                    "--$BOUNDARY\r\nContent-Type: $contentType\r\n\r\n"
            ).toByteArray(Charsets.UTF_8)
        val post = "\r\n--$BOUNDARY--".toByteArray(Charsets.UTF_8)
        val body = pre + content + post

        val (code, text) = request(UPLOAD, "POST", token, "multipart/related; boundary=$BOUNDARY", body)
        if (code !in 200..299) error("파일 업로드 실패 ($code): ${text.take(300)}")
        val res = JSONObject(text)
        val name = res.optString("name", fileName)
        val link = res.optString("webViewLink", "")
        return if (link.isNotBlank()) "$name → $link" else "$name (id ${res.optString("id")})"
    }

    // --- HTTP ---

    private fun request(
        urlStr: String,
        method: String,
        token: String,
        contentType: String? = null,
        body: ByteArray? = null,
    ): Pair<Int, String> {
        val conn =
            (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                setRequestProperty("Authorization", "Bearer $token")
                if (contentType != null) setRequestProperty("Content-Type", contentType)
                connectTimeout = 15_000
                readTimeout = 30_000
            }
        try {
            if (body != null) {
                conn.doOutput = true
                conn.outputStream.use { it.write(body) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            return code to text
        } finally {
            conn.disconnect()
        }
    }

    /** Drive 검색 q 문자열 안 값의 따옴표/백슬래시 이스케이프. */
    private fun escapeQ(v: String): String = v.replace("\\", "\\\\").replace("'", "\\'")
}
