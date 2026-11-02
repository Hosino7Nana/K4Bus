package com.xining.k4

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** 所有给人看的文案集中在这里，通知栏和界面共用 */
object Formats {

    fun stops(n: Int): String = if (n <= 0) "即将到站" else "还有 $n 站"

    fun eta(seconds: Int): String = when {
        seconds < 0 -> "时间未知"
        seconds <= 45 -> "即将到站"
        seconds < 60 -> "$seconds 秒"
        else -> "${seconds / 60} 分钟"
    }

    /** 主界面右侧那行小字 */
    fun etaDetail(seconds: Int): String = when {
        seconds < 0 -> "到站时间未知"
        seconds <= 45 -> "即将到站"
        else -> "预计 ${eta(seconds)}到站"
    }

    /** 通知里那种“约 3 分钟”的写法 */
    fun etaApprox(seconds: Int): String = when {
        seconds < 0 -> "时间未知"
        seconds <= 45 -> "即将到站"
        else -> "约 ${eta(seconds)}"
    }

    fun distance(meters: Int): String = when {
        meters < 0 -> "距离未知"
        meters < 1000 -> "$meters 米"
        else -> String.format(Locale.CHINA, "%.1f 公里", meters / 1000.0)
    }

    fun clock(ts: Long): String =
        SimpleDateFormat("HH:mm:ss", Locale.CHINA).format(Date(ts))

    fun alarmClock(ts: Long): String {
        if (ts <= 0L) return "未设置"
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = ts }
        val hm = SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(ts))
        if (sameDay(now, target)) return "今天 $hm"
        val tomorrow = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        if (sameDay(tomorrow, target)) return "明天 $hm"
        return SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date(ts))
    }

    private fun sameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    /** 通知栏 / 离线缓存用的多行摘要 */
    fun summary(r: QueryResult, stationName: String): String {
        val sb = StringBuilder()
        sb.append("K4 ").append(K4Route.START).append(" → ").append(K4Route.END).append('\n')
        sb.append("距「").append(stationName).append("」最近的 ")
            .append(r.buses.size).append(" 辆车：\n")
        if (r.buses.isEmpty()) {
            sb.append("暂时没有开往该站的车")
        } else {
            r.buses.forEachIndexed { index, bus ->
                sb.append(index + 1).append(". ")
                sb.append(bus.licence.ifEmpty { "未知车牌" })
                sb.append(" · ").append(stops(bus.stopsToTarget))
                if (bus.distanceMeters >= 0) sb.append(" · ").append(distance(bus.distanceMeters))
                if (bus.etaSeconds >= 0) sb.append(" · ").append(etaApprox(bus.etaSeconds))
                if (index != r.buses.lastIndex) sb.append('\n')
            }
        }
        return sb.toString()
    }
}
