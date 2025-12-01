#ifndef CARTRIDGE_H
#define CARTRIDGE_H

#include <cstdint>
#include <vector>
#include <memory>

/**
 * Gerenciador de cartucho NES com suporte a múltiplos mappers
 */
class Cartridge {
public:
    Cartridge();
    
    bool loadROM(const uint8_t* data, size_t size);
    
    uint8_t readPRG(uint16_t addr);
    void writePRG(uint16_t addr, uint8_t value);
    
    uint8_t readCHR(uint16_t addr);
    void writeCHR(uint16_t addr, uint8_t value);
    
    int getMapperNumber() const { return mapperNumber; }
    bool hasBattery() const { return batteryBacked; }
    int getMirroring() const { return mirroring; }
    
    // IRQ
    bool irqRequested() const { return irqFlag; }
    void resetIRQ() { irqFlag = false; }
    
private:
    int mapperNumber;
    bool batteryBacked;
    int mirroring;
    
    std::vector<uint8_t> prgRom;
    std::vector<uint8_t> chrRom;
    std::vector<uint8_t> prgRam;
    std::vector<uint8_t> chrRam;
    
    bool irqFlag;
    
    // Mapper state
    uint8_t prgBankLo;
    uint8_t prgBankHi;
    uint8_t chrBank0;
    uint8_t chrBank1;
    uint8_t chrBankA;
    uint8_t chrBankB;
    uint8_t chrBankC;
    uint8_t chrBankD;
    uint8_t chrBankE;
    uint8_t chrBankF;
    
    // Mapper-specific
    void writeMapper0(uint16_t addr, uint8_t value);
    void writeMapper1(uint16_t addr, uint8_t value);
    void writeMapper2(uint16_t addr, uint8_t value);
    void writeMapper3(uint16_t addr, uint8_t value);
    void writeMapper4(uint16_t addr, uint8_t value);
    void writeMapper7(uint16_t addr, uint8_t value);
    
    uint8_t readMapper0(uint16_t addr);
    uint8_t readMapper1(uint16_t addr);
    uint8_t readMapper2(uint16_t addr);
    uint8_t readMapper3(uint16_t addr);
    uint8_t readMapper4(uint16_t addr);
    uint8_t readMapper7(uint16_t addr);
};

#endif // CARTRIDGE_H
