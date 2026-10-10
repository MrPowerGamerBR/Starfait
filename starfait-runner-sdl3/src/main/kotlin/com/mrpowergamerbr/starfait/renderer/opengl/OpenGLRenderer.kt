package com.mrpowergamerbr.starfait.renderer.opengl

import com.mrpowergamerbr.starfait.GMLColor
import com.mrpowergamerbr.starfait.desktop.StarfaitWindow
import com.mrpowergamerbr.starfait.renderer.Renderer
import com.mrpowergamerbr.starfait.renderer.TextureHandle
import com.mrpowergamerbr.starfait.renderer.opengl.shaders.MinimalShader
import com.mrpowergamerbr.starfait.runner.Instance
import com.mrpowergamerbr.starfait.runner.RunnerRoom
import com.mrpowergamerbr.starfait.runner.StarfaitRunner
import com.mrpowergamerbr.starfait.runner.resources.RunnerFont
import com.mrpowergamerbr.starfait.runner.resources.RunnerSprite
import com.mrpowergamerbr.starfait.runner.resources.RunnerTexturePageEntry
import com.mrpowergamerbr.starfait.wad.GameWAD
import net.perfectdreams.harmony.gl.shaders.ShaderManager
import net.perfectdreams.harmony.gl.shaders.bind
import net.perfectdreams.harmony.gl.textures.Texture
import net.perfectdreams.harmony.gl.textures.TextureManager
import org.joml.Matrix3x2f
import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector3f
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.GL_SCISSOR_TEST
import org.lwjgl.opengl.GL11.glDeleteTextures
import org.lwjgl.opengl.GL11.glDisable
import org.lwjgl.opengl.GL11.glEnable
import org.lwjgl.opengl.GL11.glReadPixels
import org.lwjgl.opengl.GL11.glScissor
import org.lwjgl.opengl.GL11.glViewport
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER
import org.lwjgl.opengl.GL30.glBindFramebuffer
import org.lwjgl.system.MemoryStack
import java.nio.ByteBuffer
import java.nio.IntBuffer
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.text.iterator

class OpenGLRenderer : Renderer() {
    companion object {
        private const val MAX_BATCH_ELEMENTS = 512
        private const val FIELDS_PER_ELEMENT = 48
        private const val TOTAL_ATTRIBUTE_SIZE_PER_ELEMENT = 8
        private const val TOTAL_BATCH_SIZE = FIELDS_PER_ELEMENT * MAX_BATCH_ELEMENTS
        // TODO: This shouldn't be hardcoded!
        private const val VIEWPORT_WIDTH = 640
        private const val VIEWPORT_HEIGHT = 480
    }

    lateinit var runner: StarfaitRunner
    val textureManager = TextureManager()
    val shaderManager = ShaderManager()
    val minimalShader: MinimalShader = shaderManager.loadShader(
        StarfaitWindow::class.java.getResourceAsStream("/shaders/minimal.vsh").readAllBytes().toString(Charsets.UTF_8),
        StarfaitWindow::class.java.getResourceAsStream("/shaders/minimal.fsh").readAllBytes().toString(Charsets.UTF_8),
        ::MinimalShader
    )
    val batch = FloatArray(TOTAL_BATCH_SIZE)
    var activeBatchTexture: Texture? = null
    var currentIndex = 0
    val vao = GL30.glGenVertexArrays()
    val vbo = GL15.glGenBuffers()
    val white1x1Texture = textureManager.load1x1WhiteTexture()
    val textures = mutableMapOf<TextureHandle, Texture>()
    var currentTextureId = 0

