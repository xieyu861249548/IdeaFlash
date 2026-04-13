package com.lgjn.inspirationcapsule.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.lgjn.inspirationcapsule.R
import kotlin.math.abs

class FloatingWindowService : Service() {

    private val windowManager by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }
    private val floatingViews = mutableListOf<View>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val id      = it.getLongExtra("inspiration_id", -1L)
            val content = it.getStringExtra("inspiration_content") ?: ""
            if (id != -1L && content.isNotEmpty()) createFloatingView(content)
        }
        return START_NOT_STICKY
    }

    private fun screenWidth(): Int {
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(dm)
        return dm.widthPixels
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun createFloatingView(content: String) {
        val root = LayoutInflater.from(this)
            .inflate(R.layout.layout_floating_inspiration, null)

        // 用 View 类型接收，避免 LinearLayout / ConstraintLayout 强转崩溃
        val expandedCard : View     = root.findViewById(R.id.expandedCard)
        val collapsedBar : View     = root.findViewById(R.id.collapsedBar)
        val tvPreview    : TextView = root.findViewById(R.id.floatPreview)
        val tvContent    : TextView = root.findViewById(R.id.floatContent)
        val btnClose     : View     = root.findViewById(R.id.floatClose)
        val btnCollapse  : View     = root.findViewById(R.id.btnCollapse)
        val btnExpand    : View     = root.findViewById(R.id.btnExpand)

        tvContent.text = content
        tvPreview.text = content

        val expandedW  = dp(220)
        val collapsedW = dp(100)

        val params = WindowManager.LayoutParams(
            expandedW,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = 200 + floatingViews.size * 30
        }

        var isCollapsed = false

        fun snapToEdge() {
            val sw = screenWidth()
            params.x = if (params.x + collapsedW / 2 > sw / 2) sw - collapsedW else 0
            runCatching { windowManager.updateViewLayout(root, params) }
        }

        fun collapse() {
            isCollapsed = true
            expandedCard.visibility = View.GONE
            collapsedBar.visibility = View.VISIBLE
            params.width = collapsedW
            snapToEdge()
        }

        fun expand() {
            isCollapsed = false
            expandedCard.visibility = View.VISIBLE
            collapsedBar.visibility = View.GONE
            params.width = expandedW
            val sw = screenWidth()
            if (params.x + expandedW > sw) params.x = sw - expandedW
            runCatching { windowManager.updateViewLayout(root, params) }
        }

        fun removeThis() {
            runCatching { windowManager.removeView(root) }
            floatingViews.remove(root)
            if (floatingViews.isEmpty()) stopSelf()
        }

        // 按钮点击——直接绑定，不经过根视图的触摸拦截
        btnCollapse.setOnClickListener { collapse() }
        btnExpand.setOnClickListener   { expand() }
        btnClose.setOnClickListener    { removeThis() }

        // 拖拽：监听器只放在内容文字区域，不影响按钮区域的点击
        var lastX = 0f; var lastY = 0f
        var sx    = 0;  var sy    = 0
        var drag  = false

        tvContent.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = e.rawX; lastY = e.rawY
                    sx = params.x;  sy = params.y
                    drag = false; true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - lastX; val dy = e.rawY - lastY
                    if (abs(dx) > 8 || abs(dy) > 8) drag = true
                    if (drag) {
                        params.x = (sx + dx).toInt().coerceAtLeast(0)
                        params.y = (sy + dy).toInt().coerceAtLeast(0)
                        runCatching { windowManager.updateViewLayout(root, params) }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (drag && isCollapsed) snapToEdge()
                    true
                }
                else -> false
            }
        }

        // 收起态同样支持拖拽
        tvPreview.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = e.rawX; lastY = e.rawY
                    sx = params.x;  sy = params.y
                    drag = false; true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - lastX; val dy = e.rawY - lastY
                    if (abs(dx) > 8 || abs(dy) > 8) drag = true
                    if (drag) {
                        params.x = (sx + dx).toInt().coerceAtLeast(0)
                        params.y = (sy + dy).toInt().coerceAtLeast(0)
                        runCatching { windowManager.updateViewLayout(root, params) }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (drag) snapToEdge()
                    true
                }
                else -> false
            }
        }

        windowManager.addView(root, params)
        floatingViews.add(root)
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingViews.forEach { runCatching { windowManager.removeView(it) } }
        floatingViews.clear()
    }
}
