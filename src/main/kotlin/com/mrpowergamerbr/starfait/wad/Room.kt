package com.mrpowergamerbr.starfait.wad

import com.mrpowergamerbr.starfait.GMLColor

class Room(
    val name: StringPointer,
    val caption: StringPointer,
    val width: Int,
    val height: Int,
    val speed: Int,
    val persistent: Boolean,
    val backgroundColor: GMLColor,
    val drawBackgroundColor: Boolean,
    // -1 when the room has no creation code
    val creationCodeId: Int,
    val flags: RoomFlags,
    val physicsWorld: Boolean,
    val physicsWorldTop: Int,
    val physicsWorldLeft: Int,
    val physicsWorldRight: Int,
    val physicsWorldBottom: Int,
    val physicsWorldGravityX: Float,
    val physicsWorldGravityY: Float,
    val physicsWorldPixelsToMeters: Float,
    @DumpNested
    val backgrounds: List<Background>,
    @DumpNested
    val views: List<View>,
    @DumpNested
    val instances: List<Instance>,
    val tiles: List<Tile>
) {
    class Background(
        val enabled: Boolean,
        val foreground: Boolean,
        val backgroundIndex: Int,
        val x: Int,
        val y: Int,
        val tileX: Boolean,
        val tileY: Boolean,
        val speedX: Int,
        val speedY: Int,
        val stretch: Boolean
    )

    class View(
        val visible: Boolean,
        val viewX: Int,
        val viewY: Int,
        val viewWidth: Int,
        val viewHeight: Int,
        val portX: Int,
        val portY: Int,
        val portWidth: Int,
        val portHeight: Int,
        val borderX: Int,
        val borderY: Int,
        val speedX: Int,
        val speedY: Int,
        val objectId: Int
    )

    class Instance(
        val x: Int,
        val y: Int,
        val objectIndex: Int,
        val id: Int,
        val creationCodeId: Int,
        val scaleX: Float,
        val scaleY: Float,
        val color: Int,
        val rotation: Float,
        val preCreateCodeId: Int
    )

    class Tile(
        val x: Int,
        val y: Int,
        val backgroundIndex: Int,
        val sourceX: Int,
        val sourceY: Int,
        val width: Int,
        val height: Int,
        val depth: Int,
        val id: Int,
        val scaleX: Float,
        val scaleY: Float,
        val color: Int
    )
}
