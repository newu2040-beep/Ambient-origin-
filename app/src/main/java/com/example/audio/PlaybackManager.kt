package com.example.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class PlaybackManager(private val context: Context, private val scope: CoroutineScope) {
    val synthesizers = SoundSynthesizer.SoundType.values().associateWith { SoundSynthesizer(it) }

    private var customMediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var isPlayingPaused = false

    private val activeVolumes = mutableMapOf<SoundSynthesizer.SoundType, Float>()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.example.ambient.STOP_ALL" -> stopAll()
                "com.example.ambient.PAUSE" -> pauseAll()
                "com.example.ambient.RESUME" -> resumeAll()
            }
        }
    }

    private val _timerRemainingMs = MutableStateFlow<Long?>(null)
    val timerRemainingMs: StateFlow<Long?> = _timerRemainingMs

    private var timerJob: Job? = null
    private var fadeOutJob: Job? = null

    init {
        val filter = IntentFilter().apply {
            addAction("com.example.ambient.STOP_ALL")
            addAction("com.example.ambient.PAUSE")
            addAction("com.example.ambient.RESUME")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        // Start synthesizers with 0 volume
        synthesizers.values.forEach { it.start(scope) }
    }

    fun pauseAll() {
        isPlayingPaused = true
        synthesizers.values.forEach { it.pause() }
        customMediaPlayer?.pause()
        checkServiceState()
    }

    fun resumeAll() {
        isPlayingPaused = false
        synthesizers.values.forEach { it.resume() }
        customMediaPlayer?.start()
        checkServiceState()
    }

    private fun checkServiceState() {
        val hasActive = activeVolumes.values.any { it > 0f } || customMediaPlayer?.isPlaying == true || isPlayingPaused
        val intent = Intent(context, AudioService::class.java)
        intent.putExtra("IS_PAUSED", isPlayingPaused)
        if (hasActive) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } else {
            context.stopService(intent)
        }
    }

    fun setVolume(type: SoundSynthesizer.SoundType, volume: Float) {
        activeVolumes[type] = volume
        synthesizers[type]?.setVolume(volume)
        checkServiceState()
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
        checkServiceState()
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
        activeVolumes.clear()
        customMediaPlayer?.stop()
        customMediaPlayer?.release()
        customMediaPlayer = null
        timerJob?.cancel()
        _timerRemainingMs.value = null
        isPlayingPaused = false
        checkServiceState()
    }

    fun release() {
        try { context.unregisterReceiver(receiver) } catch (e: Exception) {}
        synthesizers.values.forEach { it.stop() }
        customMediaPlayer?.release()
    }
}
