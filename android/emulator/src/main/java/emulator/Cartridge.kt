package com.nes.emulator

/**
 * Representa um cartucho NES com suporte a múltiplos mappers.
 * Cada mapper implementa sua própria lógica de bank switching.
 */
abstract class Mapper {
    abstract fun readRom(address: Int): Int
    abstract fun writeRom(address: Int, value: Int)
    abstract fun readRam(address: Int): Int
    abstract fun writeRam(address: Int, value: Int)
    abstract fun mirrorMode(): Int  // 0=horizontal, 1=vertical
}

class Cartridge(
    val prg: ByteArray,  // Program ROM
    val chr: ByteArray,  // Character ROM
    val mapper: Mapper,
    val mirroring: Int   // 0=horizontal, 1=vertical
) {
    fun readRom(address: Int): Int {
        return mapper.readRom(address).toInt() and 0xFF
    }
    
    fun writeRom(address: Int, value: Int) {
        mapper.writeRom(address, value)
    }
    
    fun readRam(address: Int): Int {
        return mapper.readRam(address).toInt() and 0xFF
    }
    
    fun writeRam(address: Int, value: Int) {
        mapper.writeRam(address, value)
    }
    
    fun readChr(address: Int): Int {
        return chr[address % chr.size].toInt() and 0xFF
    }
    
    fun writeChr(address: Int, value: Int) {
        if (chr.isNotEmpty()) {
            chr[address % chr.size] = value.toByte()
        }
    }
}

// Mapper 0: NROM (sem bank switching)
class Mapper0(val prg: ByteArray, val chr: ByteArray) : Mapper() {
    private val ram = ByteArray(0x2000)
    
    override fun readRom(address: Int): Int {
        val addr = if (address < 0xC000) {
            (address - 0x8000) % prg.size
        } else {
            prg.size - 0x4000 + (address - 0xC000)
        }
        return prg[addr].toInt() and 0xFF
    }
    
    override fun writeRom(address: Int, value: Int) {}
    
    override fun readRam(address: Int): Int {
        return ram[(address - 0x6000) % ram.size].toInt() and 0xFF
    }
    
    override fun writeRam(address: Int, value: Int) {
        ram[(address - 0x6000) % ram.size] = value.toByte()
    }
    
    override fun mirrorMode(): Int = 0
}

// Mapper 1: MMC1 (com bank switching)
class Mapper1(val prg: ByteArray, val chr: ByteArray) : Mapper() {
    private val ram = ByteArray(0x2000)
    private var shiftRegister = 0
    private var shiftCounter = 0
    private var control = 0
    private var chrBank0 = 0
    private var chrBank1 = 0
    private var prgBank = 0
    
    override fun readRom(address: Int): Int {
        val prgSize = prg.size
        val addr = when {
            address < 0xC000 -> {
                val bank = if ((control and 0x08) != 0) prgBank else 0
                (bank * 0x4000 + (address - 0x8000)) % prgSize
            }
            else -> prgSize - 0x4000 + (address - 0xC000)
        }
        return prg[addr].toInt() and 0xFF
    }
    
    override fun writeRom(address: Int, value: Int) {
        if ((value and 0x80) != 0) {
            shiftRegister = 0
            shiftCounter = 0
            control = control or 0x0C
        } else {
            shiftRegister = (shiftRegister shr 1) or ((value and 1) shl 4)
            shiftCounter++
            if (shiftCounter == 5) {
                when (address) {
                    in 0x8000..0x9FFF -> control = shiftRegister
                    in 0xA000..0xBFFF -> chrBank0 = shiftRegister
                    in 0xC000..0xDFFF -> chrBank1 = shiftRegister
                    in 0xE000..0xFFFF -> prgBank = shiftRegister and 0x0F
                }
                shiftRegister = 0
                shiftCounter = 0
            }
        }
    }
    
    override fun readRam(address: Int): Int {
        return ram[(address - 0x6000) % ram.size].toInt() and 0xFF
    }
    
    override fun writeRam(address: Int, value: Int) {
        ram[(address - 0x6000) % ram.size] = value.toByte()
    }
    
    override fun mirrorMode(): Int = control and 0x03
}

// Mapper 2: UNROM (simples bank switching)
class Mapper2(val prg: ByteArray, val chr: ByteArray) : Mapper() {
    private val ram = ByteArray(0x2000)
    private var prgBank = 0
    
