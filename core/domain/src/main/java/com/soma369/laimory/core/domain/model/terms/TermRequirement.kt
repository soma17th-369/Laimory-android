package com.soma369.laimory.core.domain.model.terms

/** 단계가 요구하는 문서 하나와 이 계정이 그 **현재 버전**에 동의했는지. */
data class TermRequirement(
    val document: TermDocument,
    val isAgreed: Boolean,
)
