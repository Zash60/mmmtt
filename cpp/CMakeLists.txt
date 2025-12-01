cmake_minimum_required(VERSION 3.18)

# Biblioteca principal do emulador
add_library(nes_emulator_core STATIC
    src/cpu.cpp
    src/ppu.cpp
    src/apu.cpp
    src/memory.cpp
    src/cartridge.cpp
    src/console.cpp
)

target_include_directories(nes_emulator_core PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}/include
)

# Otimizações
if(MSVC)
    target_compile_options(nes_emulator_core PRIVATE /O2 /W4)
else()
    target_compile_options(nes_emulator_core PRIVATE -O3 -Wall -Wextra)
endif()
