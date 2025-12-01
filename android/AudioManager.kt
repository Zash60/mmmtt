package com.nes.android

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.nes.emulator.Console
import kotlin.math.max
import kotlin.math.min

/**
 * Gerencia reprodução de áudio do emulador.
 */
class AudioManager {
    
    companion object {
        const val SAMPLE_RATE = 44100
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }
    
    private var audioTrack: AudioTrack? = null
    private var audioThread: AudioThread? = null
    private var isRunning = false
    private var console: Console? = null
    
    fun initialize() {
        val bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        
        audioTrack = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(CHANNEL_CONFIG)
                .setEncoding(AUDIO_FORMAT)
                .build(),
            bufferSize,
            AudioTrack.MODE_STREAM,
            android.media.AudioManager.AUDIO_SESSION_ID_GENERATE
        )
    }
    
    fun setConsole(console: Console) {
        this.console = console
    }
    
    fun start() {
        if (audioTrack == null) {
            initialize()
        }
        
        audioTrack?.play()
        isRunning = true
        audioThread = AudioThread(this)
        audioThread?.start()
    }
    
    fun stop() {
        isRunning = false
        audioThread?.join()
        audioThread = null
        audioTrack?.stop()
    }
    
    fun release() {
        stop()
        audioTrack?.release()
        audioTrack = null
    }
    
    fun setVolume(volume: Float) {
        val clampedVolume = max(0f, min(1f, volume))
        audioTrack?.setVolume(clampedVolume)
    }
    
    private fun processAudio() {
        val console = console ?: return
        val audioTrack = audioTrack ?: return
        
        val audioBuffer = console.getAudioBuffer()
        if (audioBuffer.isEmpty()) return
        
        // Converter float para short (PCM 16-bit)
        val pcmBuffer = ShortArray(audioBuffer.size)
        for (i in audioBuffer.indices) {
            val sample = audioBuffer[i]
            val clampedSample = max(-1f, min(1f, sample))
            pcmBuffer[i] = (clampedSample * 32767).toInt().toShort()
        }
        
        // Escrever para AudioTrack
        audioTrack.write(pcmBuffer, 0, pcmBuffer.size, AudioTrack.WRITE_BLOCKING)
        
        // Limpar buffer
        console.clearAudioBuffer()
    }
    
    private inner class AudioThread(val audioManager: AudioManager) : Thread() {
        override fun run() {
            while (isRunning) {
                try {
                    audioManager.processAudio()
                    sleep(10)
                } catch (e: InterruptedException) {
                    break
                }
            }
        }
    }
}
