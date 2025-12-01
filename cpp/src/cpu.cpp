#include "cpu.h"
#include "memory.h"

CPU::CPU(std::shared_ptr<Memory> memory) 
    : pc(0), sp(0xFF), a(0), x(0), y(0),
      flagC(false), flagZ(false), flagI(false), flagD(false),
      flagB(false), flagV(false), flagN(false),
      cycles(0), nmiRequested(false), irqRequested(false),
      memory(memory) {}

void CPU::step() {
    if (nmiRequested) {
        nmi();
        nmiRequested = false;
    } else if (irqRequested && !flagI) {
        irq();
        irqRequested = false;
    }
    
    uint8_t opcode = memory->read(pc);
    pc++;
    execute(opcode);
}

void CPU::reset() {
    pc = 0x8000;
    sp = 0xFF;
    a = 0;
    x = 0;
    y = 0;
    flagC = false;
    flagZ = false;
    flagI = true;
    flagD = false;
    flagB = false;
    flagV = false;
    flagN = false;
    cycles = 0;
}

uint8_t CPU::getStatus() const {
    uint8_t status = 0;
    if (flagC) status |= 0x01;
    if (flagZ) status |= 0x02;
    if (flagI) status |= 0x04;
    if (flagD) status |= 0x08;
    if (flagB) status |= 0x10;
    if (flagV) status |= 0x40;
    if (flagN) status |= 0x80;
    return status;
}

void CPU::setStatus(uint8_t status) {
    flagC = (status & 0x01) != 0;
    flagZ = (status & 0x02) != 0;
    flagI = (status & 0x04) != 0;
    flagD = (status & 0x08) != 0;
    flagB = (status & 0x10) != 0;
    flagV = (status & 0x40) != 0;
    flagN = (status & 0x80) != 0;
}

void CPU::push(uint8_t value) {
    memory->write(0x100 + sp, value);
    sp--;
}

uint8_t CPU::pop() {
    sp++;
    return memory->read(0x100 + sp);
}

void CPU::pushWord(uint16_t value) {
    push((value >> 8) & 0xFF);
    push(value & 0xFF);
}

uint16_t CPU::popWord() {
    uint8_t lo = pop();
    uint8_t hi = pop();
    return (hi << 8) | lo;
}

void CPU::setZN(uint8_t value) {
    flagZ = (value == 0);
    flagN = (value & 0x80) != 0;
}

void CPU::execute(uint8_t opcode) {
    // Implementação básica - será expandida
    cycles += 2;
}

void CPU::nmi() {
    pushWord(pc);
    push(getStatus());
    flagI = true;
    pc = memory->readWord(0xFFFA);
    cycles += 7;
}

void CPU::irq() {
    pushWord(pc);
    push(getStatus());
    flagI = true;
    pc = memory->readWord(0xFFFE);
    cycles += 7;
}

uint8_t CPU::immediate() {
    return memory->read(pc++);
}

uint8_t CPU::zeroPage() {
    return memory->read(memory->read(pc++));
}

uint8_t CPU::zeroPageX() {
    return memory->read((memory->read(pc++) + x) & 0xFF);
}

uint8_t CPU::zeroPageY() {
    return memory->read((memory->read(pc++) + y) & 0xFF);
}

uint8_t CPU::absolute() {
    return memory->read(memory->readWord(pc));
}

uint8_t CPU::absoluteX() {
    return memory->read(memory->readWord(pc) + x);
}

uint8_t CPU::absoluteY() {
    return memory->read(memory->readWord(pc) + y);
}

uint8_t CPU::indirectX() {
    uint8_t addr = memory->read(pc++) + x;
    return memory->read(memory->readWord(addr));
}

uint8_t CPU::indirectY() {
    uint8_t addr = memory->read(pc++);
    return memory->read(memory->readWord(addr) + y);
}

uint16_t CPU::zeroPageAddr() {
    return memory->read(pc++);
}

uint16_t CPU::zeroPageXAddr() {
    return (memory->read(pc++) + x) & 0xFF;
}

uint16_t CPU::zeroPageYAddr() {
    return (memory->read(pc++) + y) & 0xFF;
}

uint16_t CPU::absoluteAddr() {
    return memory->readWord(pc);
}

uint16_t CPU::absoluteXAddr() {
    return memory->readWord(pc) + x;
}

uint16_t CPU::absoluteYAddr() {
    return memory->readWord(pc) + y;
}

uint16_t CPU::indirectXAddr() {
    uint8_t addr = memory->read(pc++) + x;
    return memory->readWord(addr);
}

uint16_t CPU::indirectYAddr() {
    uint8_t addr = memory->read(pc++);
    return memory->readWord(addr) + y;
}

uint16_t CPU::indirectAddr() {
    return memory->readWord(memory->readWord(pc));
}

void CPU::branch() {
    int8_t offset = memory->read(pc++);
    pc += offset;
}

void CPU::adc(uint8_t value) {
    uint16_t result = a + value + (flagC ? 1 : 0);
    flagC = (result > 0xFF);
    flagV = ((a ^ result) & (value ^ result) & 0x80) != 0;
    a = result & 0xFF;
    setZN(a);
}

void CPU::sbc(uint8_t value) {
    adc(~value);
}

void CPU::cmp(uint8_t reg, uint8_t value) {
    uint8_t result = reg - value;
    flagC = (reg >= value);
    setZN(result);
}

void CPU::bit(uint8_t value) {
    uint8_t result = a & value;
    flagZ = (result == 0);
    flagN = (value & 0x80) != 0;
    flagV = (value & 0x40) != 0;
}
