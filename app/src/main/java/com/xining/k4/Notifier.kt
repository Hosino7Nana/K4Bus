package com.xining.k4

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/** 通知栏：每天早上 8:20 把最近 3 辆车推给用户 */
object Notifier {

    const val CHANNEL_ID = "k4_daily"
    private const val NOTIFY_ID = 2001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "K4 早班车提醒",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "每天早上推送 K4 最近 3 辆车距宁张路二十里铺的站数"
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun notifyResult(context: Context, result: QueryResult) {
        val prefs = Prefs(context)
        val body = Formats.summary(result, prefs.stationName)
        val nearest = result.buses.firstOrNull()
        val title = when {
            nearest == null -> "K4 暂时没有来车"
            nearest.stopsToTarget <= 0 -> "K4 有车到「${prefs.stationName}」了"
            else -> "K4 最近一辆还有 ${nearest.stopsToTarget} 站"
        }
        post(context, title, body, false)
    }

    fun notifyError(context: Context, message: String) {
        post(
            context,
            "K4 查询失败",
            "早上好，这次没取到实时数据：$message\n可以打开 App 重试一次。",
            true
        )
    }

    private fun post(context: Context, title: String, body: String, isError: Boolean) {
        ensureChannel(context)

        val contentIntent = PendingIntent.getActivity(
            context,
            100,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            pendingFlags()
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_bus)
            .setContentTitle(title)
            .setContentText(body.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty())
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)

        if (!isError) builder.setCategory(NotificationCompat.CATEGORY_REMINDER)

        try {
            NotificationManagerCompat.from(context).notify(NOTIFY_ID, builder.build())
        } catch (e: SecurityException) {
            // 用户关掉了通知权限，忽略即可，界面上仍能看到结果
        }
    }

    private fun pendingFlags(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
}
