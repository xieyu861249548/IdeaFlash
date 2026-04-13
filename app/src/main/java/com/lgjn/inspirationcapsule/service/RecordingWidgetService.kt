package com.lgjn.inspirationcapsule.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.RemoteViews
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.lgjn.inspirationcapsule.R
import com.lgjn.inspirationcapsule.api.AudioProcessResult
import com.lgjn.inspirationcapsule.api.DifyApiService
import com.lgjn.inspirationcapsule.data.Inspiration
import com.lgjn.inspirationcapsule.data.InspirationRepository
import com.lgjn.inspirationcapsule.widget.RecordingWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 小组件录音服务（完整状态机版本）：
 *
 * STATE_IDLE      → 麦克风图标，圈不可见，显示「上次录入灵感 X前」
 * STATE_RECORDING → 停止图标，旋转圈可见，状态栏文字隐藏
 * STATE_GENERATING→ 麦克风图标，旋转圈可见，「灵感正在生成中」
 */
class RecordingWidgetService : Service() {

    companion object {
        const val ACTION_START = "com.lgjn.inspirationcapsule.WIDGET_START"
        const val ACTION_STOP  = "com.lgjn.inspirationcapsule.WIDGET_STOP"
        const val CHANNEL_ID   = "widget_recording_channel"
        const val NOTIFICATION_ID = 2001

        // SharedPreferences keys
        const val PREFS_NAME    = "widget_prefs"
        const val KEY_STATE     = "widget_state"
        const val KEY_IS_REC    = "is_recording"   // kept for RecordingWidget.updateAppWidget compat
        const val KEY_LAST_TIME    = "last_inspiration_time"
        const val KEY_LAST_CONTENT = "last_inspiration_content"

        // State values
        const val STATE_IDLE       = "idle"
        const val STATE_RECORDING  = "recording"
        const val STATE_GENERATING = "generating"

        /** MainActivity 监听此广播以实时刷新列表 */
        const val ACTION_INSPIRATION_SAVED = "com.lgjn.inspirationcapsule.INSPIRATION_SAVED"

        /** 将时间戳转为「X秒/分钟/小时/天/月前」 */
        fun formatRelativeTime(timestamp: Long): String {
            val diff = System.currentTimeMillis() - timestamp
            return when {
                diff < 60_000L          -> "${diff / 1_000}秒"
                diff < 3_600_000L       -> "${diff / 60_000}分钟"
                diff < 86_400_000L      -> "${diff / 3_600_000}小时"
                diff < 30L * 86_400_000 -> "${diff / 86_400_000}天"
                else                    -> "${diff / (30L * 86_400_000)}月"
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mediaRecorder: MediaRecorder? = null
    private var currentAudioFile: File? = null

    /** 录音 1 分钟兜底：小组件和三击音量键均走此 Service，统一在此限时 */
    private val recordingTimeoutHandler = Handler(Looper.getMainLooper())
    private val recordingTimeoutRunnable = Runnable {
        if (getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString(KEY_STATE, STATE_IDLE) == STATE_RECORDING) {
            Toast.makeText(applicationContext, "录音已自动停止（超过1分钟）", Toast.LENGTH_LONG).show()
            stopAndProcess()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_STOP  -> stopAndProcess()
            else         -> stopSelf()
        }
        return START_NOT_STICKY
    }

    // ── 开始录音 ────────────────────────────────────────────────

    private fun startRecording() {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val dir = File(cacheDir, "recordings").also { it.mkdirs() }
        currentAudioFile = File(dir, "widget_$stamp.m4a")

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(128000)
            setOutputFile(currentAudioFile!!.absolutePath)
            try {
                prepare()
                start()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(applicationContext, "录音启动失败", Toast.LENGTH_SHORT).show()
                release()
                mediaRecorder = null
                saveState(STATE_IDLE)
                stopSelf()
                return
            }
        }

        saveState(STATE_RECORDING)
        updateAllWidgets(STATE_RECORDING)
        startForeground(NOTIFICATION_ID, buildNotification("正在录音…", "再次点击小组件按钮停止录音"))
        // 启动 1 分钟兜底计时
        recordingTimeoutHandler.removeCallbacks(recordingTimeoutRunnable)
        recordingTimeoutHandler.postDelayed(recordingTimeoutRunnable, 60_000L)
    }

    // ── 停止录音并处理 ──────────────────────────────────────────

    private fun stopAndProcess() {
        // 取消兜底计时（无论是手动停止还是超时触发）
        recordingTimeoutHandler.removeCallbacks(recordingTimeoutRunnable)
        mediaRecorder?.apply {
            try { stop() } catch (_: Exception) {}
            release()
        }
        mediaRecorder = null

        val audioFile = currentAudioFile
        if (audioFile == null || !audioFile.exists() || audioFile.length() == 0L) {
            Toast.makeText(applicationContext, "录音文件无效，请重试", Toast.LENGTH_SHORT).show()
            saveState(STATE_IDLE)
            updateAllWidgets(STATE_IDLE)
            stopSelf()
            return
        }

        // 切换到"生成中"状态
        saveState(STATE_GENERATING)
        updateAllWidgets(STATE_GENERATING)
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification("正在生成灵感…", "AI 处理中，请稍候"))

        // 异步：audio-to-text → chat-messages
        serviceScope.launch {
            val result = DifyApiService.processAudio(audioFile)
            result.onSuccess { audioResult ->
                val repo = InspirationRepository(applicationContext)
                repo.insert(Inspiration(
                    content = audioResult.content,
                    transcript = audioResult.transcript,
                    createdAt = System.currentTimeMillis()
                ))
                // 保存本次录入时间和内容（供小组件状态文字使用）
                getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                    .putLong(KEY_LAST_TIME, System.currentTimeMillis())
                    .putString(KEY_LAST_CONTENT, audioResult.content)
                    .apply()
                vibrateSuccess()                   // 双震：提示保存成功
                showSaveOverlay(audioResult.content) // 底部居中飘字：灵感保存：{content}
                Toast.makeText(applicationContext, "灵感已保存！", Toast.LENGTH_SHORT).show()
                // 通知 MainActivity 实时刷新灵感列表
                sendBroadcast(Intent(ACTION_INSPIRATION_SAVED).setPackage(packageName))
            }
            result.onFailure { error ->
                Toast.makeText(
                    applicationContext,
                    "处理失败：${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
            // 无论成败，回到空闲状态
            saveState(STATE_IDLE)
            updateAllWidgets(STATE_IDLE)
            stopSelf()
        }
    }

    // ── 小组件 UI 刷新 ──────────────────────────────────────────

    private fun updateAllWidgets(state: String) {
        val manager = AppWidgetManager.getInstance(this)
        val ids = manager.getAppWidgetIds(ComponentName(this, RecordingWidget::class.java))
        if (ids.isEmpty()) return

        val views = buildWidgetViews(state)
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    /**
     * 根据 state 构建 RemoteViews：
     *   STATE_RECORDING  → 停止图标，旋转圈，点击→STOP_CLICK，状态文字=""
     *   STATE_GENERATING → 麦克风，旋转圈，点击→START_CLICK，状态文字="灵感正在生成中"
     *   STATE_IDLE       → 麦克风，无圈，点击→START_CLICK，状态文字="上次录入灵感 X前"
     */
    private fun buildWidgetViews(state: String): RemoteViews {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastContent = prefs.getString(KEY_LAST_CONTENT, "") ?: ""

        val views = RemoteViews(packageName, R.layout.widget_recording)

        when (state) {
            STATE_RECORDING -> {
                views.setImageViewResource(R.id.widgetRecordBtn, R.drawable.ic_stop)
                views.setTextViewText(R.id.widgetLabel, "点击停止")
                views.setViewVisibility(R.id.widgetRecordingRing, View.VISIBLE)
                views.setTextViewText(R.id.widgetStatus, "")
                views.setTextViewText(R.id.widgetLastContent, "")
                views.setOnClickPendingIntent(
                    R.id.widgetRecordBtn,
                    makeBroadcastPi(RecordingWidget.ACTION_STOP_CLICK, requestCode = 1)
                )
            }
            STATE_GENERATING -> {
                views.setImageViewResource(R.id.widgetRecordBtn, R.drawable.ic_mic)
                views.setTextViewText(R.id.widgetLabel, "灵感胶囊")
                views.setViewVisibility(R.id.widgetRecordingRing, View.VISIBLE)
                views.setTextViewText(R.id.widgetStatus, "灵感正在生成中")
                views.setTextViewText(R.id.widgetLastContent, "")
                views.setOnClickPendingIntent(
                    R.id.widgetRecordBtn,
                    makeBroadcastPi(RecordingWidget.ACTION_START_CLICK, requestCode = 0)
                )
            }
            else -> { // STATE_IDLE
                views.setImageViewResource(R.id.widgetRecordBtn, R.drawable.ic_mic)
                views.setTextViewText(R.id.widgetLabel, "灵感胶囊")
                views.setViewVisibility(R.id.widgetRecordingRing, View.GONE)
                if (lastContent.isNotBlank()) {
                    views.setTextViewText(R.id.widgetStatus, "上一条灵感已录入")
                    views.setTextViewText(R.id.widgetLastContent, lastContent)
                } else {
                    views.setTextViewText(R.id.widgetStatus, "")
                    views.setTextViewText(R.id.widgetLastContent, "")
                }
                views.setOnClickPendingIntent(
                    R.id.widgetRecordBtn,
                    makeBroadcastPi(RecordingWidget.ACTION_START_CLICK, requestCode = 0)
                )
            }
        }
        return views
    }

    /** 保存成功双震：150ms 震 → 100ms 停 → 150ms 震（兼容 Android 12+ VibratorManager） */
    private fun vibrateSuccess() {
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 150), -1)
        vibrateEffect(effect)
    }

    private fun vibrateEffect(effect: VibrationEffect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)
                ?.defaultVibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            (getSystemService(VIBRATOR_SERVICE) as? Vibrator)?.vibrate(effect)
        }
    }

