package com.soma369.laimory.core.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf

val LocalSnackbarHostState =
    staticCompositionLocalOf<SnackbarHostState> {
        error("No SnackbarHostState provided — wrap with CompositionLocalProvider(LocalSnackbarHostState provides ...)")
    }
