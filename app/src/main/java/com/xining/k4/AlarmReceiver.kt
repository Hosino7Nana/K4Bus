package com.xining.k4

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 8:20 闹钟到点：先排好明天的闹钟，再在后台线程里查一次实时数据并推通知。
 * 用 goAsync() 撑住广播生命周期，避免查询还没回来进程就被回收。
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmScheduler.ACTION_QUERY) return

        val appContext = context.applicationContext

        // 先排下一天，避免这次查询失败导致之后再也不提醒
        AlarmScheduler.schedule(appContext)

        val pendingResult = goAsync()
        Thread {
            var result: QueryResult? = null
            var error: String? = null
            try {
                val prefs = Prefs(appContext)
                result = BusApi.fetchRealtime(prefs)
                prefs.cachedText = Formats.summary(result, prefs.stationName)
                prefs.cachedAt = result.fetchedAt
            } catch (t: Throwable) {
                error = t.message ?: t.javaClass.simpleName
            }
            try {
                val snapshot = result
                if (snapshot != null) {
                    Notifier.notifyResult(appContext, snapshot)
                } else {
                    Notifier.notifyError(appContext, error ?: "未知错误")
                }
            } catch (t: Throwable) {
                // 通知失败不影响后续流程
            } finally {
                pendingResult.finish()
            }
        }.start()
    }
}
