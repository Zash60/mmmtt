cmake_minimum_required(VERSION 3.10)

project(NES_Emulator)

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Add C++ subdirectory
add_subdirectory(cpp)

# Android NDK setup
if(ANDROID)
    # Set Android specific flags
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fexceptions -frtti")

    # Find Android log library
    find_library(ANDROID_LOG_LIB log)
    if(ANDROID_LOG_LIB)
        message(STATUS "Found Android log library: ${ANDROID_LOG_LIB}")
    else()
        message(WARNING "Android log library not found")
    endif()
endif()
