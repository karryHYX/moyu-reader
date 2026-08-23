package com.moyu.reader.reader

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class LocalTtsState(
    val ready: Boolean = false,
    val speaking: Boolean = false,
    val message: String? = "正在准备本地朗读引擎…",
)

/** Device-local Android TTS wrapper. It owns no service, network client, or persistent process. */
class LocalTtsController(context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private val mutableState = MutableStateFlow(LocalTtsState())
    val state: StateFlow<LocalTtsState> = mutableState.asStateFlow()
    private var engine: TextToSpeech? = null
    private var generation = 0L
    private val sleepStop = Runnable {
        stop()
        mutableState.value = mutableState.value.copy(message = "定时朗读已结束")
    }

    init {
        engine = TextToSpeech(context.applicationContext) { status ->
            val current = engine
            if (status == TextToSpeech.SUCCESS && current != null) {
                val language = current.setLanguage(Locale.SIMPLIFIED_CHINESE)
                val supported = language != TextToSpeech.LANG_MISSING_DATA && language != TextToSpeech.LANG_NOT_SUPPORTED
                mutableState.value = LocalTtsState(
                    ready = supported,
                    message = if (supported) "使用手机内置语音，全文留在本机" else "当前朗读引擎缺少中文语音",
                )
                current.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        mutableState.value = mutableState.value.copy(speaking = true, message = "正在朗读")
                    }

                    override fun onDone(utteranceId: String?) {
                        if (utteranceId?.endsWith(END_SUFFIX) == true) {
                            mutableState.value = mutableState.value.copy(speaking = false, message = "本章朗读完成")
                        }
                    }

                    @Deprecated("Platform callback")
                    override fun onError(utteranceId: String?) {
                        mutableState.value = mutableState.value.copy(speaking = false, message = "朗读引擎处理失败")
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        mutableState.value = mutableState.value.copy(speaking = false, message = "朗读引擎处理失败")
                    }
                })
            } else {
                mutableState.value = LocalTtsState(message = "手机上的朗读引擎没有就绪")
            }
        }
    }

    fun speak(text: String, rate: Float, sleepMinutes: Int) {
        val current = engine ?: return
        if (!state.value.ready) return
        val content = text.trim()
        if (content.isEmpty()) {
            mutableState.value = mutableState.value.copy(message = "当前位置之后没有正文")
            return
        }
        stop(clearMessage = false)
        current.setSpeechRate(rate.coerceIn(.6f, 1.8f))
        generation++
        val chunks = content.readerTtsChunks(TextToSpeech.getMaxSpeechInputLength().coerceAtMost(3_800))
        chunks.forEachIndexed { index, chunk ->
            val queue = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val suffix = if (index == chunks.lastIndex) END_SUFFIX else ""
            current.speak(chunk, queue, null, "moyu-$generation-$index$suffix")
        }
        mutableState.value = mutableState.value.copy(speaking = true, message = if (sleepMinutes > 0) "正在朗读 · $sleepMinutes 分钟后停止" else "正在朗读")
        if (sleepMinutes > 0) handler.postDelayed(sleepStop, sleepMinutes * 60_000L)
    }

    fun stop(clearMessage: Boolean = true) {
        handler.removeCallbacks(sleepStop)
        engine?.stop()
        mutableState.value = mutableState.value.copy(speaking = false, message = if (clearMessage) "朗读已暂停" else mutableState.value.message)
    }

    fun release() {
        handler.removeCallbacksAndMessages(null)
        engine?.stop()
        engine?.shutdown()
        engine = null
    }

    private fun String.readerTtsChunks(maxLength: Int): List<String> {
        if (length <= maxLength) return listOf(this)
        val result = ArrayList<String>()
        var start = 0
        while (start < length) {
            val hardEnd = (start + maxLength).coerceAtMost(length)
            val preferred = substring(start, hardEnd).indexOfLast { it == '。' || it == '！' || it == '？' || it == '\n' }
            val end = if (preferred > maxLength / 2) start + preferred + 1 else hardEnd
            result += substring(start, end)
            start = end
        }
        return result
    }

    private companion object { const val END_SUFFIX = "-end" }
}
