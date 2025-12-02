package com.nes.emulator

/**
 * Implementação da PPU (Picture Processing Unit) do NES.
 * Responsável pela renderização gráfica.
 */
class PPU(val cartridge: Cartridge) {
    companion object {
        const val SCREEN_WIDTH = 256
        const val SCREEN_HEIGHT = 240
        const val PALETTE_SIZE = 64
    }
    
    // Memória de vídeo
    private val vram = ByteArray(0x4000)  // 16KB VRAM
    private val oam = ByteArray(256)      // OAM (Sprite data)
    private val palette = IntArray(32)    // Paleta de cores
    
    // Registradores
    var control: Int = 0
    var mask: Int = 0
    var status: Int = 0
    var oamAddress: Int = 0
    var scroll: IntArray = intArrayOf(0, 0)
    var address: Int = 0
    var data: Int = 0
    
    // Estado interno
    private var scanline = 0
    private var cycle = 0
    private var frameBuffer = IntArray(SCREEN_WIDTH * SCREEN_HEIGHT)
    private var nmiRequested = false
    
    // Paleta padrão do NES (RGB)
    private val nespalette = intArrayOf(
        0xFF7C7C7C, 0xFFFCFCFC, 0xFFF8F8F8, 0xFFFCFCFC,
        0xFF0000BC, 0xFF3C0080, 0xFF6C0040, 0xFF740000,
        0xFF780000, 0xFF6C0000, 0xFF540000, 0xFF3C0000,
        0xFF2C0000, 0xFF000000, 0xFF000000, 0xFF000000,
        0xFFBCBCBC, 0xFF0078F8, 0xFF0088F8, 0xFF0098F8,
        0xFF6C00F8, 0xFF9400F8, 0xFFB81800, 0xFFF80000,
        0xFFF80800, 0xFFF81800, 0xFFF83000, 0xFFD84000,
        0xFFA85000, 0xFF004000, 0xFF000000, 0xFF000000,
        0xFFF8F8F8, 0xFF3CBCF8, 0xFF6CBCF8, 0xFF88BCF8,
        0xFFB8B8F8, 0xFFD8B8F8, 0xFFF8B8D8, 0xFFF8A8B8,
        0xFFF8A8A8, 0xFFF89898, 0xFFF87888, 0xFFFCA878,
        0xFFFCB888, 0xFFF8D8B8, 0xFFF8F8B8, 0xFFF8F8A8,
        0xFFFCFCFC, 0xFFA8E8FC, 0xFFC8D8FC, 0xFFD8D0FC,
        0xFFE8C8FC, 0xFFF8C8F8, 0xFFFCC8D8, 0xFFFCC8C8,
        0xFFFCC8C0, 0xFFFCC0B8, 0xFFFCB8B0, 0xFFFCB8A8,
        0xFFFCB8A0, 0xFFFCB898, 0xFFFCC0A0, 0xFFFCD0B0
    )
    
    init {
        initPalette()
    }
    
    private fun initPalette() {
        for (i in 0 until PALETTE_SIZE) {
            palette[i] = nespalette[i]
        }
    }
    
    fun step() {
        cycle++
        
        if (cycle >= 341) {
            cycle = 0
            scanline++
            
            if (scanline == 241) {
                nmiRequested = true
                status = status or 0x80
            }
            
            if (scanline >= 262) {
                scanline = 0
                status = status and 0x7F
                nmiRequested = false
            }
        }
        
        if (scanline < 240 && cycle < 256) {
            renderPixel(cycle, scanline)
        }
    }
    
    private fun renderPixel(x: Int, y: Int) {
        // Renderizar background
        val bgEnabled = (mask and 0x08) != 0
        val spriteEnabled = (mask and 0x10) != 0
        
        var pixel = 0
        
        if (bgEnabled) {
            pixel = renderBackground(x, y)
        }
        
        if (spriteEnabled) {
            val spritePixel = renderSprite(x, y)
            if (spritePixel != 0) {
                pixel = spritePixel
            }
        }
        
        frameBuffer[y * SCREEN_WIDTH + x] = palette[pixel and 0x1F]
    }
    
