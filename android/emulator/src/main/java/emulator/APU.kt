package com.nes.emulator

/**
 * Implementação da APU (Audio Processing Unit) do NES.
 * Responsável pela síntese de som.
 */
class APU {
    companion object {
        const val SAMPLE_RATE = 44100
        const val FRAME_COUNTER_FREQ = 240  // Hz
    }
    
    // Canais de áudio
    private val pulse1 = PulseChannel()
    private val pulse2 = PulseChannel()
    private val triangle = TriangleChannel()
    private val noise = NoiseChannel()
    private val dmc = DMCChannel()
    
    // Registradores
    var status: Int = 0
    var frameCounter: Int = 0
    var frameCounterMode: Int = 0
    
    // Buffer de áudio
    private val audioBuffer = mutableListOf<Float>()
    private var sampleCounter = 0
    
    fun writeRegister(address: Int, value: Int) {
        when (address) {
            0x4000, 0x4001, 0x4002, 0x4003 -> pulse1.writeRegister(address - 0x4000, value)
            0x4004, 0x4005, 0x4006, 0x4007 -> pulse2.writeRegister(address - 0x4004, value)
            0x4008, 0x4009, 0x400A, 0x400B -> triangle.writeRegister(address - 0x4008, value)
            0x400C, 0x400D, 0x400E, 0x400F -> noise.writeRegister(address - 0x400C, value)
            0x4010, 0x4011, 0x4012, 0x4013 -> dmc.writeRegister(address - 0x4010, value)
            0x4015 -> {
                status = value
                pulse1.enabled = (value and 0x01) != 0
                pulse2.enabled = (value and 0x02) != 0
                triangle.enabled = (value and 0x04) != 0
                noise.enabled = (value and 0x08) != 0
                dmc.enabled = (value and 0x10) != 0
            }
            0x4017 -> frameCounter = value
        }
    }
    
    fun readRegister(address: Int): Int {
        return when (address) {
            0x4015 -> {
                var value = 0
                if (pulse1.lengthCounter > 0) value = value or 0x01
                if (pulse2.lengthCounter > 0) value = value or 0x02
                if (triangle.lengthCounter > 0) value = value or 0x04
                if (noise.lengthCounter > 0) value = value or 0x08
                if (dmc.bytesRemaining > 0) value = value or 0x10
                value
            }
            else -> 0
        }
    }
    
    fun step() {
        pulse1.step()
        pulse2.step()
        triangle.step()
        noise.step()
        dmc.step()
        
        sampleCounter++
        if (sampleCounter >= SAMPLE_RATE / 60) {
            sampleCounter = 0
            val sample = getSample()
            audioBuffer.add(sample)
        }
    }
    
    private fun getSample(): Float {
        val pulse1Sample = pulse1.getSample()
        val pulse2Sample = pulse2.getSample()
        val triangleSample = triangle.getSample()
        val noiseSample = noise.getSample()
        val dmcSample = dmc.getSample()
        
        // Misturar canais com pesos apropriados
        val pulseOut = 0.00752f * (pulse1Sample + pulse2Sample)
        val tndOut = 0.00851f * triangleSample + 0.00494f * noiseSample + 0.00335f * dmcSample
        
        return pulseOut + tndOut
    }
    
    fun getAudioBuffer(): List<Float> {
        return audioBuffer.toList()
    }
    
    fun clearAudioBuffer() {
        audioBuffer.clear()
    }
}

// Canais de áudio
abstract class AudioChannel {
    var enabled = false
    var lengthCounter = 0
    var envelopeCounter = 0
    var envelopeVolume = 15
    
    abstract fun step()
    abstract fun getSample(): Float
    abstract fun writeRegister(address: Int, value: Int)
}

class PulseChannel : AudioChannel() {
    var duty = 0
    var frequency = 0
    var phase = 0
    var envelope = 0
    var lengthCounterLoad = 0
    
    private val dutyTable = arrayOf(
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 1),
        intArrayOf(0, 0, 0, 0, 0, 0, 1, 1),
        intArrayOf(0, 0, 0, 0, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 0, 0)
    )
    
    override fun step() {
        if (enabled && lengthCounter > 0) {
            phase = (phase + 1) % 8
        }
    }
    
    override fun getSample(): Float {
        if (!enabled || lengthCounter == 0) return 0f
        return (dutyTable[duty][phase] * envelopeVolume / 15.0f)
    }
    
    override fun writeRegister(address: Int, value: Int) {
        when (address) {
            0 -> {
                duty = (value shr 6) and 0x03
                envelope = value and 0x0F
            }
            2 -> frequency = (frequency and 0xFF00) or (value and 0xFF)
            3 -> {
                frequency = (frequency and 0x00FF) or ((value and 0x07) shl 8)
                lengthCounter = value shr 3
            }
        }
    }
}

class TriangleChannel : AudioChannel() {
    var frequency = 0
    var phase = 0
    var lengthCounterLoad = 0
    
    private val waveform = intArrayOf(
        15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0,
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15
    )
    
    override fun step() {
        if (enabled && lengthCounter > 0) {
            phase = (phase + 1) % 32
        }
    }
    
    override fun getSample(): Float {
        if (!enabled || lengthCounter == 0) return 0f
        return (waveform[phase] / 15.0f)
    }
    
    override fun writeRegister(address: Int, value: Int) {
        when (address) {
            0 -> lengthCounterLoad = value shr 3
            2 -> frequency = (frequency and 0xFF00) or (value and 0xFF)
            3 -> {
                frequency = (frequency and 0x00FF) or ((value and 0x07) shl 8)
                lengthCounter = value shr 3
            }
        }
    }
}

class NoiseChannel : AudioChannel() {
    var frequency = 0
    var shiftRegister = 1
    var lengthCounterLoad = 0
    var envelope = 0  // Noise channel envelope (separate from base class)
    
    private val frequencyTable = intArrayOf(
        4, 8, 16, 32, 64, 96, 128, 160, 202, 254, 380, 508, 762, 1016, 2034, 4068
    )
    
    override fun step() {
        if (enabled && lengthCounter > 0) {
            val bit0 = shiftRegister and 1
            val bit1 = (shiftRegister shr 1) and 1
            val feedback = bit0 xor bit1
            shiftRegister = (shiftRegister shr 1) or (feedback shl 14)
        }
    }
    
    override fun getSample(): Float {
        if (!enabled || lengthCounter == 0) return 0f
        return if ((shiftRegister and 1) == 0) (envelopeVolume / 15.0f) else 0f
    }
    
    override fun writeRegister(address: Int, value: Int) {
        when (address) {
            0 -> envelope = value and 0x0F
            2 -> frequency = value and 0x0F
            3 -> lengthCounter = value shr 3
        }
    }
}

class DMCChannel : AudioChannel() {
    var frequency = 0
    var outputLevel = 0
    var sampleAddress = 0
    var sampleLength = 0
    var bytesRemaining = 0
    
    override fun step() {
        // Implementação simplificada do DMC
    }
    
    override fun getSample(): Float {
        return (outputLevel / 127.0f)
    }
    
    override fun writeRegister(address: Int, value: Int) {
        when (address) {
            0 -> frequency = value and 0x0F
            1 -> outputLevel = value and 0x7F
            2 -> sampleAddress = value
            3 -> sampleLength = value
        }
    }
}
