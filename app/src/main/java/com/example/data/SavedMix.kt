package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_mixes")
data class SavedMix(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val rainVolume: Float = 0f,
    val windVolume: Float = 0f,
    val brownNoiseVolume: Float = 0f,
    val spaceVolume: Float = 0f,
    val oceanVolume: Float = 0f,
    val birdsVolume: Float = 0f,
    val fireVolume: Float = 0f,
    val thunderVolume: Float = 0f,
    val riverVolume: Float = 0f,
    val customRecordingPath: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
