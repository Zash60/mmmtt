# NES Emulator for Android

This is a Nintendo Entertainment System (NES) emulator for Android devices.

## Features

- High accuracy NES emulation
- Save/load game states
- Customizable controls
- ROM management
- Game thumbnail generation

## Project Structure

```
nes_emulator_android/
├── android/                    # Android project
│   ├── app/                   # Main application module
│   │   ├── src/
│   │   │   ├── main/
│   │   │   │   ├── AndroidManifest.xml
│   │   │   │   ├── java/com/nes/android/  # Kotlin/Java source files
│   │   │   │   └── res/layout/           # Layout files
│   │   └── build.gradle       # App module build configuration
│   ├── settings.gradle        # Project settings
│   └── gradlew                # Gradle wrapper
├── cpp/                        # C++ core emulator
│   ├── include/               # Header files
│   ├── src/                   # Source files
│   └── CMakeLists.txt         # C++ build configuration
├── emulator/                   # Legacy Kotlin emulator (to be migrated)
├── .github/workflows/
│   └── build.yml              # CI/CD workflow
├── CMakeLists.txt             # Root CMake configuration
└── README.md                  # This file
```

## Building

### Prerequisites
- Android Studio
- Android NDK
- CMake

### Steps
1. Clone the repository
2. Open the project in Android Studio
3. Build the project using Gradle

## Usage

1. Launch the app
2. Select a ROM file
3. Start playing!

## License

MIT License
