package com.xining.k4

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 开机、改时间、覆盖安装之后，把每天 8:20 的闹钟重新排上 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> AlarmScheduler.schedule(context.applicationContext)
        }
    }
}
