#ifndef PPU_H
#define PPU_H

#include <cstdint>
#include <array>

/**
 * Picture Processing Unit (PPU) do NES
 */
class PPU {
public:
    PPU();
    
    uint8_t read(uint16_t addr);
    void write(uint16_t addr, uint8_t value);
    
    void step();
    void reset();
    
    const uint8_t* getFrameBuffer() const { return frameBuffer.data(); }
    bool isFrameReady() const { return frameReady; }
    void resetFrameReady() { frameReady = false; }
    
    bool nmiRequested() const { return nmiFlag; }
    void resetNMI() { nmiFlag = false; }
    
private:
    // Registradores
    uint8_t ppuCtrl;
    uint8_t ppuMask;
    uint8_t ppuStatus;
    uint8_t oamAddr;
    uint8_t ppuScroll;
    uint8_t ppuAddr;
    uint8_t ppuData;
    
    // Memória
    std::array<uint8_t, 0x4000> vram;  // 16KB VRAM
    std::array<uint8_t, 0x100> oam;    // OAM (Sprite)
    std::array<uint8_t, 0x20> palette; // Paleta
    
    // Frame buffer
    std::array<uint8_t, 256 * 240 * 3> frameBuffer;  // RGB
    
    // Estado interno
    uint16_t scanline;
    uint16_t cycle;
    bool frameReady;
    bool nmiFlag;
    uint8_t scrollX;
    uint8_t scrollY;
    
    void renderPixel();
    void renderBackground();
    void renderSprites();
    uint8_t getPaletteColor(uint8_t index);
};

#endif // PPU_H
