package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.audio.PlaybackManager
import com.example.data.MixRepository

class MainViewModelFactory(
    private val repository: MixRepository,
    private val playbackManager: PlaybackManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, playbackManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
