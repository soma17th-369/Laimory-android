---
name: design-tokens
description: Laimory Figma 디자인 토큰을 Jetpack Compose 테마로 매핑하는 참조 문서입니다.
---

# 디자인 토큰 매핑 (Figma → Compose)

Figma 파일: [Laimory](https://www.figma.com/design/Ln4yLrEV88RFknIwRWzycZ/Laimory) · `00 Foundation` 페이지.

## 1. 구조 원칙

Figma와 코드는 **같은 2계층 토큰 구조**를 가진다.

```
Primitives (원시 값, 1세트)            →  Color.kt 의 raw val (private)
   ↓ 참조(alias)
Semantic (역할 토큰, Light/Dark 2모드)  →  lightColorScheme / darkColorScheme + LaimoryColors
```

- **화면 코드는 Semantic만 사용한다.** `Primitive(raw hex)`를 컴포저블에서 직접 쓰지 않는다.
- Figma `Semantic` 네이밍은 **Material 3 ColorScheme 역할명과 1:1**로 맞췄다 → 그대로 `MaterialTheme.colorScheme.*`로 내려간다.
- M3 ColorScheme에 없는 색(success/warning/info, 감정 5색)은 **`LaimoryColors`**(CompositionLocal)로 확장한다.
- 다크모드는 같은 Semantic 토큰의 **다른 모드 값**일 뿐, 별도 토큰이 아니다.

## 2. Primitives (raw 값)

`core:ui/.../theme/Color.kt` — `private val`로 둔다. 화면에서 직접 참조 금지. (Theme/DesignSystem은 `:core:ui`에 둔다 — [Android 아키텍처](../../../android/architecture/SKILL.md))

```kotlin
// Neutral (warm gray)
private val Neutral0 = Color(0xFFFFFFFF); private val Neutral50 = Color(0xFFFAF8F5)
private val Neutral100 = Color(0xFFF3F0EB); private val Neutral200 = Color(0xFFE7E2DA)
private val Neutral300 = Color(0xFFD6CFC4); private val Neutral400 = Color(0xFFB5ADA0)
private val Neutral500 = Color(0xFF928A7C); private val Neutral600 = Color(0xFF6E665A)
private val Neutral700 = Color(0xFF514B41); private val Neutral800 = Color(0xFF34302A)
private val Neutral900 = Color(0xFF1F1C18); private val Neutral950 = Color(0xFF13110E)

// Primary (periwinkle)
private val Primary50 = Color(0xFFEEF1FA); private val Primary100 = Color(0xFFDCE3F4)
private val Primary200 = Color(0xFFC3CFEC); private val Primary300 = Color(0xFFA4B5E0)
private val Primary400 = Color(0xFF879BD3); private val Primary500 = Color(0xFF6F84C4)
private val Primary600 = Color(0xFF5A6EAC); private val Primary700 = Color(0xFF48588A)
private val Primary800 = Color(0xFF37446A); private val Primary900 = Color(0xFF262E46)

// Secondary (sage)
private val Secondary100 = Color(0xFFD5E4DD); private val Secondary300 = Color(0xFF92B8A7)
private val Secondary400 = Color(0xFF73A089); private val Secondary800 = Color(0xFF2C4137)
private val Secondary900 = Color(0xFF1E2C25)

// Functional
private val Success100 = Color(0xFFCFE5D4); private val Success400 = Color(0xFF7DB389)
private val Success600 = Color(0xFF4E8B5E); private val Success800 = Color(0xFF2C4F37)
private val Warning100 = Color(0xFFF4E3B8); private val Warning400 = Color(0xFFE0B65A)
private val Warning600 = Color(0xFFBC8E2E); private val Warning800 = Color(0xFF6E5114)
private val Error100 = Color(0xFFF2D2D2); private val Error400 = Color(0xFFDD8A8A)
private val Error600 = Color(0xFFC25E5E); private val Error800 = Color(0xFF6E2C2C)
private val Info100 = Color(0xFFCFDDF0); private val Info400 = Color(0xFF7C9FD6)
private val Info600 = Color(0xFF5076B4); private val Info800 = Color(0xFF2C4470)

// Emotion (활기/평온/무덤덤/지침/울적) — base / container(50) / deep(800)
private val Joy50 = Color(0xFFFCEDE4);   private val Joy100 = Color(0xFFF8DAC9);   private val Joy400 = Color(0xFFED9F7E);   private val Joy800 = Color(0xFF8A4527)
private val Calm50 = Color(0xFFE7F0F8);  private val Calm100 = Color(0xFFCFE2F1);  private val Calm400 = Color(0xFF7FB0DC);  private val Calm800 = Color(0xFF2C4F70)
private val Mellow50 = Color(0xFFF2EFE7);private val Mellow100 = Color(0xFFE7E0D2);private val Mellow400 = Color(0xFFBFB295);private val Mellow800 = Color(0xFF564E3A)
private val Weary50 = Color(0xFFF0EBF7); private val Weary100 = Color(0xFFE0D6EE); private val Weary400 = Color(0xFFA88FCB); private val Weary800 = Color(0xFF4B3A6B)
private val Down50 = Color(0xFFECEEF3);  private val Down100 = Color(0xFFD9DEE8);  private val Down400 = Color(0xFF8A93AB);  private val Down800 = Color(0xFF383F50)
```

## 3. Semantic → M3 ColorScheme

Figma `Semantic` 컬렉션의 Light/Dark 모드를 그대로 옮긴다. Figma 토큰명 → `colorScheme` 역할명이 1:1이다.

```kotlin
internal val LaimoryLightColorScheme = lightColorScheme(
    primary = Primary400,              onPrimary = Neutral0,
    primaryContainer = Primary100,     onPrimaryContainer = Primary800,
    secondary = Secondary400,          onSecondary = Neutral0,
    secondaryContainer = Secondary100, onSecondaryContainer = Secondary800,
    background = Neutral50,             onBackground = Neutral900,
    surface = Neutral0,                onSurface = Neutral900,
    surfaceVariant = Neutral100,       onSurfaceVariant = Neutral600,
    surfaceContainer = Neutral50,      surfaceContainerHigh = Neutral100,
    outline = Neutral300,              outlineVariant = Neutral200,
    error = Error600,                  onError = Neutral0,
    errorContainer = Error100,         onErrorContainer = Error800,
    scrim = Neutral900,
    inverseSurface = Neutral800,       inverseOnSurface = Neutral50,
)

internal val LaimoryDarkColorScheme = darkColorScheme(
    primary = Primary300,              onPrimary = Primary900,
    primaryContainer = Primary800,     onPrimaryContainer = Primary100,
    secondary = Secondary300,          onSecondary = Secondary900,
    secondaryContainer = Secondary800, onSecondaryContainer = Secondary100,
    background = Neutral950,            onBackground = Neutral100,
    surface = Neutral900,              onSurface = Neutral50,
    surfaceVariant = Neutral800,       onSurfaceVariant = Neutral400,
    surfaceContainer = Neutral800,     surfaceContainerHigh = Neutral700,
    outline = Neutral700,              outlineVariant = Neutral800,
    error = Error400,                  onError = Error800,
    errorContainer = Error800,         onErrorContainer = Error100,
    scrim = Neutral950,
    inverseSurface = Neutral100,       inverseOnSurface = Neutral800,
)
```

> `text-primary` = `onSurface`, `text-secondary` = `onSurfaceVariant`, `border` = `outline`. (Figma의 단순화 명칭 ↔ M3 역할 매핑)

## 4. 확장색 — LaimoryColors

M3 `ColorScheme`에 없는 success/warning/info와 **감정 5색**은 별도 구조 + CompositionLocal로 내려준다. (Figma code syntax가 `LaimoryColors.*`로 잡혀 있는 토큰들)

```kotlin
@Immutable
data class LaimoryColors(
    val success: Color, val onSuccess: Color, val successContainer: Color, val onSuccessContainer: Color,
    val warning: Color, val onWarning: Color, val warningContainer: Color, val onWarningContainer: Color,
    val info: Color, val onInfo: Color, val infoContainer: Color, val onInfoContainer: Color,
    // 감정: base / container / onContainer
    val joy: Color, val joyContainer: Color, val onJoyContainer: Color,
    val calm: Color, val calmContainer: Color, val onCalmContainer: Color,
    val mellow: Color, val mellowContainer: Color, val onMellowContainer: Color,
    val weary: Color, val wearyContainer: Color, val onWearyContainer: Color,
    val down: Color, val downContainer: Color, val onDownContainer: Color,
)

internal val LightExtendedColors = LaimoryColors(
    success = Success600, onSuccess = Neutral0, successContainer = Success100, onSuccessContainer = Success800,
    warning = Warning600, onWarning = Neutral0, warningContainer = Warning100, onWarningContainer = Warning800,
    info = Info600, onInfo = Neutral0, infoContainer = Info100, onInfoContainer = Info800,
    joy = Joy400, joyContainer = Joy50, onJoyContainer = Joy800,
    calm = Calm400, calmContainer = Calm50, onCalmContainer = Calm800,
    mellow = Mellow400, mellowContainer = Mellow50, onMellowContainer = Mellow800,
    weary = Weary400, wearyContainer = Weary50, onWearyContainer = Weary800,
    down = Down400, downContainer = Down50, onDownContainer = Down800,
)

internal val DarkExtendedColors = LaimoryColors(
    success = Success400, onSuccess = Success800, successContainer = Success800, onSuccessContainer = Success100,
    warning = Warning400, onWarning = Warning800, warningContainer = Warning800, onWarningContainer = Warning100,
    info = Info400, onInfo = Info800, infoContainer = Info800, onInfoContainer = Info100,
    joy = Joy400, joyContainer = Joy800, onJoyContainer = Joy100,
    calm = Calm400, calmContainer = Calm800, onCalmContainer = Calm100,
    mellow = Mellow400, mellowContainer = Mellow800, onMellowContainer = Mellow100,
    weary = Weary400, wearyContainer = Weary800, onWearyContainer = Weary100,
    down = Down400, downContainer = Down800, onDownContainer = Down100,
)

val LocalLaimoryColors = staticCompositionLocalOf { LightExtendedColors }
```

감정 enum과 컨테이너 색을 잇는 헬퍼를 권장한다.

```kotlin
enum class Emotion { JOY, CALM, MELLOW, WEARY, DOWN }   // 활기/평온/무덤덤/지침/울적

@Composable
fun Emotion.containerColor(): Color = with(LocalLaimoryColors.current) {
    when (this@containerColor) {
        Emotion.JOY -> joyContainer; Emotion.CALM -> calmContainer
        Emotion.MELLOW -> mellowContainer; Emotion.WEARY -> wearyContainer
        Emotion.DOWN -> downContainer
    }
}
```

## 5. Typography (M3, Noto Sans KR)

Foundation(Figma)의 타입 기준은 **Noto Sans KR**이다. 매핑 스펙은 이 기준을 그대로 따른다.

> 다른 폰트(예: Pretendard)로 전환하려면 Figma Foundation의 폰트·자간까지 함께 바꿔야 하므로 **별도 결정 항목**으로 분리한다. 코드만 폰트를 바꾸면 디자인-코드 스펙이 어긋난다.

| Style | size / lineHeight | weight |
|---|---|---|
| displayLarge | 36 / 44 | Bold(700) |
| displaySmall | 28 / 36 | Bold |
| headlineLarge | 26 / 34 | Bold |
| headlineMedium | 22 / 30 | Medium(500) |
| titleLarge | 20 / 28 | Medium |
| titleMedium | 16 / 24 | Medium |
| titleSmall | 14 / 20 | Medium |
| bodyLarge | 16 / 24 | Regular(400) |
| bodyMedium | 14 / 22 | Regular |
| bodySmall | 13 / 18 | Regular |
| labelLarge | 14 / 20 | Medium |
| labelMedium | 12 / 16 | Medium |
| labelSmall | 11 / 16 | Medium |

자간(letterSpacing)은 Figma가 **% 단위**라 Compose에서는 `.em`으로 환산한다 (`% / 100`). 예: Display `-0.5%` → `(-0.005).em`. Figma 자간 값: Display/Large -0.5%, Display/Small -0.4%, Headline/Large -0.3%, Headline/Medium -0.2%, Title/Large -0.1%, Label/Large 0.1%, Label/Medium 0.2%, Label/Small 0.3%, 나머지 0.

```kotlin
// Foundation 기준 폰트 = Noto Sans KR (Pretendard 전환은 별도 결정 항목)
private val NotoSansKr = FontFamily(/* R.font.noto_sans_kr_regular/medium/bold ... */)

val LaimoryTypography = Typography(
    displayLarge = TextStyle(fontFamily = NotoSansKr, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = (-0.005).em),
    displaySmall = TextStyle(fontFamily = NotoSansKr, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = (-0.004).em),
    headlineMedium = TextStyle(fontFamily = NotoSansKr, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 30.sp, letterSpacing = (-0.002).em),
    titleMedium = TextStyle(fontFamily = NotoSansKr, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = NotoSansKr, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 22.sp),
    labelMedium = TextStyle(fontFamily = NotoSansKr, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = (0.002).em),
    // ... 나머지 13단계 동일 패턴 (자간은 위 % 값을 .em으로 환산)
)
```

## 6. Shape & Spacing

```kotlin
// radius: none0 / xs4 / sm8 / md12 / lg16 / xl20 / 2xl28 / full999
val LaimoryShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp),
)
// 카드 16(large), 버튼/입력 16, 다이얼로그 24(2xl 근처), pill 999(=CircleShape)

// spacing: 0·2·4·8·12·16·20·24·32·40·48·64 (4dp 기반)
object Spacing {
    val xs = 4.dp; val sm = 8.dp; val md = 12.dp; val lg = 16.dp
    val xl = 20.dp; val xl2 = 24.dp; val xl3 = 32.dp; val xl4 = 40.dp
}
```

## 7. LaimoryTheme

```kotlin
@Composable
fun LaimoryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) LaimoryDarkColorScheme else LaimoryLightColorScheme
    val extended = if (darkTheme) DarkExtendedColors else LightExtendedColors
    CompositionLocalProvider(LocalLaimoryColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LaimoryTypography,
            shapes = LaimoryShapes,
            content = content,
        )
    }
}

// 사용처 단축 접근자
val MaterialTheme.laimoryColors: LaimoryColors
    @Composable get() = LocalLaimoryColors.current
```

> Figma의 "Dark Mode = Semantic 모드 토글"이 코드에선 `darkTheme` 플래그 하나로 ColorScheme + ExtendedColors를 동시에 교체하는 것과 같다. **화면 코드는 모드를 의식하지 않는다** — 토큰만 쓰면 자동 대응된다.

## 8. 빠른 매핑표

| Figma Semantic | Compose |
|---|---|
| `color/primary`, `color/on-primary` | `colorScheme.primary`, `.onPrimary` |
| `color/surface`, `color/on-surface` | `colorScheme.surface`, `.onSurface` |
| `color/surface-container` | `colorScheme.surfaceContainer` |
| `color/on-surface-variant` (=text-secondary) | `colorScheme.onSurfaceVariant` |
| `color/outline` (=border) | `colorScheme.outline` |
| `color/error` / `error-container` | `colorScheme.error` / `.errorContainer` |
| `color/success`·`warning`·`info` (+ container/on) | `laimoryColors.success` … |
| `color/emotion-{joy…down}` (+ container/on) | `laimoryColors.joy` … |
| 타입 스타일 `Title/Medium` 등 | `typography.titleMedium` 등 |
| `radius/lg`, `spacing/16` | `Shapes.large`, `Spacing.lg` |