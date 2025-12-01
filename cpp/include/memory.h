#ifndef MEMORY_H
#define MEMORY_H

#include <cstdint>
#include <array>
#include <memory>

class Cartridge;
class PPU;
class APU;

/**
 * Gerenciador de memória do NES em C++
 */
class Memory {
public:
    Memory();
    
    uint8_t read(uint16_t addr);
    void write(uint16_t addr, uint8_t value);
    uint16_t readWord(uint16_t addr);
    void writeWord(uint16_t addr, uint16_t value);
    
    void setCartridge(std::shared_ptr<Cartridge> cartridge);
    void setPPU(std::shared_ptr<class PPU> ppu);
    void setAPU(std::shared_ptr<class APU> apu);
    
    // Acesso direto para performance
    uint8_t* getRam() { return ram.data(); }
    
private:
    std::array<uint8_t, 0x800> ram;  // 2KB RAM interno
    std::shared_ptr<Cartridge> cartridge;
    std::shared_ptr<class PPU> ppu;
    std::shared_ptr<class APU> apu;
    
    uint8_t readPPU(uint16_t addr);
    void writePPU(uint16_t addr, uint8_t value);
    uint8_t readAPU(uint16_t addr);
    void writeAPU(uint16_t addr, uint8_t value);
};

#endif // MEMORY_H
