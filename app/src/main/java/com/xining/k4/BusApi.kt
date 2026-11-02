package com.xining.k4

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 与「车来了」实时数据打交道的唯一入口。
 * 协议参考开源的 chelaile-api-server（/v1 系列接口），可以自建后换成自己的地址。
 */
object BusApi {

    class ApiException(message: String) : IOException(message)

    private const val CONNECT_TIMEOUT = 7000
    private const val READ_TIMEOUT = 7000

    /** 查询：目标站前方最近的 3 辆车 */
    fun fetchRealtime(prefs: Prefs): QueryResult {
        val url = buildString {
            append(prefs.baseUrl)
            append("/v1/lines/realtime")
            append("?city_id=").append(enc(prefs.cityId))
            append("&line_id=").append(enc(prefs.lineId))
            append("&target_order=").append(prefs.targetOrder)
            append("&station_id=").append(enc(prefs.stationId))
            append("&lat=").append(enc(prefs.lat))
            append("&lng=").append(enc(prefs.lng))
        }
        return parseRealtime(httpGet(url), prefs.targetOrder)
    }

    fun parseRealtime(json: String, targetOrder: Int): QueryResult {
        val root = JSONObject(json)
        val array = root.optJSONArray("buses") ?: JSONArray()
        val approaching = ArrayList<BusInfo>()

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val order = item.optInt("order", -1)
            // order > targetOrder 说明这辆车已经开过目标站，不再算“即将到站”
            if (order < 1 || order > targetOrder) continue

            val eta = item.optJSONObject("eta")
            approaching.add(
                BusInfo(
                    licence = item.optString("licence").ifEmpty {
                        item.optString("busId")
                    },
                    order = order,
                    stopsToTarget = targetOrder - order,
                    distanceMeters = item.optInt("distanceToWaitStn", -1),
                    etaSeconds = eta?.optInt("travelTime", -1) ?: -1,
                    displayTime = eta?.optString("displayTime").orEmpty()
                )
            )
        }

        approaching.sortBy { it.stopsToTarget }

        return QueryResult(
            targetOrder = targetOrder,
            buses = approaching.take(3),
            approachingCount = approaching.size,
            realData = root.optBoolean("realData", true),
            fetchedAt = System.currentTimeMillis()
        )
    }

    /**
     * 自动发现：搜索结果里挑出「绿地·云香郡」发车的方向，
     * 再在线路详情里找到目标站的站序 / 站点 id / 坐标。
     */
    fun discover(
        baseUrl: String,
        cityId: String,
        keyword: String,
        targetStation: String
    ): DiscoverResult {
        val base = baseUrl.trim().trimEnd('/')
        val searchRoot = JSONObject(
            httpGet("$base/v1/search?city_id=${enc(cityId)}&keyword=${enc(keyword)}")
        )
        val lines = searchRoot.optJSONArray("lines") ?: JSONArray()
        if (lines.length() == 0) throw ApiException("在 $cityId 这个城市里没搜到「$keyword」")

        var matchedDirection: JSONObject? = null
        for (i in 0 until lines.length()) {
            val line = lines.optJSONObject(i) ?: continue
            if (!line.optString("name").equals(keyword, ignoreCase = true)) continue
            val directions = line.optJSONArray("directions") ?: continue
            for (j in 0 until directions.length()) {
                val direction = directions.optJSONObject(j) ?: continue
                if (direction.optString("startSn").contains("云香郡")) {
                    matchedDirection = direction
                    break
                }
            }
            if (matchedDirection != null) break
        }
        val direction = matchedDirection
            ?: throw ApiException("找到了「$keyword」，但没有「绿地·云香郡」发车的方向")

        val lineId = direction.optString("lineId")
        if (lineId.isEmpty()) throw ApiException("方向数据里没有 lineId")

        val detailJson = httpGet(
            "$base/v1/lines/detail?city_id=${enc(cityId)}&line_id=${enc(lineId)}"
        )
        val detail = JSONObject(detailJson)
        val stations = detail.optJSONArray("stations") ?: JSONArray()

        var target: JSONObject? = null
        for (i in 0 until stations.length()) {
            val station = stations.optJSONObject(i) ?: continue
            if (station.optString("sn").contains(targetStation)) {
                target = station
                break
            }
        }
        val station = target
            ?: throw ApiException("这条线路的站点里没有「$targetStation」")

        return DiscoverResult(
            lineId = lineId,
            stationId = station.optString("sId"),
            stationOrder = station.optInt("order"),
            stationName = station.optString("sn"),
            lat = station.optString("wgsLat"),
            lng = station.optString("wgsLng"),
            directionLabel = "${direction.optString("startSn")} → ${direction.optString("endSn")}"
        )
    }

    private fun httpGet(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = CONNECT_TIMEOUT
            conn.readTimeout = READ_TIMEOUT
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "K4Bus-Android/1.0")

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                throw ApiException("接口返回 HTTP $code：${body.take(160)}")
            }
            if (body.isBlank()) throw ApiException("接口返回了空内容")
            return body
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            throw ApiException("网络请求失败：${e.message ?: e.javaClass.simpleName}")
        } finally {
            conn.disconnect()
        }
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")
}
