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

    // Slider ranges are 0f..1f
    private val _rainVol = MutableStateFlow(0f)
    val rainVol: StateFlow<Float> = _rainVol
    
    private val _windVol = MutableStateFlow(0f)
    val windVol: StateFlow<Float> = _windVol
    
    private val _brownNoiseVol = MutableStateFlow(0f)
    val brownNoiseVol: StateFlow<Float> = _brownNoiseVol

    private val _spaceVol = MutableStateFlow(0f)
    val spaceVol: StateFlow<Float> = _spaceVol

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
        when(type) {
            SoundSynthesizer.SoundType.RAIN -> _rainVol.value = value
            SoundSynthesizer.SoundType.WIND -> _windVol.value = value
            SoundSynthesizer.SoundType.BROWN_NOISE -> _brownNoiseVol.value = value
            SoundSynthesizer.SoundType.SPACE -> _spaceVol.value = value
        }
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
                rainVolume = _rainVol.value,
                windVolume = _windVol.value,
                brownNoiseVolume = _brownNoiseVol.value,
                spaceVolume = _spaceVol.value,
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
        onVolumeChange(SoundSynthesizer.SoundType.RAIN, 0f)
        onVolumeChange(SoundSynthesizer.SoundType.WIND, 0f)
        onVolumeChange(SoundSynthesizer.SoundType.BROWN_NOISE, 0f)
        onVolumeChange(SoundSynthesizer.SoundType.SPACE, 0f)
        removeCustomFile()
        playbackManager.stopAll()
    }

    override fun onCleared() {
        super.onCleared()
        playbackManager.release()
    }
}