    /**
     * 在屏幕底部居中显示「灵感保存：{content}」悬浮提示，3.5 秒后自动消失。
     * 需要 SYSTEM_ALERT_WINDOW 权限（FloatingWindowService 引导用户授权）。
     */
    private fun showSaveOverlay(content: String) {
        if (!Settings.canDrawOverlays(this)) return  // 未授权则跳过，不影响主流程

        val wm = getSystemService(WindowManager::class.java) ?: return

        // 截断内容，最多显示 30 字
        val displayText = "灵感保存：" +
            if (content.length > 30) content.substring(0, 30) + "…" else content

        val density = resources.displayMetrics.density

        val tv = TextView(this).apply {
            text = displayText
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(0xFFFFFFFF.toInt())
            val h = (18 * density).toInt()
            val v = (12 * density).toInt()
            setPadding(h * 3, v, h * 3, v)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(0xEE1A0840.toInt())   // 深紫，约 93% 不透明
                cornerRadius = 64f * density
            }
            elevation = 24f
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = (160 * density).toInt()   // 距底部 160dp
        }

        wm.addView(tv, params)

        // 3.5 秒后移除
        Handler(Looper.getMainLooper()).postDelayed({
            try { wm.removeView(tv) } catch (_: Exception) {}
        }, 3500L)
    }

    private fun makeBroadcastPi(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            this,
            requestCode,
            Intent(this, RecordingWidget::class.java).apply { this.action = action },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    // ── 持久化状态 ──────────────────────────────────────────────

    private fun saveState(state: String) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_STATE, state)
            .putBoolean(KEY_IS_REC, state == STATE_RECORDING)
            .apply()
    }

    // ── 通知 ────────────────────────────────────────────────────

    private fun buildNotification(title: String, text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_mic)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "小组件录音",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    // ── 生命周期 ────────────────────────────────────────────────

    override fun onDestroy() {
        super.onDestroy()
        recordingTimeoutHandler.removeCallbacks(recordingTimeoutRunnable)
        serviceScope.cancel()
        mediaRecorder?.apply {
            try { stop() } catch (_: Exception) {}
            release()
        }
        mediaRecorder = null
    }
}