    init {
        // For transparency in sprites
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

        GL30.glBindVertexArray(vao)
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo)

        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, Float.SIZE_BYTES * FIELDS_PER_ELEMENT.toLong() * MAX_BATCH_ELEMENTS, GL15.GL_DYNAMIC_DRAW)

        // aPosition
        // stride is from the START of the current "section"!
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, TOTAL_ATTRIBUTE_SIZE_PER_ELEMENT * Float.SIZE_BYTES, 0L)
        GL20.glEnableVertexAttribArray(0)

        // aTexCoord
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, TOTAL_ATTRIBUTE_SIZE_PER_ELEMENT * Float.SIZE_BYTES, 2L * Float.SIZE_BYTES)
        GL20.glEnableVertexAttribArray(1)

        // aBlendColor
        GL20.glVertexAttribPointer(2, 4, GL11.GL_FLOAT, false, TOTAL_ATTRIBUTE_SIZE_PER_ELEMENT * Float.SIZE_BYTES, 4L * Float.SIZE_BYTES)
        GL20.glEnableVertexAttribArray(2)
    }

    private fun getGLTextureFromStarfaitHandle(handle: TextureHandle): Texture {
        return this.textures[handle]!!
    }

    override fun uploadTexture(texture: com.mrpowergamerbr.starfait.wad.Texture): TextureHandle {
        val uploadedTexture = this.textureManager.loadTexture(texture.blob, GL11.GL_NEAREST)
        return allocateAndStoreTextureHandle(uploadedTexture)
    }

    private fun allocateAndStoreTextureHandle(texture: Texture): TextureHandle {
        val textureHandle = TextureHandle(this.currentTextureId++)
        this.textures[textureHandle] = texture
        return textureHandle
    }

    override fun drawInstance(instance: Instance) {
        // Only attempt to render instances that DO have a sprite index
        if (instance.spriteIndex >= 0) {
            val sprite = runner.resources.sprites[instance.spriteIndex.toInt()]
            drawSprite(
                sprite,
                instance.imageIndex.toInt(),
                instance.x,
                instance.y,
                instance.imageXScale,
                instance.imageYScale,
                instance.imageAngle,
                GMLColor(instance.imageBlend.toInt()),
                instance.imageAlpha
            )
        } else {
            // println("Instance ${instance.objectName} has no sprite index!")
        }
    }

    override fun drawSprite(sprite: RunnerSprite, subimg: Int, x: Float, y: Float, xscale: Float, yscale: Float, rot: Float, color: GMLColor, alpha: Float) {
        // Only attempt to render instances that DO have a sprite index
        // See GameMaker-HTML5's yySprite.js Draw, which wraps the subimage: _ind = (~ ~_ind) % this.numb
        val tpag = sprite.frames[Math.floorMod(subimg, sprite.frames.size)]
        val texture = this.textures[tpag.textureIndex]!!

        val rad = Math.toRadians(rot.toDouble())

        val matrix4f = Matrix4f()
            .translate(x, y, 0f)
            .rotateZ(-rad.toFloat())

        // Think of this like if it was a sprite, and the sprite may had included transparent borders, so the values below are in relation to that "canvas"
        // targetX, targetY = where the first non-opaque pixel is

        // Those are relative to "local"
        val left = ((tpag.targetX.toFloat() - sprite.originX) * xscale)
        val right = (((tpag.targetX.toFloat() - sprite.originX + tpag.targetWidth.toInt()) * xscale))
        val top = ((tpag.targetY.toFloat() - sprite.originY) * yscale)
        val bottom = (((tpag.targetY.toFloat() - sprite.originY + tpag.targetHeight.toInt()) * yscale))

        val x1y1Transformed = matrix4f.transformPosition(Vector3f(left, top, 0f), Vector3f())
        val x2y2Transformed = matrix4f.transformPosition(Vector3f(right, top, 0f), Vector3f())
        val x3y3Transformed = matrix4f.transformPosition(Vector3f(right, bottom, 0f), Vector3f())
        val x4y4Transformed = matrix4f.transformPosition(Vector3f(left, bottom, 0f), Vector3f())

        drawQuad(
            x1y1Transformed.x,
            x1y1Transformed.y,
            x2y2Transformed.x,
            x2y2Transformed.y,
            x3y3Transformed.x,
            x3y3Transformed.y,
            x4y4Transformed.x,
            x4y4Transformed.y,
            tpag.sourceX.toFloat() / texture.width,
            tpag.sourceY.toFloat() / texture.height,
            (tpag.sourceX.toFloat() + tpag.sourceWidth.toFloat()) / texture.width,
            (tpag.sourceY.toFloat() + tpag.sourceHeight.toFloat()) / texture.height,
            texture,
            color,
            color,
            color,
            color,
            alpha
        )
    }

    override fun drawSpritePart(
        sprite: RunnerSprite,
        subimg: Int,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        x: Float,
        y: Float,
        xscale: Float,
        yscale: Float,
        color: GMLColor,
        alpha: Float
    ) {
        val tpag = sprite.frames[Math.floorMod(subimg, sprite.frames.size)]

        val texture = textures[tpag.textureIndex]!!
        // See GameMaker-HTML5's yyWebGL.js WebGL_DrawPart_RELEASE
        var partLeft = left
        var partTop = top
        var partWidth = width
        var partHeight = height
        var partX = x
        var partY = y

        val xOffset = tpag.targetX.toFloat()
        val yOffset = tpag.targetY.toFloat()

        // CLIP the drawing area.
        if (xOffset > partLeft) {
            val off = xOffset - partLeft
            partX += off * xscale
            partWidth -= off
            partLeft = 0f
        } else {
            partLeft -= xOffset
        }

        if (yOffset > partTop) {
            val off = yOffset - partTop
            partY += off * yscale
            partHeight -= off
            partTop = 0f
        } else {
            partTop -= yOffset
        }

        val cropWidth = tpag.sourceWidth.toFloat()
        val cropHeight = tpag.sourceHeight.toFloat()
        if (partWidth > cropWidth - partLeft) partWidth = cropWidth - partLeft
        if (partHeight > cropHeight - partTop) partHeight = cropHeight - partTop
        if (0 >= partWidth || 0 >= partHeight) return

        drawQuad(
            partX,
            partY,
            (tpag.sourceX.toFloat() + partLeft) / texture.width,
            (tpag.sourceY.toFloat() + partTop) / texture.height,
            (tpag.sourceX.toFloat() + partLeft + partWidth) / texture.width,
            (tpag.sourceY.toFloat() + partTop + partHeight) / texture.height,
            partWidth * xscale,
            partHeight * yscale,
            texture,
            color,
            alpha
        )
    }

    override fun drawBackground(texturePageEntry: RunnerTexturePageEntry, x: Float, y: Float, width: Float, height: Float, tileX: Boolean, tileY: Boolean, roomWidth: Int, roomHeight: Int, alpha: Float) {
        val texture = textures[texturePageEntry.textureIndex]!!

        val tpag = texturePageEntry

        val howManyTilesX = if (tileX) ceil((roomWidth - x) / width).toInt() else 1
        val howManyTilesY = if (tileY) ceil((roomHeight - y) / height).toInt() else 1

        // println("X tiles: $howManyTilesX, Y tiles: $howManyTilesY, $width, $height")
        repeat(howManyTilesY) { cellY ->
            repeat(howManyTilesX) { cellX ->
                val drawX = (x + tpag.targetX.toInt()) + (cellX * width)
                val drawY = (y + tpag.targetY.toInt()) + (cellY * height)

                // println("$drawX, $drawY")
                drawQuad(
                    drawX,
                    drawY,
                    tpag.sourceX.toFloat() / texture.width,
                    tpag.sourceY.toFloat() / texture.height,
                    (tpag.sourceX.toFloat() + tpag.sourceWidth.toFloat()) / texture.width,
                    (tpag.sourceY.toFloat() + tpag.sourceHeight.toFloat()) / texture.height,
                    width,
                    height,
                    textures[tpag.textureIndex]!!,
                    GMLColor(0xFFFFFF),
                    alpha
                )
            }
        }
    }

    override fun drawTile(texturePageEntry: RunnerTexturePageEntry, tile: RunnerRoom.RunnerTile) {
        val texture = textures[texturePageEntry.textureIndex]!!

        val tpag = texturePageEntry

        val entryX = tpag.sourceX.toInt() + tile.sourceX
        val entryY = tpag.sourceY.toInt() + tile.sourceY
        val entryWidth = tile.width
        val entryHeight = tile.height

        drawQuad(
            tile.x + tpag.targetX.toFloat(),
            tile.y + tpag.targetY.toFloat(),
            entryX.toFloat() / texture.width,
            entryY.toFloat() / texture.height,
            (entryX + entryWidth.toFloat()) / texture.width,
            (entryY + entryHeight.toFloat()) / texture.height,
            tile.width.toFloat(),
            tile.height.toFloat(),
            textures[tpag.textureIndex]!!,
            GMLColor(0xFFFFFF),
            1.0f
        )
    }

    override fun drawText(font: RunnerFont, x: Float, y: Float, xscale: Float, yscale: Float, rot: Float, text: String) {
        val tpag = font.texturePageEntry
        val texture = getGLTextureFromStarfaitHandle(tpag.textureIndex)

        val startX = x
        val startY = y
        var x = x
        var y = y

        flushIfNeeded(texture)

        // TODO: We need to change this for GameMaker: Studio 2+!
        val lines = text.split("#")

        val rad = Math.toRadians(rot.toDouble())

        // Calculate the string width first
        // TODO: Maybe we could refactor so that the function already provides the position of each glyph?
        var stringWidth = 0f
        var stringHeight = 0f

        for (line in lines) {
            var maxHeightOnThisLine = 0f
            for (char in line) {
                val glyph = font.characterToGlyph[char]

                if (glyph != null) {
                    stringWidth += (glyph.offset * xscale)
                    stringWidth += (glyph.shift * xscale)
                    maxHeightOnThisLine = max(maxHeightOnThisLine, (glyph.height.toFloat() * yscale))
                }
            }
            stringHeight += maxHeightOnThisLine
        }

        // Where the text will be rotated around (the center of the string)
        val pivotX = x + (stringWidth / 2f)
        val pivotY = y + (stringHeight / 2f)

        // Render around center (backwards!)
        val matrix = Matrix3x2f()
            // Translate it back to the pivot
            .translate(pivotX, pivotY)
            // Rotate it
            .rotate(rad.toFloat())
            // Translate it to the world's origin
            .translate(-pivotX, -pivotY)

        // Now we ACTUALLY draw it
        for (line in lines) {
            var maxHeightOnThisLine = 0f

            for (char in line) {
                val glyph = font.characterToGlyph[char]

                if (glyph != null) {
                    x += (glyph.offset * xscale)

                    val width = (glyph.width.toFloat() * xscale)
                    val height = (glyph.height.toFloat() * yscale)

                    val xy1 = matrix.transformPosition(Vector2f(x, y), Vector2f())
                    val xy2 = matrix.transformPosition(Vector2f(x + width, y), Vector2f())
                    val xy3 = matrix.transformPosition(Vector2f(x + width, y + height), Vector2f())
                    val xy4 = matrix.transformPosition(Vector2f(x, y + height), Vector2f())

                    drawQuad(
                        xy1.x,
                        xy1.y,
                        xy2.x,
                        xy2.y,
                        xy3.x,
                        xy3.y,
                        xy4.x,
                        xy4.y,
                        (tpag.sourceX.toFloat() + glyph.x.toFloat()) / texture.width,
                        (tpag.sourceY.toFloat() + glyph.y.toFloat()) / texture.height,
                        (tpag.sourceX.toFloat() + glyph.x.toFloat() + glyph.width.toFloat()) / texture.width,
                        (tpag.sourceY.toFloat() + glyph.y.toFloat() + glyph.height.toFloat()) / texture.height,
                        texture,
                        runner.drawState.color,
                        runner.drawState.color,
                        runner.drawState.color,
                        runner.drawState.color,
                        runner.drawState.alpha
                    )

                    x += (glyph.shift * xscale)
                    maxHeightOnThisLine = max(maxHeightOnThisLine, (glyph.height.toFloat() * yscale))
                }
            }
            x = startX
            y += maxHeightOnThisLine
        }
    }

    override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, thickness: Float, color1: GMLColor, color2: GMLColor, alpha: Float) {
        // Essentially to draw a line you get the perpendicular vector to the line, and then you "inflate" the coordinates to get the point of each of the quad's corner
        val p1 = Vector2f(x1, y1)
        val p2 = Vector2f(x2, y2)
        val dir = Vector2f(p2).sub(p1).normalize()
        val perpendicular = Vector2f(-dir.y, dir.x).mul(thickness * 0.5f)

        // These follow the same convention as the drawQuad (top left, top right, bottom right, bottom left)
        val c1 = Vector2f(p1).add(perpendicular)
        val c2 = Vector2f(p1).sub(perpendicular)
        val c3 = Vector2f(p2).sub(perpendicular)
        val c4 = Vector2f(p2).add(perpendicular)

        drawQuad(
            c1.x,
            c1.y,
            c2.x,
            c2.y,
            c3.x,
            c3.y,
            c4.x,
            c4.y,
            0f,
            0f,
            1f,
            1f,
            white1x1Texture,
            color1,
            color1,
            color2,
            color2,
            1.0f
        )
    }

    override fun drawRectangle(x: Float, y: Float, width: Float, height: Float, color: GMLColor, alpha: Float, outline: Boolean) {
        if (outline) {
            // top
            drawQuad(
                x,
                y,
                0.0f,
                0.0f,
                1.0f,
                1.0f,
                width,
                1f,
                white1x1Texture,
                color,
                alpha
            )

            // bottom
            drawQuad(
                x,
                y + height - 1f,
                0.0f,
                0.0f,
                1.0f,
                1.0f,
                width,
                1f,
                white1x1Texture,
                color,
                alpha
            )

            // left
            drawQuad(
                x,
                y,
                0.0f,
                0.0f,
                1.0f,
                1.0f,
                1f,
                height,
                white1x1Texture,
                color,
                alpha
            )

            // right
            drawQuad(
                x + width - 1f,
                y,
                0.0f,
                0.0f,
                1.0f,
                1.0f,
                1f,
                height,
                white1x1Texture,
                color,
                alpha
            )
        } else {
            drawQuad(
                x,
                y,
                0.0f,
                0.0f,
                1.0f,
                1.0f,
                width,
                height,
                white1x1Texture,
                color,
                alpha
            )
        }
    }

    override fun drawTriangle(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float,
        blend1: GMLColor,
        blend2: GMLColor,
        blend3: GMLColor,
        alpha: Float,
        outline: Boolean
    ) {
        if (outline) {
            // TODO: The lines are being rendered off screen if they are at 0, 0?
            drawLine(x1, y1, x2, y2, 1f, blend1, blend2, alpha)
            drawLine(x2, y2, x3, y3, 1f, blend2, blend3, alpha)
            drawLine(x3, y3, x1, y1, 1f, blend3, blend1, alpha)
        } else {
            val texture = this.white1x1Texture
            flushIfNeeded(texture)

            // vertex #1
            batch[currentIndex++] = x1; batch[currentIndex++] = y1;
            batch[currentIndex++] = 0.0f; batch[currentIndex++] = 0.0f;

            batch[currentIndex++] = blend1.redF
            batch[currentIndex++] = blend1.greenF
            batch[currentIndex++] = blend1.blueF
            batch[currentIndex++] = alpha

            // vertex #2
            batch[currentIndex++] = x2; batch[currentIndex++] = y2;
            batch[currentIndex++] = 1.0f; batch[currentIndex++] = 0.0f;

            batch[currentIndex++] = blend2.redF
            batch[currentIndex++] = blend2.greenF
            batch[currentIndex++] = blend2.blueF
            batch[currentIndex++] = alpha

            // vertex #3
            batch[currentIndex++] = x3; batch[currentIndex++] = y3;
            batch[currentIndex++] = 1.0f; batch[currentIndex++] = 1.0f;

            batch[currentIndex++] = blend3.redF
            batch[currentIndex++] = blend3.greenF
            batch[currentIndex++] = blend3.blueF
            batch[currentIndex++] = alpha

            this.activeBatchTexture = texture
        }
    }

    override fun drawCircle(x: Float, y: Float, radius: Float, circlePrecision: Int, col1: GMLColor, col2: GMLColor, alpha: Float, outline: Boolean) {
        val steps = circlePrecision

        data class Point(val x: Float, val y: Float)

        val points = mutableListOf<Point>()

        repeat(steps) {
            val degrees = (it / steps.toFloat()) * 360f
            val rads = Math.toRadians(degrees.toDouble()).toFloat()
            val x = cos(rads) * radius
            val y = sin(rads) * radius

            // TODO: Is this really correct? Because it feels very wonky that we need to add +1 to the coordinate
            //  And the drawLine has a similar issue too...?
            points.add(Point(x + 1f, y + 1f))
        }

        if (outline) {
            repeat(steps) {
                val point1 = points[it]
                val point2 = points[(it + 1) % steps]

                // TODO: This looks very hacky
                // "Whether the circle is an outline (true) or not (false). If true, col1 is irrelevant."
                drawLine(point1.x + x, point1.y + y, point2.x + x, point2.y + y, 0.93f, col2, col2, alpha)
            }
        } else {
            val texture = this.white1x1Texture
            flushIfNeeded(texture)

            val requiredElements = (steps * 3) * TOTAL_ATTRIBUTE_SIZE_PER_ELEMENT
            if (this.currentIndex + requiredElements >= TOTAL_BATCH_SIZE) {
                flush()
            }

            repeat(steps) {
                val point1 = points[it]
                val point2 = points[(it + 1) % steps]

                // vertex #1
                batch[currentIndex++] = x; batch[currentIndex++] = y;
                batch[currentIndex++] = 0.0f; batch[currentIndex++] = 0.0f;

                batch[currentIndex++] = col1.redF
                batch[currentIndex++] = col1.greenF
                batch[currentIndex++] = col1.blueF
                batch[currentIndex++] = alpha

                // vertex #2
                batch[currentIndex++] = x + point1.x; batch[currentIndex++] = y + point1.y;
                batch[currentIndex++] = 1.0f; batch[currentIndex++] = 0.0f;

                batch[currentIndex++] = col2.redF
                batch[currentIndex++] = col2.greenF
                batch[currentIndex++] = col2.blueF
                batch[currentIndex++] = alpha

                // vertex #3
                batch[currentIndex++] = x + point2.x; batch[currentIndex++] = y + point2.y;
                batch[currentIndex++] = 1.0f; batch[currentIndex++] = 1.0f;

                batch[currentIndex++] = col2.redF
                batch[currentIndex++] = col2.greenF
                batch[currentIndex++] = col2.blueF
                batch[currentIndex++] = alpha
            }

            this.activeBatchTexture = texture
        }
    }

    override fun createTextureFromSurface(surfaceId: Int, x: Int, y: Int, width: Int, height: Int, removeback: Boolean, smooth: Boolean): TextureHandle {
        // This is a bit painful but it is what it is

        // We flush anything pending so that we actually COPY the screen with anything that hasn't been flushed recently
        flush()

        // TODO: Other surfaces

        // This sadly very much sucks quite a bit
        // TODO: Maybe replace this with a framebuffer? We draw directly to the fb and flip it on the GPU
        val flippedByteBuffer = ByteBuffer.allocateDirect(width * height * 4)
        glReadPixels(x, VIEWPORT_HEIGHT - y - height, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, flippedByteBuffer)

        // We can't use ".array()" directly because it is a ooo allocateDirect
        val flippedByteArray = ByteArray(width * height * 4)
        flippedByteBuffer.get(flippedByteArray)
        val byteBuffer = ByteBuffer.allocateDirect(width * height * 4)
        for (i in (height - 1) downTo 0) {
            byteBuffer.put(flippedByteArray, i * width * 4, width * 4)
        }
        byteBuffer.position(0)

        // 1. Generate and bind a new texture ID
        val textureId = GL11.glGenTextures()
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId)

        // 2. Tell OpenGL how to unpack the RGBA bytes.
        // Each component is 1 byte size
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1)

        // Create the texture first
        GL11.glTexImage2D(
            GL11.GL_TEXTURE_2D,
            0,
            GL11.GL_RGBA,
            width,
            height,
            0,
            GL11.GL_RGBA,
            GL11.GL_UNSIGNED_BYTE,
            byteBuffer
        )

        // 3. Set texture parameters
        // Repeat image in both directions
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT)
        // When stretching the image, pixelate
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
        // When shrinking the image, pixelate
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)

        glBindFramebuffer(GL_READ_FRAMEBUFFER, 0)

        return allocateAndStoreTextureHandle(Texture(textureId, width, height))
    }

    override fun deleteTextures(textures: List<TextureHandle>) {
        val texturesToBeDeleted = textures.mapNotNull { this.textures.remove(it) }

        if (texturesToBeDeleted.isNotEmpty()) {
            MemoryStack.stackPush().use {
                val textures = it.ints(*texturesToBeDeleted.map { it.textureId }.toIntArray())
                glDeleteTextures(textures)
            }
        }
    }

    override fun withView(view: RunnerRoom.RunnerView?, block: () -> Unit) {
        flush()

        glEnable(GL_SCISSOR_TEST)

        if (view != null) {
            val halfWidth = view.viewWidth.toFloat() / 2f
            val halfHeight = view.viewHeight.toFloat() / 2f

            // portX/portY = where it will be rendered in the screen
            // We do - for the Y because OpenGL is y up
            glViewport(view.portX, VIEWPORT_HEIGHT - (view.portY + view.portHeight), view.portWidth, view.portHeight)
            glScissor(view.portX, VIEWPORT_HEIGHT - (view.portY + view.portHeight), view.portWidth, view.portHeight)

            minimalShader.uProjection.set(
                Matrix4f()
                    .ortho(
                        view.viewX.toFloat(),
                        view.viewX.toFloat() + view.viewWidth.toFloat(),
                        view.viewY.toFloat() + view.viewHeight.toFloat(),
                        view.viewY.toFloat(),
                        1.0f,
                        -1.0f
                    )
                    // Essentially what we do here is that we want to move the center of the camera to (0, 0), then we apply the rotation, then we move it back to where it is supposed to be
                    .translate(halfWidth + view.viewX.toFloat(), halfHeight + view.viewY.toFloat(), 0f)
                    .rotateZ(Math.toRadians(-view.angle.toDouble()).toFloat())
                    .translate(-halfWidth - view.viewX.toFloat(), -halfHeight - view.viewY.toFloat(), 0f)
                    .get(FloatArray(16))
            )
        } else {
            glViewport(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
            glScissor(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)

            minimalShader.uProjection.set(
                Matrix4f()
                    .ortho(0.0f, runner.currentRoom.width.toFloat(), runner.currentRoom.height.toFloat(), 0.0f, 1.0f, -1.0f)
                    .get(FloatArray(16))
            )
        }

        block()

        glDisable(GL_SCISSOR_TEST)
    }

    override fun frame(block: () -> Unit) {
        // TODO: Respect the room's drawBackgroundColor
        val color = runner.currentRoom.backgroundColor
        GL11.glClearColor(color.redF, color.greenF, color.blueF, 1.0f)
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT)

        block()

        if (runner.enableCollisionRendering) {
            for (instance in runner.currentRoom.instances) {
                val bbox = instance.getBoundingBox()

                drawQuad(
                    bbox.left,
                    bbox.top,
                    0.0f,
                    0.0f,
                    1.0f,
                    1.0f,
                    bbox.right - bbox.left,
                    bbox.bottom - bbox.top,
                    white1x1Texture,
                    GMLColor(0x00ff00),
                    0.25f
                )

                if (instance.spriteIndex >= 0f) {
                    val collisionSprite = instance.getCollisionSprite()
                    if (collisionSprite != null && collisionSprite.hasCollisionMask) {
                        // This is very much NOT optimized
                        // I think the best way would be to convert the mask into a image, then blit it to a texture
                        for (y in 0 until (collisionSprite.height * instance.imageYScale).toInt()) {
                            for (x in 0 until (collisionSprite.width * instance.imageXScale).toInt()) {
                                if (collisionSprite.isPixelSolidOnCollisionMask(instance.imageIndex.toInt(), (x / instance.imageXScale).toInt(), (y / instance.imageYScale).toInt(), runner.collisionCompatibilityMode)) {
                                    drawQuad(
                                        x + instance.x,
                                        y + instance.y,
                                        0.0f,
                                        0.0f,
                                        1.0f,
                                        1.0f,
                                        1f,
                                        1f,
                                        white1x1Texture,
                                        GMLColor(0x00ffff),
                                        0.25f
                                    )
                                }
                            }
                        }
                    }
                }
            }

            for (debugRectangle in runner.debugRectangles) {
                drawQuad(
                    debugRectangle.bbox.left,
                    debugRectangle.bbox.top,
                    0.0f,
                    0.0f,
                    1.0f,
                    1.0f,
                    debugRectangle.bbox.right - debugRectangle.bbox.left,
                    debugRectangle.bbox.bottom - debugRectangle.bbox.top,
                    white1x1Texture,
                    debugRectangle.color,
                    0.5f
                )
            }
        }

        runner.debugRectangles.clear()

        flush()
    }

    fun flushIfNeeded(texture: Texture) {
        if (this.activeBatchTexture != texture) {
            flush()
            return
        }

        if (this.currentIndex == batch.size) {
            flush()
            return
        }
    }

    fun drawQuad(
        x: Float,
        y: Float,
        u1: Float,
        v1: Float,
        u2: Float,
        v2: Float,
        width: Float,
        height: Float,
        texture: Texture,
        blend: GMLColor,
        alpha: Float
    ) = drawQuad(x, y, x + width, y, x + width, y + height, x, y + height, u1, v1, u2, v2, texture, blend, blend, blend, blend, alpha)

    // Clockwise pattern for the coordinates
    fun drawQuad(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float,
        x4: Float,
        y4: Float,
        u1: Float,
        v1: Float,
        u2: Float,
        v2: Float,
        texture: Texture,
        blend1: GMLColor,
        blend2: GMLColor,
        blend3: GMLColor,
        blend4: GMLColor,
        alpha: Float
    ) {
        flushIfNeeded(texture)

        // top-left
        batch[currentIndex++] = x1; batch[currentIndex++] = y1;
        batch[currentIndex++] = u1; batch[currentIndex++] = v1;

        batch[currentIndex++] = blend1.redF
        batch[currentIndex++] = blend1.greenF
        batch[currentIndex++] = blend1.blueF
        batch[currentIndex++] = alpha

        // bottom-right
        batch[currentIndex++] = x3; batch[currentIndex++] = y3;
        batch[currentIndex++] = u2; batch[currentIndex++] = v2;

        batch[currentIndex++] = blend3.redF
        batch[currentIndex++] = blend3.greenF
        batch[currentIndex++] = blend3.blueF
        batch[currentIndex++] = alpha

        // bottom-left
        batch[currentIndex++] = x4; batch[currentIndex++] = y4;
        batch[currentIndex++] = u1; batch[currentIndex++] = v2;

        batch[currentIndex++] = blend4.redF
        batch[currentIndex++] = blend4.greenF
        batch[currentIndex++] = blend4.blueF
        batch[currentIndex++] = alpha

        // top-left
        batch[currentIndex++] = x1; batch[currentIndex++] = y1;
        batch[currentIndex++] = u1; batch[currentIndex++] = v1;

        batch[currentIndex++] = blend1.redF
        batch[currentIndex++] = blend1.greenF
        batch[currentIndex++] = blend1.blueF
        batch[currentIndex++] = alpha

        // top-right
        batch[currentIndex++] = x2; batch[currentIndex++] = y2;
        batch[currentIndex++] = u2; batch[currentIndex++] = v1;

        batch[currentIndex++] = blend2.redF
        batch[currentIndex++] = blend2.greenF
        batch[currentIndex++] = blend2.blueF
        batch[currentIndex++] = alpha

        // bottom-right
        batch[currentIndex++] = x3; batch[currentIndex++] = y3;
        batch[currentIndex++] = u2; batch[currentIndex++] = v2;

        batch[currentIndex++] = blend3.redF
        batch[currentIndex++] = blend3.greenF
        batch[currentIndex++] = blend3.blueF
        batch[currentIndex++] = alpha

        this.activeBatchTexture = texture
    }

    fun flush() {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo)
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, batch)

        val activeBatchTexture = this.activeBatchTexture
        if (activeBatchTexture != null)
            minimalShader.uTexture.set(0, activeBatchTexture)

        minimalShader.bind {
            GL30.glBindVertexArray(vao)
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, currentIndex / TOTAL_ATTRIBUTE_SIZE_PER_ELEMENT)
        }

        this.currentIndex = 0
        // We don't need to clear the current batch even though it has TRASH in it
    }
}