    override fun readRom(address: Int): Int {
        val addr = when {
            address < 0xC000 -> (prgBank * 0x4000 + (address - 0x8000)) % prg.size
            else -> prg.size - 0x4000 + (address - 0xC000)
        }
        return prg[addr].toInt() and 0xFF
    }
    
    override fun writeRom(address: Int, value: Int) {
        prgBank = value and 0x0F
    }
    
    override fun readRam(address: Int): Int {
        return ram[(address - 0x6000) % ram.size].toInt() and 0xFF
    }
    
    override fun writeRam(address: Int, value: Int) {
        ram[(address - 0x6000) % ram.size] = value.toByte()
    }
    
    override fun mirrorMode(): Int = 0
}

// Mapper 3: CNROM (CHR bank switching)
class Mapper3(val prg: ByteArray, val chr: ByteArray) : Mapper() {
    private val ram = ByteArray(0x2000)
    private var chrBank = 0
    
    override fun readRom(address: Int): Int {
        val addr = (address - 0x8000) % prg.size
        return prg[addr].toInt() and 0xFF
    }
    
    override fun writeRom(address: Int, value: Int) {
        chrBank = value and 0x03
    }
    
    override fun readRam(address: Int): Int {
        return ram[(address - 0x6000) % ram.size].toInt() and 0xFF
    }
    
    override fun writeRam(address: Int, value: Int) {
        ram[(address - 0x6000) % ram.size] = value.toByte()
    }
    
    override fun mirrorMode(): Int = 0
}

// Mapper 4: MMC3 (avançado com bank switching)
class Mapper4(val prg: ByteArray, val chr: ByteArray) : Mapper() {
    private val ram = ByteArray(0x2000)
    private var bankSelect = 0
    private val prgBanks = IntArray(4)
    private val chrBanks = IntArray(8)
    private var mirrorMode = 0
    
    init {
        prgBanks[2] = (prg.size / 0x2000 - 2) * 2
        prgBanks[3] = (prg.size / 0x2000 - 1) * 2
    }
    
    override fun readRom(address: Int): Int {
        val bank = when (address) {
            in 0x8000..0x9FFF -> prgBanks[0]
            in 0xA000..0xBFFF -> prgBanks[1]
            in 0xC000..0xDFFF -> prgBanks[2]
            in 0xE000..0xFFFF -> prgBanks[3]
            else -> 0
        }
        val addr = (bank * 0x2000 + (address and 0x1FFF)) % prg.size
        return prg[addr].toInt() and 0xFF
    }
    
    override fun writeRom(address: Int, value: Int) {
        when (address and 0xE001) {
            0x8000 -> bankSelect = value
            0x8001 -> {
                val reg = bankSelect and 0x07
                when (reg) {
                    0, 1 -> chrBanks[reg] = value
                    2, 3, 4, 5 -> prgBanks[reg - 2] = value and 0x3F
                    6 -> prgBanks[0] = value and 0x3F
                    7 -> prgBanks[1] = value and 0x3F
                }
            }
            0xA000 -> mirrorMode = value and 1
            0xC000 -> {}  // IRQ latch
            0xC001 -> {}  // IRQ reload
            0xE000 -> {}  // IRQ disable
            0xE001 -> {}  // IRQ enable
        }
    }
    
    override fun readRam(address: Int): Int {
        return ram[(address - 0x6000) % ram.size].toInt() and 0xFF
    }
    
    override fun writeRam(address: Int, value: Int) {
        ram[(address - 0x6000) % ram.size] = value.toByte()
    }
    
    override fun mirrorMode(): Int = mirrorMode
}

// Mapper 7: AOROM (simples bank switching)
class Mapper7(val prg: ByteArray, val chr: ByteArray) : Mapper() {
    private val ram = ByteArray(0x2000)
    private var prgBank = 0
    
    override fun readRom(address: Int): Int {
        val addr = (prgBank * 0x8000 + (address - 0x8000)) % prg.size
        return prg[addr].toInt() and 0xFF
    }
    
    override fun writeRom(address: Int, value: Int) {
        prgBank = value and 0x0F
    }
    
    override fun readRam(address: Int): Int {
        return ram[(address - 0x6000) % ram.size].toInt() and 0xFF
    }
    
    override fun writeRam(address: Int, value: Int) {
        ram[(address - 0x6000) % ram.size] = value.toByte()
    }
    
    override fun mirrorMode(): Int = 1
}
