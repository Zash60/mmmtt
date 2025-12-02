package com.nes.emulator

/**
 * Implementação da CPU 6502 do NES.
 * Executa instruções em ciclos, gerencia registradores e flags.
 */
class CPU(val memory: Memory) {
    // Registradores
    var pc: Int = 0x8000      // Program Counter
    var sp: Int = 0xFF        // Stack Pointer
    var a: Int = 0            // Acumulador
    var x: Int = 0            // Índice X
    var y: Int = 0            // Índice Y
    
    // Flags (P - Processor Status)
    var flagC: Boolean = false // Carry
    var flagZ: Boolean = false // Zero
    var flagI: Boolean = false // Interrupt Disable
    var flagD: Boolean = false // Decimal Mode
    var flagB: Boolean = false // Break
    var flagV: Boolean = false // Overflow
    var flagN: Boolean = false // Negative
    
    var cycles: Long = 0
    var nmiRequested: Boolean = false
    var irqRequested: Boolean = false
    
    fun getStatus(): Int {
        var status = 0
        if (flagC) status = status or 0x01
        if (flagZ) status = status or 0x02
        if (flagI) status = status or 0x04
        if (flagD) status = status or 0x08
        if (flagB) status = status or 0x10
        if (flagV) status = status or 0x40
        if (flagN) status = status or 0x80
        return status
    }
    
    fun setStatus(status: Int) {
        flagC = (status and 0x01) != 0
        flagZ = (status and 0x02) != 0
        flagI = (status and 0x04) != 0
        flagD = (status and 0x08) != 0
        flagB = (status and 0x10) != 0
        flagV = (status and 0x40) != 0
        flagN = (status and 0x80) != 0
    }
    
    fun push(value: Int) {
        memory.write(0x100 + sp, value)
        sp = (sp - 1) and 0xFF
    }
    
    fun pop(): Int {
        sp = (sp + 1) and 0xFF
        return memory.read(0x100 + sp)
    }
    
    fun pushWord(value: Int) {
        push((value shr 8) and 0xFF)
        push(value and 0xFF)
    }
    
    fun popWord(): Int {
        val low = pop()
        val high = pop()
        return (high shl 8) or low
    }
    
    fun setZN(value: Int) {
        flagZ = (value and 0xFF) == 0
        flagN = (value and 0x80) != 0
    }
    
    fun step() {
        // Verificar interrupções
        if (nmiRequested) {
            nmi()
            nmiRequested = false
        } else if (irqRequested && !flagI) {
            irq()
            irqRequested = false
        }
        
        val opcode = memory.read(pc)
        pc = (pc + 1) and 0xFFFF
        
        execute(opcode)
    }
    
    private fun nmi() {
        pushWord(pc)
        push(getStatus() or 0x20)
        flagI = true
        pc = memory.readWord(0xFFFA)
        cycles += 7
    }
    
    private fun irq() {
        pushWord(pc)
        push(getStatus() or 0x20)
        flagI = true
        pc = memory.readWord(0xFFFE)
        cycles += 7
    }
    
