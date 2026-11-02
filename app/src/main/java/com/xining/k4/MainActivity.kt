package com.xining.k4

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var tvRoute: TextView
    private lateinit var tvTarget: TextView
    private lateinit var tvAlarm: TextView
    private lateinit var tvBanner: TextView
    private lateinit var tvUpdated: TextView
    private lateinit var tvHint: TextView
    private lateinit var busContainer: LinearLayout
    private lateinit var btnRefresh: Button

    private val io: ExecutorService = Executors.newSingleThreadExecutor()

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(this, "没有通知权限，就收不到 8:20 的提醒了", Toast.LENGTH_LONG).show()
            }
            updateBanner()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = Prefs(this)
        Notifier.ensureChannel(this)

        tvRoute = findViewById(R.id.tvRoute)
        tvTarget = findViewById(R.id.tvTarget)
        tvAlarm = findViewById(R.id.tvAlarm)
        tvBanner = findViewById(R.id.tvBanner)
        tvUpdated = findViewById(R.id.tvUpdated)
        tvHint = findViewById(R.id.tvHint)
        busContainer = findViewById(R.id.busContainer)
        btnRefresh = findViewById(R.id.btnRefresh)

        btnRefresh.setOnClickListener { refresh() }
        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<Button>(R.id.btnTest).setOnClickListener { sendTestNotification() }
        tvBanner.setOnClickListener { handleBannerClick() }

        askNotificationPermission()
        AlarmScheduler.schedule(this)
        showCachedHint()
    }

    override fun onResume() {
        super.onResume()
        AlarmScheduler.schedule(this)
        updateHeader()
        updateBanner()
        refresh()
    }

    override fun onDestroy() {
        super.onDestroy()
        io.shutdown()
    }

    // ---------------------------------------------------------------- 查询

    private fun refresh() {
        if (io.isShutdown) return
        tvHint.text = "正在查询实时位置…"
        val snapshot = Prefs(this)
        io.execute {
            try {
                val result = BusApi.fetchRealtime(snapshot)
                snapshot.cachedText = Formats.summary(result, snapshot.stationName)
                snapshot.cachedAt = result.fetchedAt
                runOnUiThread { if (!isFinishing && !isDestroyed) render(result) }
            } catch (t: Throwable) {
                val message = t.message ?: t.javaClass.simpleName
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    tvHint.text = "查询失败：$message"
                    if (snapshot.cachedAt > 0L) {
                        tvUpdated.text = "上次成功 " + Formats.clock(snapshot.cachedAt)
                    }
                }
            }
        }
    }

    private fun render(result: QueryResult) {
        busContainer.removeAllViews()
        tvUpdated.text = "更新于 " + Formats.clock(result.fetchedAt)

        if (result.buses.isEmpty()) {
            tvHint.text = "现在没有开往「${prefs.stationName}」的车。K4 首末班 06:30–19:00。"
            return
        }

        tvHint.text = "前方共 ${result.approachingCount} 辆车驶来，下面是最近的 3 辆"
        val inflater = LayoutInflater.from(this)
        result.buses.forEachIndexed { index, bus ->
            val row = inflater.inflate(R.layout.item_bus, busContainer, false)
            row.findViewById<TextView>(R.id.tvRank).text = (index + 1).toString()
            row.findViewById<TextView>(R.id.tvLicence).text =
                bus.licence.ifEmpty { "未知车牌" }
            row.findViewById<TextView>(R.id.tvEta).text =
                Formats.etaDetail(bus.etaSeconds)
            row.findViewById<TextView>(R.id.tvStops).text =
                Formats.stops(bus.stopsToTarget)
            row.findViewById<TextView>(R.id.tvDist).text =
                Formats.distance(bus.distanceMeters)
            busContainer.addView(row)
        }
    }

    private fun showCachedHint() {
        val cachedAt = prefs.cachedAt
        if (cachedAt > 0L) {
            tvUpdated.text = "上次成功 " + Formats.clock(cachedAt)
        }
    }

    // ---------------------------------------------------------------- 界面状态

    private fun updateHeader() {
        tvRoute.text = "K4  ${K4Route.START} → ${K4Route.END}"
        tvTarget.text = "目标站：${prefs.stationName}（第 ${prefs.targetOrder} 站）"

        val time = String.format(
            Locale.CHINA,
            "%02d:%02d",
            prefs.alarmHour,
            prefs.alarmMinute
        )
        val repeat = if (prefs.weekdaysOnly) "周一至周五" else "每天"
        val next = if (prefs.enabled) Formats.alarmClock(prefs.nextAlarmAt) else "已关闭"
        tvAlarm.text = "提醒 $time（$repeat）· 下次 $next"
    }

    private fun updateBanner() {
        val exact = AlarmScheduler.canScheduleExact(this)
        val notifications = hasNotificationPermission()
        when {
            !exact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                tvBanner.visibility = View.VISIBLE
                tvBanner.text = "⚠ 还没授予「闹钟和提醒」权限，8:20 的提醒可能晚几分钟。点这里去开启"
                tvBanner.tag = "exact"
            }
            !notifications -> {
                tvBanner.visibility = View.VISIBLE
                tvBanner.text = "⚠ 通知权限没开，收不到早班车提醒。点这里去开启"
                tvBanner.tag = "notify"
            }
            else -> {
                tvBanner.visibility = View.GONE
                tvBanner.tag = null
            }
        }
    }

    private fun handleBannerClick() {
        when (tvBanner.tag) {
            "exact" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:$packageName")
                        )
                    )
                }
            }
            "notify" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    openAppNotificationSettings()
                }
            }
            else -> openAppNotificationSettings()
        }
    }

    private fun openAppNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
    }

    // ---------------------------------------------------------------- 权限

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission()) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun sendTestNotification() {
        val sample = QueryResult(
            targetOrder = prefs.targetOrder,
            buses = listOf(
                BusInfo("青A-09391D", 19, 2, 1337, 149, "09:45"),
                BusInfo("青A-03532D", 17, 4, 2606, 383, "09:49"),
                BusInfo("青A-03356D", 12, 9, 6462, 1039, "10:00")
            ),
            approachingCount = 5,
            realData = true,
            fetchedAt = System.currentTimeMillis()
        )
        Notifier.notifyResult(this, sample)
        Toast.makeText(this, "已发送一条示例通知（下拉通知栏看看）", Toast.LENGTH_SHORT).show()
    }
}
