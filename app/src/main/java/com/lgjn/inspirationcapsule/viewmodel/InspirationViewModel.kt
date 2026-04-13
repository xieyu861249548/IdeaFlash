package com.lgjn.inspirationcapsule.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.lgjn.inspirationcapsule.api.DifyApiService
import com.lgjn.inspirationcapsule.data.Inspiration
import com.lgjn.inspirationcapsule.data.InspirationRepository
import kotlinx.coroutines.launch
import java.io.File

sealed class ProcessingState {
    object Idle : ProcessingState()
    /** 语音转写中（Step 1） */
    object Transcribing : ProcessingState()
    /** AI 生成文案中（Step 2 / 文案路径） */
    object Processing : ProcessingState()
    data class Success(val text: String) : ProcessingState()
    data class Error(val message: String) : ProcessingState()
}

class InspirationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = InspirationRepository(application)

    private val _inspirations = MutableLiveData<List<Inspiration>>(emptyList())
    val inspirations: LiveData<List<Inspiration>> = _inspirations

    private val _processingState = MutableLiveData<ProcessingState>(ProcessingState.Idle)
    val processingState: LiveData<ProcessingState> = _processingState

    init {
        loadInspirations()
    }

    /** 主页只加载 active 状态的灵感 */
    fun loadInspirations() {
        viewModelScope.launch {
            _inspirations.value = repository.getActive()
        }
    }

    fun loadByStatus(status: String, target: MutableLiveData<List<Inspiration>>) {
        viewModelScope.launch {
            target.value = repository.getByStatus(status)
        }
    }

    /**
     * 处理语音录音（两步）：
     *   Step 1: audio-to-text 转写 → Transcribing 状态
     *   Step 2: chat-messages 提炼 → Processing 状态
     *   完成后 → Success 状态
     */
    fun processAudioFile(audioFile: File) {
        viewModelScope.launch {
            _processingState.value = ProcessingState.Transcribing

            val result = DifyApiService.processAudio(audioFile)

            result.onSuccess { audioResult ->
                val inspiration = Inspiration(
                    content = audioResult.content,
                    transcript = audioResult.transcript,
                    createdAt = System.currentTimeMillis()
                )
                repository.insert(inspiration)
                _processingState.value = ProcessingState.Success(audioResult.content)
                loadInspirations()
            }
            result.onFailure { error ->
                _processingState.value = ProcessingState.Error(
                    error.message ?: "AI处理失败，请检查网络后重试"
                )
            }
        }
    }

    /**
     * 处理文案输入（一步）：
     *   chat-messages 提炼 → Processing 状态 → Success
     *   transcript 保存用户原始输入
     */
    fun processTextInput(text: String) {
        viewModelScope.launch {
            _processingState.value = ProcessingState.Processing
            val result = DifyApiService.processText(text)
            result.onSuccess { aiText ->
                val inspiration = Inspiration(
                    content = aiText,
                    transcript = text,
                    createdAt = System.currentTimeMillis()
                )
                repository.insert(inspiration)
                _processingState.value = ProcessingState.Success(aiText)
                loadInspirations()
            }
            result.onFailure { error ->
                _processingState.value = ProcessingState.Error(
                    error.message ?: "AI处理失败，请检查网络后重试"
                )
            }
        }
    }

    /** 直接保存灵感（不经过AI，transcript = content） */
    fun addManualInspiration(content: String) {
        viewModelScope.launch {
            repository.insert(Inspiration(content = content, transcript = content))
            loadInspirations()
        }
    }

    fun updateContent(id: Long, content: String) {
        viewModelScope.launch {
            repository.updateContent(id, content)
            loadInspirations()
        }
    }

    fun updateStatus(id: Long, status: String) {
        viewModelScope.launch {
            repository.updateStatus(id, status)
            loadInspirations()
        }
    }

    fun deleteInspiration(id: Long) {
        viewModelScope.launch {
            repository.delete(id)
            loadInspirations()
        }
    }

    fun resetProcessingState() {
        _processingState.value = ProcessingState.Idle
    }
}
