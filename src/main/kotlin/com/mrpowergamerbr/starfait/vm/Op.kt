package com.mrpowergamerbr.starfait.vm

import kotlin.reflect.full.hasAnnotation

object Op {
    @Opcode const val PUSH = 0xC0
    @Opcode const val PUSH_LOCAL = 0xC1
    @Opcode const val PUSH_GLOBAL = 0xC2
    @Opcode const val PUSH_BUILTIN = 0xC3
    @Opcode const val PUSH_IMMEDIATE = 0x84
    @Opcode const val POP = 0x45
    @Opcode const val DUP = 0x86
    @Opcode const val CONV = 0x07
    @Opcode const val MUL = 0x08
    @Opcode const val DIV = 0x09
    @Opcode const val REM = 0x0A
    @Opcode const val MOD = 0x0B
    @Opcode const val ADD = 0x0C
    @Opcode const val SUB = 0x0D
    @Opcode const val AND = 0x0E
    @Opcode const val OR = 0x0F
    @Opcode const val XOR = 0x10
    @Opcode const val NEG = 0x11
    @Opcode const val NOT = 0x12
    @Opcode const val SHL = 0x13
    @Opcode const val SHR = 0x14
    @Opcode const val CMP = 0x15
    @Opcode const val B = 0xB6
    @Opcode const val BT = 0xB7
    @Opcode const val BF = 0xB8
    @Opcode const val CALL = 0xD9
    @Opcode const val CALLV = 0x99
    @Opcode const val PUSH_ENV = 0xBA
    @Opcode const val POP_ENV = 0xBB
    @Opcode const val RET = 0x9C
    @Opcode const val EXIT = 0x9D
    @Opcode const val POPZ = 0x9E
    @Opcode const val BREAK = 0xFF

    private val opcodeToName = Op::class.members.filter { it.hasAnnotation<Opcode>() } .associateBy({ it.call() }) { it.name }

    fun getOpcodeName(opcode: Int) = opcodeToName[opcode] ?: "UNKNOWN"

    private annotation class Opcode
}
