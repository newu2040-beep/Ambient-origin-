package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.PlaybackManager
import com.example.audio.SoundSynthesizer
import com.example.data.MixRepository
import com.example.data.SavedMix
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(
    private val repository: MixRepository,
    private val playbackManager: PlaybackManager
) : ViewModel() {

    private val _volumes = MutableStateFlow(SoundSynthesizer.SoundType.values().associateWith { 0f })
    val volumes: StateFlow<Map<SoundSynthesizer.SoundType, Float>> = _volumes

    private val _customVol = MutableStateFlow(0f)
    val customVol: StateFlow<Float> = _customVol

    private val _currentCustomAudio = MutableStateFlow<String?>(null)
    val currentCustomAudio: StateFlow<String?> = _currentCustomAudio

    val timerRemainingMs = playbackManager.timerRemainingMs

    val allMixes = repository.allMixes.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun onVolumeChange(type: SoundSynthesizer.SoundType, value: Float) {
        _volumes.value = _volumes.value.toMutableMap().apply { put(type, value) }
        playbackManager.setVolume(type, value)
    }

    fun onCustomVolumeChange(value: Float) {
        _customVol.value = value
        playbackManager.playCustomAudio(_currentCustomAudio.value, value)
    }

    fun playCustomFile(filePath: String) {
        _currentCustomAudio.value = filePath
        // default volume 0.5 when selected
        if (_customVol.value == 0f) _customVol.value = 0.5f
        playbackManager.playCustomAudio(filePath, _customVol.value)
    }
    
    fun removeCustomFile() {
        _currentCustomAudio.value = null
        playbackManager.playCustomAudio(null, 0f)
    }

    fun saveMix(name: String) {
        viewModelScope.launch {
            val mix = SavedMix(
                name = name,
                rainVolume = _volumes.value[SoundSynthesizer.SoundType.RAIN] ?: 0f,
                windVolume = _volumes.value[SoundSynthesizer.SoundType.WIND] ?: 0f,
                brownNoiseVolume = _volumes.value[SoundSynthesizer.SoundType.BROWN_NOISE] ?: 0f,
                spaceVolume = _volumes.value[SoundSynthesizer.SoundType.SPACE] ?: 0f,
                oceanVolume = _volumes.value[SoundSynthesizer.SoundType.OCEAN] ?: 0f,
                birdsVolume = _volumes.value[SoundSynthesizer.SoundType.BIRDS] ?: 0f,
                fireVolume = _volumes.value[SoundSynthesizer.SoundType.FIRE] ?: 0f,
                thunderVolume = _volumes.value[SoundSynthesizer.SoundType.THUNDER] ?: 0f,
                riverVolume = _volumes.value[SoundSynthesizer.SoundType.RIVER] ?: 0f,
                cricketsVolume = _volumes.value[SoundSynthesizer.SoundType.CRICKETS] ?: 0f,
                frogsVolume = _volumes.value[SoundSynthesizer.SoundType.FROGS] ?: 0f,
                trainVolume = _volumes.value[SoundSynthesizer.SoundType.TRAIN] ?: 0f,
                cityVolume = _volumes.value[SoundSynthesizer.SoundType.CITY] ?: 0f,
                fanVolume = _volumes.value[SoundSynthesizer.SoundType.FAN] ?: 0f,
                customRecordingPath = _currentCustomAudio.value
            )
            repository.insert(mix)
        }
    }

    fun loadMix(mix: SavedMix) {
        onVolumeChange(SoundSynthesizer.SoundType.RAIN, mix.rainVolume)
        onVolumeChange(SoundSynthesizer.SoundType.WIND, mix.windVolume)
        onVolumeChange(SoundSynthesizer.SoundType.BROWN_NOISE, mix.brownNoiseVolume)
        onVolumeChange(SoundSynthesizer.SoundType.SPACE, mix.spaceVolume)
        onVolumeChange(SoundSynthesizer.SoundType.OCEAN, mix.oceanVolume)
        onVolumeChange(SoundSynthesizer.SoundType.BIRDS, mix.birdsVolume)
        onVolumeChange(SoundSynthesizer.SoundType.FIRE, mix.fireVolume)
        onVolumeChange(SoundSynthesizer.SoundType.THUNDER, mix.thunderVolume)
        onVolumeChange(SoundSynthesizer.SoundType.RIVER, mix.riverVolume)
        onVolumeChange(SoundSynthesizer.SoundType.CRICKETS, mix.cricketsVolume)
        onVolumeChange(SoundSynthesizer.SoundType.FROGS, mix.frogsVolume)
        onVolumeChange(SoundSynthesizer.SoundType.TRAIN, mix.trainVolume)
        onVolumeChange(SoundSynthesizer.SoundType.CITY, mix.cityVolume)
        onVolumeChange(SoundSynthesizer.SoundType.FAN, mix.fanVolume)
        
        if (mix.customRecordingPath != null) {
            playCustomFile(mix.customRecordingPath)
        } else {
            removeCustomFile()
        }
    }

    fun deleteMix(mix: SavedMix) {
        viewModelScope.launch {
            repository.deleteById(mix.id)
        }
    }

    fun setTimer(minutes: Int, fadeOutMinutes: Int) {
        playbackManager.setTimer(minutes, fadeOutMinutes)
    }
    
    fun exportMix(context: Context, mix: SavedMix) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val notificationManager = context.getSystemService(android.app.NotificationManager::class.java)
            val channelId = "export_channel"
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(channelId, "Downloads", android.app.NotificationManager.IMPORTANCE_LOW)
                notificationManager?.createNotificationChannel(channel)
            }
            val notifId = mix.name.hashCode()
            val startNotif = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setContentTitle("Exporting ${mix.name}...")
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setProgress(100, 0, true)
                .build()
            notificationManager?.notify(notifId, startNotif)

            try {
                val resolver = context.contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "${mix.name.replace(" ", "_")}_AmbientMix.wav")
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "audio/wav")
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                    }
                }
                val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                } else {
                    val dir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                    dir.mkdirs()
                    val file = File(dir, "${mix.name.replace(" ", "_")}_AmbientMix.wav")
                    android.net.Uri.fromFile(file)
                }

                if (uri != null) {
                    val outputStream = if (uri.scheme == "file") {
                        java.io.FileOutputStream(File(uri.path!!))
                    } else {
                        resolver.openOutputStream(uri)
                    }
                    outputStream?.use { out ->
                        val sampleRate = 44100
                        val durationInSeconds = 10
                        val totalSamples = sampleRate * durationInSeconds
                        val channels = 1
                        val bitsPerSample = 16
                        
                        val byteRate = sampleRate * channels * bitsPerSample / 8
                        val totalDataLen = totalSamples * channels * bitsPerSample / 8
                        val totalAudioLen = totalDataLen + 36
                        
                        val header = ByteArray(44)
                        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
                        header[4] = (totalAudioLen and 0xff).toByte()
                        header[5] = ((totalAudioLen shr 8) and 0xff).toByte()
                        header[6] = ((totalAudioLen shr 16) and 0xff).toByte()
                        header[7] = ((totalAudioLen shr 24) and 0xff).toByte()
                        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
                        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
                        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0
                        header[20] = 1; header[21] = 0
                        header[22] = channels.toByte(); header[23] = 0
                        header[24] = (sampleRate and 0xff).toByte()
                        header[25] = ((sampleRate shr 8) and 0xff).toByte()
                        header[26] = ((sampleRate shr 16) and 0xff).toByte()
                        header[27] = ((sampleRate shr 24) and 0xff).toByte()
                        header[28] = (byteRate and 0xff).toByte()
                        header[29] = ((byteRate shr 8) and 0xff).toByte()
                        header[30] = ((byteRate shr 16) and 0xff).toByte()
                        header[31] = ((byteRate shr 24) and 0xff).toByte()
                        header[32] = (channels * bitsPerSample / 8).toByte(); header[33] = 0
                        header[34] = bitsPerSample.toByte(); header[35] = 0
                        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
                        header[40] = (totalDataLen and 0xff).toByte()
                        header[41] = ((totalDataLen shr 8) and 0xff).toByte()
                        header[42] = ((totalDataLen shr 16) and 0xff).toByte()
                        header[43] = ((totalDataLen shr 24) and 0xff).toByte()
                        
                        out.write(header)
                        
                        val buffer = ByteArray(4096)
                        var bytesWritten = 0
                        while (bytesWritten < totalDataLen) {
                            for(i in buffer.indices) {
                                buffer[i] = (kotlin.math.sin(bytesWritten * 0.05) * 100).toInt().toByte()
                            }
                            out.write(buffer)
                            bytesWritten += buffer.size
                            
                            // progress update
                            if (bytesWritten % (4096 * 10) == 0) {
                                val progressResult = androidx.core.app.NotificationCompat.Builder(context, channelId)
                                    .setContentTitle("Exporting ${mix.name}...")
                                    .setSmallIcon(android.R.drawable.stat_sys_download)
                                    .setProgress(totalDataLen, bytesWritten, false)
                                    .build()
                                notificationManager?.notify(notifId, progressResult)
                            }
                        }
                    }
                    val finishNotif = androidx.core.app.NotificationCompat.Builder(context, channelId)
                        .setContentTitle("Export Complete")
                        .setContentText("${mix.name} saved to Downloads")
                        .setSmallIcon(android.R.drawable.stat_sys_download_done)
                        .setAutoCancel(true)
                        .build()
                    notificationManager?.notify(notifId, finishNotif)
                } else {
                    throw Exception("Could not create file")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val failNotif = androidx.core.app.NotificationCompat.Builder(context, channelId)
                    .setContentTitle("Export Failed")
                    .setContentText(e.message)
                    .setSmallIcon(android.R.drawable.stat_sys_warning)
                    .build()
                notificationManager?.notify(notifId, failNotif)
            }
        }
    }

    fun stopAll() {
        SoundSynthesizer.SoundType.values().forEach {
            onVolumeChange(it, 0f)
        }
        removeCustomFile()
        playbackManager.stopAll()
    }

    override fun onCleared() {
        super.onCleared()
        playbackManager.release()
    }
}
