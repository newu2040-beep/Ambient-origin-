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
