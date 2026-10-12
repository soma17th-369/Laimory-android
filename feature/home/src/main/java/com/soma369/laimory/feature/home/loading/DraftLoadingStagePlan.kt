package com.soma369.laimory.feature.home.loading

import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * 진행 카드 앞 세 줄이 끝나는 시각(경과 기준, 누적). AI 줄은 여기 없다 — 서버 완료로만 끝난다.
 *
 * 줄마다 **고른 개수에 비례**해 길어진다. 사진을 하나도 안 골랐는데 "사진 분석 중"이 오래 돌다가 `0장 완료`가
 * 뜨거나, 30장과 1장이 같은 시각에 끝나면 연출이 내용과 어긋나 보인다.
 *
 * 그래도 실제 진척은 아니다. 길이에 ±[JITTER] 흔들림을 섞어 매번 같은 박자로 끝나지 않게 한다 — 같은
 * 박자가 반복되면 정해진 애니메이션이라는 것이 드러난다. 흔들림은 [seed] 로만 정해져 한 번의 생성 안에서는
 * 다시 계산해도 같은 값이 나온다(틱마다 다시 만들어도 줄이 앞뒤로 흔들리지 않는다).
 */
data class DraftLoadingStagePlan(
    val deadlines: Map<DraftLoadingStage, Duration>,
) {
    /** 앞 세 줄이 모두 끝나는 시각. 이후로는 AI 줄만 진행 중이다. */
    val lastDeadline: Duration get() = deadlines.values.max()

    companion object {
        fun of(
            photoCount: Int,
            calendarCount: Int,
            stayCount: Int,
            seed: Long,
        ): DraftLoadingStagePlan {
            var end = Duration.ZERO
            val deadlines =
                listOf(
                    DraftLoadingStage.PHOTO to PHOTO.durationFor(photoCount, seed),
                    DraftLoadingStage.CALENDAR to CALENDAR.durationFor(calendarCount, seed),
                    DraftLoadingStage.STAY to STAY.durationFor(stayCount, seed),
                ).associate { (stage, length) ->
                    end += length
                    stage to end
                }
            return DraftLoadingStagePlan(deadlines)
        }

        /** 고른 것이 없는 줄도 이만큼은 돈다 — 순식간에 지나가면 그 줄을 읽을 틈이 없다. */
        val MIN_STAGE: Duration = 2.seconds

        /** 길이를 이 비율 안에서 흔든다. */
        const val JITTER = 0.15

        private val PHOTO = StageRate(stage = DraftLoadingStage.PHOTO, base = 4.seconds, perItem = 1.seconds, max = 25.seconds)
        private val CALENDAR = StageRate(stage = DraftLoadingStage.CALENDAR, base = 3.seconds, perItem = 1.seconds, max = 12.seconds)
        private val STAY = StageRate(stage = DraftLoadingStage.STAY, base = 4.seconds, perItem = 1500.milliseconds, max = 20.seconds)
    }

    /**
     * 한 줄의 길이 규칙. 고른 게 있으면 `base + perItem × 개수` 를 [max] 까지, 없으면 [MIN_STAGE] 다.
     * 그 위에 흔들림을 곱하고 다시 `[MIN_STAGE, max]` 로 묶는다.
     */
    private data class StageRate(
        val stage: DraftLoadingStage,
        val base: Duration,
        val perItem: Duration,
        val max: Duration,
    ) {
        fun durationFor(
            count: Int,
            seed: Long,
        ): Duration {
            val nominal = if (count <= 0) MIN_STAGE else (base + perItem * count).coerceAtMost(max)
            // 줄마다 다른 난수를 쓴다. 같은 난수를 쓰면 세 줄이 함께 길어지거나 짧아져 흔들림이 박자로 보인다.
            val factor = 1 + Random(seed * 31 + stage.ordinal).nextDouble(-JITTER, JITTER)
            return (nominal * factor).coerceIn(MIN_STAGE, max)
        }
    }
}
