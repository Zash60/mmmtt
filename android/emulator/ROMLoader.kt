package com.nes.emulator

import java.io.File

/**
 * Parser e carregador de ROMs no formato iNES.
 */
object ROMLoader {
    
    fun loadROM(file: File): Console? {
        val data = file.readBytes()
        
        if (data.size < 16) {
            return null
        }
        
        // Verificar assinatura iNES
        if (data[0].toInt() != 0x4E || data[1].toInt() != 0x45 || 
            data[2].toInt() != 0x53 || data[3].toInt() != 0x1A) {
            return null
        }
        
        // Ler cabeçalho iNES
        val prgSize = (data[4].toInt() and 0xFF) * 16384
        val chrSize = (data[5].toInt() and 0xFF) * 8192
        val controlByte1 = data[6].toInt() and 0xFF
        val controlByte2 = data[7].toInt() and 0xFF
        
        val mapperNumber = ((controlByte2 and 0xF0) shl 4) or ((controlByte1 and 0xF0) shr 4)
        val mirroring = controlByte1 and 0x01
        val batteryBacked = (controlByte1 and 0x02) != 0
        val trainer = (controlByte1 and 0x04) != 0
        
        // Calcular offset dos dados
        var offset = 16
        if (trainer) {
            offset += 512
        }
        
        // Ler PRG ROM
        val prg = ByteArray(prgSize)
        for (i in 0 until prgSize) {
            if (offset + i < data.size) {
                prg[i] = data[offset + i]
            }
        }
        offset += prgSize
        
        // Ler CHR ROM
        val chr = if (chrSize > 0) {
            val chrData = ByteArray(chrSize)
            for (i in 0 until chrSize) {
                if (offset + i < data.size) {
                    chrData[i] = data[offset + i]
                }
            }
            chrData
        } else {
            ByteArray(8192)  // CHR RAM se não houver CHR ROM
        }
        
        // Criar mapper apropriado
        val mapper = when (mapperNumber) {
            0 -> Mapper0(prg, chr)
            1 -> Mapper1(prg, chr)
            2 -> Mapper2(prg, chr)
            3 -> Mapper3(prg, chr)
            4 -> Mapper4(prg, chr)
            7 -> Mapper7(prg, chr)
            else -> {
                // Fallback para Mapper 0
                Mapper0(prg, chr)
            }
        }
        
        // Criar cartucho e console
        val cartridge = Cartridge(prg, chr, mapper, mirroring)
        val console = Console(cartridge)
        console.reset()
        
        return console
    }
    
    fun getROMInfo(file: File): ROMInfo? {
        val data = file.readBytes()
        
        if (data.size < 16) {
            return null
        }
        
        // Verificar assinatura iNES
        if (data[0].toInt() != 0x4E || data[1].toInt() != 0x45 || 
            data[2].toInt() != 0x53 || data[3].toInt() != 0x1A) {
            return null
        }
        
        val prgSize = (data[4].toInt() and 0xFF) * 16384
        val chrSize = (data[5].toInt() and 0xFF) * 8192
        val controlByte1 = data[6].toInt() and 0xFF
        val controlByte2 = data[7].toInt() and 0xFF
        
        val mapperNumber = ((controlByte2 and 0xF0) shl 4) or ((controlByte1 and 0xF0) shr 4)
        val mirroring = controlByte1 and 0x01
        val batteryBacked = (controlByte1 and 0x02) != 0
        
        return ROMInfo(
            name = file.nameWithoutExtension,
            path = file.absolutePath,
            mapperNumber = mapperNumber,
            prgSize = prgSize,
            chrSize = chrSize,
            mirroring = mirroring,
            batteryBacked = batteryBacked
        )
    }
}

data class ROMInfo(
    val name: String,
    val path: String,
    val mapperNumber: Int,
    val prgSize: Int,
    val chrSize: Int,
    val mirroring: Int,
    val batteryBacked: Boolean
)
