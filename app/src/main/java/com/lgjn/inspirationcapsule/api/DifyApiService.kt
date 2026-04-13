package com.lgjn.inspirationcapsule.api

import android.util.Log
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Dify Agent 模式接口封装
 *
 * 语音路径（两步）：
 *   1. POST /v1/audio-to-text  → 将录音转写为文字
 *   2. POST /v1/chat-messages  → 把转写文字发给 Agent，提炼 ≤15 字灵感
 *
 * 文案路径（一步）：
 *   POST /v1/chat-messages     → 把用户输入文字发给 Agent，提炼 ≤15 字灵感
 *
 * Agent 只处理文本 query，不能直接读取音频文件附件，
 * 因此必须先转写再发给 Agent，而不是把音频作为 files[] 发送。
 */
/** 语音处理结果：content = AI 提炼的灵感，transcript = Whisper 原始转写 */
data class AudioProcessResult(val content: String, val transcript: String)

object DifyApiService {

    private const val TAG = "DifyApiService"
    private const val API_KEY = "app-mgDcUYxxdjU6vKRYCklmGHbO"
    private const val BASE_URL = "https://api.dify.ai/v1"
    private const val USER_ID = "lingganjiaonang_user"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    // ──────────────────── 语音路径（两步） ────────────────────

    /**
     * 处理语音录音：
     *   Step 1. /v1/audio-to-text  → 转写为文字
     *   Step 2. /v1/chat-messages  → Agent 提炼 ≤15 字灵感（与文案路径共用）
     */
    suspend fun processAudio(audioFile: File): Result<AudioProcessResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Step1 开始转写音频: ${audioFile.name}")

            // Step 1: 音频 → 文字
            val transcribeResult = transcribeAudio(audioFile)
            transcribeResult.onFailure { return@withContext Result.failure(it) }
            val transcription = transcribeResult.getOrThrow()
            Log.d(TAG, "Step1 转写结果: $transcription")

            // Step 1 完成后立即删除本地音频文件（转写已完成，无需保留）
            if (audioFile.exists()) {
                audioFile.delete()
                Log.d(TAG, "本地录音文件已删除: ${audioFile.name}")
            }

