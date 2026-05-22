package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.audio.PlaybackManager
import com.example.data.AppDatabase
import com.example.data.MixRepository
import com.example.ui.AmbientApp
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private lateinit var db: AppDatabase
    private lateinit var repository: MixRepository
    private lateinit var playbackManager: PlaybackManager
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "ambient_db"
        ).build()
        repository = MixRepository(db.savedMixDao())
        playbackManager = PlaybackManager(applicationContext, lifecycleScope)
        
        val factory = MainViewModelFactory(repository, playbackManager)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AmbientApp(viewModel)
                }
            }
        }
    }
}

