package com.mrpowergamerbr.starfait.vm

object Op {
    const val PUSH = 0xC0
    const val PUSH_LOCAL = 0xC1
    const val PUSH_GLOBAL = 0xC2
    const val PUSH_BUILTIN = 0xC3
    const val PUSH_IMMEDIATE = 0x84
    const val POP = 0x45
    const val DUP = 0x86
    const val CONV = 0x07
    const val MUL = 0x08
    const val DIV = 0x09
    const val REM = 0x0A
    const val MOD = 0x0B
    const val ADD = 0x0C
    const val SUB = 0x0D
    const val AND = 0x0E
    const val OR = 0x0F
    const val XOR = 0x10
    const val NEG = 0x11
    const val NOT = 0x12
    const val SHL = 0x13
    const val SHR = 0x14
    const val CMP = 0x15
    const val B = 0xB6
    const val BT = 0xB7
    const val BF = 0xB8
    const val CALL = 0xD9
    const val CALLV = 0x99
    const val PUSH_ENV = 0xBA
    const val POP_ENV = 0xBB
    const val RET = 0x9C
    const val EXIT = 0x9D
    const val POPZ = 0x9E
    const val BREAK = 0xFF
}