            // Step 2: 转写文字 → Agent 提炼灵感（复用文案路径）
            Log.d(TAG, "Step2 发送转写文字给 Agent")
            val contentResult = sendChatMessageWithText(transcription)
            contentResult.map { content -> AudioProcessResult(content = content, transcript = transcription) }
        } catch (e: Exception) {
            Log.e(TAG, "processAudio 异常", e)
            Result.failure(Exception("处理失败: ${e.message}"))
        }
    }

    // ──────────────────── 文案路径（一步） ────────────────────

    /**
     * 处理文案输入：
     *   直接将用户输入文字作为 query 发给 Agent，返回 ≤15 字灵感
     */
    suspend fun processText(text: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "处理文案输入: $text")
            sendChatMessageWithText(text)
        } catch (e: Exception) {
            Log.e(TAG, "processText 异常", e)
            Result.failure(Exception("网络请求失败: ${e.message}"))
        }
    }

    // ──────────────────── 内部实现 ────────────────────

    /**
     * Step 1：调用 Dify audio-to-text API 将音频转写为文字。
     * 直接发送文件字节，无需先上传获取 file_id。
     */
    private fun transcribeAudio(audioFile: File): Result<String> {
        // .m4a 文件使用 audio/m4a 作为 MIME 类型（比 audio/mp4 更精确，Whisper 明确支持）
        // audio/mp4 容易被识别为视频，audio/m4a 是 .m4a 音频文件约定俗成的类型标识
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file", audioFile.name,
                audioFile.asRequestBody("audio/m4a".toMediaType())
            )
            .addFormDataPart("user", USER_ID)
            .build()

        val request = Request.Builder()
            .url("$BASE_URL/audio-to-text")
            .addHeader("Authorization", "Bearer $API_KEY")
            .post(requestBody)
            .build()

        return try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            Log.d(TAG, "audio-to-text 响应 [${response.code}]: $body")

            if (!response.isSuccessful) {
                return Result.failure(
                    Exception("录音转写失败 (${response.code}): ${extractDifyMessage(body)}")
                )
            }

            val text = JsonParser.parseString(body)
                .asJsonObject.get("text")?.asString?.trim()
                ?: return Result.failure(Exception("转写响应缺少 text 字段: $body"))

            if (text.isBlank()) {
                return Result.failure(Exception("录音内容识别为空，请重新录音"))
            }

            Result.success(text)
        } catch (e: Exception) {
            Result.failure(Exception("录音转写异常: ${e.message}"))
        }
    }

    /**
     * Step 2 / 文案路径：以 streaming 模式调用 /v1/chat-messages，
     * 把文字作为 query 发给 Agent，逐行解析 SSE 流，拼接 answer 片段。
     */
    private fun sendChatMessageWithText(text: String): Result<String> {
        val escaped = text.replace("\\", "\\\\").replace("\"", "\\\"")
        val jsonBody = """
            {
                "inputs": {},
                "query": "$escaped",
                "response_mode": "streaming",
                "conversation_id": "",
                "user": "$USER_ID"
            }
        """.trimIndent()

        val request = Request.Builder()
            .url("$BASE_URL/chat-messages")
            .addHeader("Authorization", "Bearer $API_KEY")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: ""
                Log.e(TAG, "chat-messages 失败 [${response.code}]: $errBody")
                return Result.failure(
                    Exception("AI请求失败 (${response.code})\n${extractDifyMessage(errBody)}")
                )
            }

            // ── 解析 SSE 事件流 ──────────────────────────────────
            val rawBody = response.body?.string() ?: ""
            Log.d(TAG, "SSE 原始响应（前500字）: ${rawBody.take(500)}")

            val answerBuilder = StringBuilder()

            for (line in rawBody.lines()) {
                val trimmed = line.trim()
                if (!trimmed.startsWith("data:")) continue

                val jsonStr = trimmed.removePrefix("data:").trim()
                if (jsonStr.isEmpty() || jsonStr == "[DONE]") continue

                try {
                    val obj = JsonParser.parseString(jsonStr).asJsonObject
                    when (val event = obj.get("event")?.asString) {
                        // agent_message：每个包含一个增量 answer 片段，累加
                        "agent_message", "message" -> {
                            val chunk = obj.get("answer")?.asString ?: ""
                            answerBuilder.append(chunk)
                            Log.d(TAG, "SSE chunk [$event]: $chunk")
                        }
                        // message_end：流结束
                        "message_end" -> {
                            Log.d(TAG, "SSE 流结束")
                            break
                        }
                        // error：AI 处理出错
                        "error" -> {
                            val msg = obj.get("message")?.asString ?: "未知错误"
                            return Result.failure(Exception("AI 流式处理出错: $msg"))
                        }
                        else -> Log.d(TAG, "SSE 事件忽略: $event")
                    }
                } catch (_: Exception) {
                    // 非 JSON 行（空行、注释行）直接跳过
                }
            }

            val answer = answerBuilder.toString().trim()
            if (answer.isNotBlank()) {
                Log.d(TAG, "AI 最终文案: $answer")
                Result.success(answer)
            } else {
                Result.failure(Exception("AI 未返回有效文案，请检查 Agent 是否正确配置了输出"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("请求异常: ${e.message}"))
        }
    }

    /** 从 Dify 响应体中提取可读错误信息 */
    private fun extractDifyMessage(body: String): String {
        if (body.isBlank()) return "（无详细信息）"
        return try {
            val obj = JsonParser.parseString(body).asJsonObject
            obj.get("message")?.asString
                ?: obj.get("msg")?.asString
                ?: obj.get("error")?.asString
                ?: body.take(400)
        } catch (_: Exception) {
            body.take(400)
        }
    }
}
