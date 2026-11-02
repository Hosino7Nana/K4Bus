package com.xining.k4

import android.content.Context
import android.content.SharedPreferences

/**
 * 所有可变配置都存在 SharedPreferences 里，默认值已经是西宁 K4 的现成参数，
 * 装好就能用；换线路时只需要在设置页改一次。
 */
class Prefs(context: Context) {

    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var baseUrl: String
        get() = (sp.getString(KEY_BASE_URL, DEF_BASE_URL) ?: DEF_BASE_URL).trim().trimEnd('/')
        set(value) = sp.edit().putString(KEY_BASE_URL, value.trim().trimEnd('/')).apply()

    var cityId: String
        get() = sp.getString(KEY_CITY_ID, DEF_CITY_ID) ?: DEF_CITY_ID
        set(value) = sp.edit().putString(KEY_CITY_ID, value.trim()).apply()

    var lineId: String
        get() = sp.getString(KEY_LINE_ID, DEF_LINE_ID) ?: DEF_LINE_ID
        set(value) = sp.edit().putString(KEY_LINE_ID, value.trim()).apply()

    var targetOrder: Int
        get() = sp.getInt(KEY_TARGET_ORDER, DEF_TARGET_ORDER)
        set(value) = sp.edit().putInt(KEY_TARGET_ORDER, value).apply()

    var stationId: String
        get() = sp.getString(KEY_STATION_ID, DEF_STATION_ID) ?: DEF_STATION_ID
        set(value) = sp.edit().putString(KEY_STATION_ID, value.trim()).apply()

    var stationName: String
        get() = sp.getString(KEY_STATION_NAME, DEF_STATION_NAME) ?: DEF_STATION_NAME
        set(value) = sp.edit().putString(KEY_STATION_NAME, value.trim()).apply()

    var lat: String
        get() = sp.getString(KEY_LAT, DEF_LAT) ?: DEF_LAT
        set(value) = sp.edit().putString(KEY_LAT, value.trim()).apply()

    var lng: String
        get() = sp.getString(KEY_LNG, DEF_LNG) ?: DEF_LNG
        set(value) = sp.edit().putString(KEY_LNG, value.trim()).apply()

    var alarmHour: Int
        get() = sp.getInt(KEY_ALARM_HOUR, DEF_HOUR)
        set(value) = sp.edit().putInt(KEY_ALARM_HOUR, value).apply()

    var alarmMinute: Int
        get() = sp.getInt(KEY_ALARM_MINUTE, DEF_MINUTE)
        set(value) = sp.edit().putInt(KEY_ALARM_MINUTE, value).apply()

    var weekdaysOnly: Boolean
        get() = sp.getBoolean(KEY_WEEKDAYS, false)
        set(value) = sp.edit().putBoolean(KEY_WEEKDAYS, value).apply()

    var enabled: Boolean
        get() = sp.getBoolean(KEY_ENABLED, true)
        set(value) = sp.edit().putBoolean(KEY_ENABLED, value).apply()

    /** 下次闹钟触发的绝对时间（毫秒），0 表示没有排期 */
    var nextAlarmAt: Long
        get() = sp.getLong(KEY_NEXT_ALARM, 0L)
        set(value) = sp.edit().putLong(KEY_NEXT_ALARM, value).apply()

    /** 最近一次查询结果的纯文本，用于没网时在界面上兜底显示 */
    var cachedText: String
        get() = sp.getString(KEY_CACHE_TEXT, "") ?: ""
        set(value) = sp.edit().putString(KEY_CACHE_TEXT, value).apply()

    var cachedAt: Long
        get() = sp.getLong(KEY_CACHE_AT, 0L)
        set(value) = sp.edit().putLong(KEY_CACHE_AT, value).apply()

    companion object {
        private const val FILE = "k4_prefs"

        /** 车来了数据的公开兼容接口，可换成自建实例 */
        const val DEF_BASE_URL = "https://ts-api.tundrey.com"
        /** 车来了城市编号：西宁 */
        const val DEF_CITY_ID = "566"
        /** K4 方向：绿地·云香郡 → 青海师范大学东 */
        const val DEF_LINE_ID = "566308918036639"
        /** 宁张路二十里铺在这一方向上是第 21 站 */
        const val DEF_TARGET_ORDER = 21
        const val DEF_STATION_ID = "566-374"
        const val DEF_STATION_NAME = "宁张路二十里铺"
        const val DEF_LAT = "36.712286"
        const val DEF_LNG = "101.762056"

        const val DEF_HOUR = 8
        const val DEF_MINUTE = 20

        private const val KEY_BASE_URL = "base_url"
        private const val KEY_CITY_ID = "city_id"
        private const val KEY_LINE_ID = "line_id"
        private const val KEY_TARGET_ORDER = "target_order"
        private const val KEY_STATION_ID = "station_id"
        private const val KEY_STATION_NAME = "station_name"
        private const val KEY_LAT = "lat"
        private const val KEY_LNG = "lng"
        private const val KEY_ALARM_HOUR = "alarm_hour"
        private const val KEY_ALARM_MINUTE = "alarm_minute"
        private const val KEY_WEEKDAYS = "weekdays_only"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_NEXT_ALARM = "next_alarm_at"
        private const val KEY_CACHE_TEXT = "cache_text"
        private const val KEY_CACHE_AT = "cache_at"
    }
}
