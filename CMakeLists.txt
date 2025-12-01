cmake_minimum_required(VERSION 3.18)
project(nes_emulator)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Adicionar diretório de código-fonte
add_subdirectory(cpp)

# Criar biblioteca compartilhada para Android
if(ANDROID)
    add_library(nes_emulator_native SHARED
        cpp/src/jni_bridge.cpp
    )
    
    target_link_libraries(nes_emulator_native
        nes_emulator_core
        log
    )
endif()
