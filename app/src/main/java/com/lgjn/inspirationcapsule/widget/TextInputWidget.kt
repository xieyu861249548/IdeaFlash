package com.lgjn.inspirationcapsule.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.lgjn.inspirationcapsule.R
import com.lgjn.inspirationcapsule.TextInputActivity

/**
 * 文案输入小组件 Provider（2×3）
 *
 * 点击"直接保存"或"✨ AI提炼"均打开 TextInputActivity，
 * TextInputActivity 以透明 Dialog 形式展示输入框并处理保存逻辑。
 */
class TextInputWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { updateAppWidget(context, appWidgetManager, it) }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_text_input)

            // 两个按钮均打开同一个 TextInputActivity（内部有直接保存和 AI 提炼两个选项）
            val openIntent = Intent(context, TextInputActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val directPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId * 10 + 1,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val aiPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId * 10 + 2,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 输入区域、两个图标按钮均可打开 TextInputActivity
            views.setOnClickPendingIntent(R.id.widgetInputArea, directPendingIntent)
            views.setOnClickPendingIntent(R.id.widgetBtnSaveDirect, directPendingIntent)
            views.setOnClickPendingIntent(R.id.widgetBtnAI, aiPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
