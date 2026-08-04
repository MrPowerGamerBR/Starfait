package com.mrpowergamerbr.starfait.wad

class Code(
    val name: StringPointer,
    val length: UInt,
    val localsCount: UShort,
    val argumentsCount: UShort,
    val bytecodeRelativeOffsetFieldPosition: Int,
    val bytecodeRelativeOffset: Int,
    val offset: Int
)