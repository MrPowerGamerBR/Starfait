package com.mrpowergamerbr.starfait

import com.mrpowergamerbr.starfait.wad.Pointer
import com.mrpowergamerbr.starfait.wad.StringPointer
import com.mrpowergamerbr.starfait.wad.TexturePageEntryPointer
import java.nio.ByteBuffer
import java.nio.ByteOrder

class StarfaitByteBuffer private constructor(private val byteBuffer: ByteBuffer) {
    companion object {
        fun fromByteArray(byteArray: ByteArray) = StarfaitByteBuffer(ByteBuffer.wrap(byteArray))
        fun fromByteBuffer(byteBuffer: ByteBuffer) = StarfaitByteBuffer(byteBuffer)
    }

    init {
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
    }

    var position: Int
        get() = byteBuffer.position()
        set(value) {
            byteBuffer.position(value)
        }

    var byteOrder: ByteOrder
        get() = byteBuffer.order()
        set(value) {
            byteBuffer.order(value)
        }

    fun readByte(): Byte {
        return byteBuffer.get()
    }

    fun readBytes(size: Int): ByteArray {
        val bytes = ByteArray(size)
        byteBuffer.get(bytes)
        return bytes
    }

    fun readShort(): Short {
        return byteBuffer.getShort()
    }

    fun readUShort(): UShort {
        return readShort().toUShort()
    }

    fun readInt(): Int {
        return byteBuffer.getInt()
    }

    fun readUInt(): UInt {
        return readInt().toUInt()
    }

    fun readPointer(): Pointer {
        return Pointer(readInt())
    }

    fun readStringPointer(): StringPointer {
        return StringPointer(readInt())
    }

    fun readTexturePageEntryPointer(): TexturePageEntryPointer {
        return TexturePageEntryPointer(readInt())
    }

    fun readFloat(): Float {
        return byteBuffer.getFloat()
    }

    fun readDouble(): Double {
        return byteBuffer.getDouble()
    }

    fun readLong(): Long {
        return byteBuffer.getLong()
    }

    fun readByteBoolean(): Boolean {
        return readByte() != 0.toByte()
    }

    fun readIntBoolean(): Boolean {
        return readInt() != 0
    }

    fun readAddresses(): List<Int> {
        val count = readInt()
        val addresses = ArrayList<Int>(count)
        repeat(count) {
            addresses.add(readInt())
        }
        return addresses
    }

    fun readUTF8String(stringLength: Int): String {
        val bytes = ByteArray(stringLength)
        byteBuffer.get(bytes)
        return bytes.toString(Charsets.UTF_8)
    }

    fun writeInt(value: Int) = byteBuffer.putInt(value)

    fun hasRemaining() = byteBuffer.hasRemaining()
}