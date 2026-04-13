package com.lgjn.inspirationcapsule.adapter

import android.annotation.SuppressLint
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lgjn.inspirationcapsule.R
import com.lgjn.inspirationcapsule.data.Inspiration
import kotlin.math.abs

class InspirationAdapter(
    private var items: MutableList<Inspiration> = mutableListOf(),
    private val onCardActivated: (position: Int, inspiration: Inspiration) -> Unit,
    private val onCardSwiped: (inspiration: Inspiration, status: String) -> Unit,
    private val onItemClick: (Int) -> Unit = {},
    /** 滑动进度回调：dy 当前位移，maxDy 触发阈值参考值 */
    private val onSwipeProgress: (dy: Float, maxDy: Float) -> Unit = { _, _ -> },
    /** 滑动结束（抬手/取消）回调 */
    private val onSwipeEnd: () -> Unit = {}
) : RecyclerView.Adapter<InspirationAdapter.ViewHolder>() {

    var activatedPosition: Int = -1

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvContent: TextView = view.findViewById(R.id.tvContent)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val swipeUpHint: LinearLayout = view.findViewById(R.id.swipeUpHint)
        val swipeDownHint: LinearLayout = view.findViewById(R.id.swipeDownHint)

        private var startY = 0f
        private var startX = 0f
        private var isTracking = false
        private var isVertical = false
        private var wasOverThreshold = false

        private val gestureDetector = GestureDetector(
            view.context,
            object : GestureDetector.SimpleOnGestureListener() {
                // onDown 必须返回 true，GestureDetector 才能正确处理后续事件
                override fun onDown(e: MotionEvent): Boolean = true

                override fun onLongPress(e: MotionEvent) {
                    val pos = bindingAdapterPosition
                    if (pos == RecyclerView.NO_POSITION || pos >= items.size) return
                    // 触发震动反馈
                    itemView.performHapticFeedback(
                        android.view.HapticFeedbackConstants.LONG_PRESS
                    )
                    onCardActivated(pos, items[pos])
                }

                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) onItemClick(pos)
                    return true
                }
            }
        )

        @SuppressLint("ClickableViewAccessibility")
        fun bindTouchListener() {
            itemView.setOnTouchListener { v, event ->
                // 激活态：禁用长按检测，但保留竖向滑动（完成/丢弃）
                if (activatedPosition != -1) {
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            startX = event.rawX
                            startY = event.rawY
                            isTracking = false
                            isVertical = false
                            wasOverThreshold = false
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dx = event.rawX - startX
                            val dy = event.rawY - startY
                            if (!isTracking && (abs(dx) > 8 || abs(dy) > 8)) {
                                isTracking = true
                                isVertical = abs(dy) > abs(dx) * 1.1f
                            }
                            if (isVertical) {
                                v.parent?.requestDisallowInterceptTouchEvent(true)
                                applySwipeTransform(dy)
                            }
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            v.parent?.requestDisallowInterceptTouchEvent(false)
                            val dy = event.rawY - startY
                            if (isVertical) {
                                val threshold = itemView.height * 0.22f
                                val pos = bindingAdapterPosition
                                when {
                                    dy < -threshold && pos != RecyclerView.NO_POSITION ->
                                        flyOutCard(up = true, pos)
                                    dy > threshold && pos != RecyclerView.NO_POSITION ->
                                        flyOutCard(up = false, pos)
                                    else -> springBack()
                                }
                            }
                            resetHints()
                            isTracking = false
                            isVertical = false
                            wasOverThreshold = false
                        }
                    }
                    return@setOnTouchListener true
                }

                // 先给 GestureDetector 处理（用于长按 / 点击识别）
                gestureDetector.onTouchEvent(event)

                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.rawX
                        startY = event.rawY
                        isTracking = false
                        isVertical = false
                        wasOverThreshold = false
                        true  // 必须返回 true，否则 MOVE / UP 不会再来
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - startX
                        val dy = event.rawY - startY
                        // 超过触发阈值后判断方向（宽松比例，方便竖向触发）
                        if (!isTracking && (abs(dx) > 8 || abs(dy) > 8)) {
                            isTracking = true
                            isVertical = abs(dy) > abs(dx) * 1.1f
                        }
                        if (isVertical) {
                            // 阻止 ViewPager2 拦截竖向滑动
                            v.parent?.requestDisallowInterceptTouchEvent(true)
                            applySwipeTransform(dy)
                        }
                        true
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.parent?.requestDisallowInterceptTouchEvent(false)
                        val dy = event.rawY - startY
                        if (isVertical) {
                            val threshold = itemView.height * 0.22f
                            val pos = bindingAdapterPosition
                            when {
                                dy < -threshold && pos != RecyclerView.NO_POSITION ->
                                    flyOutCard(up = true, pos)
                                dy > threshold && pos != RecyclerView.NO_POSITION ->
                                    flyOutCard(up = false, pos)
                                else -> springBack()
                            }
                        } else if (event.actionMasked == MotionEvent.ACTION_CANCEL) {
                            springBack()
                        }
                        onSwipeEnd()
                        isTracking = false
                        isVertical = false
                        wasOverThreshold = false
                        true
                    }

                    else -> false
                }
            }
        }

        /** 应用滑动变换：位移 + 缩放 + 微旋转 + 临界点震动 */
        private fun applySwipeTransform(dy: Float) {
            val threshold = (itemView.height * 0.22f).coerceAtLeast(1f)
            val progress = (abs(dy) / threshold).coerceIn(0f, 1.2f)
            // 跟手位移：系数 0.6f，跟手感更强
            itemView.translationY = dy * 0.6f
            // 微缩小：随进度 1.0 → 0.92
            val scale = 1f - 0.08f * progress.coerceAtMost(1f)
            itemView.scaleX = scale
            itemView.scaleY = scale
            // 轻微旋转：±3° 摆动
            itemView.rotation = (dy / threshold).coerceIn(-1f, 1f) * 3f
            // 临界点震动反馈（仅跨越阈值瞬间触发一次）
            val overThreshold = progress >= 1f
            if (overThreshold && !wasOverThreshold) {
                val feedback = if (android.os.Build.VERSION.SDK_INT >= 30)
                    android.view.HapticFeedbackConstants.CONFIRM
                else
                    android.view.HapticFeedbackConstants.LONG_PRESS
                itemView.performHapticFeedback(feedback)
            }
            wasOverThreshold = overThreshold
            // 通知 MainActivity 更新顶部/底部按钮
            onSwipeProgress(dy, threshold)
        }

        private fun updateSwipeHints(dy: Float) {
            val maxDy = (itemView.height * 0.3f).coerceAtLeast(1f)
            when {
                dy < 0 -> {
                    swipeUpHint.alpha = (-dy / maxDy).coerceIn(0f, 1f)
                    swipeDownHint.alpha = 0f
                }
                dy > 0 -> {
                    swipeDownHint.alpha = (dy / maxDy).coerceIn(0f, 1f)
                    swipeUpHint.alpha = 0f
                }
                else -> {
                    swipeUpHint.alpha = 0f
                    swipeDownHint.alpha = 0f
                }
            }
        }

        private fun resetHints() {
            swipeUpHint.animate().alpha(0f).setDuration(150).start()
            swipeDownHint.animate().alpha(0f).setDuration(150).start()
        }

        private fun flyOutCard(up: Boolean, adapterPos: Int) {
            val targetY = if (up) -itemView.height.toFloat() * 2f
                         else itemView.height.toFloat() * 2f
            val status = if (up) Inspiration.STATUS_COMPLETED else Inspiration.STATUS_DISCARDED
            // 在动画开始前立即捕获 inspiration，防止 items 在动画结束前被更新
            if (adapterPos >= items.size) return
            val inspiration = items[adapterPos]

            itemView.animate()
                .translationY(targetY)
                .alpha(0f)
                .setDuration(260)
                .setInterpolator(AccelerateInterpolator())
                .withEndAction {
                    // 重置视图状态（可能被 onBindViewHolder 复用）
                    itemView.translationY = 0f
                    itemView.alpha = 1f
                    itemView.scaleX = 1f
                    itemView.scaleY = 1f
                    itemView.rotation = 0f
                    swipeUpHint.alpha = 0f
                    swipeDownHint.alpha = 0f
                    onCardSwiped(inspiration, status)
                }
                .start()
        }

        private fun springBack() {
            itemView.animate()
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .rotation(0f)
                .setDuration(220)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inspiration, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvContent.text = item.content
        holder.tvDate.text = item.formattedDate()
        // 确保每次绑定时视图状态干净
        holder.swipeUpHint.alpha = 0f
        holder.swipeDownHint.alpha = 0f
        holder.itemView.translationY = 0f
        holder.itemView.alpha = 1f
        holder.itemView.scaleX = 1f
        holder.itemView.scaleY = 1f
        holder.itemView.rotation = 0f
        holder.bindTouchListener()
    }

    override fun getItemCount(): Int = items.size

    /**
     * 全量替换数据（主页刷新用）
     * 使用精细 diff 避免 notifyDataSetChanged() 在 ViewPager2 上不稳定
     */
    /**
     * 全量替换数据，使用 notifyDataSetChanged 是 ViewPager2 最安全的刷新方式：
     * 不会因为不知道哪一项被删除而通知错位置，ViewPager2 自动保持当前页或调整到合法页。
     */
    fun updateData(newItems: List<Inspiration>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
