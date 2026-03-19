package com.aiclock.smartalarm.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.aiclock.smartalarm.model.Alarm

object AlarmPlaybackManager {
    private const val MAX_VOLUME = 1f
    private const val MIN_VOLUME = 0.08f
    private const val STEP = 0.08f
    private const val STEP_MS = 1500L

    private var activeAlarmId: Int? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentVolume = MIN_VOLUME
    private val handler = Handler(Looper.getMainLooper())

    private val volumeTask = object : Runnable {
        override fun run() {
            val player = mediaPlayer ?: return
            currentVolume = (currentVolume + STEP).coerceAtMost(MAX_VOLUME)
            player.setVolume(currentVolume, currentVolume)
            if (currentVolume < MAX_VOLUME) {
                handler.postDelayed(this, STEP_MS)
            }
        }
    }

    fun start(context: Context, alarm: Alarm) {
        if (activeAlarmId != null && activeAlarmId != alarm.id) {
            stop(context, activeAlarmId)
        }

        val player = runCatching { createPlayer(context, resolveUri(alarm)) }
            .getOrElse { createPlayer(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)) }

        mediaPlayer?.release()
        mediaPlayer = player
        activeAlarmId = alarm.id
        currentVolume = MIN_VOLUME

        player.setVolume(currentVolume, currentVolume)
        player.start()
        handler.removeCallbacks(volumeTask)
        handler.postDelayed(volumeTask, STEP_MS)

        startVibration(context)
    }

    fun stop(context: Context, alarmId: Int?) {
        if (alarmId != null && activeAlarmId != alarmId) {
            return
        }

        handler.removeCallbacks(volumeTask)

        mediaPlayer?.let { player ->
            runCatching {
                if (player.isPlaying) {
                    player.stop()
                }
            }
            player.release()
        }

        mediaPlayer = null
        activeAlarmId = null
        currentVolume = MIN_VOLUME
        cancelVibration(context)
    }

    private fun resolveUri(alarm: Alarm): Uri {
        if (alarm.ringtoneUri.isNotBlank()) {
            return Uri.parse(alarm.ringtoneUri)
        }
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
    }

    private fun createPlayer(context: Context, uri: Uri): MediaPlayer {
        return MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setDataSource(context, uri)
            isLooping = true
            prepare()
        }
    }

    private fun startVibration(context: Context) {
        val pattern = longArrayOf(0, 300, 240, 350)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibrator = (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 1))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, 1)
            }
        }
    }

    private fun cancelVibration(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibrator = (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            vibrator.cancel()
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.cancel()
        }
    }
}
