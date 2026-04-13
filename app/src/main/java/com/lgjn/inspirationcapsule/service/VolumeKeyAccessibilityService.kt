package com.lgjn.inspirationcapsule.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * 无障碍服务：三击音量上键快速触发录音
 *
 * 亮屏/锁屏状态下，600ms 窗口内连续三击音量上键：
 *   空闲  → 震动 → 开始录音
 *   录音中 → 震动 → 停止并 AI 处理
 *   生成中 → 静默忽略
 *
 * 音量副作用：第 1、2 击不消费（音量各 +1），第 3 击消费（音量不变）。
 */
class VolumeKeyAccessibilityService : AccessibilityService() {

    companion object {
        private const val TRIPLE_CLICK_WINDOW_MS = 600L
    }

    private var clickCount = 0
    private val handler = Handler(Looper.getMainLooper())
    private val resetRunnable = Runnable { clickCount = 0 }

    // ── 服务生命周期 ────────────────────────────────────────────

    override fun onServiceConnected() {
        serviceInfo = AccessibilityServiceInfo().apply {
            flags = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            notificationTimeout = 100
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    // ── 按键拦截 ────────────────────────────────────────────────

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_VOLUME_UP) return false
        if (event.action != KeyEvent.ACTION_DOWN) return false

        handler.removeCallbacks(resetRunnable)
        clickCount++

        return if (clickCount >= 3) {
            clickCount = 0
            handleTripleClick()
            true   // 消费第 3 击，音量不再变化
        } else {
            handler.postDelayed(resetRunnable, TRIPLE_CLICK_WINDOW_MS)
            false  // 放行第 1、2 击
        }
    }

    // ── 三击处理 ────────────────────────────────────────────────

    private fun handleTripleClick() {
        val state = getSharedPreferences(RecordingWidgetService.PREFS_NAME, MODE_PRIVATE)
            .getString(RecordingWidgetService.KEY_STATE, RecordingWidgetService.STATE_IDLE)
            ?: RecordingWidgetService.STATE_IDLE

        when (state) {
            RecordingWidgetService.STATE_IDLE -> {
                vibrate(120L)
                startForegroundService(
                    Intent(this, RecordingWidgetService::class.java).apply {
                        action = RecordingWidgetService.ACTION_START
                    }
                )
            }
            RecordingWidgetService.STATE_RECORDING -> {
                vibrate(120L)
                startService(
                    Intent(this, RecordingWidgetService::class.java).apply {
                        action = RecordingWidgetService.ACTION_STOP
                    }
                )
            }
            RecordingWidgetService.STATE_GENERATING -> { /* 静默忽略 */ }
        }
    }

    // ── 震动（兼容 Android 12+ VibratorManager）──────────────────

    private fun vibrate(durationMs: Long) {
        val effect = VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)
                ?.defaultVibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            (getSystemService(VIBRATOR_SERVICE) as? Vibrator)?.vibrate(effect)
        }
    }
}
