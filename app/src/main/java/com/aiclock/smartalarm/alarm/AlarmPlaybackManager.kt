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
    private const val INITIAL_VOLUME = 0.18f
    private const val VOLUME_STEP = 0.12f
    private const val VOLUME_STEP_MS = 1000L
    private const val ESCALATION_DELAY_MS = 3000L

    private var activeAlarmId: Int? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentVolume = INITIAL_VOLUME
    private var appContext: Context? = null
    private val handler = Handler(Looper.getMainLooper())

    private val volumeTask = object : Runnable {
        override fun run() {
            val player = mediaPlayer ?: return
            currentVolume = (currentVolume + VOLUME_STEP).coerceAtMost(MAX_VOLUME)
            player.setVolume(currentVolume, currentVolume)
            if (currentVolume < MAX_VOLUME) {
                handler.postDelayed(this, VOLUME_STEP_MS)
            }
        }
    }

    private val escalationTask = object : Runnable {
        override fun run() {
            val context = appContext ?: return
            if (mediaPlayer == null || activeAlarmId == null) {
                return
            }

            startVibration(context)
            volumeTask.run()
        }
    }

    fun start(context: Context, alarm: Alarm) {
        if (activeAlarmId != null && activeAlarmId != alarm.id) {
            stop(context, activeAlarmId)
        }

        val applicationContext = context.applicationContext
        val player = runCatching { createPlayer(applicationContext, resolveUri(alarm)) }
            .getOrElse { createPlayer(applicationContext, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)) }

        mediaPlayer?.release()
        mediaPlayer = player
        activeAlarmId = alarm.id
        currentVolume = INITIAL_VOLUME
        appContext = applicationContext

        player.setVolume(currentVolume, currentVolume)
        player.start()
        handler.removeCallbacks(volumeTask)
        handler.removeCallbacks(escalationTask)
        handler.postDelayed(escalationTask, ESCALATION_DELAY_MS)
    }

    fun stop(context: Context, alarmId: Int?) {
        if (alarmId != null && activeAlarmId != alarmId) {
            return
        }

        handler.removeCallbacks(volumeTask)
        handler.removeCallbacks(escalationTask)

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
        currentVolume = INITIAL_VOLUME
        appContext = null
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
