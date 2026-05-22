package com.example.ui

import android.Manifest
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.ui.theme.ThemeManager
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

data class SoundData(val type: SoundSynthesizer.SoundType, val name: String, val icon: ImageVector)

val ALL_SOUNDS = listOf(
    SoundData(SoundSynthesizer.SoundType.RAIN, "Rain Drops", Icons.Default.WaterDrop),
    SoundData(SoundSynthesizer.SoundType.WIND, "Breeze", Icons.Default.Air),
    SoundData(SoundSynthesizer.SoundType.BROWN_NOISE, "Deep Rumble", Icons.Default.Waves),
    SoundData(SoundSynthesizer.SoundType.SPACE, "Space Float", Icons.Default.Public),
    SoundData(SoundSynthesizer.SoundType.OCEAN, "Ocean Waves", Icons.Default.Pool),
    SoundData(SoundSynthesizer.SoundType.BIRDS, "Forest Birds", Icons.Default.Park),
    SoundData(SoundSynthesizer.SoundType.FIRE, "Campfire", Icons.Default.LocalFireDepartment),
    SoundData(SoundSynthesizer.SoundType.THUNDER, "Thunderstorm", Icons.Default.FlashOn),
    SoundData(SoundSynthesizer.SoundType.RIVER, "River Stream", Icons.Default.Water)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MixerScreen(viewModel: MainViewModel) {
    val volumes by viewModel.volumes.collectAsStateWithLifecycle()
    val customVol by viewModel.customVol.collectAsStateWithLifecycle()
    val customAudioPath by viewModel.currentCustomAudio.collectAsStateWithLifecycle()
    val currentTheme by ThemeManager.currentTheme.collectAsStateWithLifecycle()
    
    var showSaveDialog by remember { mutableStateOf(false) }
    var mixName by remember { mutableStateOf("") }
    var showThemeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Current Mix",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = { showThemeDialog = true }) {
                Icon(Icons.Default.Palette, contentDescription = "Themes", tint = MaterialTheme.colorScheme.primary)
            }
        }

        WaveIndicator(
            volumes = volumes,
            modifier = Modifier.fillMaxWidth().height(60.dp).padding(bottom = 16.dp)
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(ALL_SOUNDS) { sound ->
                SoundSliderRow(
                    name = sound.name,
                    icon = sound.icon,
                    value = volumes[sound.type] ?: 0f,
                    onValueChange = { viewModel.onVolumeChange(sound.type, it) }
                )
            }

            if (customAudioPath != null) {
                item {
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
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
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

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("App Theme") },
            text = {
                Column {
                    ThemeManager.ThemeOption.values().forEach { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                                .clickable { ThemeManager.currentTheme.value = option; showThemeDialog = false }
                                .padding(vertical = 12.dp)
                        ) {
                            RadioButton(selected = currentTheme == option, onClick = null)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(option.label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
fun WaveIndicator(volumes: Map<SoundSynthesizer.SoundType, Float>, modifier: Modifier = Modifier) {
    val totalVol = volumes.values.sum()
    val infiniteTransition = rememberInfiniteTransition()
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "phase"
    )
    
    val primaryColor = MaterialTheme.colorScheme.primary
    
    Canvas(modifier = modifier) {
        val path = Path()
        val width = size.width
        val height = size.height
        val midY = height / 2f
        
        val amplitude = (totalVol).coerceIn(0f, 3f) * (height / 2.5f)
        
        path.moveTo(0f, midY)
        for (i in 0 until width.toInt() step 5) {
            val x = i.toFloat()
            val normalizedX = x / width
            val y = midY + kotlin.math.sin(normalizedX * 4f * Math.PI + phase).toFloat() * amplitude
            path.lineTo(x, y)
        }
        
        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
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
    val context = LocalContext.current

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
                            IconButton(onClick = { 
                                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                val file = File(downloadsDir, "${mix.name.replace(" ", "_")}_AmbientMix.txt")
                                try {
                                    file.writeText("Ambient Origin Mix: ${mix.name}\nExported offline copy.")
                                    Toast.makeText(context, "Exported to Downloads!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Default.Download, contentDescription = "Download", tint = MaterialTheme.colorScheme.tertiary)
                            }
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
