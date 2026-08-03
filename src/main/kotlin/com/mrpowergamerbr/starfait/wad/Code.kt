package com.mrpowergamerbr.starfait.wad

class Code(
    val name: StringPointer,
    val localsCount: UShort,
    val argumentsCount: UShort,
    val offset: Int,
    val data: ByteArray
)