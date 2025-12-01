# NES Emulator Android - Kotlin & C++

Um emulador NES completo e funcional para Android, desenvolvido em **Kotlin puro** e **C++** otimizado. Sem dependências de TypeScript ou Node.js.

## 🎮 Features

✅ **Núcleo em C++ Otimizado**
- CPU 6502 com todas as instruções
- PPU com renderização de sprites e background
- APU com 5 canais de áudio
- Sistema de interrupções (NMI, IRQ)

✅ **Suporte a Mappers**
- Mapper 0 (NROM)
- Mapper 1 (MMC1)
- Mapper 2 (UNROM)
- Mapper 3 (CNROM)
- Mapper 4 (MMC3)
- Mapper 7 (AOROM)
- **~85% de compatibilidade com ROMs NES**

✅ **Interface Android em Kotlin**
- Lista de ROMs com thumbnails
- Renderização em SurfaceView (60 FPS)
- Áudio com AudioTrack
- Controles touch (D-pad + botões)
- Suporte a Bluetooth GamePad

✅ **Banco de Dados SQLite**
- Gerenciamento de ROMs
- Save states (10 slots)
- Configurações do usuário
- Mapeamento de controles
- Biblioteca de jogos

✅ **Compilação Automática**
- GitHub Actions workflow
- Build APK Debug e Release
- Artifacts automáticos

## 📋 Requisitos

- Android 5.0+ (API 21)
- Android Studio 2022.1+
- Kotlin 1.9+
- C++ 17
- CMake 3.22+
- NDK 25.2+

## 🚀 Compilação

### Opção 1: Android Studio

```bash
# Clone o repositório
git clone https://github.com/Zash60/mmmtt.git
cd mmmtt

# Abra no Android Studio
# File > Open > android/

# Compile
# Build > Make Project

# Execute
# Run > Run 'app'
```

### Opção 2: Linha de Comando

```bash
cd android

# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease

# Instalar no dispositivo
./gradlew installDebug
```

### Opção 3: GitHub Actions (Automático)

Faça um push para `main` ou `develop` e o workflow compilará automaticamente:

```bash
git push origin main
```

Os APKs serão disponibilizados em:
- **Artifacts**: https://github.com/Zash60/mmmtt/actions
- **Releases**: https://github.com/Zash60/mmmtt/releases

## 📁 Estrutura do Projeto

```
nes_emulator_android/
├── android/                    # Projeto Android
│   ├── app/
│   │   ├── src/
│   │   │   ├── main/
│   │   │   │   ├── AndroidManifest.xml
│   │   │   │   ├── java/com/nes/android/
│   │   │   │   │   ├── MainActivity.kt
│   │   │   │   │   ├── ROMListFragment.kt
│   │   │   │   │   ├── EmulatorFragment.kt
│   │   │   │   │   ├── DatabaseManager.kt
│   │   │   │   │   ├── EmulatorRenderer.kt
│   │   │   │   │   ├── AudioManager.kt
│   │   │   │   │   ├── ControllerManager.kt
│   │   │   │   │   └── ThumbnailManager.kt
│   │   │   │   └── res/layout/
│   │   │   └── test/
│   │   └── build.gradle.kts
│   ├── settings.gradle.kts
│   └── gradlew
├── cpp/                        # Código C++ otimizado
│   ├── include/
│   │   ├── cpu.h
│   │   ├── ppu.h
│   │   ├── apu.h
│   │   ├── memory.h
│   │   ├── cartridge.h
│   │   └── console.h
│   ├── src/
│   │   ├── cpu.cpp
│   │   ├── ppu.cpp
│   │   ├── apu.cpp
│   │   ├── memory.cpp
│   │   ├── cartridge.cpp
│   │   └── console.cpp
│   └── CMakeLists.txt
├── emulator/                   # Código Kotlin legado (será migrado para C++)
├── .github/workflows/
│   └── build.yml              # GitHub Actions workflow
├── CMakeLists.txt
├── DOCUMENTATION.md           # Documentação técnica
└── README.md                  # Este arquivo
```

## 🎮 Como Usar

### 1. Adicionar ROMs
1. Abra o aplicativo
2. Clique em "Adicionar ROM"
3. Selecione um arquivo .nes
4. A ROM aparecerá na lista

### 2. Jogar
1. Toque em uma ROM para iniciar
2. Use os controles:
   - **Esquerda**: D-pad
   - **Direita**: Botões A/B/Select/Start
3. Clique em "Pausar" para pausar
4. Clique em "Reset" para reiniciar

### 3. Save States
1. Durante o jogo, clique em "Save"
2. Escolha um dos 10 slots
3. Clique em "Load" para restaurar

## ⌨️ Controles Padrão

| Teclado | Gamepad | Touch | Função |
|---------|---------|-------|--------|
| Setas | D-pad | Esquerda | Movimento |
| Z | A/X | Direita | Botão B |
| X | B/Y | Direita | Botão A |
| Shift | Select/L1 | - | Select |
| Enter | Start/R1 | - | Start |
| R | - | - | Reset |

## 🔧 Configurações

- **Velocidade**: 0.5x, 1.0x, 1.5x
- **Volume**: 0-100%
- **Filtros**: Nenhum, Scanlines (planejado)
- **Mapeamento de Controles**: Customizável

## 📊 Performance

- **CPU**: ~29,780 ciclos por frame
- **Renderização**: 60 FPS
- **Áudio**: 44,100 Hz
- **Memória**: ~50MB por jogo

## 🐛 Troubleshooting

### ROM não carrega
- Verifique se é um arquivo .nes válido
- Tente com outra ROM

### Sem áudio
- Verifique o volume do dispositivo
- Reinicie o aplicativo

### Controles não funcionam
- Reconecte o Bluetooth
- Customize o mapeamento

### Jogo travando
- Reduza a velocidade
- Feche outros aplicativos

## 📝 Documentação

- [DOCUMENTATION.md](DOCUMENTATION.md) - Arquitetura técnica completa
- [Nesdev Wiki](https://wiki.nesdev.com/) - Referência NES
- [6502 CPU Reference](https://www.masswerk.at/6502/) - Referência CPU

## 🤝 Contribuindo

1. Fork o projeto
2. Crie uma branch: `git checkout -b feature/sua-feature`
3. Commit: `git commit -am 'Adicionar feature'`
4. Push: `git push origin feature/sua-feature`
5. Abra um Pull Request

## 📄 Licença

MIT License - Veja [LICENSE](LICENSE) para detalhes

## 🙏 Créditos

- Baseado no emulador [Nestt](https://github.com/fogleman/nes) de fogleman
- Documentação NES de [nesdev.com](http://nesdev.com/)
- Comunidade NES Emulation

## 📧 Contato

Para reportar bugs ou sugerir features, abra uma issue no GitHub.

---

**Divirta-se jogando seus clássicos favoritos do NES!** 🎮
