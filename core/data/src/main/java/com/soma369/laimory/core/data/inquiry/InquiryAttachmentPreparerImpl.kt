package com.soma369.laimory.core.data.inquiry

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import androidx.core.net.toUri
import com.soma369.laimory.core.domain.exception.InquiryAttachmentException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 모든 첨부를 **JPEG 로 다시 저장**한다. 원본을 그대로 올리지 않는 이유는 셋이다.
 * - 서버는 JPG · PNG · WebP 와 장당 5MB 만 받는다. 갤러리 사진은 HEIC 이거나 5MB 를 넘기 쉽다.
 * - 카메라 사진의 EXIF 에는 촬영 위치가 들어 있다. 문의에 필요 없는 위치가 관리자에게 가지 않게 뺀다.
 * - 관리자가 보기에는 긴 변 [MAX_LONG_EDGE] 이면 충분하다.
 *
 * 회전은 [ImageDecoder] 가 EXIF 방향대로 이미 적용해 준다.
 */
internal class InquiryAttachmentPreparerImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : InquiryAttachmentPreparer {
        override suspend fun prepare(sourceUri: String): PreparedInquiryAttachment =
            withContext(Dispatchers.IO) {
                val bitmap =
                    try {
                        decode(sourceUri)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        throw InquiryAttachmentException("첨부 사진을 읽을 수 없습니다")
                    }
                val file = File.createTempFile(FILE_PREFIX, ".jpg", cacheDir())
                val opaque = bitmap.withoutAlpha()
                try {
                    var quality = INITIAL_QUALITY
                    while (true) {
                        file.outputStream().use { opaque.compress(Bitmap.CompressFormat.JPEG, quality, it) }
                        if (file.length() <= MAX_BYTES || quality <= MIN_QUALITY) break
                        quality -= QUALITY_STEP
                    }
                    if (file.length() > MAX_BYTES) throw InquiryAttachmentException("첨부 사진을 줄이지 못했습니다")
                    PreparedInquiryAttachment(file = file, contentType = CONTENT_TYPE, size = file.length())
                } catch (e: Throwable) {
                    file.delete()
                    throw e
                } finally {
                    if (opaque !== bitmap) opaque.recycle()
                    bitmap.recycle()
                }
            }

        private fun decode(sourceUri: String): Bitmap {
            val source = ImageDecoder.createSource(context.contentResolver, sourceUri.toUri())
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                // 압축하려면 픽셀을 읽어야 한다. 하드웨어 비트맵은 읽을 수 없다.
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val longEdge = max(info.size.width, info.size.height)
                if (longEdge > MAX_LONG_EDGE) {
                    val scale = MAX_LONG_EDGE.toFloat() / longEdge
                    decoder.setTargetSize(
                        (info.size.width * scale).roundToInt().coerceAtLeast(1),
                        (info.size.height * scale).roundToInt().coerceAtLeast(1),
                    )
                }
            }
        }

        /** JPEG 는 투명도가 없다. 그대로 두면 투명한 자리가 검게 나오므로 흰 바탕에 얹는다. */
        private fun Bitmap.withoutAlpha(): Bitmap {
            if (!hasAlpha()) return this
            val opaque = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            Canvas(opaque).apply {
                drawColor(Color.WHITE)
                drawBitmap(this@withoutAlpha, 0f, 0f, null)
            }
            return opaque
        }

        private fun cacheDir(): File = File(context.cacheDir, CACHE_DIR).apply { mkdirs() }

        private companion object {
            const val CONTENT_TYPE = "image/jpeg"
            const val MAX_LONG_EDGE = 2048
            const val MAX_BYTES = 5L * 1024 * 1024
            const val INITIAL_QUALITY = 85
            const val MIN_QUALITY = 55
            const val QUALITY_STEP = 10
            const val CACHE_DIR = "inquiry_attachments"
            const val FILE_PREFIX = "inquiry_"
        }
    }