    private fun execute(opcode: Int) {
        when (opcode) {
            // LDA - Load Accumulator
            0xA9 -> { a = immediate(); setZN(a); cycles += 2 }
            0xA5 -> { a = zeroPage(); setZN(a); cycles += 3 }
            0xB5 -> { a = zeroPageX(); setZN(a); cycles += 4 }
            0xAD -> { a = absolute(); setZN(a); cycles += 4 }
            0xBD -> { a = absoluteX(); setZN(a); cycles += 4 }
            0xB9 -> { a = absoluteY(); setZN(a); cycles += 4 }
            0xA1 -> { a = indirectX(); setZN(a); cycles += 6 }
            0xB1 -> { a = indirectY(); setZN(a); cycles += 5 }
            
            // LDX - Load X
            0xA2 -> { x = immediate(); setZN(x); cycles += 2 }
            0xA6 -> { x = zeroPage(); setZN(x); cycles += 3 }
            0xB6 -> { x = zeroPageY(); setZN(x); cycles += 4 }
            0xAE -> { x = absolute(); setZN(x); cycles += 4 }
            0xBE -> { x = absoluteY(); setZN(x); cycles += 4 }
            
            // LDY - Load Y
            0xA0 -> { y = immediate(); setZN(y); cycles += 2 }
            0xA4 -> { y = zeroPage(); setZN(y); cycles += 3 }
            0xB4 -> { y = zeroPageX(); setZN(y); cycles += 4 }
            0xAC -> { y = absolute(); setZN(y); cycles += 4 }
            0xBC -> { y = absoluteX(); setZN(y); cycles += 4 }
            
            // STA - Store Accumulator
            0x85 -> { memory.write(zeroPageAddr(), a); cycles += 3 }
            0x95 -> { memory.write(zeroPageXAddr(), a); cycles += 4 }
            0x8D -> { memory.write(absoluteAddr(), a); cycles += 4 }
            0x9D -> { memory.write(absoluteXAddr(), a); cycles += 5 }
            0x99 -> { memory.write(absoluteYAddr(), a); cycles += 5 }
            0x81 -> { memory.write(indirectXAddr(), a); cycles += 6 }
            0x91 -> { memory.write(indirectYAddr(), a); cycles += 6 }
            
            // STX - Store X
            0x86 -> { memory.write(zeroPageAddr(), x); cycles += 3 }
            0x96 -> { memory.write(zeroPageYAddr(), x); cycles += 4 }
            0x8E -> { memory.write(absoluteAddr(), x); cycles += 4 }
            
            // STY - Store Y
            0x84 -> { memory.write(zeroPageAddr(), y); cycles += 3 }
            0x94 -> { memory.write(zeroPageXAddr(), y); cycles += 4 }
            0x8C -> { memory.write(absoluteAddr(), y); cycles += 4 }
            
            // ADC - Add with Carry
            0x69 -> { adc(immediate()); cycles += 2 }
            0x65 -> { adc(zeroPage()); cycles += 3 }
            0x75 -> { adc(zeroPageX()); cycles += 4 }
            0x6D -> { adc(absolute()); cycles += 4 }
            0x7D -> { adc(absoluteX()); cycles += 4 }
            0x79 -> { adc(absoluteY()); cycles += 4 }
            0x61 -> { adc(indirectX()); cycles += 6 }
            0x71 -> { adc(indirectY()); cycles += 5 }
            
            // SBC - Subtract with Carry
            0xE9 -> { sbc(immediate()); cycles += 2 }
            0xE5 -> { sbc(zeroPage()); cycles += 3 }
            0xF5 -> { sbc(zeroPageX()); cycles += 4 }
            0xED -> { sbc(absolute()); cycles += 4 }
            0xFD -> { sbc(absoluteX()); cycles += 4 }
            0xF9 -> { sbc(absoluteY()); cycles += 4 }
            0xE1 -> { sbc(indirectX()); cycles += 6 }
            0xF1 -> { sbc(indirectY()); cycles += 5 }
            
            // CMP - Compare
            0xC9 -> { cmp(a, immediate()); cycles += 2 }
            0xC5 -> { cmp(a, zeroPage()); cycles += 3 }
            0xD5 -> { cmp(a, zeroPageX()); cycles += 4 }
            0xCD -> { cmp(a, absolute()); cycles += 4 }
            0xDD -> { cmp(a, absoluteX()); cycles += 4 }
            0xD9 -> { cmp(a, absoluteY()); cycles += 4 }
            0xC1 -> { cmp(a, indirectX()); cycles += 6 }
            0xD1 -> { cmp(a, indirectY()); cycles += 5 }
            
            // CPX - Compare X
            0xE0 -> { cmp(x, immediate()); cycles += 2 }
            0xE4 -> { cmp(x, zeroPage()); cycles += 3 }
            0xEC -> { cmp(x, absolute()); cycles += 4 }
            
            // CPY - Compare Y
            0xC0 -> { cmp(y, immediate()); cycles += 2 }
            0xC4 -> { cmp(y, zeroPage()); cycles += 3 }
            0xCC -> { cmp(y, absolute()); cycles += 4 }
            
            // INC - Increment Memory
            0xE6 -> { val addr = zeroPageAddr(); memory.write(addr, (memory.read(addr) + 1) and 0xFF); cycles += 5 }
            0xF6 -> { val addr = zeroPageXAddr(); memory.write(addr, (memory.read(addr) + 1) and 0xFF); cycles += 6 }
            0xEE -> { val addr = absoluteAddr(); memory.write(addr, (memory.read(addr) + 1) and 0xFF); cycles += 6 }
            0xFE -> { val addr = absoluteXAddr(); memory.write(addr, (memory.read(addr) + 1) and 0xFF); cycles += 7 }
            
            // DEC - Decrement Memory
            0xC6 -> { val addr = zeroPageAddr(); memory.write(addr, (memory.read(addr) - 1) and 0xFF); cycles += 5 }
            0xD6 -> { val addr = zeroPageXAddr(); memory.write(addr, (memory.read(addr) - 1) and 0xFF); cycles += 6 }
            0xCE -> { val addr = absoluteAddr(); memory.write(addr, (memory.read(addr) - 1) and 0xFF); cycles += 6 }
            0xDE -> { val addr = absoluteXAddr(); memory.write(addr, (memory.read(addr) - 1) and 0xFF); cycles += 7 }
            
            // INX - Increment X
            0xE8 -> { x = (x + 1) and 0xFF; setZN(x); cycles += 2 }
            
            // DEX - Decrement X
            0xCA -> { x = (x - 1) and 0xFF; setZN(x); cycles += 2 }
            
            // INY - Increment Y
            0xC8 -> { y = (y + 1) and 0xFF; setZN(y); cycles += 2 }
            
            // DEY - Decrement Y
            0x88 -> { y = (y - 1) and 0xFF; setZN(y); cycles += 2 }
            
            // AND - Logical AND
            0x29 -> { a = a and immediate(); setZN(a); cycles += 2 }
            0x25 -> { a = a and zeroPage(); setZN(a); cycles += 3 }
            0x35 -> { a = a and zeroPageX(); setZN(a); cycles += 4 }
            0x2D -> { a = a and absolute(); setZN(a); cycles += 4 }
            0x3D -> { a = a and absoluteX(); setZN(a); cycles += 4 }
            0x39 -> { a = a and absoluteY(); setZN(a); cycles += 4 }
            0x21 -> { a = a and indirectX(); setZN(a); cycles += 6 }
            0x31 -> { a = a and indirectY(); setZN(a); cycles += 5 }
            
            // ORA - Logical OR
            0x09 -> { a = a or immediate(); setZN(a); cycles += 2 }
            0x05 -> { a = a or zeroPage(); setZN(a); cycles += 3 }
            0x15 -> { a = a or zeroPageX(); setZN(a); cycles += 4 }
            0x0D -> { a = a or absolute(); setZN(a); cycles += 4 }
            0x1D -> { a = a or absoluteX(); setZN(a); cycles += 4 }
            0x19 -> { a = a or absoluteY(); setZN(a); cycles += 4 }
            0x01 -> { a = a or indirectX(); setZN(a); cycles += 6 }
            0x11 -> { a = a or indirectY(); setZN(a); cycles += 5 }
            
            // EOR - Exclusive OR
            0x49 -> { a = a xor immediate(); setZN(a); cycles += 2 }
            0x45 -> { a = a xor zeroPage(); setZN(a); cycles += 3 }
            0x55 -> { a = a xor zeroPageX(); setZN(a); cycles += 4 }
            0x4D -> { a = a xor absolute(); setZN(a); cycles += 4 }
            0x5D -> { a = a xor absoluteX(); setZN(a); cycles += 4 }
            0x59 -> { a = a xor absoluteY(); setZN(a); cycles += 4 }
            0x41 -> { a = a xor indirectX(); setZN(a); cycles += 6 }
            0x51 -> { a = a xor indirectY(); setZN(a); cycles += 5 }
            
            // ASL - Arithmetic Shift Left
            0x0A -> { flagC = (a and 0x80) != 0; a = (a shl 1) and 0xFF; setZN(a); cycles += 2 }
            0x06 -> { val addr = zeroPageAddr(); val v = memory.read(addr); flagC = (v and 0x80) != 0; memory.write(addr, (v shl 1) and 0xFF); cycles += 5 }
            0x16 -> { val addr = zeroPageXAddr(); val v = memory.read(addr); flagC = (v and 0x80) != 0; memory.write(addr, (v shl 1) and 0xFF); cycles += 6 }
            0x0E -> { val addr = absoluteAddr(); val v = memory.read(addr); flagC = (v and 0x80) != 0; memory.write(addr, (v shl 1) and 0xFF); cycles += 6 }
            0x1E -> { val addr = absoluteXAddr(); val v = memory.read(addr); flagC = (v and 0x80) != 0; memory.write(addr, (v shl 1) and 0xFF); cycles += 7 }
            
            // LSR - Logical Shift Right
            0x4A -> { flagC = (a and 0x01) != 0; a = (a shr 1) and 0x7F; setZN(a); cycles += 2 }
            0x46 -> { val addr = zeroPageAddr(); val v = memory.read(addr); flagC = (v and 0x01) != 0; memory.write(addr, (v shr 1) and 0x7F); cycles += 5 }
            0x56 -> { val addr = zeroPageXAddr(); val v = memory.read(addr); flagC = (v and 0x01) != 0; memory.write(addr, (v shr 1) and 0x7F); cycles += 6 }
            0x4E -> { val addr = absoluteAddr(); val v = memory.read(addr); flagC = (v and 0x01) != 0; memory.write(addr, (v shr 1) and 0x7F); cycles += 6 }
            0x5E -> { val addr = absoluteXAddr(); val v = memory.read(addr); flagC = (v and 0x01) != 0; memory.write(addr, (v shr 1) and 0x7F); cycles += 7 }
            
            // ROL - Rotate Left
            0x2A -> { val c = if (flagC) 1 else 0; flagC = (a and 0x80) != 0; a = ((a shl 1) or c) and 0xFF; setZN(a); cycles += 2 }
            0x26 -> { val addr = zeroPageAddr(); val v = memory.read(addr); val c = if (flagC) 1 else 0; flagC = (v and 0x80) != 0; memory.write(addr, ((v shl 1) or c) and 0xFF); cycles += 5 }
            0x36 -> { val addr = zeroPageXAddr(); val v = memory.read(addr); val c = if (flagC) 1 else 0; flagC = (v and 0x80) != 0; memory.write(addr, ((v shl 1) or c) and 0xFF); cycles += 6 }
            0x2E -> { val addr = absoluteAddr(); val v = memory.read(addr); val c = if (flagC) 1 else 0; flagC = (v and 0x80) != 0; memory.write(addr, ((v shl 1) or c) and 0xFF); cycles += 6 }
            0x3E -> { val addr = absoluteXAddr(); val v = memory.read(addr); val c = if (flagC) 1 else 0; flagC = (v and 0x80) != 0; memory.write(addr, ((v shl 1) or c) and 0xFF); cycles += 7 }
            
            // ROR - Rotate Right
            0x6A -> { val c = if (flagC) 0x80 else 0; flagC = (a and 0x01) != 0; a = ((a shr 1) or c) and 0xFF; setZN(a); cycles += 2 }
            0x66 -> { val addr = zeroPageAddr(); val v = memory.read(addr); val c = if (flagC) 0x80 else 0; flagC = (v and 0x01) != 0; memory.write(addr, ((v shr 1) or c) and 0xFF); cycles += 5 }
            0x76 -> { val addr = zeroPageXAddr(); val v = memory.read(addr); val c = if (flagC) 0x80 else 0; flagC = (v and 0x01) != 0; memory.write(addr, ((v shr 1) or c) and 0xFF); cycles += 6 }
            0x6E -> { val addr = absoluteAddr(); val v = memory.read(addr); val c = if (flagC) 0x80 else 0; flagC = (v and 0x01) != 0; memory.write(addr, ((v shr 1) or c) and 0xFF); cycles += 6 }
            0x7E -> { val addr = absoluteXAddr(); val v = memory.read(addr); val c = if (flagC) 0x80 else 0; flagC = (v and 0x01) != 0; memory.write(addr, ((v shr 1) or c) and 0xFF); cycles += 7 }
            
            // BIT - Bit Test
            0x24 -> { bit(zeroPage()); cycles += 3 }
            0x2C -> { bit(absolute()); cycles += 4 }
            
            // BEQ - Branch if Equal (Zero flag set)
            0xF0 -> { if (flagZ) branch(); else { pc = (pc + 1) and 0xFFFF }; cycles += 2 }
            
            // BNE - Branch if Not Equal (Zero flag clear)
            0xD0 -> { if (!flagZ) branch(); else { pc = (pc + 1) and 0xFFFF }; cycles += 2 }
            
            // BCS - Branch if Carry Set
            0xB0 -> { if (flagC) branch(); else { pc = (pc + 1) and 0xFFFF }; cycles += 2 }
            
            // BCC - Branch if Carry Clear
            0x90 -> { if (!flagC) branch(); else { pc = (pc + 1) and 0xFFFF }; cycles += 2 }
            
            // BMI - Branch if Minus (Negative flag set)
            0x30 -> { if (flagN) branch(); else { pc = (pc + 1) and 0xFFFF }; cycles += 2 }
            
            // BPL - Branch if Plus (Negative flag clear)
            0x10 -> { if (!flagN) branch(); else { pc = (pc + 1) and 0xFFFF }; cycles += 2 }
            
            // BVS - Branch if Overflow Set
            0x70 -> { if (flagV) branch(); else { pc = (pc + 1) and 0xFFFF }; cycles += 2 }
            
            // BVC - Branch if Overflow Clear
            0x50 -> { if (!flagV) branch(); else { pc = (pc + 1) and 0xFFFF }; cycles += 2 }
            
            // JMP - Jump
            0x4C -> { pc = absoluteAddr(); cycles += 3 }
            0x6C -> { pc = indirectAddr(); cycles += 5 }
            
            // JSR - Jump to Subroutine
            0x20 -> { pushWord((pc + 1) and 0xFFFF); pc = absoluteAddr(); cycles += 6 }
            
            // RTS - Return from Subroutine
            0x60 -> { pc = (popWord() + 1) and 0xFFFF; cycles += 6 }
            
            // BRK - Break
            0x00 -> { pc = (pc + 1) and 0xFFFF; push(getStatus() or 0x10); flagI = true; pc = memory.readWord(0xFFFE); cycles += 7 }
            
            // RTI - Return from Interrupt
            0x40 -> { setStatus(pop()); pc = popWord(); cycles += 6 }
            
            // CLC - Clear Carry
            0x18 -> { flagC = false; cycles += 2 }
            
            // SEC - Set Carry
            0x38 -> { flagC = true; cycles += 2 }
            
            // CLI - Clear Interrupt Disable
            0x58 -> { flagI = false; cycles += 2 }
            
            // SEI - Set Interrupt Disable
            0x78 -> { flagI = true; cycles += 2 }
            
            // CLV - Clear Overflow
            0xB8 -> { flagV = false; cycles += 2 }
            
            // CLD - Clear Decimal
            0xD8 -> { flagD = false; cycles += 2 }
            
            // SED - Set Decimal
            0xF8 -> { flagD = true; cycles += 2 }
            
            // NOP - No Operation
            0xEA -> { cycles += 2 }
            
            // PHA - Push Accumulator
            0x48 -> { push(a); cycles += 3 }
            
            // PLA - Pull Accumulator
            0x68 -> { a = pop(); setZN(a); cycles += 4 }
            
            // PHP - Push Processor Status
            0x08 -> { push(getStatus() or 0x10); cycles += 3 }
            
            // PLP - Pull Processor Status
            0x28 -> { setStatus(pop()); cycles += 4 }
            
            // TAX - Transfer Accumulator to X
            0xAA -> { x = a; setZN(x); cycles += 2 }
            
            // TXA - Transfer X to Accumulator
            0x8A -> { a = x; setZN(a); cycles += 2 }
            
            // TAY - Transfer Accumulator to Y
            0xA8 -> { y = a; setZN(y); cycles += 2 }
            
            // TYA - Transfer Y to Accumulator
            0x98 -> { a = y; setZN(a); cycles += 2 }
            
            // TSX - Transfer Stack Pointer to X
            0xBA -> { x = sp; setZN(x); cycles += 2 }
            
            // TXS - Transfer X to Stack Pointer
            0x9A -> { sp = x; cycles += 2 }
            
            else -> { cycles += 2 }  // Instrução desconhecida
        }
    }
    
