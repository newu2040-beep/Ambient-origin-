package com.example.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlin.random.Random

class SoundSynthesizer(val type: SoundType) {
    enum class SoundType { RAIN, WIND, BROWN_NOISE, SPACE }
    
    private val sampleRate = 44100
    private var isPlaying = false
    private var targetVolume = 0f
    private var currentVolume = 0f
    private var masterVolume = 1f
    private var audioTrack: AudioTrack? = null
    private var synthJob: Job? = null
    
    fun start(scope: CoroutineScope) {
        if (isPlaying) return
        isPlaying = true
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        // Adjust for mono vs stereo? Mono is fine for these
        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
            AudioTrack.MODE_STREAM
        )
        audioTrack?.play()
        
        synthJob = scope.launch(Dispatchers.Default) {
            val buffer = ShortArray(bufferSize)
            var phase = 0.0
            var lastOut = 0.0
            
            while (isActive && isPlaying) {
                // smooth volume transition
                if (currentVolume < targetVolume) {
                    currentVolume += 0.01f
                    if (currentVolume > targetVolume) currentVolume = targetVolume
                } else if (currentVolume > targetVolume) {
                    currentVolume -= 0.01f
                    if (currentVolume < targetVolume) currentVolume = targetVolume
                }
                
                val effectiveVol = currentVolume * masterVolume

                for (i in buffer.indices) {
                    var sample = 0.0
                    when (type) {
                        SoundType.BROWN_NOISE -> {
                            val white = (Random.nextDouble() * 2 - 1.0)
                            sample = (lastOut + 0.02 * white) / 1.02
                            lastOut = sample
                            sample *= 3.0 
                        }
                        SoundType.RAIN -> {
                            val white = (Random.nextDouble() * 2 - 1.0)
                            // A bit more high frequency than brown noise
                            sample = lastOut + 0.1 * (white - lastOut)
                            lastOut = sample
                            sample *= 2.0
                        }
                        SoundType.WIND -> {
                             val white = (Random.nextDouble() * 2 - 1.0)
                             val lfo = Math.sin(phase * 0.00005) 
                             val filterCoeff = 0.02 + 0.01 * lfo
                             sample = lastOut + filterCoeff * (white - lastOut)
                             lastOut = sample
                             sample *= 5.0
                             phase++
                        }
                        SoundType.SPACE -> {
                            val lfo = Math.sin(phase * 0.00001)
                            sample = Math.sin(phase * 2.0 * Math.PI * 100.0 / sampleRate) * 0.5 +
                                     Math.sin(phase * 2.0 * Math.PI * (103.0 + lfo) / sampleRate) * 0.5
                            phase++
                        }
                    }
                    
                    val finalSample = (sample * 32767 * effectiveVol).toInt()
                    buffer[i] = finalSample.coerceIn(-32768, 32767).toShort()
                }
                audioTrack?.write(buffer, 0, buffer.size)
            }
        }
    }
    
    fun setVolume(vol: Float) {
        this.targetVolume = vol
    }

    fun setMasterVolume(vol: Float) {
        this.masterVolume = vol
    }
    
    fun stop() {
        isPlaying = false
        synthJob?.cancel()
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }
}