    private fun renderBackground(x: Int, y: Int): Int {
        val scrollX = scroll[0]
        val scrollY = scroll[1]
        
        val tileX = ((x + scrollX) / 8) and 0x1F
        val tileY = ((y + scrollY) / 8) and 0x1F
        
        val pixelX = (x + scrollX) and 7
        val pixelY = (y + scrollY) and 7
        
        val nametableAddr = 0x2000 + (tileY * 32) + tileX
        val tileIndex = vram[nametableAddr].toInt() and 0xFF
        
        val attrAddr = 0x23C0 + ((tileY / 4) * 8) + (tileX / 4)
        val attr = vram[attrAddr].toInt() and 0xFF
        
        val chrAddr = (tileIndex * 16) + pixelY
        val chrData0 = cartridge.readChr(chrAddr).toInt() and 0xFF
        val chrData1 = cartridge.readChr(chrAddr + 8).toInt() and 0xFF
        
        val bit = 7 - pixelX
        val pixel = ((chrData1 shr bit) and 1) shl 1 or ((chrData0 shr bit) and 1)
        
        val paletteIndex = (attr shr (((tileY and 2) shl 1) + (tileX and 2))) and 3
        
        return (paletteIndex shl 2) or pixel
    }
    
    private fun renderSprite(x: Int, y: Int): Int {
        for (i in 0 until 64) {
            val spriteY = oam[i * 4].toInt() and 0xFF
            val tileIndex = oam[i * 4 + 1].toInt() and 0xFF
            val attr = oam[i * 4 + 2].toInt() and 0xFF
            val spriteX = oam[i * 4 + 3].toInt() and 0xFF
            
            if (x >= spriteX && x < spriteX + 8 && y >= spriteY && y < spriteY + 8) {
                val pixelX = x - spriteX
                val pixelY = y - spriteY
                
                val flipH = (attr and 0x40) != 0
                val flipV = (attr and 0x80) != 0
                
                val px = if (flipH) 7 - pixelX else pixelX
                val py = if (flipV) 7 - pixelY else pixelY
                
                val chrAddr = (tileIndex * 16) + py
                val chrData0 = cartridge.readChr(chrAddr).toInt() and 0xFF
                val chrData1 = cartridge.readChr(chrAddr + 8).toInt() and 0xFF
                
                val bit = 7 - px
                val pixel = ((chrData1 shr bit) and 1) shl 1 or ((chrData0 shr bit) and 1)
                
                if (pixel != 0) {
                    val paletteIndex = (attr and 0x03)
                    return 0x10 + (paletteIndex shl 2) + pixel
                }
            }
        }
        return 0
    }
    
    fun readRegister(address: Int): Int {
        return when (address and 0x7) {
            2 -> status
            4 -> oam[oamAddress].toInt() and 0xFF
            7 -> {
                val value = vram[address and 0x3FFF].toInt() and 0xFF
                this.address += if ((control and 0x04) != 0) 32 else 1
                value
            }
            else -> 0
        }
    }
    
    fun writeRegister(address: Int, value: Int) {
        when (address and 0x7) {
            0 -> control = value
            1 -> mask = value
            3 -> oamAddress = value
            4 -> oam[oamAddress] = value.toByte()
            5 -> scroll[if (scroll[0] == 0) 0 else 1] = value
            6 -> this.address = (address and 0xFF) or (value shl 8)
            7 -> {
                vram[this.address and 0x3FFF] = value.toByte()
                this.address += if ((control and 0x04) != 0) 32 else 1
            }
        }
    }
    
    fun getFrameBuffer(): IntArray {
        return frameBuffer
    }
    
    fun isNmiRequested(): Boolean {
        return nmiRequested && (control and 0x80) != 0
    }
    
    fun getScanline(): Int = scanline
    fun getCycle(): Int = cycle
}
