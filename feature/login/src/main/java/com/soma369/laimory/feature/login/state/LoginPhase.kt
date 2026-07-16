package com.soma369.laimory.feature.login.state

enum class LoginPhase {
    IDLE,
    PREPARING,
    WAITING_CALLBACK,
    EXCHANGING_TOKEN,
}
