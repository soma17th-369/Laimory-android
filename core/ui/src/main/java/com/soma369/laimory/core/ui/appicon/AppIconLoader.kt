package com.soma369.laimory.core.ui.appicon

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 설치된 앱의 애플리케이션 아이콘을 packageName 으로 조회하는 프로세스 수명 메모리 캐시.
 *
 * 아이콘 바이너리는 메모리에만 두고 Room 이나 서버 payload 에 저장하지 않는다.
 * 앱 삭제나 패키지 visibility(Android 11+) 제약으로 조회에 실패하면 실패 사실을 캐시해
 * 같은 패키지를 반복 조회하지 않는다 — 호출부는 null 을 받아 자체 fallback 을 그린다.
 */
object AppIconLoader {
    private val cache = LruCache<String, CachedAppIcon>(MAX_CACHE_ENTRIES)

    /** 이미 조회를 마친 아이콘. 미조회와 조회 실패 모두 null 이라 첫 컴포지션의 초기값으로만 쓴다. */
    fun cached(
        packageName: String,
        sizePx: Int,
    ): ImageBitmap? = cache.get(cacheKey(packageName, sizePx))?.bitmap

    /** [packageName] 의 아이콘을 [sizePx] 정사각형으로 조회한다. 실패하면 null. */
    suspend fun load(
        context: Context,
        packageName: String,
        sizePx: Int,
    ): ImageBitmap? {
        val key = cacheKey(packageName, sizePx)
        cache.get(key)?.let { return it.bitmap }
        val bitmap =
            withContext(Dispatchers.IO) {
                runCatching { context.packageManager.getApplicationIcon(packageName).toIconBitmap(sizePx) }.getOrNull()
            }
        cache.put(key, CachedAppIcon(bitmap))
        return bitmap
    }

    private fun cacheKey(
        packageName: String,
        sizePx: Int,
    ): String = "$packageName@$sizePx"

    /** 24dp 헤더 아이콘 기준 한 장이 약 20KB 라, 화면 전환을 오가도 여유 있는 크기로 둔다. */
    private const val MAX_CACHE_ENTRIES = 64
}

/**
 * [packageName] 앱 아이콘을 비동기로 조회한다.
 *
 * 이미 캐시된 패키지는 첫 컴포지션에서 바로 값을 돌려줘 목록 스크롤 중 아이콘이 깜빡이지 않는다.
 * 조회에 실패하면 계속 null 이므로 호출부가 fallback 표시를 책임진다.
 */
@Composable
fun rememberAppIcon(
    packageName: String,
    size: Dp,
): ImageBitmap? {
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    return produceState(
        initialValue = AppIconLoader.cached(packageName, sizePx),
        packageName,
        sizePx,
    ) {
        if (value == null) value = AppIconLoader.load(context, packageName, sizePx)
    }.value
}

/** 조회를 마친 결과. 실패(null)도 값으로 담아 재조회를 막는다. */
private class CachedAppIcon(
    val bitmap: ImageBitmap?,
)

/**
 * 아이콘 [Drawable] 을 [sizePx] 정사각 비트맵으로 그린다.
 *
 * 적응형 아이콘은 규격상 108dp 전체 중 런처 마스크에 보이는 영역이 가운데 72dp 뿐이라,
 * 전체를 그대로 그리면 로고가 작고 여백만 크게 보인다. 확대해 그린 뒤 가운데를 잘라낸다.
 */
private fun Drawable.toIconBitmap(sizePx: Int): ImageBitmap {
    if (this !is AdaptiveIconDrawable) return toBitmap(sizePx, sizePx).asImageBitmap()
    val scaledSize = (sizePx * ADAPTIVE_ICON_SCALE).toInt()
    val inset = (scaledSize - sizePx) / 2
    return Bitmap
        .createBitmap(toBitmap(scaledSize, scaledSize), inset, inset, sizePx, sizePx)
        .asImageBitmap()
}

/** 적응형 아이콘의 전체 규격. */
private const val ADAPTIVE_ICON_FULL_DP = 108f

/**
 * 108dp 중 실제로 잘라 쓰는 영역.
 *
 * 런처 마스크 규격은 가운데 72dp 지만 그 폭으로 자르면 로고가 헤더를 꽉 채워
 * 옆 텍스트보다 무겁게 보인다. 조금 넓게 잘라 배경 여백을 남긴다.
 */
private const val ADAPTIVE_ICON_VISIBLE_DP = 88f

private const val ADAPTIVE_ICON_SCALE = ADAPTIVE_ICON_FULL_DP / ADAPTIVE_ICON_VISIBLE_DP
