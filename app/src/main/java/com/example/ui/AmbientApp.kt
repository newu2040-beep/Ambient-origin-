package com.example.ui

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.SoundSynthesizer
import com.example.data.SavedMix
import kotlinx.coroutines.launch
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.example.audio.CustomAudioRecorder
import java.io.File
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun AmbientApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    var currentRoute by remember { mutableStateOf("Mixer") }

    val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
    }
    val permissionState = rememberMultiplePermissionsState(permissions)

    LaunchedEffect(Unit) {
        if (!permissionState.allPermissionsGranted) {
            permissionState.launchMultiplePermissionRequest()
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                NavigationBarItem(
                    selected = currentRoute == "Mixer",
                    onClick = { currentRoute = "Mixer" },
                    icon = { Icon(Icons.Default.Tune, contentDescription = "Mixer") },
                    label = { Text("Mixer") }
                )
                NavigationBarItem(
                    selected = currentRoute == "Library",
                    onClick = { currentRoute = "Library" },
                    icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Library") },
                    label = { Text("Library") }
                )
                NavigationBarItem(
                    selected = currentRoute == "Record",
                    onClick = { currentRoute = "Record" },
                    icon = { Icon(Icons.Default.Mic, contentDescription = "Record") },
                    label = { Text("Record") }
                )
                NavigationBarItem(
                    selected = currentRoute == "Timer",
                    onClick = { currentRoute = "Timer" },
                    icon = { Icon(Icons.Default.Timer, contentDescription = "Timer") },
                    label = { Text("Timer") }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentRoute) {
                "Mixer" -> MixerScreen(viewModel)
                "Library" -> LibraryScreen(viewModel)
                "Record" -> RecordScreen(
                    viewModel,
                    File(context.filesDir, "custom_recordings").also { it.mkdirs() }
                )
                "Timer" -> TimerScreen(viewModel)
            }
            
            // Global timer overlay
            val timerMs by viewModel.timerRemainingMs.collectAsStateWithLifecycle()
            if (timerMs != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    val mins = timerMs!! / 60000
                    val secs = (timerMs!! % 60000) / 1000
                    Text(
                        "${String.format("%02d", mins)}:${String.format("%02d", secs)} remaining",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MixerScreen(viewModel: MainViewModel) {
    val rainVol by viewModel.rainVol.collectAsStateWithLifecycle()
    val windVol by viewModel.windVol.collectAsStateWithLifecycle()
    val brownNoiseVol by viewModel.brownNoiseVol.collectAsStateWithLifecycle()
    val spaceVol by viewModel.spaceVol.collectAsStateWithLifecycle()
    val customVol by viewModel.customVol.collectAsStateWithLifecycle()
    val customAudioPath by viewModel.currentCustomAudio.collectAsStateWithLifecycle()
    
    var showSaveDialog by remember { mutableStateOf(false) }
    var mixName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Current Mix",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        SoundSliderRow(
            name = "Rain Drops",
            icon = Icons.Default.WaterDrop,
            value = rainVol,
            onValueChange = { viewModel.onVolumeChange(SoundSynthesizer.SoundType.RAIN, it) }
        )
        SoundSliderRow(
            name = "Breeze",
            icon = Icons.Default.Air,
            value = windVol,
            onValueChange = { viewModel.onVolumeChange(SoundSynthesizer.SoundType.WIND, it) }
        )
        SoundSliderRow(
            name = "Deep Rumble",
            icon = Icons.Default.Waves,
            value = brownNoiseVol,
            onValueChange = { viewModel.onVolumeChange(SoundSynthesizer.SoundType.BROWN_NOISE, it) }
        )
        SoundSliderRow(
            name = "Space Float",
            icon = Icons.Default.Public,
            value = spaceVol,
            onValueChange = { viewModel.onVolumeChange(SoundSynthesizer.SoundType.SPACE, it) }
        )

        if (customAudioPath != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AudioFile, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Custom Layer", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                    Slider(
                        value = customVol,
                        onValueChange = { viewModel.onCustomVolumeChange(it) }
                    )
                }
                IconButton(onClick = { viewModel.removeCustomFile() }) {
                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(onClick = { viewModel.stopAll() }) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Stop All")
            }
            Button(onClick = { showSaveDialog = true }) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Mix")
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Mix") },
            text = {
                OutlinedTextField(
                    value = mixName,
                    onValueChange = { mixName = it },
                    label = { Text("Mix Name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (mixName.isNotBlank()) {
                        viewModel.saveMix(mixName)
                        showSaveDialog = false
                        mixName = ""
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SoundSliderRow(
    name: String,
    icon: ImageVector,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Slider(
                value = value,
                onValueChange = onValueChange
            )
        }
    }
}

@Composable
fun LibraryScreen(viewModel: MainViewModel) {
    val mixes by viewModel.allMixes.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Saved Soundscapes",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        if (mixes.isEmpty()) {
            Text("No saved mixes yet.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(mixes) { mix ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable { viewModel.loadMix(mix) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PlayCircle, contentDescription = "Play", tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(mix.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.deleteMix(mix) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecordScreen(viewModel: MainViewModel, dir: File) {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var currentRecorder by remember { mutableStateOf<CustomAudioRecorder?>(null) }
    var recordingFiles by remember { mutableStateOf(dir.listFiles()?.toList() ?: emptyList()) }

    DisposableEffect(Unit) {
        onDispose {
            currentRecorder?.stop()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Record Custom Loop", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(32.dp))
        
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    .clickable {
                        if (isRecording) {
                            currentRecorder?.stop()
                            isRecording = false
                            recordingFiles = dir.listFiles()?.toList() ?: emptyList()
                        } else {
                            val newFile = File(dir, "Record_${System.currentTimeMillis()}.m4a")
                            currentRecorder = CustomAudioRecorder(context, newFile).apply { start() }
                            isRecording = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = "Record Toggle",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Text("Saved Recordings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(recordingFiles) { file ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(file.name, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.playCustomFile(file.absolutePath) }) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Add to mix", tint = MaterialTheme.colorScheme.secondary)
                    }
                    IconButton(onClick = {
                        file.delete()
                        recordingFiles = dir.listFiles()?.toList() ?: emptyList()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun TimerScreen(viewModel: MainViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Sleep Timer",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        Text(
            "Set a timer to automatically fade out and stop the sounds.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        val options = listOf(15, 30, 45, 60, 90)
        options.forEach { minutes ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { viewModel.setTimer(minutes, 5) }, // 5 min fadeout
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("$minutes Minutes", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        OutlinedButton(
            onClick = { viewModel.setTimer(0, 0) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel Timer")
        }
    }
}
