package com.nes.emulator

/**
 * Gerencia toda a memória do NES, incluindo RAM, ROM e registradores.
 * Arquitetura de memória:
 * 0x0000-0x07FF: RAM interna (2KB)
 * 0x0800-0x0FFF: Espelho de RAM
 * 0x1000-0x1FFF: Espelho de RAM
 * 0x2000-0x2007: Registradores PPU
 * 0x2008-0x3FFF: Espelho de registradores PPU
 * 0x4000-0x4017: Registradores APU e entrada
 * 0x4018-0x401F: Teste de ROM
 * 0x4020-0x5FFF: Cartridge RAM
 * 0x6000-0x7FFF: Cartridge RAM
 * 0x8000-0xFFFF: Cartridge ROM
 */
class Memory(val cartridge: Cartridge) {
    private val ram = ByteArray(0x800)  // 2KB RAM interna
    private val ppuRegisters = ByteArray(8)
    private val apuRegisters = ByteArray(0x18)
    
    var ppuAddress: Int = 0
    var ppuData: Int = 0
    var ppuScroll: Int = 0
    var ppuControl: Int = 0
    var ppuStatus: Int = 0
    
    var apuStatus: Int = 0
    var apuFrameCounter: Int = 0
    
    var controllerData: Int = 0
    var controllerStrobe: Boolean = false
    
    fun read(address: Int): Int {
        return when {
            address < 0x2000 -> ram[address and 0x7FF].toInt() and 0xFF
            address < 0x4000 -> {
                when (address and 0x7) {
                    2 -> ppuStatus
                    4 -> 0  // OAM data
                    7 -> ppuData
                    else -> 0
                }
            }
            address < 0x4020 -> {
                when (address) {
                    0x4015 -> apuStatus
                    0x4016 -> controllerData
                    0x4017 -> 0
                    else -> 0
                }
            }
            address < 0x6000 -> cartridge.readRam(address)
            address < 0x8000 -> cartridge.readRam(address)
            else -> cartridge.readRom(address)
        }
    }
    
    fun write(address: Int, value: Int) {
        when {
            address < 0x2000 -> ram[address and 0x7FF] = value.toByte()
            address < 0x4000 -> {
                when (address and 0x7) {
                    0 -> ppuControl = value
                    1 -> {}  // PPU mask
                    3 -> {}  // OAM address
                    4 -> {}  // OAM data
                    5 -> ppuScroll = value
                    6 -> ppuAddress = (ppuAddress and 0xFF) or (value shl 8)
                    7 -> ppuData = value
                }
            }
            address < 0x4020 -> {
                when (address) {
                    0x4014 -> {}  // OAM DMA
                    0x4015 -> apuStatus = value
                    0x4016 -> controllerStrobe = (value and 1) != 0
                    0x4017 -> apuFrameCounter = value
                    else -> if (address < 0x4018) apuRegisters[address - 0x4000] = value.toByte()
                }
            }
            address < 0x6000 -> cartridge.writeRam(address, value)
            address < 0x8000 -> cartridge.writeRam(address, value)
            else -> cartridge.writeRom(address, value)
        }
    }
    
    fun readWord(address: Int): Int {
        val low = read(address)
        val high = read(address + 1)
        return (high shl 8) or low
    }
    
    fun writeWord(address: Int, value: Int) {
        write(address, value and 0xFF)
        write(address + 1, (value shr 8) and 0xFF)
    }
}
