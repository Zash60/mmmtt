#include "console.h"
#include "cpu.h"
#include "ppu.h"
#include "apu.h"
#include "memory.h"
#include "cartridge.h"

Console::Console() : frameCount(0), cyclesPerFrame(29780), emulationSpeed(1.0f), 
                     showFPS(false) {
    memory = std::make_shared<Memory>();
    cpu = std::make_shared<CPU>(memory);
    ppu = std::make_shared<PPU>();
    apu = std::make_shared<APU>();
    cartridge = std::make_shared<Cartridge>();
    
    memory->setPPU(ppu);
    memory->setAPU(apu);
    memory->setCartridge(cartridge);
    
    buttonStates.fill(false);
}

Console::~Console() {}

bool Console::loadROM(const uint8_t* data, size_t size) {
    if (!cartridge->loadROM(data, size)) {
        return false;
    }
    reset();
    return true;
}

void Console::reset() {
    cpu->reset();
    ppu->reset();
    apu->reset();
    frameCount = 0;
    buttonStates.fill(false);
}

void Console::runFrame() {
    uint64_t startCycles = cpu->cycles;
    uint64_t targetCycles = startCycles + (cyclesPerFrame / emulationSpeed);
    
    while (cpu->cycles < targetCycles) {
        runCycle();
    }
}

void Console::runCycle() {
    // CPU executa 1 ciclo
    cpu->step();
    
    // PPU executa 3 ciclos (PPU é 3x mais rápido que CPU)
    for (int i = 0; i < 3; i++) {
        ppu->step();
    }
    
    // APU executa 1 ciclo
    apu->step();
    
    handleInterrupts();
}

const uint8_t* Console::getFrameBuffer() const {
    return ppu->getFrameBuffer();
}

float Console::getAudioSample() {
    return apu->getSample();
}

bool Console::hasAudioData() const {
    return apu->hasAudioData();
}

void Console::setButtonState(int button, bool pressed) {
    if (button >= 0 && button < 8) {
        buttonStates[button] = pressed;
    }
}

std::vector<uint8_t> Console::getState() const {
    std::vector<uint8_t> state;
    
    // Salvar estado da CPU
    state.push_back(cpu->pc >> 8);
    state.push_back(cpu->pc & 0xFF);
    state.push_back(cpu->sp);
    state.push_back(cpu->a);
    state.push_back(cpu->x);
    state.push_back(cpu->y);
    state.push_back(cpu->getStatus());
    
    // Salvar estado da PPU
    // ... (implementação simplificada)
    
    // Salvar estado da memória RAM
    const uint8_t* ram = memory->getRam();
    for (int i = 0; i < 0x800; i++) {
        state.push_back(ram[i]);
    }
    
    return state;
}

bool Console::setState(const uint8_t* data, size_t size) {
    if (size < 7) return false;
    
    // Restaurar estado da CPU
    cpu->pc = (data[0] << 8) | data[1];
    cpu->sp = data[2];
    cpu->a = data[3];
    cpu->x = data[4];
    cpu->y = data[5];
    cpu->setStatus(data[6]);
    
    return true;
}

uint64_t Console::getCycles() const {
    return cpu->cycles;
}

void Console::handleInterrupts() {
    if (ppu->nmiRequested()) {
        cpu->nmiRequested = true;
        ppu->resetNMI();
    }
    
    if (cpu->irqRequested) {
        // Processar IRQ
    }
}
