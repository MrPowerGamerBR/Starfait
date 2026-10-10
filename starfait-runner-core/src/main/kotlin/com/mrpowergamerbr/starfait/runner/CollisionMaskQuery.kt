package com.mrpowergamerbr.starfait.runner

import com.mrpowergamerbr.starfait.MathUtils
import com.mrpowergamerbr.starfait.runner.resources.RunnerSprite
import com.mrpowergamerbr.starfait.wad.Sprite
import org.joml.Matrix3x2f
import org.joml.Vector2f
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class CollisionMaskQuery(
    val sprite: RunnerSprite,
    val spriteX: Float,
    val spriteY: Float,
    val imageXScale: Float,
    val imageYScale: Float,
    val imageAngle: Float,
    val collisionCompatibilityMode: Boolean
) {
    companion object {
        fun computeOverlappingBoundingBox(bb1: BoundingBox, bb2: BoundingBox): BoundingBox {
            val left = max(bb1.left, bb2.left)
            val right = min(bb1.right, bb2.right)
            val top = max(bb1.top, bb2.top)
            val bottom = min(bb1.bottom, bb2.bottom)
            return BoundingBox.fromLeftRightTopBottom(left, right, top, bottom)
        }

        // See GameMaker-HTML5's "OrigPreciseCollision"
        // However our implementation diverges quite a bit so that it is more compact and simpler to understand
        fun checkPreciseCollisionInstanceInstance(
            inst1: Instance,
            bb1: BoundingBox,
            sprite1: RunnerSprite,
            inst2: Instance,
            bb2: BoundingBox,
            sprite2: RunnerSprite,
            collisionCompatibilityMode: Boolean
        ): Boolean {
            val query1 = CollisionMaskQuery(sprite1, inst1.x, inst1.y, inst1.imageXScale, inst1.imageYScale, inst1.imageAngle, collisionCompatibilityMode)
            val query2 = CollisionMaskQuery(sprite2, inst2.x, inst2.y, inst2.imageXScale, inst2.imageYScale, inst2.imageAngle, collisionCompatibilityMode)

            val (left, right, top, bottom) = computeOverlappingBoundingBox(bb1, bb2)

            for (x in left.toInt()..right.toInt()) {
                for (y in top.toInt()..bottom.toInt()) {
                    if (query1.isPixelSolidOnCollisionMask(inst1.imageIndex.toInt(), x.toFloat(), y.toFloat()) && query2.isPixelSolidOnCollisionMask(inst2.imageIndex.toInt(), x.toFloat(), y.toFloat()))
                        return true
                }
            }

            return false
        }

        fun checkPreciseCollisionInstanceRectangle(
            inst1: Instance,
            bb1: BoundingBox,
            sprite1: RunnerSprite,
            bb2: BoundingBox,
            collisionCompatibilityMode: Boolean
        ): Boolean {
            val query1 = CollisionMaskQuery(sprite1, inst1.x, inst1.y, inst1.imageXScale, inst1.imageYScale, inst1.imageAngle, collisionCompatibilityMode)

            // Compute overlapping bounding box
            val (left, right, top, bottom) = computeOverlappingBoundingBox(bb1, bb2)

            for (x in left.toInt()..right.toInt()) {
                for (y in top.toInt()..bottom.toInt()) {
                    if (query1.isPixelSolidOnCollisionMask(inst1.imageIndex.toInt(), x.toFloat(), y.toFloat()))
                        return true
                }
            }

            return false
        }

        fun checkPreciseCollisionInstanceCircle(
            inst1: Instance,
            bb1: BoundingBox,
            sprite1: RunnerSprite,
            circle: Circle,
            collisionCompatibilityMode: Boolean
        ): Boolean {
            val query1 = CollisionMaskQuery(sprite1, inst1.x, inst1.y, inst1.imageXScale, inst1.imageYScale, inst1.imageAngle, collisionCompatibilityMode)

            val circleBB = circle.getBoundingBox()

            // Compute overlapping bounding box
            val (left, right, top, bottom) = computeOverlappingBoundingBox(bb1, circleBB)

            // This is a bit wonky but essentially we check if the circle overlaps with the bounding box by calculating if the point is inside the circle
            for (x in left.toInt()..right.toInt()) {
                for (y in top.toInt()..bottom.toInt()) {
                    if (query1.isPixelSolidOnCollisionMask(inst1.imageIndex.toInt(), x.toFloat(), y.toFloat()))
                        return true
                }
            }

            return false
        }

        fun checkPreciseCollisionInstancePoint(
            inst1: Instance,
            sprite1: RunnerSprite,
            x: Float,
            y: Float,
            collisionCompatibilityMode: Boolean
        ): Boolean {
            val query = CollisionMaskQuery(sprite1, inst1.x, inst1.y, inst1.imageXScale, inst1.imageYScale, inst1.imageAngle, collisionCompatibilityMode)
            return query.isPixelSolidOnCollisionMask(inst1.imageIndex.toInt(), x, y)
        }
    }

    private val matrix: Matrix3x2f

    init {
        val x1 = MathUtils.delphiRound(spriteX).toFloat()
        val y1 = MathUtils.delphiRound(spriteY).toFloat()

        // A matrix that converts from the "world" location to the "sprite" local location, used to convert coordinates into local-sprite coordinates
        // So essentially what we do is (remember that Matrix operations are backwards!):
        // 1. We translate FROM the sprite's position in the world
        // 2. We undo the rotation (we use "imageAngle" instead of "-imageAngle" because we already use "-imageAngle" when drawing the sprite)
        // 3. We scale the image *down* based on the current scale, to convert the "world" size to the "sprite" size
        // 4. We translate to the sprite's origin (in the sprite itself, NOT in the world)
        // And that's all that there's to it!
        this.matrix = Matrix3x2f()
            .translate(this.sprite.originX.toFloat(), this.sprite.originY.toFloat())
            .scale(1.0f / this.imageXScale, 1.0f / this.imageYScale)
            .rotate(Math.toRadians(imageAngle.toDouble()).toFloat())
            .translate(-x1, -y1)
    }

    fun isPixelSolidOnCollisionMask(imageIndex: Int, x: Float, y: Float): Boolean {
        val img1 = Math.floorMod(imageIndex, sprite.collisionMasks.size.coerceAtLeast(1))
        val transformedPosition = matrix.transformPosition(Vector2f(x, y))
        return sprite.isPixelSolidOnCollisionMask(img1, floor(transformedPosition.x).toInt(), floor(transformedPosition.y).toInt(), collisionCompatibilityMode)
    }
}