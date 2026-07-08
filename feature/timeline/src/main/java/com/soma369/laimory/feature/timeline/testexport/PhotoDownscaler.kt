package com.soma369.laimory.feature.timeline.testexport

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
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
    /** [uri] 를 그대로 읽어 원본 바이트 반환(EXIF 포함). 실패 시 null. */
    fun readOriginal(
        context: Context,
        uri: Uri,
    ): ByteArray? =
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()

    /**
     * [uri] 이미지를 최대 변 [maxDimension] px 이내로 줄여 JPEG([quality]) 바이트로 반환. 실패 시 null.
     *
     * 재인코딩하면 EXIF orientation 이 사라지므로, 원본 orientation 을 읽어 픽셀을 실제로 회전시켜
     * "굽는다"(세로 사진이 가로로 눕는 문제 방지). 다운스케일을 끄려면 [readOriginal] 을 쓴다.
     */
    fun downscaleJpeg(
        context: Context,
        uri: Uri,
        maxDimension: Int,
        quality: Int,
    ): ByteArray? =
        runCatching {
            val resolver = context.contentResolver
            val orientation = readOrientation(resolver, uri)

            // 1) 경계만 디코드해 원본 크기 파악 → inSampleSize 로 메모리 절감.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val srcW = bounds.outWidth
            val srcH = bounds.outHeight
            if (srcW <= 0 || srcH <= 0) return null

            val sampled = BitmapFactory.Options().apply { inSampleSize = sampleSize(srcW, srcH, maxDimension) }
            val decoded =
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, sampled) } ?: return null

            // 2) 남은 초과분을 정확히 맞춰 스케일한 뒤, EXIF orientation 을 픽셀에 반영.
            val scaled = scaleWithin(decoded, maxDimension)
            if (scaled !== decoded) decoded.recycle()
            val oriented = applyOrientation(scaled, orientation)
            if (oriented !== scaled) scaled.recycle()

            ByteArrayOutputStream().use { out ->
                oriented.compress(Bitmap.CompressFormat.JPEG, quality, out)
                oriented.recycle()
                out.toByteArray()
            }
        }.getOrNull()

    /** 원본 EXIF orientation 태그. 못 읽으면 [ExifInterface.ORIENTATION_NORMAL]. */
    private fun readOrientation(
        resolver: ContentResolver,
        uri: Uri,
    ): Int =
        runCatching {
            resolver.openInputStream(uri)?.use { input ->
                ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    /** orientation 대로 회전·반전을 픽셀에 굽는다. NORMAL 이면 원본 그대로 반환. */
    private fun applyOrientation(
        bitmap: Bitmap,
        orientation: Int,
    ): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

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