    private fun immediate(): Int {
        val value = memory.read(pc)
        pc = (pc + 1) and 0xFFFF
        return value
    }
    
    private fun zeroPageAddr(): Int {
        val addr = memory.read(pc)
        pc = (pc + 1) and 0xFFFF
        return addr
    }
    
    private fun zeroPage(): Int {
        return memory.read(zeroPageAddr())
    }
    
    private fun zeroPageXAddr(): Int {
        val addr = (memory.read(pc) + x) and 0xFF
        pc = (pc + 1) and 0xFFFF
        return addr
    }
    
    private fun zeroPageX(): Int {
        return memory.read(zeroPageXAddr())
    }
    
    private fun zeroPageYAddr(): Int {
        val addr = (memory.read(pc) + y) and 0xFF
        pc = (pc + 1) and 0xFFFF
        return addr
    }
    
    private fun zeroPageY(): Int {
        return memory.read(zeroPageYAddr())
    }
    
    private fun absoluteAddr(): Int {
        val low = memory.read(pc)
        val high = memory.read(pc + 1)
        pc = (pc + 2) and 0xFFFF
        return (high shl 8) or low
    }
    
    private fun absolute(): Int {
        return memory.read(absoluteAddr())
    }
    
    private fun absoluteXAddr(): Int {
        val low = memory.read(pc)
        val high = memory.read(pc + 1)
        pc = (pc + 2) and 0xFFFF
        return ((high shl 8) or low) + x
    }
    
