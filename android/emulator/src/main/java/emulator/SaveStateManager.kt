package com.nes.emulator

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.Date

/**
 * Gerencia save states do emulador.
 */
class SaveStateManager(val romPath: String, val saveDir: File) {
    
    init {
        if (!saveDir.exists()) {
            saveDir.mkdirs()
        }
    }
    
    fun saveState(console: Console, slotNumber: Int): Boolean {
        return try {
            val stateFile = File(saveDir, "${File(romPath).nameWithoutExtension}_slot_$slotNumber.nes")
            val state = SaveState(
                timestamp = System.currentTimeMillis(),
                cpuState = console.cpu.run {
                    CPUState(pc, sp, a, x, y, getStatus(), cycles)
                },
                memoryState = console.memory.getRam(),
                ppuState = console.ppu.run {
                    PPUState(control, mask, status, oamAddress, scroll.copyOf(), address, data)
                }
            )
            
            FileOutputStream(stateFile).use { fos ->
                ObjectOutputStream(fos).use { oos ->
                    oos.writeObject(state)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun loadState(console: Console, slotNumber: Int): Boolean {
        return try {
            val stateFile = File(saveDir, "${File(romPath).nameWithoutExtension}_slot_$slotNumber.nes")
            if (!stateFile.exists()) {
                return false
            }
            
            FileInputStream(stateFile).use { fis ->
                ObjectInputStream(fis).use { ois ->
                    val state = ois.readObject() as SaveState
                    
                    // Restaurar estado da CPU
                    console.cpu.apply {
                        pc = state.cpuState.pc
                        sp = state.cpuState.sp
                        a = state.cpuState.a
                        x = state.cpuState.x
                        y = state.cpuState.y
                        setStatus(state.cpuState.status)
                        cycles = state.cpuState.cycles
                    }
                    
                    // Restaurar memória
                    console.memory.setRam(state.memoryState)
                    
                    // Restaurar estado da PPU
                    console.ppu.apply {
                        control = state.ppuState.control
                        mask = state.ppuState.mask
                        status = state.ppuState.status
                        oamAddress = state.ppuState.oamAddress
                        scroll = state.ppuState.scroll.copyOf()
                        address = state.ppuState.address
                        data = state.ppuState.data
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun deleteState(slotNumber: Int): Boolean {
        val stateFile = File(saveDir, "${File(romPath).nameWithoutExtension}_slot_$slotNumber.nes")
        return stateFile.delete()
    }
    
    fun getStateInfo(slotNumber: Int): SaveStateInfo? {
        return try {
            val stateFile = File(saveDir, "${File(romPath).nameWithoutExtension}_slot_$slotNumber.nes")
            if (!stateFile.exists()) {
                return null
            }
            
            FileInputStream(stateFile).use { fis ->
                ObjectInputStream(fis).use { ois ->
                    val state = ois.readObject() as SaveState
                    SaveStateInfo(
                        slotNumber = slotNumber,
                        timestamp = state.timestamp,
                        romName = File(romPath).nameWithoutExtension
                    )
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    fun getAllSaveStates(): List<SaveStateInfo> {
        val states = mutableListOf<SaveStateInfo>()
        for (slot in 0..9) {
            val info = getStateInfo(slot)
            if (info != null) {
                states.add(info)
            }
        }
        return states
    }
}


data class SaveState(
    val timestamp: Long,
    val cpuState: CPUState,
    val memoryState: ByteArray,
    val ppuState: PPUState
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SaveState) return false
        
        if (timestamp != other.timestamp) return false
        if (cpuState != other.cpuState) return false
        if (!memoryState.contentEquals(other.memoryState)) return false
        if (ppuState != other.ppuState) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + cpuState.hashCode()
        result = 31 * result + memoryState.contentHashCode()
        result = 31 * result + ppuState.hashCode()
        return result
    }
}


data class CPUState(
    val pc: Int,
    val sp: Int,
    val a: Int,
    val x: Int,
    val y: Int,
    val status: Int,
    val cycles: Long
) : Serializable


data class PPUState(
    val control: Int,
    val mask: Int,
    val status: Int,
    val oamAddress: Int,
    val scroll: IntArray,
    val address: Int,
    val data: Int
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PPUState) return false
        
        if (control != other.control) return false
        if (mask != other.mask) return false
        if (status != other.status) return false
        if (oamAddress != other.oamAddress) return false
        if (!scroll.contentEquals(other.scroll)) return false
        if (address != other.address) return false
        if (data != other.data) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = control
        result = 31 * result + mask
        result = 31 * result + status
        result = 31 * result + oamAddress
        result = 31 * result + scroll.contentHashCode()
        result = 31 * result + address
        result = 31 * result + data
        return result
    }
}

data class SaveStateInfo(
    val slotNumber: Int,
    val timestamp: Long,
    val romName: String
) {
    val date: Date
        get() = Date(timestamp)
}
