package com.soma369.laimory.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.soma369.laimory.core.ui.R

// Foundation 기준 폰트 = Pretendard. Figma 타입 스타일 13개를 그대로 매핑한다
// (size·lineHeight 동일, 굵기 Regular/Medium/Bold 1:1). 자간은 Figma % 값을 .em 으로 환산(% / 100).
private val Pretendard =
    FontFamily(
        Font(R.font.pretendard_regular, FontWeight.Normal),
        Font(R.font.pretendard_medium, FontWeight.Medium),
        Font(R.font.pretendard_bold, FontWeight.Bold),
    )

// 시그니처 폰트 = 고운 바탕(serif). 기본 타이포가 아니라 [LaimorySignature] 로만 명시 사용한다.
// 굵기는 Regular(400)/Bold(700) 두 종만 제공.
private val GowunBatang =
    FontFamily(
        Font(R.font.gowun_batang_regular, FontWeight.Normal),
        Font(R.font.gowun_batang_bold, FontWeight.Bold),
    )

val LaimoryTypography =
    Typography(
        displayLarge =
            TextStyle(
                fontFamily = Pretendard,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
                lineHeight = 44.sp,
                letterSpacing = (-0.005).em,
            ),
        displaySmall =
            TextStyle(
                fontFamily = Pretendard,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                letterSpacing = (-0.004).em,
            ),
        headlineLarge =
            TextStyle(
                fontFamily = Pretendard,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                lineHeight = 34.sp,
                letterSpacing = (-0.003).em,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = Pretendard,
                fontWeight = FontWeight.Medium,
                fontSize = 22.sp,
                lineHeight = 30.sp,
                letterSpacing = (-0.002).em,
            ),
        titleLarge =
            TextStyle(
                fontFamily = Pretendard,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
                lineHeight = 28.sp,
                letterSpacing = (-0.001).em,
            ),
        titleMedium =
            TextStyle(
                fontFamily = Pretendard,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
        titleSmall =
            TextStyle(
                fontFamily = Pretendard,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = Pretendard,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = Pretendard,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 22.sp,
            ),
        bodySmall =
            TextStyle(
                fontFamily = Pretendard,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            ),
        labelLarge =
            TextStyle(
                fontFamily = Pretendard,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = (0.001).em,
            ),
        labelMedium =
            TextStyle(
                fontFamily = Pretendard,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = (0.002).em,
            ),
        labelSmall =
            TextStyle(
                fontFamily = Pretendard,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                letterSpacing = (0.003).em,
            ),
    )

// 시그니처 타이포 — 고운 바탕. M3 기본 역할을 덮지 않고, 강조가 필요한 곳(모먼트 제목·AI 요약 등)에서
// [MaterialTheme.laimorySignature] 로 명시적으로 골라 쓴다.
object LaimorySignature {
    val large =
        TextStyle(
            fontFamily = GowunBatang,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 40.sp,
            letterSpacing = (-0.004).em,
        )
    val medium =
        TextStyle(
            fontFamily = GowunBatang,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 32.sp,
            letterSpacing = (-0.002).em,
        )
    val small =
        TextStyle(
            fontFamily = GowunBatang,
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp,
            lineHeight = 28.sp,
        )
    val note =
        TextStyle(
            fontFamily = GowunBatang,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 20.sp,
        )
}

// 시그니처 타이포 단축 접근자. 색과 달리 다크/라이트로 바뀌지 않으므로 CompositionLocal 없이 정적으로 노출.
val MaterialTheme.laimorySignature: LaimorySignature
    get() = LaimorySignature
