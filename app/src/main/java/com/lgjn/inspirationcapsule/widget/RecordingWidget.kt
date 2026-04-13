package com.lgjn.inspirationcapsule.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.lgjn.inspirationcapsule.R
import com.lgjn.inspirationcapsule.service.RecordingWidgetService
import com.lgjn.inspirationcapsule.service.RecordingWidgetService.Companion.KEY_LAST_CONTENT
import com.lgjn.inspirationcapsule.service.RecordingWidgetService.Companion.KEY_STATE
import com.lgjn.inspirationcapsule.service.RecordingWidgetService.Companion.PREFS_NAME
import com.lgjn.inspirationcapsule.service.RecordingWidgetService.Companion.STATE_GENERATING
import com.lgjn.inspirationcapsule.service.RecordingWidgetService.Companion.STATE_IDLE
import com.lgjn.inspirationcapsule.service.RecordingWidgetService.Companion.STATE_RECORDING

/**
 * 录音小组件 Provider（2×2）
 *
 * 点击流程：
 *   空闲 → ACTION_START_CLICK → onReceive → startForegroundService(ACTION_START)
 *   录音中 → ACTION_STOP_CLICK → onReceive → startService(ACTION_STOP)
 *
 * 小组件图标 / 状态文字 / 旋转圈 由 RecordingWidgetService 主动刷新，
 * onUpdate 仅负责初始渲染（从 SharedPreferences 读取持久状态）。
 */
class RecordingWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_START_CLICK = "com.lgjn.inspirationcapsule.WIDGET_START_CLICK"
        const val ACTION_STOP_CLICK  = "com.lgjn.inspirationcapsule.WIDGET_STOP_CLICK"

        /**
         * 从 SharedPreferences 读取当前状态并构建对应的 RemoteViews。
         * 在 onUpdate（系统触发）和启动器重建时使用。
         */
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefs       = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val state       = prefs.getString(KEY_STATE, STATE_IDLE) ?: STATE_IDLE
            val lastContent = prefs.getString(KEY_LAST_CONTENT, "") ?: ""

            val views = RemoteViews(context.packageName, R.layout.widget_recording)

            when (state) {
                STATE_RECORDING -> {
                    views.setImageViewResource(R.id.widgetRecordBtn, R.drawable.ic_stop)
                    views.setTextViewText(R.id.widgetLabel, "点击停止")
                    views.setViewVisibility(R.id.widgetRecordingRing, View.VISIBLE)
                    views.setTextViewText(R.id.widgetStatus, "")
                    views.setTextViewText(R.id.widgetLastContent, "")
                    views.setOnClickPendingIntent(
                        R.id.widgetRecordBtn,
                        makePi(context, ACTION_STOP_CLICK, requestCode = 1)
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
                        makePi(context, ACTION_START_CLICK, requestCode = 0)
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
                        makePi(context, ACTION_START_CLICK, requestCode = 0)
                    )
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun makePi(context: Context, action: String, requestCode: Int): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                requestCode,
                Intent(context, RecordingWidget::class.java).apply { this.action = action },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { updateAppWidget(context, appWidgetManager, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_START_CLICK -> context.startForegroundService(
                Intent(context, RecordingWidgetService::class.java).apply {
                    action = RecordingWidgetService.ACTION_START
                }
            )
            ACTION_STOP_CLICK -> context.startService(
                Intent(context, RecordingWidgetService::class.java).apply {
                    action = RecordingWidgetService.ACTION_STOP
                }
            )
        }
    }
}
