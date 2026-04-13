package com.lgjn.inspirationcapsule

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.ColorDrawable
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.viewpager2.widget.ViewPager2
import com.lgjn.inspirationcapsule.adapter.InspirationAdapter
import com.lgjn.inspirationcapsule.data.Inspiration
import com.lgjn.inspirationcapsule.databinding.ActivityMainBinding
import com.lgjn.inspirationcapsule.service.FloatingWindowService
import com.lgjn.inspirationcapsule.service.RecordingWidgetService
import com.lgjn.inspirationcapsule.service.VolumeKeyAccessibilityService
import com.lgjn.inspirationcapsule.util.SoundPlayer
import com.lgjn.inspirationcapsule.viewmodel.InspirationViewModel
import com.lgjn.inspirationcapsule.viewmodel.ProcessingState
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: InspirationViewModel by viewModels()
    private lateinit var adapter: InspirationAdapter
    private lateinit var soundPlayer: SoundPlayer

    private var mediaRecorder: MediaRecorder? = null
    private var currentAudioFile: File? = null
    private var isRecording = false

    /** 当前激活的灵感（长按后设置，退出激活态清空） */
    private var activatedInspiration: Inspiration? = null

    /** PastInspirationsFragment instance — created once, kept in drawer */
    private var pastFragment: PastInspirationsFragment? = null

    /** 上一次的灵感 ID 列表，用于判断数据是否真正变化（避免无意义的 pop-in 动画） */
    private var lastInspirationIds: List<Long> = emptyList()

    /** 当前卡片是否正在显示原文（翻转状态） */
    private var isShowingTranscript = false

    /** 录音最长时长兜底：60 秒自动停止 */
    private val recordingTimeoutHandler = Handler(Looper.getMainLooper())
    private val recordingTimeoutRunnable = Runnable {
        if (isRecording) {
            stopRecording()
            binding.tvRecordHint.text = "点击录音"
            binding.recordingWave.visibility = View.GONE
            Toast.makeText(this, "录音已自动停止（超过1分钟）", Toast.LENGTH_LONG).show()
        }
    }

    /** 监听来自 RecordingWidgetService 的保存成功广播，实时刷新列表 */
    private val inspirationSavedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            viewModel.loadInspirations()
            binding.viewPager.postDelayed({ binding.viewPager.currentItem = 0 }, 300)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (!permissions.values.all { it })
            Toast.makeText(this, "需要录音权限才能使用此功能", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 统一状态栏颜色为渐变顶部色
        window.statusBarColor = android.graphics.Color.parseColor("#2D1B6B")

        soundPlayer = SoundPlayer(this)

        setupViewPager()
        setupRecordButton()
        setupManualAddButton()
        setupAccessibilityBanner()
        setupActivatedState()
        setupDrawer()
        observeViewModel()
        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadInspirations()
        val filter = IntentFilter(RecordingWidgetService.ACTION_INSPIRATION_SAVED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(inspirationSavedReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(inspirationSavedReceiver, filter)
        }
        binding.btnEnableAccessibility.visibility =
            if (isAccessibilityServiceEnabled()) View.GONE else View.VISIBLE
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(inspirationSavedReceiver) } catch (_: Exception) {}
    }

    // ──────────────────── ViewPager ────────────────────

    private fun setupViewPager() {
        adapter = InspirationAdapter(
            onCardActivated = { pos, inspiration -> activateCard(pos, inspiration) },
            onCardSwiped = { inspiration, status ->
                // 音效已禁用
                viewModel.updateStatus(inspiration.id, status)
                hideSwipeHints()
            },
            onItemClick = { pos ->
                if (adapter.activatedPosition != -1) {
                    deactivateCard()
                } else if (binding.viewPager.currentItem != pos) {
                    binding.viewPager.setCurrentItem(pos, true)
                }
            },
            onSwipeProgress = { dy, maxDy ->
                updateSwipeHints(dy, maxDy)
            },
            onSwipeEnd = {
                hideSwipeHints()
            }
        )

        binding.viewPager.adapter = adapter
        binding.viewPager.offscreenPageLimit = 3

        val pageMarginPx = resources.getDimensionPixelOffset(R.dimen.page_margin)
        val pagerWidth = resources.getDimensionPixelOffset(R.dimen.pager_width)
        val screenWidth = resources.displayMetrics.widthPixels
        val offsetPx = screenWidth - pageMarginPx - pagerWidth
        val cameraDistPx = resources.displayMetrics.density * 8000f

        // 禁用安卓原生 overscroll 边缘效果（不符合 iOS 风格）
        (binding.viewPager.getChildAt(0) as? androidx.recyclerview.widget.RecyclerView)
            ?.overScrollMode = View.OVER_SCROLL_NEVER

        binding.viewPager.setPageTransformer { page, position ->
            val absPos = Math.abs(position)
            // position > 1.0 部分做 20% 阻尼，防止边缘 overscroll 时邻居卡片被推出屏幕
            val dampedAbs = if (absPos <= 1f) absPos else 1f + (absPos - 1f) * 0.2f
            val dampedPos = if (position >= 0) dampedAbs else -dampedAbs

            page.cameraDistance = cameraDistPx
            page.rotationY = dampedPos * -18f
            val scale = (1f - dampedAbs * 0.14f).coerceAtLeast(0.70f)
            page.scaleX = scale
            page.scaleY = scale
            page.alpha = when {
                dampedAbs >= 2.2f -> 0f
                else -> (1f - dampedAbs * 0.35f).coerceAtLeast(0f)
            }
            page.translationZ = (1f - dampedAbs) * 24f
            page.translationX = -dampedPos * offsetPx
        }

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position)
                // 翻页音效已禁用
            }
        })
    }

    /** 数据刷新后对当前页播放轻微 pop-in 动画（填充卡片的视觉补偿） */
    private fun playCardPopIn() {
        val rv = binding.viewPager.getChildAt(0) as? androidx.recyclerview.widget.RecyclerView ?: return
        val currentItem = binding.viewPager.currentItem
        for (i in 0 until rv.childCount) {
            val child = rv.getChildAt(i)
            val pos = rv.getChildAdapterPosition(child)
            if (pos == currentItem && pos != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                // 从当前 scale 的 0.92x 开始弹到实际位置（OvershootInterpolator 产生弹性）
                val originalScaleX = child.scaleX
                val originalScaleY = child.scaleY
                child.scaleX = originalScaleX * 0.92f
                child.scaleY = originalScaleY * 0.92f
                child.animate()
                    .scaleX(originalScaleX)
                    .scaleY(originalScaleY)
                    .setDuration(280)
                    .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
                    .start()
                break
            }
        }
    }

    /** 激活态：淡出/淡入相邻卡片（不包含当前页） */
    private fun fadeSiblingPages(hide: Boolean) {
        val rv = binding.viewPager.getChildAt(0) as? androidx.recyclerview.widget.RecyclerView ?: return
        val currentItem = binding.viewPager.currentItem
        val targetAlpha = if (hide) 0f else 1f
        for (i in 0 until rv.childCount) {
            val child = rv.getChildAt(i)
            val pos = rv.getChildAdapterPosition(child)
            if (pos != currentItem && pos != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                child.animate().alpha(targetAlpha).setDuration(200).start()
            }
        }
    }

    /** 滑动进度提示：复用激活态按钮显示在顶部/底部 */
    private fun updateSwipeHints(dy: Float, maxDy: Float) {
        val rawProgress = (kotlin.math.abs(dy) / maxDy).coerceIn(0f, 1.2f)
        val atCritical = rawProgress >= 0.95f
        val isActivated = adapter.activatedPosition != -1
        // 线性段：0~0.85 对应 scale 0.85~1.0；临界段：pop 到 1.15x
        val scale = when {
            atCritical -> 1.15f
            else -> 0.85f + 0.15f * (rawProgress / 0.95f).coerceIn(0f, 1f)
        }
        // 激活态：按钮常驻 alpha=1.0；非激活态：随进度淡入
        val alpha = if (isActivated) 1f else rawProgress.coerceAtMost(1f)
        if (dy < 0) {
            // 上滑 → 完成
            binding.activatedCompleteBtn.apply {
                visibility = View.VISIBLE
                this.alpha = alpha
                translationZ = 40f
                scaleX = scale
                scaleY = scale
                text = if (atCritical) "✓ 松手完成" else "✓ 完成"
            }
            // 激活态下反向按钮保持不变；非激活态下隐藏
            if (!isActivated) {
                binding.activatedDiscardBtn.alpha = 0f
                binding.activatedDiscardBtn.visibility = View.GONE
            }
        } else if (dy > 0) {
            // 下滑 → 丢弃
            binding.activatedDiscardBtn.apply {
                visibility = View.VISIBLE
                this.alpha = alpha
                translationZ = 40f
                scaleX = scale
                scaleY = scale
                text = if (atCritical) "✕ 松手丢弃" else "✕ 丢弃"
            }
            if (!isActivated) {
                binding.activatedCompleteBtn.alpha = 0f
                binding.activatedCompleteBtn.visibility = View.GONE
            }
        }
    }

    /** 滑动结束：隐藏提示并重置文案 */
    private fun hideSwipeHints() {
        val isActivated = adapter.activatedPosition != -1
        if (isActivated) {
            // 激活态：按钮常驻，只重置 scale 和文案
            binding.activatedCompleteBtn.animate()
                .scaleX(1f).scaleY(1f).setDuration(150)
                .withEndAction { binding.activatedCompleteBtn.text = "✓ 完成" }
                .start()
            binding.activatedDiscardBtn.animate()
                .scaleX(1f).scaleY(1f).setDuration(150)
                .withEndAction { binding.activatedDiscardBtn.text = "✕ 丢弃" }
                .start()
            return
        }
        binding.activatedCompleteBtn.animate()
            .alpha(0f).setDuration(150)
            .withEndAction {
                if (adapter.activatedPosition == -1) {
                    binding.activatedCompleteBtn.visibility = View.GONE
                    binding.activatedCompleteBtn.text = "✓ 完成"
                    binding.activatedCompleteBtn.scaleX = 1f
                    binding.activatedCompleteBtn.scaleY = 1f
                }
            }.start()
        binding.activatedDiscardBtn.animate()
            .alpha(0f).setDuration(150)
            .withEndAction {
                if (adapter.activatedPosition == -1) {
                    binding.activatedDiscardBtn.visibility = View.GONE
                    binding.activatedDiscardBtn.text = "✕ 丢弃"
                    binding.activatedDiscardBtn.scaleX = 1f
                    binding.activatedDiscardBtn.scaleY = 1f
                }
            }.start()
    }

    private fun updateDots(selected: Int) {
        val count = adapter.itemCount
        binding.dotsContainer.removeAllViews()
        for (i in 0 until count) {
            val params = android.widget.LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(if (i == selected) R.dimen.dot_selected else R.dimen.dot_normal),
                resources.getDimensionPixelSize(if (i == selected) R.dimen.dot_selected else R.dimen.dot_normal)
            ).also { it.setMargins(6, 0, 6, 0) }
            binding.dotsContainer.addView(View(this).apply {
                layoutParams = params
                setBackgroundResource(if (i == selected) R.drawable.dot_selected else R.drawable.dot_normal)
            })
        }
    }

    // ──────────────────── 激活态（长按卡片）────────────────────

    /** 按钮点击 bounce 动画 */
    private fun View.bounce(onEnd: () -> Unit = {}) {
        animate().scaleX(0.9f).scaleY(0.9f).setDuration(80)
            .withEndAction {
                animate().scaleX(1f).scaleY(1f).setDuration(120).withEndAction { onEnd() }.start()
            }.start()
    }

    private fun setupActivatedState() {
        // 点击蒙层退出激活态
        binding.dimOverlay.setOnClickListener { deactivateCard() }

        // 完成按钮 → 标记已完成
        binding.activatedCompleteBtn.setOnClickListener { v ->
            v.bounce {
                activatedInspiration?.let { viewModel.updateStatus(it.id, Inspiration.STATUS_COMPLETED) }
                deactivateCard()
            }
        }

        // 丢弃按钮 → 标记已丢弃
        binding.activatedDiscardBtn.setOnClickListener { v ->
            v.bounce {
                activatedInspiration?.let { viewModel.updateStatus(it.id, Inspiration.STATUS_DISCARDED) }
                deactivateCard()
            }
        }

        // 浮窗按钮
        binding.btnActivatedFloat.setOnClickListener { v ->
            v.bounce {
                activatedInspiration?.let { launchFloatingWindow(it) }
                deactivateCard()
            }
        }

        // 编辑按钮
        binding.btnActivatedEdit.setOnClickListener { v ->
            v.bounce {
                activatedInspiration?.let { showEditContentDialog(it) }
                deactivateCard()
            }
        }

        // 原文 / 灵感 翻转按钮
        binding.btnActivatedTranscript.setOnClickListener { v ->
            v.bounce {
                activatedInspiration?.let { flipCard(it) }
            }
        }
    }

    private fun activateCard(position: Int, inspiration: Inspiration) {
        activatedInspiration = inspiration
        adapter.activatedPosition = position

        binding.viewPager.post {
            binding.viewPager.isUserInputEnabled = false
        }

        // 状态栏颜色：激活态下改为深紫，与 dimOverlay 混色一致
        window.statusBarColor = android.graphics.Color.parseColor("#1A0F2E")

        // Z-order: dimOverlay=25, ViewPager=30, buttons=40
        binding.dimOverlay.translationZ = 25f
        binding.viewPager.translationZ = 30f
        binding.activatedCompleteBtn.translationZ = 40f
        binding.activatedActionsRow.translationZ = 40f
        binding.activatedDiscardBtn.translationZ = 40f

        // Hide bottom section (INVISIBLE to preserve layout)
        binding.bottomSection.visibility = View.INVISIBLE

        // 仅缩放当前页（同时自动隐藏相邻卡片因为 ViewPager 整体 alpha 不变但通过 RecyclerView 遍历设置）
        binding.viewPager.animate()
            .scaleX(0.94f)
            .scaleY(0.94f)
            .translationY(-resources.displayMetrics.density * 20f)
            .setDuration(250)
            .start()

        // 隐藏除当前页外的所有相邻卡片
        fadeSiblingPages(hide = true)

        // 毛玻璃模糊 header / bottomSection（API 31+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val blur = RenderEffect.createBlurEffect(25f, 25f, Shader.TileMode.CLAMP)
                binding.headerLayout.setRenderEffect(blur)
                binding.bottomSection.setRenderEffect(blur)
            } catch (_: Exception) { /* Xiaomi HyperOS 兼容：忽略，回退到纯暗色蒙层 */ }
        }

        // Show dim overlay
        binding.dimOverlay.visibility = View.VISIBLE
        binding.dimOverlay.animate().alpha(1f).setDuration(220).start()

        // Animate activatedCompleteBtn: fade + scale from 0.8 → 1.0
        binding.activatedCompleteBtn.apply {
            visibility = View.VISIBLE
            alpha = 0f
            scaleX = 0.8f
            scaleY = 0.8f
            text = "✓ 完成"
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(250)
                .start()
        }

        // Animate activatedDiscardBtn: fade + scale from 0.8 → 1.0
        binding.activatedDiscardBtn.apply {
            visibility = View.VISIBLE
            alpha = 0f
            scaleX = 0.8f
            scaleY = 0.8f
            text = "✕ 丢弃"
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(250)
                .start()
        }

        // Animate activatedActionsRow: slide up from +60dp + fade in
        val slideOffset = resources.displayMetrics.density * 60f
        binding.activatedActionsRow.apply {
            visibility = View.VISIBLE
            alpha = 0f
            translationY = slideOffset
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(250)
                .start()
        }
    }

    /** 卡片翻转：灵感 ↔ 原文 */
    private fun flipCard(inspiration: Inspiration) {
        val transcript = inspiration.transcript
        if (transcript.isNullOrBlank()) {
            Toast.makeText(this, "该灵感没有原文记录", Toast.LENGTH_SHORT).show()
            return
        }

        val rv = binding.viewPager.getChildAt(0) as? androidx.recyclerview.widget.RecyclerView ?: return
        val currentItem = binding.viewPager.currentItem
        var cardView: View? = null
        for (i in 0 until rv.childCount) {
            val child = rv.getChildAt(i)
            val pos = rv.getChildAdapterPosition(child)
            if (pos == currentItem && pos != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                cardView = child
                break
            }
        }
        cardView ?: return

        val tvContent = cardView.findViewById<TextView>(R.id.tvContent)
        val tvDate = cardView.findViewById<TextView>(R.id.tvDate)
        val container = cardView.findViewById<View>(R.id.cardContainer) as ViewGroup

        val cameraDistPx = resources.displayMetrics.density * 8000f
        container.cameraDistance = cameraDistPx

        // 翻转前关闭 clip，防止 3D 透视被裁
        container.clipChildren = false
        container.clipToPadding = false
        (cardView as? ViewGroup)?.clipChildren = false
        (cardView as? ViewGroup)?.clipToPadding = false
        rv.clipChildren = false

        // 第一半翻转：0° → 90°（卡片变窄消失）
        container.animate()
            .rotationY(90f)
            .setDuration(180)
            .withEndAction {
                // 在卡片侧面时切换内容
                if (!isShowingTranscript) {
                    tvContent.text = transcript
                    tvDate.text = "原文"
                    binding.btnActivatedTranscript.text = "灵感"
                    isShowingTranscript = true
                } else {
                    tvContent.text = inspiration.content
                    tvDate.text = inspiration.formattedDate()
                    binding.btnActivatedTranscript.text = "原文"
                    isShowingTranscript = false
                }
                // 第二半翻转：-90° → 0°（新内容展开）
                container.rotationY = -90f
                container.animate()
                    .rotationY(0f)
                    .setDuration(180)
                    .withEndAction {
                        // 翻转结束恢复 clip
                        container.clipChildren = true
                        container.clipToPadding = true
                    }
                    .start()
            }
            .start()
    }

    private fun deactivateCard() {
        // 如果正在显示原文，先翻回灵感
        if (isShowingTranscript) {
            isShowingTranscript = false
            binding.btnActivatedTranscript.text = "原文"
            // 直接恢复内容（不做动画，因为 deactivate 本身有动画）
            val rv = binding.viewPager.getChildAt(0) as? androidx.recyclerview.widget.RecyclerView
            if (rv != null) {
                val currentItem = binding.viewPager.currentItem
                for (i in 0 until rv.childCount) {
                    val child = rv.getChildAt(i)
                    val pos = rv.getChildAdapterPosition(child)
                    if (pos == currentItem && pos != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                        val tvContent = child.findViewById<TextView>(R.id.tvContent)
                        val tvDate = child.findViewById<TextView>(R.id.tvDate)
                        val container = child.findViewById<View>(R.id.cardContainer)
                        activatedInspiration?.let {
                            tvContent.text = it.content
                            tvDate.text = it.formattedDate()
                        }
                        container.rotationY = 0f
                        break
                    }
                }
            }
        }
        adapter.activatedPosition = -1
        activatedInspiration = null

        binding.viewPager.post {
            binding.viewPager.isUserInputEnabled = true
        }

        // 恢复状态栏颜色（与渐变背景顶部色一致）
        window.statusBarColor = android.graphics.Color.parseColor("#2D1B6B")

        // Reset translationZ
        binding.dimOverlay.translationZ = 0f
        binding.viewPager.translationZ = 0f
        binding.activatedCompleteBtn.translationZ = 0f
        binding.activatedActionsRow.translationZ = 0f
        binding.activatedDiscardBtn.translationZ = 0f

        // Restore bottom section
        binding.bottomSection.visibility = View.VISIBLE

        // Restore ViewPager scale and position
        binding.viewPager.animate()
            .scaleX(1f)
            .scaleY(1f)
            .translationY(0f)
            .setDuration(200)
            .start()

        // 恢复相邻卡片可见
        fadeSiblingPages(hide = false)

        // 移除毛玻璃模糊
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                binding.headerLayout.setRenderEffect(null)
                binding.bottomSection.setRenderEffect(null)
            } catch (_: Exception) {}
        }

        // Fade out dim overlay
        binding.dimOverlay.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction { binding.dimOverlay.visibility = View.GONE }
            .start()

        // Fade + scale out activatedCompleteBtn
        binding.activatedCompleteBtn.animate()
            .alpha(0f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(180)
            .withEndAction { binding.activatedCompleteBtn.visibility = View.GONE }
            .start()

        // Fade + scale out activatedDiscardBtn
        binding.activatedDiscardBtn.animate()
            .alpha(0f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(180)
            .withEndAction { binding.activatedDiscardBtn.visibility = View.GONE }
            .start()

        // Slide down + fade out activatedActionsRow
        val slideOffset = resources.displayMetrics.density * 60f
        binding.activatedActionsRow.animate()
            .alpha(0f)
            .translationY(slideOffset)
            .setDuration(180)
            .withEndAction {
                binding.activatedActionsRow.visibility = View.GONE
                binding.activatedActionsRow.translationY = 0f
            }
            .start()
    }

    // ──────────────────── 抽屉（以往灵感）────────────────────

    private fun setupDrawer() {
        binding.btnMenu.setOnClickListener {
            openDrawer()
        }

        binding.drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerOpened(drawerView: View) {
                ensurePastFragment()
                pastFragment?.refresh()
            }
            override fun onDrawerClosed(drawerView: View) {
                viewModel.loadInspirations()
            }
            override fun onDrawerStateChanged(newState: Int) {}
        })
    }

    private fun openDrawer() {
        ensurePastFragment()
        binding.drawerLayout.openDrawer(Gravity.END)
    }

    private fun ensurePastFragment() {
        if (pastFragment == null) {
            pastFragment = PastInspirationsFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.drawerContainer, pastFragment!!)
                .commitAllowingStateLoss()
        }
    }

    // ──────────────────── 录音（点击切换模式）────────────────────

    private fun setupRecordButton() {
        binding.btnRecordSection.setOnClickListener {
            if (!isRecording) {
                if (hasRecordPermission()) {
                    startRecording()
                    binding.tvRecordHint.text = "停止录音"
                    binding.recordingWave.visibility = View.VISIBLE
                } else {
                    requestPermissions()
                }
            } else {
                stopRecording()
                binding.tvRecordHint.text = "点击录音"
                binding.recordingWave.visibility = View.GONE
            }
        }
    }

    private fun startRecording() {
        if (isRecording) return
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val dir = File(cacheDir, "recordings").also { it.mkdirs() }
        currentAudioFile = File(dir, "rec_$stamp.m4a")

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(128000)
            setOutputFile(currentAudioFile!!.absolutePath)
            try {
                prepare(); start(); isRecording = true
                recordingTimeoutHandler.postDelayed(recordingTimeoutRunnable, 60_000L)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "录音启动失败", Toast.LENGTH_SHORT).show()
                release(); mediaRecorder = null
            }
        }
    }

    private fun stopRecording() {
        if (!isRecording) return
        recordingTimeoutHandler.removeCallbacks(recordingTimeoutRunnable)
        isRecording = false
        mediaRecorder?.apply { try { stop() } catch (_: Exception) {}; release() }
        mediaRecorder = null
        currentAudioFile?.let { file ->
            if (file.exists() && file.length() > 0) viewModel.processAudioFile(file)
            else Toast.makeText(this, "录音文件无效", Toast.LENGTH_SHORT).show()
        }
    }

    // ──────────────────── 按钮 ────────────────────

    private fun setupManualAddButton() {
        binding.btnManualSection.setOnClickListener { showTextInputDialog() }
    }

    // ──────────────────── 无障碍服务引导 ────────────────────

    private fun setupAccessibilityBanner() {
        binding.btnEnableAccessibility.setOnClickListener { showAccessibilityGuideDialog() }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val target = ComponentName(this, VolumeKeyAccessibilityService::class.java).flattenToString()
        return enabled.split(":").any { it.equals(target, ignoreCase = true) }
    }

    private fun showAccessibilityGuideDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_accessibility_guide, null)
        val dialog = AlertDialog.Builder(this).setView(view).create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        view.findViewById<TextView>(R.id.dialogBtnCancel).setOnClickListener { dialog.dismiss() }
        view.findViewById<TextView>(R.id.dialogBtnConfirm).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    // ──────────────────── ViewModel 观察 ────────────────────

    private fun observeViewModel() {
        viewModel.inspirations.observe(this) { list ->
            val newIds = list.map { it.id }
            val dataChanged = newIds != lastInspirationIds
            lastInspirationIds = newIds

            adapter.updateData(list)
            binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            binding.viewPager.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
            binding.dotsContainer.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
            if (list.isNotEmpty()) updateDots(binding.viewPager.currentItem)
            // 仅在数据实际变化时刷新 transform + pop-in（避免关闭抽屉时的无意义跳动）
            if (dataChanged) {
                binding.viewPager.post {
                    binding.viewPager.requestTransform()
                    playCardPopIn()
                }
            }
        }

        viewModel.processingState.observe(this) { state ->
            when (state) {
                is ProcessingState.Transcribing -> {
                    binding.processingHint.visibility = View.VISIBLE
                    binding.tvLoading.text = "正在转写录音…"
                    binding.btnRecordSection.isEnabled = false
                    binding.btnManualSection.isEnabled = false
                }
                is ProcessingState.Processing -> {
                    binding.processingHint.visibility = View.VISIBLE
                    binding.tvLoading.text = "AI 正在生成文案…"
                    binding.btnRecordSection.isEnabled = false
                    binding.btnManualSection.isEnabled = false
                }
                is ProcessingState.Success -> {
                    binding.processingHint.visibility = View.GONE
                    binding.btnRecordSection.isEnabled = true
                    binding.btnManualSection.isEnabled = true
                    Toast.makeText(this, "灵感已保存！", Toast.LENGTH_SHORT).show()
                    viewModel.resetProcessingState()
                    binding.viewPager.postDelayed({ binding.viewPager.currentItem = 0 }, 300)
                }
                is ProcessingState.Error -> {
                    binding.processingHint.visibility = View.GONE
                    binding.btnRecordSection.isEnabled = true
                    binding.btnManualSection.isEnabled = true
                    if (isRecording) {
                        isRecording = false
                        binding.tvRecordHint.text = "点击录音"
                        binding.recordingWave.visibility = View.GONE
                    }
                    showApiErrorDialog(state.message)
                    viewModel.resetProcessingState()
                }
                is ProcessingState.Idle -> {
                    binding.processingHint.visibility = View.GONE
                    binding.btnRecordSection.isEnabled = true
                    binding.btnManualSection.isEnabled = true
                }
            }
        }
    }

    // ──────────────────── 弹窗 ────────────────────

    private fun showApiErrorDialog(errorMessage: String) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_confirm, null)
        view.findViewById<TextView>(R.id.dialogTitle).text = "AI 请求失败"
        view.findViewById<TextView>(R.id.dialogMessage).text = errorMessage
        view.findViewById<TextView>(R.id.dialogBtnConfirm).text = "知道了"
        view.findViewById<TextView>(R.id.dialogBtnCancel).visibility = View.GONE

        val dialog = AlertDialog.Builder(this).setView(view).create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        view.findViewById<TextView>(R.id.dialogBtnConfirm).setOnClickListener { dialog.dismiss() }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun showTextInputDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_text_input, null)
        val etText = view.findViewById<EditText>(R.id.dialogEditText)
        val dialog = AlertDialog.Builder(this).setView(view).create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        view.findViewById<TextView>(R.id.dialogBtnCancel).setOnClickListener { dialog.dismiss() }
        view.findViewById<TextView>(R.id.dialogBtnSaveDirect).setOnClickListener {
            val text = etText.text.toString().trim()
            if (text.isEmpty()) { Toast.makeText(this, "内容不能为空", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            viewModel.addManualInspiration(text)
            Toast.makeText(this, "灵感已保存！", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            binding.viewPager.postDelayed({ binding.viewPager.currentItem = 0 }, 300)
        }
        view.findViewById<TextView>(R.id.dialogBtnAI).setOnClickListener {
            val text = etText.text.toString().trim()
            if (text.isEmpty()) { Toast.makeText(this, "内容不能为空", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            viewModel.processTextInput(text)
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun showEditContentDialog(inspiration: Inspiration) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_inspiration_edit, null)
        val etText = view.findViewById<EditText>(R.id.dialogEditText)
        view.findViewById<TextView>(R.id.dialogTitle).text = "编辑灵感"
        etText.setText(inspiration.content)
        etText.hint = "在这里修改你的灵感…"
        etText.setSelection(inspiration.content.length)

        val dialog = AlertDialog.Builder(this).setView(view).create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        view.findViewById<TextView>(R.id.dialogBtnCancel).setOnClickListener { dialog.dismiss() }
        view.findViewById<TextView>(R.id.dialogBtnConfirm).setOnClickListener {
            val text = etText.text.toString().trim()
            if (text.isEmpty()) { Toast.makeText(this, "内容不能为空", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            viewModel.updateContent(inspiration.id, text)
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.88).toInt(),
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    /** 查看录音原文 / 原始输入文字 — iOS-style BottomSheet */
    private fun showTranscriptDialog(inspiration: Inspiration) {
        val transcript = inspiration.transcript
        if (transcript.isNullOrBlank()) {
            Toast.makeText(this, "该灵感没有原文记录", Toast.LENGTH_SHORT).show()
            return
        }
        showTranscriptSheet(transcript)
    }

    private fun showTranscriptSheet(transcript: String) {
        val sheet = com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.TransparentBottomSheet)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_transcript, null)
        view.findViewById<TextView>(R.id.tvTranscriptContent).text = transcript.ifBlank { "暂无原文" }
        view.findViewById<TextView>(R.id.btnTranscriptClose).setOnClickListener { sheet.dismiss() }
        sheet.setContentView(view)
        sheet.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        sheet.show()
    }

    private fun showConfirmDialog(
        title: String,
        message: String,
        confirmText: String = "确认",
        onConfirm: () -> Unit
    ) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_confirm, null)
        view.findViewById<TextView>(R.id.dialogTitle).text = title
        view.findViewById<TextView>(R.id.dialogMessage).text = message
        view.findViewById<TextView>(R.id.dialogBtnConfirm).text = confirmText

        val dialog = AlertDialog.Builder(this).setView(view).create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        view.findViewById<TextView>(R.id.dialogBtnCancel).setOnClickListener { dialog.dismiss() }
        view.findViewById<TextView>(R.id.dialogBtnConfirm).setOnClickListener {
            onConfirm(); dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.82).toInt(),
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    // ──────────────────── 悬浮窗 ────────────────────

    private fun launchFloatingWindow(inspiration: Inspiration) {
        if (!Settings.canDrawOverlays(this)) {
            showConfirmDialog("需要悬浮窗权限", "灵感胶囊需要悬浮窗权限才能将灵感显示在屏幕上", "去设置") {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }
            return
        }
        startService(Intent(this, FloatingWindowService::class.java).apply {
            putExtra("inspiration_id", inspiration.id)
            putExtra("inspiration_content", inspiration.content)
        })
        Toast.makeText(this, "灵感已悬浮到屏幕", Toast.LENGTH_SHORT).show()
    }

    // ──────────────────── 权限 ────────────────────

    private fun hasRecordPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED

    private fun checkPermissions() {
        val missing = arrayOf(Manifest.permission.RECORD_AUDIO).filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) requestPermissionLauncher.launch(missing.toTypedArray())
    }

    private fun requestPermissions() {
        requestPermissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
    }

    /**
     * Back press priority:
     * 1. Close drawer if open
     * 2. Deactivate card if activated
     * 3. Default behaviour
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            binding.drawerLayout.isDrawerOpen(Gravity.END) -> {
                binding.drawerLayout.closeDrawer(Gravity.END)
            }
            adapter.activatedPosition != -1 -> {
                deactivateCard()
            }
            else -> {
                @Suppress("DEPRECATION")
                super.onBackPressed()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPlayer.release()
        recordingTimeoutHandler.removeCallbacks(recordingTimeoutRunnable)
        if (isRecording) {
            mediaRecorder?.apply { try { stop() } catch (_: Exception) {} }
        }
        mediaRecorder?.release()
        mediaRecorder = null
    }
}
