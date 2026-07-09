package com.soma369.laimory.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** 감정 5색 축 — 활기/평온/무덤덤/지침/울적. Figma emotion 팔레트와 1:1. */
enum class Emotion { JOY, CALM, MELLOW, WEARY, DOWN }

/** 감정 base 색(강조/닷용). */
@Composable
fun Emotion.color(): Color =
    with(LocalLaimoryColors.current) {
        when (this@color) {
            Emotion.JOY -> joy
            Emotion.CALM -> calm
            Emotion.MELLOW -> mellow
            Emotion.WEARY -> weary
            Emotion.DOWN -> down
        }
    }

/** 감정 컨테이너(카드 배경)색. */
@Composable
fun Emotion.containerColor(): Color =
    with(LocalLaimoryColors.current) {
        when (this@containerColor) {
            Emotion.JOY -> joyContainer
            Emotion.CALM -> calmContainer
            Emotion.MELLOW -> mellowContainer
            Emotion.WEARY -> wearyContainer
            Emotion.DOWN -> downContainer
        }
    }

/** 감정 컨테이너 위 텍스트/아이콘 색. */
@Composable
fun Emotion.onContainerColor(): Color =
    with(LocalLaimoryColors.current) {
        when (this@onContainerColor) {
            Emotion.JOY -> onJoyContainer
            Emotion.CALM -> onCalmContainer
            Emotion.MELLOW -> onMellowContainer
            Emotion.WEARY -> onWearyContainer
            Emotion.DOWN -> onDownContainer
        }
    }
