package com.example.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlin.random.Random

class SoundSynthesizer(val type: SoundType) {
    enum class SoundType { RAIN, WIND, BROWN_NOISE, SPACE, OCEAN, BIRDS, FIRE, THUNDER, RIVER, CRICKETS, FROGS, TRAIN, CITY, FAN }
    
    private val sampleRate = 44100
    private var isPlaying = false
    private var isPlayingPaused = false
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
        audioTrack = android.media.AudioTrack.Builder()
            .setAudioAttributes(android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                .build())
            .setAudioFormat(AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build())
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        audioTrack?.play()
        
        synthJob = scope.launch(Dispatchers.Default) {
            val buffer = ShortArray(bufferSize)
            var phase = 0.0
            var lastOut = 0.0
            
            while (isActive && isPlaying) {
                if (currentVolume < targetVolume) {
                    currentVolume += 0.01f
                    if (currentVolume > targetVolume) currentVolume = targetVolume
                } else if (currentVolume > targetVolume) {
                    currentVolume -= 0.01f
                    if (currentVolume < targetVolume) currentVolume = targetVolume
                }
                
                val effectiveVol = if (isPlayingPaused) 0f else currentVolume * masterVolume

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
                        SoundType.OCEAN -> {
                            val white = (Random.nextDouble() * 2 - 1.0)
                            val lfo = Math.sin(phase * 0.00001)
                            val filterCoeff = 0.01 + 0.015 * (lfo + 1.0) / 2.0
                            sample = lastOut + filterCoeff * (white - lastOut)
                            lastOut = sample
                            sample *= 4.0
                            phase++
                        }
                        SoundType.RIVER -> {
                            val white = (Random.nextDouble() * 2 - 1.0)
                            val filterCoeff = 0.05
                            sample = lastOut + filterCoeff * (white - lastOut)
                            lastOut = sample
                            sample *= 3.0
                        }
                        SoundType.FIRE -> {
                            val white = (Random.nextDouble() * 2 - 1.0)
                            val crackle = if (Random.nextDouble() > 0.999) (Random.nextDouble() * 2 - 1.0) * 10 else 0.0
                            sample = lastOut + 0.02 * (white - lastOut)
                            lastOut = sample
                            sample = (sample * 2.0) + crackle
                        }
                        SoundType.BIRDS -> {
                            val lfo = Math.sin(phase * 0.000005)
                            if (lfo > 0.9) {
                                val chirpPhase = phase * (0.05 + 0.01 * Math.sin(phase * 0.001))
                                sample = Math.sin(chirpPhase) * 0.5
                            } else {
                                sample = 0.0
                            }
                            phase++
                        }
                        SoundType.THUNDER -> {
                            val lfo = Math.sin(phase * 0.000002)
                            if (lfo > 0.99) {
                                val white = (Random.nextDouble() * 2 - 1.0)
                                sample = lastOut + 0.005 * (white - lastOut)
                            } else {
                                sample = lastOut * 0.9999
                            }
                            lastOut = sample
                            sample *= 10.0
                            phase++
                        }
                        SoundType.CRICKETS -> {
                            val env = if (Math.sin(phase * 0.0001) > 0.5) 1.0 else 0.0
                            sample = Math.sin(phase * 0.2) * env * 0.5
                            phase++
                        }
                        SoundType.FROGS -> {
                            val env = if (Math.sin(phase * 0.00005) > 0.95) 1.0 else 0.0
                            sample = (Random.nextDouble() * 2 - 1.0) * env * 0.3
                            phase++
                        }
                        SoundType.TRAIN -> {
                            val lfo = Math.sin(phase * 0.00002)
                            val white = (Random.nextDouble() * 2 - 1.0)
                            sample = lastOut + 0.05 * (white - lastOut) * (lfo + 1.0) / 2.0
                            lastOut = sample
                            sample *= 2.0
                            phase++
                        }
                        SoundType.CITY -> {
                            val white = (Random.nextDouble() * 2 - 1.0)
                            sample = lastOut + 0.03 * (white - lastOut)
                            lastOut = sample
                            sample *= 1.5
                            val beep = if (Random.nextDouble() > 0.99995) Math.sin(phase * 0.1) * 0.5 else 0.0
                            sample += beep
                            phase++
                        }
                        SoundType.FAN -> {
                            val white = (Random.nextDouble() * 2 - 1.0)
                            sample = lastOut + 0.08 * (white - lastOut)
                            lastOut = sample
                            sample *= 2.5
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

    fun pause() {
        isPlayingPaused = true
    }

    fun resume() {
        isPlayingPaused = false
    }
    
    fun stop() {
        isPlaying = false
        synthJob?.cancel()
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }
}
