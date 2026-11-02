package com.xining.k4

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

/** 用 AlarmManager 排每天早上 8:20 的一次性精确闹钟 */
object AlarmScheduler {

    const val ACTION_QUERY = "com.xining.k4.action.QUERY"
    private const val REQUEST_CODE = 1001

    fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_QUERY
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }

    /** 计算下一次触发的毫秒时间戳 */
    fun nextTriggerMillis(prefs: Prefs, from: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = from
        calendar.set(Calendar.HOUR_OF_DAY, prefs.alarmHour)
        calendar.set(Calendar.MINUTE, prefs.alarmMinute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        if (calendar.timeInMillis <= from + 1_000L) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        if (prefs.weekdaysOnly) {
            while (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
            ) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return calendar.timeInMillis
    }

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return manager.canScheduleExactAlarms()
    }

    fun schedule(context: Context) {
        val prefs = Prefs(context)
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val operation = pendingIntent(context)
        manager.cancel(operation)
        if (!prefs.enabled) {
            prefs.nextAlarmAt = 0L
            return
        }
        val triggerAt = nextTriggerMillis(prefs)
        try {
            if (canScheduleExact(context)) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
            } else {
                // 没拿到“闹钟和提醒”权限时退化为非精确闹钟，可能晚几分钟
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
            }
        } catch (e: SecurityException) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
        }
        prefs.nextAlarmAt = triggerAt
    }

    fun cancel(context: Context) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        manager.cancel(pendingIntent(context))
        Prefs(context).nextAlarmAt = 0L
    }
}
