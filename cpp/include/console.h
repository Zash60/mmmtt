#ifndef CONSOLE_H
#define CONSOLE_H

#include <cstdint>
#include <memory>
#include <array>

class CPU;
class PPU;
class APU;
class Memory;
class Cartridge;

/**
 * Emulador NES completo
 */
class Console {
public:
    Console();
    ~Console();
    
    bool loadROM(const uint8_t* data, size_t size);
    void reset();
    void runFrame();
    void runCycle();
    
    const uint8_t* getFrameBuffer() const;
    float getAudioSample();
    bool hasAudioData() const;
    
    void setButtonState(int button, bool pressed);
    
    // Save states
    std::vector<uint8_t> getState() const;
    bool setState(const uint8_t* data, size_t size);
    
    // Configurações
    void setEmulationSpeed(float speed) { emulationSpeed = speed; }
    float getEmulationSpeed() const { return emulationSpeed; }
    
    void setShowFPS(bool show) { showFPS = show; }
    bool getShowFPS() const { return showFPS; }
    
    uint64_t getCycles() const;
    uint64_t getFrameCount() const { return frameCount; }
    
private:
    std::shared_ptr<CPU> cpu;
    std::shared_ptr<PPU> ppu;
    std::shared_ptr<APU> apu;
    std::shared_ptr<Memory> memory;
    std::shared_ptr<Cartridge> cartridge;
    
    uint64_t frameCount;
    uint64_t cyclesPerFrame;
    float emulationSpeed;
    bool showFPS;
    
    std::array<bool, 8> buttonStates;  // A, B, Select, Start, Up, Down, Left, Right
    
    void handleInterrupts();
};

#endif // CONSOLE_H
