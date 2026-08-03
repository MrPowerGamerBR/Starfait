package com.mrpowergamerbr.starfait.wad.chunks

import com.mrpowergamerbr.starfait.StarfaitByteReader
import com.mrpowergamerbr.starfait.wad.StringPointer

class GEN8(
    val debugWad: Boolean,
    val wadVersion: Byte,
    val gameTitle: StringPointer,
    val yoyoConfig: StringPointer,
    val maxObjectId: Int,
    val maxTileId: Int,
    val gameId: Int,
    val gameGuid: ByteArray,
    val gameProjectName: StringPointer,
    val major: Int,
    val minor: Int,
    val release: Int,
    val build: Int,
    val initialApplicationWidth: Int,
    val initialApplicationHeight: Int,
    val initialScreenFlags: Int,
    val md5Crc: Int,
    val md5: ByteArray,
    val wadDateTime: Long,
    val gameDisplayName: StringPointer,
    val licensedTargets: Long,
    val functionClassifications: Long,
    val steamAppId: Int,
    val debuggerServerPort: Int,
    val roomOrder: List<Int>
) {
    companion object {
        fun read(reader: StarfaitByteReader): GEN8 {
            val debugWad = reader.readByteBoolean()
            val wadVersion = reader.readByte()
            reader.readBytes(2) // Padding
            val gameTitle = reader.readStringPointer()
            val yoyoConfig = reader.readStringPointer()
            val maxObjectId = reader.readInt()
            val maxTileId = reader.readInt()
            val gameId = reader.readInt()
            val gameGuid = reader.readBytes(16)
            val gameProjectName = reader.readStringPointer()
            val major = reader.readInt()
            val minor = reader.readInt()
            val release = reader.readInt()
            val build = reader.readInt()
            val initialApplicationWidth = reader.readInt()
            val initialApplicationHeight = reader.readInt()
            val initialScreenFlags = reader.readInt()
            val md5Crc = reader.readInt()
            val md5 = reader.readBytes(16)
            val wadDateTime = reader.readLong()
            val gameDisplayName = reader.readStringPointer()
            val licensedTargets = reader.readLong()
            val functionClassifications = reader.readLong()
            val steamAppId = reader.readInt()
            val debuggerServerPort = reader.readInt()
            val roomOrderCount = reader.readInt()
            val roomOrder = ArrayList<Int>(roomOrderCount)

            repeat(roomOrderCount) {
                roomOrder.add(reader.readInt())
            }

            return GEN8(
                debugWad = debugWad,
                wadVersion = wadVersion,
                gameTitle = gameTitle,
                yoyoConfig = yoyoConfig,
                maxObjectId = maxObjectId,
                maxTileId = maxTileId,
                gameId = gameId,
                gameGuid = gameGuid,
                gameProjectName = gameProjectName,
                major = major,
                minor = minor,
                release = release,
                build = build,
                initialApplicationWidth = initialApplicationWidth,
                initialApplicationHeight = initialApplicationHeight,
                initialScreenFlags = initialScreenFlags,
                md5Crc = md5Crc,
                md5 = md5,
                wadDateTime = wadDateTime,
                gameDisplayName = gameDisplayName,
                licensedTargets = licensedTargets,
                functionClassifications = functionClassifications,
                steamAppId = steamAppId,
                debuggerServerPort = debuggerServerPort,
                roomOrder = roomOrder
            )
        }
    }
}