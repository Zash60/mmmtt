# NES Emulator Android - TODO

## Status Geral
- [x] Núcleo do emulador em Kotlin
- [x] Núcleo otimizado em C++ com CMake
- [x] Interface Android completa
- [x] Banco de dados SQLite
- [x] GitHub Actions workflow para build APK
- [ ] Testes unitários

## Núcleo do Emulador (C++)
- [x] CPU 6502 (headers + implementação básica)
- [x] Memória (headers + implementação)
- [x] PPU (headers + implementação básica)
- [x] APU (headers + implementação básica)
- [x] Cartridge com suporte a mappers (headers + implementação)
- [x] Console integrado (headers + implementação)
- [ ] Otimizações de performance
- [ ] Testes unitários C++

## Mappers (C++)
- [x] Mapper 0 (NROM)
- [x] Mapper 1 (MMC1) - básico
- [x] Mapper 2 (UNROM)
- [x] Mapper 3 (CNROM)
- [x] Mapper 4 (MMC3) - básico
- [x] Mapper 7 (AOROM)
- [ ] Mappers adicionais

## Interface Android (Kotlin)
- [x] MainActivity
- [x] ROMListFragment
- [x] EmulatorFragment
- [x] Layouts XML
- [x] DatabaseManager (SQLite)
- [x] EmulatorRenderer
- [x] AudioManager
- [x] ControllerManager
- [x] ThumbnailManager
- [ ] Testes Android

## Build & CI/CD
- [x] CMakeLists.txt (raiz)
- [x] CMakeLists.txt (cpp/)
- [x] build.gradle.kts
- [x] settings.gradle.kts
- [x] GitHub Actions workflow (build.yml)
- [ ] Assinatura de APK Release
- [ ] Publicação automática em releases

## Documentação
- [x] README.md (atualizado)
- [x] DOCUMENTATION.md (arquitetura)
- [ ] Guia de desenvolvimento
- [ ] Guia de contribuição
- [ ] API documentation

## Testes
- [ ] Testes unitários (C++)
- [ ] Testes de integração (Android)
- [ ] Testes de compatibilidade de ROMs
- [ ] Testes de performance

## Otimizações
- [ ] Inline assembly para operações críticas
- [ ] SIMD para renderização
- [ ] Multithreading para CPU/PPU/APU
- [ ] Cache de instruções

## Features Futuras
- [ ] Filtros de vídeo (scanlines, blur)
- [ ] Suporte a cheat codes
- [ ] Gravação de vídeo
- [ ] Multiplayer local
- [ ] Sincronização na nuvem
- [ ] Suporte a periféricos (Zapper)
- [ ] Emulação de Famicom Disk System

## Bugs Conhecidos
- [ ] Timing de PPU pode não ser 100% preciso
- [ ] Alguns mappers não implementados completamente
- [ ] Periféricos especiais não funcionam

## Notas
- Projeto convertido de TypeScript/Node.js para Kotlin/C++
- Sem dependências externas (apenas Android SDK)
- GitHub Actions compila automaticamente em cada push
- APKs disponíveis em artifacts e releases
