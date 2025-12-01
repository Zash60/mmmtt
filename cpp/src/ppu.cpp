#include "ppu.h"

PPU::PPU() : ppuCtrl(0), ppuMask(0), ppuStatus(0), oamAddr(0), ppuScroll(0),
             ppuAddr(0), ppuData(0), scanline(0), cycle(0), frameReady(false),
             nmiFlag(false), scrollX(0), scrollY(0) {
    vram.fill(0);
    oam.fill(0);
    palette.fill(0);
    frameBuffer.fill(0);
}

uint8_t PPU::read(uint16_t addr) {
    switch (addr) {
        case 0x2002: return ppuStatus;
        case 0x2004: return oam[oamAddr];
        case 0x2007: return ppuData;
        default: return 0;
    }
}

void PPU::write(uint16_t addr, uint8_t value) {
    switch (addr) {
        case 0x2000: ppuCtrl = value; break;
        case 0x2001: ppuMask = value; break;
        case 0x2003: oamAddr = value; break;
        case 0x2004: oam[oamAddr++] = value; break;
        case 0x2005: ppuScroll = value; break;
        case 0x2006: ppuAddr = value; break;
        case 0x2007: ppuData = value; break;
    }
}

void PPU::step() {
    renderPixel();
    
    cycle++;
    if (cycle >= 341) {
        cycle = 0;
        scanline++;
        if (scanline >= 262) {
            scanline = 0;
            frameReady = true;
            if (ppuCtrl & 0x80) {
                nmiFlag = true;
            }
        }
    }
}

void PPU::reset() {
    ppuCtrl = 0;
    ppuMask = 0;
    ppuStatus = 0;
    scanline = 0;
    cycle = 0;
    frameReady = false;
    nmiFlag = false;
}

void PPU::renderPixel() {
    if (scanline < 240 && cycle < 256) {
        renderBackground();
        renderSprites();
    }
}

void PPU::renderBackground() {
    // Implementação básica
}

void PPU::renderSprites() {
    // Implementação básica
}

uint8_t PPU::getPaletteColor(uint8_t index) {
    return palette[index & 0x1F];
}
