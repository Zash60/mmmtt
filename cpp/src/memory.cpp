#include "memory.h"
#include "cartridge.h"
#include "ppu.h"
#include "apu.h"

Memory::Memory() {
    ram.fill(0);
}

uint8_t Memory::read(uint16_t addr) {
    if (addr < 0x2000) {
        return ram[addr & 0x7FF];
    } else if (addr < 0x4000) {
        return readPPU(addr);
    } else if (addr < 0x4020) {
        return readAPU(addr);
    } else if (addr < 0x6000) {
        return 0;
    } else if (addr < 0x8000) {
        if (cartridge) {
            return cartridge->readPRG(addr);
        }
        return 0;
    } else {
        if (cartridge) {
            return cartridge->readPRG(addr);
        }
        return 0;
    }
}

void Memory::write(uint16_t addr, uint8_t value) {
    if (addr < 0x2000) {
        ram[addr & 0x7FF] = value;
    } else if (addr < 0x4000) {
        writePPU(addr, value);
    } else if (addr < 0x4020) {
        writeAPU(addr, value);
    } else if (addr < 0x6000) {
        // Open bus
    } else if (addr < 0x8000) {
        if (cartridge) {
            cartridge->writePRG(addr, value);
        }
    } else {
        if (cartridge) {
            cartridge->writePRG(addr, value);
        }
    }
}

uint16_t Memory::readWord(uint16_t addr) {
    uint8_t lo = read(addr);
    uint8_t hi = read(addr + 1);
    return (hi << 8) | lo;
}

void Memory::writeWord(uint16_t addr, uint16_t value) {
    write(addr, value & 0xFF);
    write(addr + 1, (value >> 8) & 0xFF);
}

void Memory::setCartridge(std::shared_ptr<Cartridge> cartridge) {
    this->cartridge = cartridge;
}

void Memory::setPPU(std::shared_ptr<class PPU> ppu) {
    this->ppu = ppu;
}

void Memory::setAPU(std::shared_ptr<class APU> apu) {
    this->apu = apu;
}

uint8_t Memory::readPPU(uint16_t addr) {
    if (ppu) {
        return ppu->read(addr);
    }
    return 0;
}

void Memory::writePPU(uint16_t addr, uint8_t value) {
    if (ppu) {
        ppu->write(addr, value);
    }
}

uint8_t Memory::readAPU(uint16_t addr) {
    if (apu) {
        return apu->read(addr);
    }
    return 0;
}

void Memory::writeAPU(uint16_t addr, uint8_t value) {
    if (apu) {
        apu->write(addr, value);
    }
}