    private fun absoluteX(): Int {
        return memory.read(absoluteXAddr())
    }
    
    private fun absoluteYAddr(): Int {
        val low = memory.read(pc)
        val high = memory.read(pc + 1)
        pc = (pc + 2) and 0xFFFF
        return ((high shl 8) or low) + y
    }
    
    private fun absoluteY(): Int {
        return memory.read(absoluteYAddr())
    }
    
    private fun indirectAddr(): Int {
        val low = memory.read(pc)
        val high = memory.read(pc + 1)
        pc = (pc + 2) and 0xFFFF
        val addr = (high shl 8) or low
        val addrLow = memory.read(addr)
        val addrHigh = memory.read((addr and 0xFF00) or ((addr + 1) and 0xFF))
        return (addrHigh shl 8) or addrLow
    }
    
    private fun indirectXAddr(): Int {
        val zp = (memory.read(pc) + x) and 0xFF
        pc = (pc + 1) and 0xFFFF
        val low = memory.read(zp)
        val high = memory.read((zp + 1) and 0xFF)
        return (high shl 8) or low
    }
    
    private fun indirectX(): Int {
        return memory.read(indirectXAddr())
    }
    
    private fun indirectYAddr(): Int {
        val zp = memory.read(pc)
        pc = (pc + 1) and 0xFFFF
        val low = memory.read(zp)
        val high = memory.read((zp + 1) and 0xFF)
        return ((high shl 8) or low) + y
    }
    
    private fun indirectY(): Int {
        return memory.read(indirectYAddr())
    }
    
    private fun branch() {
        val offset = memory.read(pc).toByte().toInt()
        pc = (pc + 1 + offset) and 0xFFFF
        cycles += 1
    }
    
    private fun adc(value: Int) {
        val carry = if (flagC) 1 else 0
        val result = a + value + carry
        flagC = result > 0xFF
        flagV = ((a xor result) and (value xor result) and 0x80) != 0
        a = result and 0xFF
        setZN(a)
    }
    
    private fun sbc(value: Int) {
        val carry = if (flagC) 0 else 1
        val result = a - value - carry
        flagC = result >= 0
        flagV = ((a xor result) and ((a xor value) xor 0x80) and 0x80) != 0
        a = result and 0xFF
        setZN(a)
    }
    
    private fun cmp(register: Int, value: Int) {
        val result = register - value
        flagC = register >= value
        setZN(result and 0xFF)
    }
    
    private fun bit(value: Int) {
        val result = a and value
        flagZ = result == 0
        flagV = (value and 0x40) != 0
        flagN = (value and 0x80) != 0
    }
}
