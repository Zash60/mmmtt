package com.nes.emulator

/**
 * Classe Console que integra todos os componentes do NES.
 * Responsável pela sincronização entre CPU, PPU e APU.
 */
class Console(val cartridge: Cartridge) {
    val memory: Memory
    val cpu: CPU
    val ppu: PPU
    val apu: APU
    
    var running = false
    var paused = false
    var frameCount = 0
    var speed = 1.0f  // 1.0 = velocidade normal
    
    // Controladores
    var controller1 = 0
    var controller2 = 0
    var controllerStrobe = false
    var controllerIndex1 = 0
    var controllerIndex2 = 0
    
    init {
        memory = Memory(cartridge)
        cpu = CPU(memory)
        ppu = PPU(cartridge)
        apu = APU()
        
        // Inicializar PC para o endereço de reset
        cpu.pc = memory.readWord(0xFFFC)
    }
    
    fun reset() {
        cpu.pc = memory.readWord(0xFFFC)
        cpu.sp = 0xFF
        cpu.a = 0
        cpu.x = 0
        cpu.y = 0
        cpu.setStatus(0x24)
        frameCount = 0
    }
    
    fun step() {
        if (paused) return
        
        // CPU executa 3 ciclos para cada ciclo de PPU
        for (i in 0 until 3) {
            cpu.step()
            
            // Verificar NMI da PPU
            if (ppu.isNmiRequested()) {
                cpu.nmiRequested = true
            }
        }
        
        // PPU executa
        ppu.step()
        ppu.step()
        ppu.step()
        
        // APU executa
        apu.step()
        
        // Verificar fim de frame
        if (ppu.getScanline() == 0 && ppu.getCycle() == 0) {
            frameCount++
        }
    }
    
    fun runFrame() {
        val startCycles = cpu.cycles
        val targetCycles = (29780.5 / speed).toLong()  // Ciclos por frame em velocidade normal
        
        while (cpu.cycles - startCycles < targetCycles) {
            step()
        }
    }
    
    fun getFrameBuffer(): IntArray {
        return ppu.getFrameBuffer()
    }
    
    fun getAudioBuffer(): List<Float> {
        return apu.getAudioBuffer()
    }
    
    fun clearAudioBuffer() {
        apu.clearAudioBuffer()
    }
    
    fun setController1(value: Int) {
        controller1 = value
        if (controllerStrobe) {
            controllerIndex1 = 0
        }
    }
    
    fun setController2(value: Int) {
        controller2 = value
        if (controllerStrobe) {
            controllerIndex2 = 0
        }
    }
    
    fun readController1(): Int {
        if (controllerStrobe) {
            controllerIndex1 = 0
        }
        
        val value = if ((controller1 shr controllerIndex1) and 1 != 0) 1 else 0
        controllerIndex1++
        if (controllerIndex1 > 7) {
            controllerIndex1 = 7
        }
        return value
    }
    
    fun readController2(): Int {
        if (controllerStrobe) {
            controllerIndex2 = 0
        }
        
        val value = if ((controller2 shr controllerIndex2) and 1 != 0) 1 else 0
        controllerIndex2++
        if (controllerIndex2 > 7) {
            controllerIndex2 = 7
        }
        return value
    }
    
    fun setControllerStrobe(value: Boolean) {
        controllerStrobe = value
        if (value) {
            controllerIndex1 = 0
            controllerIndex2 = 0
        }
    }
    
    // Save State
    fun saveState(): ByteArray {
        val state = mutableListOf<Byte>()
        
        // Salvar estado da CPU
        state.addAll(cpu.pc.toBytes())
        state.addAll(cpu.sp.toBytes())
        state.add(cpu.a.toByte())
        state.add(cpu.x.toByte())
        state.add(cpu.y.toByte())
        state.add(cpu.getStatus().toByte())
        
        // Salvar memória
        for (byte in memory.getRam()) {
            state.add(byte)
        }
        
        return state.toByteArray()
    }
    
    fun loadState(state: ByteArray) {
        var offset = 0
        
        // Carregar estado da CPU
        cpu.pc = state.getInt(offset)
        offset += 4
        cpu.sp = state[offset++].toInt() and 0xFF
        cpu.a = state[offset++].toInt() and 0xFF
        cpu.x = state[offset++].toInt() and 0xFF
        cpu.y = state[offset++].toInt() and 0xFF
        cpu.setStatus(state[offset++].toInt() and 0xFF)
        
        // Carregar memória
        val ramCopy = ByteArray(0x800)
        for (i in 0 until 0x800) {
            if (offset < state.size) {
                ramCopy[i] = state[offset++]
            }
        }
        memory.setRam(ramCopy)
    }
    
    private fun Int.toBytes(): List<Byte> {
        return listOf(
            (this and 0xFF).toByte(),
            ((this shr 8) and 0xFF).toByte(),
            ((this shr 16) and 0xFF).toByte(),
            ((this shr 24) and 0xFF).toByte()
        )
    }
    
    private fun ByteArray.getInt(offset: Int): Int {
        return (this[offset].toInt() and 0xFF) or
               ((this[offset + 1].toInt() and 0xFF) shl 8) or
               ((this[offset + 2].toInt() and 0xFF) shl 16) or
               ((this[offset + 3].toInt() and 0xFF) shl 24)
    }
}
