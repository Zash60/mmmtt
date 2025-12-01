#include "cartridge.h"

Cartridge::Cartridge() : mapperNumber(0), batteryBacked(false), mirroring(0),
                         irqFlag(false), prgBankLo(0), prgBankHi(0),
                         chrBank0(0), chrBank1(0), chrBankA(0), chrBankB(0),
                         chrBankC(0), chrBankD(0), chrBankE(0), chrBankF(0) {}

bool Cartridge::loadROM(const uint8_t* data, size_t size) {
    if (size < 16) return false;
    
    // Parser iNES
    if (data[0] != 'N' || data[1] != 'E' || data[2] != 'S' || data[3] != 0x1A) {
        return false;
    }
    
    uint8_t prgRomSize = data[4];
    uint8_t chrRomSize = data[5];
    uint8_t flags6 = data[6];
    uint8_t flags7 = data[7];
    
    mapperNumber = ((flags7 & 0xF0) | (flags6 >> 4));
    mirroring = flags6 & 0x01;
    batteryBacked = (flags6 & 0x02) != 0;
    
    size_t offset = 16;
    
    // Carregar PRG ROM
    size_t prgSize = prgRomSize * 16384;
    if (offset + prgSize > size) return false;
    
    prgRom.resize(prgSize);
    std::copy(data + offset, data + offset + prgSize, prgRom.begin());
    offset += prgSize;
    
    // Carregar CHR ROM
    size_t chrSize = chrRomSize * 8192;
    if (chrSize > 0) {
        if (offset + chrSize > size) return false;
        chrRom.resize(chrSize);
        std::copy(data + offset, data + offset + chrSize, chrRom.begin());
    } else {
        chrRam.resize(8192);
    }
    
    // Inicializar PRG RAM
    prgRam.resize(8192);
    
    return true;
}

uint8_t Cartridge::readPRG(uint16_t addr) {
    if (addr < 0x6000) {
        return 0;
    } else if (addr < 0x8000) {
        return prgRam[addr - 0x6000];
    } else {
        size_t prgAddr = addr - 0x8000;
        if (prgAddr < prgRom.size()) {
            return prgRom[prgAddr];
        }
        return 0;
    }
}

void Cartridge::writePRG(uint16_t addr, uint8_t value) {
    if (addr >= 0x6000 && addr < 0x8000) {
        prgRam[addr - 0x6000] = value;
    } else if (addr >= 0x8000) {
        switch (mapperNumber) {
            case 0: writeMapper0(addr, value); break;
            case 1: writeMapper1(addr, value); break;
            case 2: writeMapper2(addr, value); break;
            case 3: writeMapper3(addr, value); break;
            case 4: writeMapper4(addr, value); break;
            case 7: writeMapper7(addr, value); break;
        }
    }
}

uint8_t Cartridge::readCHR(uint16_t addr) {
    if (!chrRom.empty()) {
        if (addr < chrRom.size()) {
            return chrRom[addr];
        }
    } else if (!chrRam.empty()) {
        if (addr < chrRam.size()) {
            return chrRam[addr];
        }
    }
    return 0;
}

void Cartridge::writeCHR(uint16_t addr, uint8_t value) {
    if (!chrRam.empty() && addr < chrRam.size()) {
        chrRam[addr] = value;
    }
}

void Cartridge::writeMapper0(uint16_t addr, uint8_t value) {
    // NROM - sem banco switching
}

void Cartridge::writeMapper1(uint16_t addr, uint8_t value) {
    // MMC1 - implementação básica
}

void Cartridge::writeMapper2(uint16_t addr, uint8_t value) {
    // UNROM
    prgBankLo = (value & 0x0F);
}

void Cartridge::writeMapper3(uint16_t addr, uint8_t value) {
    // CNROM
    chrBank0 = (value & 0x03);
}

void Cartridge::writeMapper4(uint16_t addr, uint8_t value) {
    // MMC3 - implementação básica
}

void Cartridge::writeMapper7(uint16_t addr, uint8_t value) {
    // AOROM
    prgBankLo = (value & 0x07);
}

uint8_t Cartridge::readMapper0(uint16_t addr) {
    return prgRom[addr - 0x8000];
}

uint8_t Cartridge::readMapper1(uint16_t addr) {
    return prgRom[addr - 0x8000];
}

uint8_t Cartridge::readMapper2(uint16_t addr) {
    return prgRom[addr - 0x8000];
}

uint8_t Cartridge::readMapper3(uint16_t addr) {
    return prgRom[addr - 0x8000];
}

uint8_t Cartridge::readMapper4(uint16_t addr) {
    return prgRom[addr - 0x8000];
}

uint8_t Cartridge::readMapper7(uint16_t addr) {
    return prgRom[addr - 0x8000];
}
