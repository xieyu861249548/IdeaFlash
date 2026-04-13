package com.lgjn.inspirationcapsule

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.lgjn.inspirationcapsule.api.DifyApiService
import com.lgjn.inspirationcapsule.data.Inspiration
import com.lgjn.inspirationcapsule.data.InspirationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 文案输入小组件的承载 Activity（透明背景，仅展示 AlertDialog）。
 *
 * 不调用 setContentView，直接弹出 dialog_text_input 对话框。
 * 对话框关闭时 finish() 销毁 Activity，不留任何视觉痕迹。
 */
class TextInputActivity : AppCompatActivity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 透明背景，不需要自己的 content view
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        showInputDialog()
    }

    private fun showInputDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_text_input, null)
        val etText = view.findViewById<EditText>(R.id.dialogEditText)

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setOnDismissListener { finish() }

        view.findViewById<TextView>(R.id.dialogBtnCancel).setOnClickListener {
            dialog.dismiss()
        }

        // 直接保存（不经过 AI）
        view.findViewById<TextView>(R.id.dialogBtnSaveDirect).setOnClickListener {
            val text = etText.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(this, "内容不能为空", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            scope.launch {
                val repo = InspirationRepository(applicationContext)
                repo.insert(Inspiration(content = text, createdAt = System.currentTimeMillis()))
                saveLastInspiration(text)
                Toast.makeText(applicationContext, "灵感已保存！", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        // AI 提炼
        view.findViewById<TextView>(R.id.dialogBtnAI).setOnClickListener {
            val text = etText.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(this, "内容不能为空", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            dialog.dismiss()
            processWithAI(text)
        }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun processWithAI(text: String) {
        Toast.makeText(this, "AI 处理中…", Toast.LENGTH_SHORT).show()
        scope.launch {
            val result = DifyApiService.processText(text)
            result.onSuccess { aiText ->
                val repo = InspirationRepository(applicationContext)
                repo.insert(Inspiration(content = aiText, createdAt = System.currentTimeMillis()))
                saveLastInspiration(aiText)
                Toast.makeText(applicationContext, "灵感已保存！", Toast.LENGTH_SHORT).show()
            }
            result.onFailure { error ->
                Toast.makeText(
                    applicationContext,
                    "AI 处理失败：${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
            finish()
        }
    }

    /** 保存最新灵感内容到 SharedPreferences，供小组件状态文字读取 */
    private fun saveLastInspiration(content: String) {
        getSharedPreferences("widget_prefs", Context.MODE_PRIVATE).edit()
            .putLong("last_inspiration_time", System.currentTimeMillis())
            .putString("last_inspiration_content", content)
            .apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
