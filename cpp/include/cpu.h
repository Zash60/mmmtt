#ifndef CPU_H
#define CPU_H

#include <cstdint>
#include <memory>

class Memory;

/**
 * Implementação otimizada da CPU 6502 em C++
 */
class CPU {
public:
    explicit CPU(std::shared_ptr<Memory> memory);
    
    // Registradores
    uint16_t pc;    // Program Counter
    uint8_t sp;     // Stack Pointer
    uint8_t a;      // Acumulador
    uint8_t x;      // Índice X
    uint8_t y;      // Índice Y
    
    // Flags
    bool flagC;     // Carry
    bool flagZ;     // Zero
    bool flagI;     // Interrupt Disable
    bool flagD;     // Decimal Mode
    bool flagB;     // Break
    bool flagV;     // Overflow
    bool flagN;     // Negative
    
    uint64_t cycles;
    bool nmiRequested;
    bool irqRequested;
    
    void step();
    void reset();
    uint8_t getStatus() const;
    void setStatus(uint8_t status);
    
private:
    std::shared_ptr<Memory> memory;
    
    void push(uint8_t value);
    uint8_t pop();
    void pushWord(uint16_t value);
    uint16_t popWord();
    void setZN(uint8_t value);
    
    void execute(uint8_t opcode);
    void nmi();
    void irq();
    
    // Modos de endereçamento
    uint8_t immediate();
    uint8_t zeroPage();
    uint8_t zeroPageX();
    uint8_t zeroPageY();
    uint8_t absolute();
    uint8_t absoluteX();
    uint8_t absoluteY();
    uint8_t indirectX();
    uint8_t indirectY();
    
    uint16_t zeroPageAddr();
    uint16_t zeroPageXAddr();
    uint16_t zeroPageYAddr();
    uint16_t absoluteAddr();
    uint16_t absoluteXAddr();
    uint16_t absoluteYAddr();
    uint16_t indirectXAddr();
    uint16_t indirectYAddr();
    uint16_t indirectAddr();
    
    void branch();
    void adc(uint8_t value);
    void sbc(uint8_t value);
    void cmp(uint8_t reg, uint8_t value);
    void bit(uint8_t value);
};

#endif // CPU_H
