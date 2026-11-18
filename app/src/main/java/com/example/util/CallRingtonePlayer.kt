package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * WhatsApp-style audio ringing & vibration controller:
 * - Incoming call: continuous looping system ringtone + rhythmic vibration until answered/declined/ended.
 * - Outgoing call: soft repeating dual-burst ringback ("trrr-trrr ... trrr-trrr") via voice-call stream.
 */
class CallRingtonePlayer(
    private val appContext: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var mediaPlayer: MediaPlayer? = null
    private var fallbackRingtone: Ringtone? = null
    private var toneGenerator: ToneGenerator? = null
    private var outgoingJob: Job? = null
    private var currentMode: Mode = Mode.IDLE

    private enum class Mode {
        IDLE,
        INCOMING,
        OUTGOING,
    }

    @Synchronized
    fun startIncomingRingtone() {
        if (currentMode == Mode.INCOMING) return
        stopAll()
        currentMode = Mode.INCOMING

        val audioManager = runCatching {
            appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        }.getOrNull()
        val ringerMode = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL

        if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            val startedPlayer = runCatching {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .build()
                    )
                    setDataSource(appContext, uri)
                    isLooping = true
                    prepare()
                    start()
                }
            }.onSuccess { player ->
                mediaPlayer = player
            }.isSuccess

            if (!startedPlayer) {
                runCatching {
                    val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    RingtoneManager.getRingtone(appContext, uri)?.apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            isLooping = true
                        }
                        play()
                        fallbackRingtone = this
                    }
                }
            }
        }

        if (ringerMode != AudioManager.RINGER_MODE_SILENT) {
            startVibrationLoop()
        }
    }

    @Synchronized
    fun startOutgoingRingback() {
        if (currentMode == Mode.OUTGOING) return
        stopAll()
        currentMode = Mode.OUTGOING

        runCatching {
            val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
        }

        val generator = runCatching {
            ToneGenerator(AudioManager.STREAM_VOICE_CALL, 55)
        }.getOrNull()
        toneGenerator = generator

        outgoingJob = scope.launch {
            while (isActive && currentMode == Mode.OUTGOING) {
                runCatching { generator?.startTone(ToneGenerator.TONE_SUP_RINGTONE, 420) }
                delay(560L)
                if (!isActive || currentMode != Mode.OUTGOING) break
                runCatching { generator?.startTone(ToneGenerator.TONE_SUP_RINGTONE, 420) }
                delay(2400L)
            }
        }
    }

    @Synchronized
    fun stopAll() {
        currentMode = Mode.IDLE
        outgoingJob?.cancel()
        outgoingJob = null

        runCatching {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) mp.stop()
                mp.release()
            }
        }
        mediaPlayer = null

        runCatching {
            fallbackRingtone?.stop()
        }
        fallbackRingtone = null

        runCatching {
            toneGenerator?.stopTone()
            toneGenerator?.release()
        }
        toneGenerator = null

        stopVibration()
    }

    private fun startVibrationLoop() {
        val pattern = longArrayOf(0L, 550L, 400L, 550L, 1200L)
        runCatching {
            val vibrator = getVibrator() ?: return
            if (!vibrator.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, 0)
            }
        }
    }

    private fun stopVibration() {
        runCatching {
            getVibrator()?.cancel()
        }
    }

    private fun getVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
