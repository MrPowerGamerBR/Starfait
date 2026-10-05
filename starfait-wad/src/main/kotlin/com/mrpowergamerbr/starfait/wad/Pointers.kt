package com.mrpowergamerbr.starfait.wad

sealed interface WadPointer {
    val address: Int

    val isNull: Boolean
        get() = address == 0
}

@JvmInline
value class Pointer(override val address: Int) : WadPointer

@JvmInline
value class TexturePageEntryPointer(override val address: Int) : WadPointer

@JvmInline
value class StringPointer(override val address: Int) : WadPointer