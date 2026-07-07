package com.soma369.laimory.core.collection.notification

import android.content.Context
import android.content.Intent
import com.soma369.laimory.core.domain.source.InstalledApp
import com.soma369.laimory.core.domain.source.InstalledAppsProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** PackageManager 로 런처 앱 목록을 읽는 [InstalledAppsProvider] 구현. */
internal class InstalledAppsProviderImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : InstalledAppsProvider {
        override suspend fun launchableApps(): List<InstalledApp> =
            withContext(Dispatchers.IO) {
                val packageManager = context.packageManager
                val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                packageManager
                    .queryIntentActivities(launcherIntent, 0)
                    .map { resolveInfo ->
                        InstalledApp(
                            packageName = resolveInfo.activityInfo.packageName,
                            label = resolveInfo.loadLabel(packageManager).toString(),
                        )
                    }.distinctBy { it.packageName }
                    .sortedBy { it.label.lowercase() }
            }
    }
