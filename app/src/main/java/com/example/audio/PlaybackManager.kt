package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class PlaybackManager(private val context: Context, private val scope: CoroutineScope) {
    val synthesizers = mapOf(
        SoundSynthesizer.SoundType.RAIN to SoundSynthesizer(SoundSynthesizer.SoundType.RAIN),
        SoundSynthesizer.SoundType.WIND to SoundSynthesizer(SoundSynthesizer.SoundType.WIND),
        SoundSynthesizer.SoundType.BROWN_NOISE to SoundSynthesizer(SoundSynthesizer.SoundType.BROWN_NOISE),
        SoundSynthesizer.SoundType.SPACE to SoundSynthesizer(SoundSynthesizer.SoundType.SPACE)
    )

    private var customMediaPlayer: MediaPlayer? = null
    private var isPlaying = false

    private val _timerRemainingMs = MutableStateFlow<Long?>(null)
    val timerRemainingMs: StateFlow<Long?> = _timerRemainingMs

    private var timerJob: Job? = null
    private var fadeOutJob: Job? = null

    init {
        // Start synthesizers with 0 volume
        synthesizers.values.forEach { it.start(scope) }
    }

    fun setVolume(type: SoundSynthesizer.SoundType, volume: Float) {
        synthesizers[type]?.setVolume(volume)
    }

    fun playCustomAudio(filePath: String?, volume: Float) {
        customMediaPlayer?.stop()
        customMediaPlayer?.release()
        customMediaPlayer = null

        if (filePath != null && File(filePath).exists()) {
            try {
                customMediaPlayer = MediaPlayer().apply {
                    setDataSource(filePath)
                    isLooping = true
                    setVolume(volume, volume)
                    prepare()
                    start()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setTimer(minutes: Int, fadeOutMinutes: Int) {
        timerJob?.cancel()
        fadeOutJob?.cancel()
        setMasterVolume(1f) // Reset master volume

        if (minutes <= 0) {
            _timerRemainingMs.value = null
            return
        }

        val totalMs = minutes * 60 * 1000L
        val fadeMs = fadeOutMinutes * 60 * 1000L
        _timerRemainingMs.value = totalMs

        timerJob = scope.launch {
            var remaining = totalMs
            val tickInterval = 1000L
            while (remaining > 0) {
                delay(tickInterval)
                remaining -= tickInterval
                _timerRemainingMs.value = remaining

                if (remaining <= fadeMs && remaining % 5000L < 1000L) {
                    val vol = (remaining.toFloat() / fadeMs.toFloat()).coerceIn(0f, 1f)
                    setMasterVolume(vol)
                }
            }
            _timerRemainingMs.value = null
            stopAll()
        }
    }

    private fun setMasterVolume(vol: Float) {
        synthesizers.values.forEach { it.setMasterVolume(vol) }
        customMediaPlayer?.setVolume(vol, vol)
    }

    fun stopAll() {
        synthesizers.values.forEach { it.setVolume(0f) }
        customMediaPlayer?.stop()
        customMediaPlayer?.release()
        customMediaPlayer = null
        timerJob?.cancel()
        _timerRemainingMs.value = null
    }

    fun release() {
        synthesizers.values.forEach { it.stop() }
        customMediaPlayer?.release()
    }
}
