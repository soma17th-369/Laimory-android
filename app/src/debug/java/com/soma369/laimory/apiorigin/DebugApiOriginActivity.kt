package com.soma369.laimory.apiorigin

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import com.soma369.laimory.MainActivity

/**
 * debug 빌드의 런처. 새 프로세스에서 처음 켜면 붙을 서버를 묻고 [MainActivity] 로 넘긴다.
 *
 * 앱 화면보다 **먼저** 묻는 이유: 화면이 뜬 뒤 다이얼로그를 올리면 이어서 뜨는 공지 시트·업데이트 안내 같은
 * 창이 그 위를 덮는다. debug 매니페스트에만 선언하고 main 의 런처 intent-filter 를 지운다 — release·qa 는
 * 지금처럼 [MainActivity] 가 런처다.
 *
 * 앱은 이 화면 전에도 마지막으로 고른 서버로 이미 시작해 있다(푸시 등록 등은 Application 에서 돈다). 그래서
 * **다른 서버를 고르면 저장하고 프로세스를 다시 시작한다** — 주소만 바꾸면 한 프로세스에 두 서버의 상태가
 * 섞인다. 같은 서버면 그대로 넘긴다. 알림·App Link 진입과 백그라운드 작업은 이 화면을 거치지 않고 마지막
 * 값을 쓴다.
 */
class DebugApiOriginActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra(EXTRA_RESTARTED, false)) decidedInProcess = true
        // 이미 고른 프로세스에서 런처를 다시 누르면 묻지 않는다.
        if (decidedInProcess) {
            openMain()
            return
        }
        // 회전 등으로 다시 만들어져도 다이얼로그를 새로 띄운다(창은 이전 인스턴스와 함께 사라졌다).
        showPicker(DebugApiOriginStore(this))
    }

    private fun showPicker(store: DebugApiOriginStore) {
        val origins = DebugApiOrigin.entries
        val current = store.selected
        var chosen = current
        AlertDialog
            .Builder(this, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
            .setTitle("API 서버 선택 (debug)")
            .setSingleChoiceItems(origins.map { "${it.label}\n${it.baseUrl}" }.toTypedArray(), origins.indexOf(current)) { _, which ->
                chosen = origins[which]
            }.setPositiveButton("접속") { _, _ ->
                if (chosen == current) {
                    decidedInProcess = true
                    openMain()
                } else {
                    store.save(chosen)
                    restartProcess()
                }
            }.setOnCancelListener { finish() }
            .show()
    }

    /** 런처로 켠 것과 같은 Intent 로 넘긴다 — [MainActivity] 는 `ACTION_MAIN` 으로 워드마크 스플래시를 판단한다. */
    private fun openMain() {
        startActivity(launcherIntent(ComponentName(this, MainActivity::class.java)))
        finish()
    }

    /** 새 프로세스에서 이 화면을 다시 거치되 묻지 않고 넘긴다. */
    private fun restartProcess() {
        val restart =
            Intent
                .makeRestartActivityTask(ComponentName(this, DebugApiOriginActivity::class.java))
                .putExtra(EXTRA_RESTARTED, true)
        startActivity(restart)
        Runtime.getRuntime().exit(0)
    }

    private fun launcherIntent(component: ComponentName): Intent =
        Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(component)

    private companion object {
        /** 이 프로세스에서 서버를 이미 정했는지. 프로세스가 새로 뜨면 false 로 돌아간다. */
        var decidedInProcess = false

        const val EXTRA_RESTARTED = "com.soma369.laimory.apiorigin.RESTARTED"
    }
}
