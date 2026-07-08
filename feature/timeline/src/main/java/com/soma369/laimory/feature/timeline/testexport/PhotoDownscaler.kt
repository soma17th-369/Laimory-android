package com.soma369.laimory.feature.timeline.testexport

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 임시 테스트 전용(삭제 예정) — 사진 Uri 를 업로드용 바이트로 읽는다.
 *
 * 다운스케일은 [downscaleJpeg] 로 분리해, 적용 여부를 호출부에서 토글할 수 있게 한다
 * (다운스케일 없이 [readOriginal] 로 원본 업로드도 가능).
 */
internal object PhotoDownscaler {
    /** [uri] 를 그대로 읽어 원본 바이트 반환. 실패 시 null. */
    fun readOriginal(
        context: Context,
        uri: Uri,
    ): ByteArray? =
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()

    /**
     * [uri] 이미지를 최대 변 [maxDimension] px 이내로 줄여 JPEG([quality]) 바이트로 반환. 실패 시 null.
     * 다운스케일을 끄려면 이 메서드 대신 [readOriginal] 을 쓴다.
     */
    fun downscaleJpeg(
        context: Context,
        uri: Uri,
        maxDimension: Int,
        quality: Int,
    ): ByteArray? =
        runCatching {
            val resolver = context.contentResolver

            // 1) 경계만 디코드해 원본 크기 파악 → inSampleSize 로 메모리 절감.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val srcW = bounds.outWidth
            val srcH = bounds.outHeight
            if (srcW <= 0 || srcH <= 0) return null

            val sampled = BitmapFactory.Options().apply { inSampleSize = sampleSize(srcW, srcH, maxDimension) }
            val decoded =
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, sampled) } ?: return null

            // 2) 남은 초과분을 정확히 맞춰 스케일.
            val scaled = scaleWithin(decoded, maxDimension)
            if (scaled !== decoded) decoded.recycle()

            ByteArrayOutputStream().use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                scaled.recycle()
                out.toByteArray()
            }
        }.getOrNull()

    /** 2의 거듭제곱 inSampleSize: 긴 변이 [maxDimension] 근처가 되도록 줄인다. */
    private fun sampleSize(
        width: Int,
        height: Int,
        maxDimension: Int,
    ): Int {
        var sample = 1
        var longest = max(width, height)
        while (longest / 2 >= maxDimension) {
            longest /= 2
            sample *= 2
        }
        return sample
    }

    private fun scaleWithin(
        bitmap: Bitmap,
        maxDimension: Int,
    ): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxDimension) return bitmap
        val ratio = maxDimension.toFloat() / longest
        val w = (bitmap.width * ratio).roundToInt().coerceAtLeast(1)
        val h = (bitmap.height * ratio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }
}
