package com.lgjn.inspirationcapsule

import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.lgjn.inspirationcapsule.databinding.ActivityRecordingOverlayBinding
import com.lgjn.inspirationcapsule.viewmodel.InspirationViewModel
import androidx.activity.viewModels
import com.lgjn.inspirationcapsule.viewmodel.ProcessingState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Transparent full-screen activity launched by widget button.
 * Records audio immediately on launch.
 */
class RecordingOverlayActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecordingOverlayBinding
    private val viewModel: InspirationViewModel by viewModels()

    private var mediaRecorder: MediaRecorder? = null
    private var currentAudioFile: File? = null
    private var isRecording = false
    private val handler = Handler(Looper.getMainLooper())
    private var elapsedSeconds = 0

    private val timerRunnable = object : Runnable {
        override fun run() {
            elapsedSeconds++
            val minutes = elapsedSeconds / 60
            val seconds = elapsedSeconds % 60
            binding.tvTimer.text = String.format("%02d:%02d", minutes, seconds)
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordingOverlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startRecording()
        startPulseAnimation()
        handler.post(timerRunnable)

        binding.btnStop.setOnClickListener {
            stopRecordingAndProcess()
        }

        binding.btnCancel.setOnClickListener {
            cancelRecording()
        }

        // Tap background to stop
        binding.root.setOnClickListener {
            if (isRecording) stopRecordingAndProcess()
        }

        observeViewModel()
    }

    private fun startRecording() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val audioDir = File(cacheDir, "recordings").also { it.mkdirs() }
        currentAudioFile = File(audioDir, "widget_$timeStamp.m4a")

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
                isRecording = true
                binding.tvStatus.text = "正在录音..."
            } catch (e: Exception) {
                e.printStackTrace()
                binding.tvStatus.text = "录音失败，请检查权限"
                Toast.makeText(this@RecordingOverlayActivity, "录音启动失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startPulseAnimation() {
        val scaleAnim = ScaleAnimation(
            1f, 1.3f, 1f, 1.3f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 700
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
        binding.ivMicPulse.startAnimation(scaleAnim)
    }

    private fun stopRecordingAndProcess() {
        if (!isRecording) return
        isRecording = false
        handler.removeCallbacks(timerRunnable)
        stopAnimation()

        mediaRecorder?.apply {
            try { stop() } catch (e: Exception) { e.printStackTrace() }
            release()
        }
        mediaRecorder = null

        currentAudioFile?.let { file ->
            if (file.exists() && file.length() > 0) {
                binding.tvStatus.text = "AI正在生成文案..."
                binding.btnStop.isEnabled = false
                binding.btnCancel.isEnabled = false
                viewModel.processAudioFile(file)
            } else {
                Toast.makeText(this, "录音文件无效", Toast.LENGTH_SHORT).show()
                finish()
            }
        } ?: finish()
    }

    private fun cancelRecording() {
        isRecording = false
        handler.removeCallbacks(timerRunnable)
        mediaRecorder?.apply {
            try { stop() } catch (e: Exception) { }
            release()
        }
        mediaRecorder = null
        currentAudioFile?.delete()
        finish()
    }

    private fun stopAnimation() {
        binding.ivMicPulse.clearAnimation()
    }

    private fun observeViewModel() {
        viewModel.processingState.observe(this) { state ->
            when (state) {
                is ProcessingState.Success -> {
                    Toast.makeText(this, "灵感已保存！", Toast.LENGTH_SHORT).show()
                    viewModel.resetProcessingState()
                    finish()
                }
                is ProcessingState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                    viewModel.resetProcessingState()
                    finish()
                }
                else -> {}
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable)
        mediaRecorder?.release()
        mediaRecorder = null
    }
}
