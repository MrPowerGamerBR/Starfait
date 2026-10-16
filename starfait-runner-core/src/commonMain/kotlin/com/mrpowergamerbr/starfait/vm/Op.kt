package com.mrpowergamerbr.starfait.vm

import com.mrpowergamerbr.starfait.NamedConstant

object Op {
    @NamedConstant const val PUSH = 0xC0
    @NamedConstant const val PUSH_LOCAL = 0xC1
    @NamedConstant const val PUSH_GLOBAL = 0xC2
    @NamedConstant const val PUSH_BUILTIN = 0xC3
    @NamedConstant const val PUSH_IMMEDIATE = 0x84
    @NamedConstant const val POP = 0x45
    @NamedConstant const val DUP = 0x86
    @NamedConstant const val CONV = 0x07
    @NamedConstant const val MUL = 0x08
    @NamedConstant const val DIV = 0x09
    @NamedConstant const val REM = 0x0A
    @NamedConstant const val MOD = 0x0B
    @NamedConstant const val ADD = 0x0C
    @NamedConstant const val SUB = 0x0D
    @NamedConstant const val AND = 0x0E
    @NamedConstant const val OR = 0x0F
    @NamedConstant const val XOR = 0x10
    @NamedConstant const val NEG = 0x11
    @NamedConstant const val NOT = 0x12
    @NamedConstant const val SHL = 0x13
    @NamedConstant const val SHR = 0x14
    @NamedConstant const val CMP = 0x15
    @NamedConstant const val B = 0xB6
    @NamedConstant const val BT = 0xB7
    @NamedConstant const val BF = 0xB8
    @NamedConstant const val CALL = 0xD9
    @NamedConstant const val CALLV = 0x99
    @NamedConstant const val PUSH_ENV = 0xBA
    @NamedConstant const val POP_ENV = 0xBB
    @NamedConstant const val RET = 0x9C
    @NamedConstant const val EXIT = 0x9D
    @NamedConstant const val POPZ = 0x9E
    @NamedConstant const val BREAK = 0xFF

    fun getOpcodeName(opcode: Int) = OpNames[opcode] ?: "UNKNOWN"
}
