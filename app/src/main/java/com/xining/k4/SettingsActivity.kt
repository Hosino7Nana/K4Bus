package com.xining.k4

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var etBaseUrl: EditText
    private lateinit var etCityId: EditText
    private lateinit var etLineId: EditText
    private lateinit var etStationName: EditText
    private lateinit var etOrder: EditText
    private lateinit var etStationId: EditText
    private lateinit var etLat: EditText
    private lateinit var etLng: EditText
    private lateinit var etTime: EditText
    private lateinit var cbWeekdays: CheckBox
    private lateinit var cbEnabled: CheckBox
    private lateinit var tvStatus: TextView

    private val io: ExecutorService = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        prefs = Prefs(this)

        etBaseUrl = findViewById(R.id.etBaseUrl)
        etCityId = findViewById(R.id.etCityId)
        etLineId = findViewById(R.id.etLineId)
        etStationName = findViewById(R.id.etStationName)
        etOrder = findViewById(R.id.etOrder)
        etStationId = findViewById(R.id.etStationId)
        etLat = findViewById(R.id.etLat)
        etLng = findViewById(R.id.etLng)
        etTime = findViewById(R.id.etTime)
        cbWeekdays = findViewById(R.id.cbWeekdays)
        cbEnabled = findViewById(R.id.cbEnabled)
        tvStatus = findViewById(R.id.tvStatus)

        findViewById<Button>(R.id.btnDiscover).setOnClickListener { discover() }
        findViewById<Button>(R.id.btnSave).setOnClickListener { save() }

        load()
    }

    override fun onDestroy() {
        super.onDestroy()
        io.shutdown()
    }

    private fun load() {
        etBaseUrl.setText(prefs.baseUrl)
        etCityId.setText(prefs.cityId)
        etLineId.setText(prefs.lineId)
        etStationName.setText(prefs.stationName)
        etOrder.setText(prefs.targetOrder.toString())
        etStationId.setText(prefs.stationId)
        etLat.setText(prefs.lat)
        etLng.setText(prefs.lng)
        etTime.setText(String.format(Locale.CHINA, "%02d:%02d", prefs.alarmHour, prefs.alarmMinute))
        cbWeekdays.isChecked = prefs.weekdaysOnly
        cbEnabled.isChecked = prefs.enabled
        tvStatus.text = "当前线路：K4 ${K4Route.START} → ${K4Route.END}，共 ${K4Route.stations.size} 站"
    }

    private fun save() {
        val time = parseTime(etTime.text.toString())
        if (time == null) {
            Toast.makeText(this, "时间格式要写成 08:20 这样", Toast.LENGTH_SHORT).show()
            return
        }
        val order = etOrder.text.toString().trim().toIntOrNull()
        if (order == null || order < 1) {
            Toast.makeText(this, "站序必须是大于 0 的整数", Toast.LENGTH_SHORT).show()
            return
        }
        val baseUrl = etBaseUrl.text.toString().trim()
        if (!baseUrl.startsWith("http")) {
            Toast.makeText(this, "接口地址要以 http:// 或 https:// 开头", Toast.LENGTH_SHORT).show()
            return
        }

        prefs.baseUrl = baseUrl
        prefs.cityId = etCityId.text.toString().trim()
        prefs.lineId = etLineId.text.toString().trim()
        prefs.stationName = etStationName.text.toString().trim()
        prefs.targetOrder = order
        prefs.stationId = etStationId.text.toString().trim()
        prefs.lat = etLat.text.toString().trim()
        prefs.lng = etLng.text.toString().trim()
        prefs.alarmHour = time.first
        prefs.alarmMinute = time.second
        prefs.weekdaysOnly = cbWeekdays.isChecked
        prefs.enabled = cbEnabled.isChecked

        AlarmScheduler.schedule(this)
        Toast.makeText(this, "已保存，闹钟已重新安排", Toast.LENGTH_SHORT).show()
        finish()
    }

    /** 用接口反查 lineId / 站序 / 站点编号 / 坐标，省得手抄 */
    private fun discover() {
        val keyword = "K4"
        val station = etStationName.text.toString().trim().ifEmpty { Prefs.DEF_STATION_NAME }
        tvStatus.text = "正在向接口查询线路参数…"
        val baseUrl = etBaseUrl.text.toString().trim()
        val cityId = etCityId.text.toString().trim()
        io.execute {
            try {
                val result = BusApi.discover(baseUrl, cityId, keyword, station)
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    etLineId.setText(result.lineId)
                    etStationId.setText(result.stationId)
                    etOrder.setText(result.stationOrder.toString())
                    etStationName.setText(result.stationName)
                    if (result.lat.isNotEmpty()) etLat.setText(result.lat)
                    if (result.lng.isNotEmpty()) etLng.setText(result.lng)
                    tvStatus.text = "已找到：${result.directionLabel}\n" +
                        "「${result.stationName}」是第 ${result.stationOrder} 站，点“保存”生效"
                }
            } catch (t: Throwable) {
                val message = t.message ?: t.javaClass.simpleName
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    tvStatus.text = "自动获取失败：$message"
                }
            }
        }
    }

    private fun parseTime(raw: String): Pair<Int, Int>? {
        val text = raw.trim().replace('：', ':')
        val parts = text.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].trim().toIntOrNull() ?: return null
        val minute = parts[1].trim().toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour to minute
    }
}
