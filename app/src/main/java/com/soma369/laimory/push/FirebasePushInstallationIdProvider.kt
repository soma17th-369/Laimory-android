package com.soma369.laimory.push

import com.google.firebase.installations.FirebaseInstallations
import com.soma369.laimory.core.domain.provider.PushInstallationIdProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class FirebasePushInstallationIdProvider
    @Inject
    constructor() : PushInstallationIdProvider {
        override suspend fun getCurrentId(): String =
            suspendCancellableCoroutine { continuation ->
                FirebaseInstallations
                    .getInstance()
                    .id
                    .addOnCompleteListener { task ->
                        if (!continuation.isActive) return@addOnCompleteListener
                        if (task.isSuccessful) {
                            val installationId = task.result
                            if (installationId.isNullOrBlank()) {
                                continuation.resumeWithException(IllegalStateException("Firebase installation ID is unavailable"))
                            } else {
                                continuation.resume(installationId)
                            }
                        } else {
                            continuation.resumeWithException(
                                task.exception ?: IllegalStateException("Firebase installation ID lookup failed"),
                            )
                        }
                    }
            }
    }
