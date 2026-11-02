package com.xining.k4

/** 一辆正在驶向目标站的车 */
data class BusInfo(
    /** 车牌，例如 青A-09391D */
    val licence: String,
    /** 车辆当前所在的站序（即将到达或刚刚离开的那一站） */
    val order: Int,
    /** 距离目标站还有几站 */
    val stopsToTarget: Int,
    /** 距离目标站还有多少米，-1 表示未知 */
    val distanceMeters: Int,
    /** 预计还有多少秒到站，-1 表示未知 */
    val etaSeconds: Int,
    /** 接口给的到站时刻文本，例如 09:45，可能为空 */
    val displayTime: String
)

/** 一次查询的完整结果 */
data class QueryResult(
    val targetOrder: Int,
    /** 已按距离由近到远排序、最多 3 辆 */
    val buses: List<BusInfo>,
    /** 目标站前方一共有多少辆车（不只是 3 辆） */
    val approachingCount: Int,
    /** 接口是否返回了实时定位 */
    val realData: Boolean,
    val fetchedAt: Long
)

/** 自动发现线路参数的结果 */
data class DiscoverResult(
    val lineId: String,
    val stationId: String,
    val stationOrder: Int,
    val stationName: String,
    val lat: String,
    val lng: String,
    val directionLabel: String
)
