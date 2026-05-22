package com.example.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

class CustomAudioRecorder(private val context: Context, private val outputFile: File) {
    private var recorder: MediaRecorder? = null
    var isRecording = false
        private set

    fun start() {
        try {
            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            isRecording = true
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Recording failed", e)
        }
    }


    fun stop() {
        if (isRecording) {
            try {
                recorder?.stop()
                recorder?.release()
            } catch (e: Exception) {
               Log.e("AudioRecorder", "Stop failed", e)
            } finally {
                recorder = null
                isRecording = false
            }
        }
    }
}
