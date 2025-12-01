#ifndef APU_H
#define APU_H

#include <cstdint>
#include <array>
#include <queue>

/**
 * Audio Processing Unit (APU) do NES
 */
class APU {
public:
    APU();
    
    uint8_t read(uint16_t addr);
    void write(uint16_t addr, uint8_t value);
    
    void step();
    void reset();
    
    float getSample();
    bool hasAudioData() const { return !audioBuffer.empty(); }
    
private:
    // Registradores
    uint8_t pulse1Ctrl;
    uint8_t pulse1Sweep;
    uint8_t pulse1Timer;
    uint8_t pulse1TimerHi;
    
    uint8_t pulse2Ctrl;
    uint8_t pulse2Sweep;
    uint8_t pulse2Timer;
    uint8_t pulse2TimerHi;
    
    uint8_t triangleCtrl;
    uint8_t triangleTimer;
    uint8_t triangleTimerHi;
    
    uint8_t noiseCtrl;
    uint8_t noisePeriod;
    uint8_t noiseLengthCounter;
    
    uint8_t dmcCtrl;
    uint8_t dmcDirect;
    uint8_t dmcAddr;
    uint8_t dmcLength;
    
    uint8_t statusReg;
    uint8_t frameCounter;
    
    // Estado interno
    std::queue<float> audioBuffer;
    uint64_t cycles;
    
    float generatePulse(uint8_t channel);
    float generateTriangle();
    float generateNoise();
    float generateDMC();
    void updateEnvelopes();
    void updateSweeps();
    void updateLengthCounters();
};

#endif // APU_H
