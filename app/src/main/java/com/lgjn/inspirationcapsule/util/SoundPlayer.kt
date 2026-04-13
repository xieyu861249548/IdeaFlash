package com.lgjn.inspirationcapsule.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator

/**
 * 卡片交互音效播放器，遵循系统静音。
 */
class SoundPlayer(context: Context) {

    private val audioManager = context.getSystemService(AudioManager::class.java)

    /** 左右切换：轻微点击音 */
    fun playPageSwitch() {
        playTone(ToneGenerator.TONE_PROP_BEEP, 35, 45)
    }

    /** 向上滑（完成）：清脆叮一声 */
    fun playComplete() {
        playTone(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, 65, 90)
    }

    /** 向下滑（丢弃）：低沉嗡一声 */
    fun playDiscard() {
        playTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 55, 70)
    }

    fun release() {
        // SoundPool 如有需要在此 release，当前只用 ToneGenerator（自动回收）
    }

    private fun playTone(toneType: Int, volume: Int, durationMs: Int) {
        if (!isSoundOn()) return
        try {
            val tg = ToneGenerator(AudioManager.STREAM_SYSTEM, volume)
            tg.startTone(toneType, durationMs)
            // 在音调时长后释放，避免资源泄漏
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try { tg.release() } catch (_: Exception) {}
            }, (durationMs + 50).toLong())
        } catch (_: Exception) {
            // ToneGenerator 在部分设备上可能抛出 RuntimeException，直接忽略
        }
    }

    private fun isSoundOn(): Boolean =
        audioManager?.ringerMode == AudioManager.RINGER_MODE_NORMAL
}
