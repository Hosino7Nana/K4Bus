package com.xining.k4

/**
 * K4 路「绿地·云香郡 → 青海师范大学东」方向的站序（2026-07 调线后的走向）。
 * 只用于界面展示和线路自检，真正的站序以接口返回为准。
 */
object K4Route {

    const val START = "绿地·云香郡"
    const val END = "青海师范大学东"

    val stations: List<String> = listOf(
        "绿地·云香郡",
        "桃李小学",
        "西关大街通海路口",
        "财富中心",
        "唐道·637",
        "文逸小学",
        "海湖路中",
        "建设巷口",
        "小桥十字西",
        "小桥",
        "小桥北站",
        "北杏园",
        "幸福城小区",
        "新世纪花园",
        "城北国际村",
        "纬三路东口",
        "生物园区路口",
        "石头磊",
        "宁张路新村",
        "二十里铺南站",
        "宁张路二十里铺",
        "莫家泉湾",
        "青海大学东",
        "西交大一附院青海医院",
        "青海师范大学东"
    )

    fun indexOf(name: String): Int = stations.indexOfFirst { it.contains(name) } + 1
}
