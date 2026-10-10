package com.mrpowergamerbr.starfait.runner.resources

import com.mrpowergamerbr.starfait.renderer.TextureHandle
import com.mrpowergamerbr.starfait.wad.CollisionMask
import com.mrpowergamerbr.starfait.wad.GameWAD
import com.mrpowergamerbr.starfait.wad.Sprite
import com.mrpowergamerbr.starfait.wad.Sprite.BoundingBoxMode
import com.mrpowergamerbr.starfait.wad.Sprite.Type
import com.mrpowergamerbr.starfait.wad.Texture

class RunnerSprite private constructor(
    val name: String,
    val width: Int,
    val height: Int,
    val bboxLeft: Int,
    val bboxRight: Int,
    val bboxBottom: Int,
    val bboxTop: Int,
    val transparent: Boolean,
    val smooth: Boolean,
    val preload: Boolean,
    val bboxMode: BoundingBoxMode,
    val precise: Boolean,
    val originX: Int,
    val originY: Int,
    val type: Type,
    val frames: List<RunnerTexturePageEntry>,
    val collisionMasks: List<CollisionMask>
) {
    companion object {
        fun fromWAD(gameWAD: GameWAD, sprite: Sprite, textureHandles: Map<Short, TextureHandle>) = RunnerSprite(
            sprite.name.let { gameWAD.strg.getString(it) },
            sprite.width,
            sprite.height,
            sprite.bboxLeft,
            sprite.bboxRight,
            sprite.bboxBottom,
            sprite.bboxTop,
            sprite.transparent,
            sprite.smooth,
            sprite.preload,
            sprite.bboxMode,
            sprite.precise,
            sprite.originX,
            sprite.originY,
            sprite.type,
            sprite.frames.map { RunnerTexturePageEntry.fromWAD(gameWAD.tpag.entries[it]!!, textureHandles) },
            sprite.collisionMasks
        )

        fun fromDynamic(index: Int, width: Int, height: Int, originX: Int, originY: Int, frames: List<RunnerTexturePageEntry>) = RunnerSprite(
            // Name used by the GM:S 1.4.9999 runner
            "__newsprite$index",
            width,
            height,
            0,
            width - 1,
            height - 1,
            0,
            false,
            false,
            false,
            BoundingBoxMode.FULL_IMAGE,
            false,
            originX,
            originY,
            Type.NORMAL,
            frames,
            emptyList()
        )
    }

    // Unlike GameMaker-HTML5, where only precise sprites get a mask built, the WAD stores masks for every sprite
    // So "has a mask" is decided by the precise flag
    val hasCollisionMask get() = precise && collisionMasks.isNotEmpty()

    fun getCollisionMask(imageIndex: Int) = if (this.hasCollisionMask) this.collisionMasks[imageIndex] else null

    fun isPixelSolidOnBoundingBox(x: Int, y: Int, collisionCompatibilityMode: Boolean): Boolean {
        return if (collisionCompatibilityMode)
            (x in bboxLeft..bboxRight && y in bboxTop..bboxBottom)
        else
            (x in bboxLeft until bboxRight && y in bboxTop until bboxBottom)
    }

    fun isPixelSolidOnCollisionMask(imageIndex: Int, x: Int, y: Int, collisionCompatibilityMode: Boolean): Boolean {
        // If a sprite doesn't has a collision mask, then we assume it's entirely solid as long as the x, y falls into the sprite's width and height
        val thisCollisionMaskSprite = getCollisionMask(imageIndex) ?: return isPixelSolidOnBoundingBox(x, y, collisionCompatibilityMode)

        return thisCollisionMaskSprite.isPixelSolid(this.width, this.height, x, y)
    }
}