package com.mrpowergamerbr.starfait

import com.mrpowergamerbr.starfait.wad.Pointer
import com.mrpowergamerbr.starfait.wad.StringPointer
import com.mrpowergamerbr.starfait.wad.TexturePageEntryPointer

class StarfaitByteBuffer private constructor(
    private val byteArray: ByteArray,
    private val offset: Int,
    private val sliceSize: Int
) {
    companion object {
        fun fromByteArray(byteArray: ByteArray, offset: Int, sliceSize: Int = byteArray.size - offset) = StarfaitByteBuffer(
            byteArray,
            offset,
            sliceSize
        )
    }

    var byteOrder = ByteOrder.LITTLE_ENDIAN

    var position = this.offset
        set(value) {
            if (value > this.offset + this.sliceSize) throw IndexOutOfBoundsException("StarfaitByteBuffer buffer overflow! $value > ${this.offset + this.sliceSize}")
            if (this.offset > value) throw IndexOutOfBoundsException("StarfaitByteBuffer buffer underflow! ${this.offset} > $value")

            field = value
        }

    fun readByte() = this.byteArray[position++]

    fun readBytes(size: Int): ByteArray {
        require(this.sliceSize >= this.position + size)

        val data = this.byteArray.copyOfRange(this.position, this.position + size)
        this.position += size
        return data
    }

    fun readShort(): Short {
        val b0 = (this.byteArray[position++].toInt() and 0xFF)
        val b1 = (this.byteArray[position++].toInt() and 0xFF)

        return when (this.byteOrder) {
            ByteOrder.LITTLE_ENDIAN -> (b0 or (b1 shl 8)).toShort()
            ByteOrder.BIG_ENDIAN -> ((b0 shl 8) or b1).toShort()
        }
    }

    fun readUShort(): UShort {
        return readShort().toUShort()
    }

    fun readInt(): Int {
        val b0 = (this.byteArray[position++].toInt() and 0xFF)
        val b1 = (this.byteArray[position++].toInt() and 0xFF)
        val b2 = (this.byteArray[position++].toInt() and 0xFF)
        val b3 = (this.byteArray[position++].toInt() and 0xFF)
        return when (this.byteOrder) {
            ByteOrder.LITTLE_ENDIAN -> b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
            ByteOrder.BIG_ENDIAN -> (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
        }
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

    fun readStringPointerAndReadNullTerminatedString(): String {
        val pointer = readStringPointer()
        val positionAfterAddress = this.position
        this.position = pointer.address
        val string = readNullTerminatedString()
        this.position = positionAfterAddress
        return string
    }

    fun readNullTerminatedString(): String {
        val startPosition = this.position
        var size = 0
        while (true) {
            val byte = readByte()
            if (byte == 0.toByte())
                break
            size++
        }
        this.position = startPosition
        val string = this.readBytes(size).decodeToString()
        this.position++ // null
        return string
    }

    fun readTexturePageEntryPointer(): TexturePageEntryPointer {
        return TexturePageEntryPointer(readInt())
    }

    fun readFloat() = Float.fromBits(readInt())

    fun readLong(): Long {
        val b0 = (this.byteArray[position++].toLong() and 0xFF)
        val b1 = (this.byteArray[position++].toLong() and 0xFF)
        val b2 = (this.byteArray[position++].toLong() and 0xFF)
        val b3 = (this.byteArray[position++].toLong() and 0xFF)
        val b4 = (this.byteArray[position++].toLong() and 0xFF)
        val b5 = (this.byteArray[position++].toLong() and 0xFF)
        val b6 = (this.byteArray[position++].toLong() and 0xFF)
        val b7 = (this.byteArray[position++].toLong() and 0xFF)

        return when (this.byteOrder) {
            ByteOrder.LITTLE_ENDIAN -> b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24) or (b4 shl 32) or (b5 shl 40) or (b6 shl 48) or (b7 shl 56)
            ByteOrder.BIG_ENDIAN -> (b0 shl 56) or (b1 shl 48) or (b2 shl 40) or (b3 shl 32) or (b4 shl 24) or (b5 shl 16) or (b6 shl 8) or b7
        }
    }

    fun readDouble() = Double.fromBits(readLong())

    fun readByteBoolean(): Boolean {
        return readByte() != 0.toByte()
    }

    fun readIntBoolean(): Boolean {
        val value = readInt()
        require(value == 0 || value == 1) { "Cannot convert $value to a boolean!" }
        return value != 0
    }

    fun readAddresses(): List<Int> {
        val count = readInt()
        val addresses = ArrayList<Int>(count)
        repeat(count) {
            addresses.add(readInt())
        }
        return addresses
    }

    fun readUTF8String(stringLength: Int) = readBytes(stringLength).decodeToString()

    fun writeInt(value: Int) {
        val b0 = (value shr 24) and 0xFF
        val b1 = (value shr 16) and 0xFF
        val b2 = (value shr 8) and 0xFF
        val b3 = (value and 0xFF)

        if (this.byteOrder == ByteOrder.LITTLE_ENDIAN) {
            this.byteArray[this.position++] = b3.toByte()
            this.byteArray[this.position++] = b2.toByte()
            this.byteArray[this.position++] = b1.toByte()
            this.byteArray[this.position++] = b0.toByte()
        } else {
            this.byteArray[this.position++] = b0.toByte()
            this.byteArray[this.position++] = b1.toByte()
            this.byteArray[this.position++] = b2.toByte()
            this.byteArray[this.position++] = b3.toByte()
        }
    }

    fun hasRemaining() = this.position != this.offset + this.sliceSize
}