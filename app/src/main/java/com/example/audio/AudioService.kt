package com.example.audio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class AudioService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channelId = "ambient_origin_playback"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Playback Active", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }

        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE)
        
        val pauseIntent = Intent("com.example.ambient.PAUSE")
        val pausePendingIntent = PendingIntent.getBroadcast(this, 2, pauseIntent, PendingIntent.FLAG_IMMUTABLE)

        val resumeIntent = Intent("com.example.ambient.RESUME")
        val resumePendingIntent = PendingIntent.getBroadcast(this, 3, resumeIntent, PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent("com.example.ambient.STOP_ALL")
        val stopPendingIntent = PendingIntent.getBroadcast(this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val isPaused = intent?.getBooleanExtra("IS_PAUSED", false) ?: false

        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Ambient Origin Background Audio")
            .setContentText(if (isPaused) "Paused" else "Playing nature sounds...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            
        if (isPaused) {
            builder.addAction(android.R.drawable.ic_media_play, "Resume", resumePendingIntent)
        } else {
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent)
        }
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
        builder.setOngoing(!isPaused)

        startForeground(1, builder.build())
        return START_STICKY
    }
}

