package com.ayni.mobile.ui.proximity

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

/** Beep sintetizado que respeta el volumen multimedia, no el de notificaciones. */
internal class MediaProximityBeepPlayer {
    private val sampleRate = 24_000
    private val track: AudioTrack? = runCatching {
        val minimumBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(minimumBuffer.coerceAtLeast(sampleRate / 5))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()
            .also { it.setVolume(0.70f) }
    }.getOrNull()

    fun beep(frequencyHz: Int, durationMillis: Int) {
        val audioTrack = track?.takeIf { it.state == AudioTrack.STATE_INITIALIZED } ?: return
        val sampleCount = (sampleRate * durationMillis / 1_000).coerceAtLeast(1)
        val edgeSamples = (sampleCount * 0.16).toInt().coerceAtLeast(1)
        val samples = ShortArray(sampleCount) { index ->
            val envelope = when {
                index < edgeSamples -> index.toDouble() / edgeSamples
                index >= sampleCount - edgeSamples -> (sampleCount - index - 1).toDouble() / edgeSamples
                else -> 1.0
            }.coerceIn(0.0, 1.0)
            val wave = sin(2.0 * PI * frequencyHz * index / sampleRate)
            (wave * envelope * Short.MAX_VALUE * 0.34).toInt().toShort()
        }
        runCatching {
            if (audioTrack.playState != AudioTrack.PLAYSTATE_PLAYING) audioTrack.play()
            audioTrack.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
        }
    }

    fun release() {
        track?.let { audioTrack ->
            runCatching { audioTrack.pause() }
            runCatching { audioTrack.flush() }
            runCatching { audioTrack.release() }
        }
    }
}
