package com.mrpowergamerbr.starfait.renderer.opengl

import com.mrpowergamerbr.starfait.GMLColor
import com.mrpowergamerbr.starfait.desktop.StarfaitWindow
import com.mrpowergamerbr.starfait.renderer.opengl.shaders.MinimalShader
import com.mrpowergamerbr.starfait.runner.Instance
import com.mrpowergamerbr.starfait.renderer.Renderer
import com.mrpowergamerbr.starfait.runner.RunnerRoom
import com.mrpowergamerbr.starfait.runner.StarfaitRunner
import com.mrpowergamerbr.starfait.wad.Font
import com.mrpowergamerbr.starfait.wad.GameWAD
import com.mrpowergamerbr.starfait.wad.Sprite
import com.mrpowergamerbr.starfait.wad.TexturePageEntry
import net.perfectdreams.harmony.gl.shaders.ShaderManager
import net.perfectdreams.harmony.gl.shaders.bind
import net.perfectdreams.harmony.gl.textures.Texture
import net.perfectdreams.harmony.gl.textures.TextureManager
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30
import kotlin.text.iterator

class OpenGLRenderer(val gameWAD: GameWAD) : Renderer() {
    companion object {
        private const val MAX_BATCH_ELEMENTS = 512
        private const val FIELDS_PER_ELEMENT = 48
        private const val TOTAL_ATTRIBUTE_SIZE_PER_ELEMENT = 8
    }

    lateinit var runner: StarfaitRunner
    val textureManager = TextureManager()
    val shaderManager = ShaderManager()
    val minimalShader: MinimalShader = shaderManager.loadShader(
        StarfaitWindow::class.java.getResourceAsStream("/shaders/minimal.vsh").readAllBytes().toString(Charsets.UTF_8),
        StarfaitWindow::class.java.getResourceAsStream("/shaders/minimal.fsh").readAllBytes().toString(Charsets.UTF_8),
        ::MinimalShader
    )
    val batch = FloatArray(FIELDS_PER_ELEMENT * MAX_BATCH_ELEMENTS)
    var activeBatchTexture: Texture? = null
    var currentIndex = 0
    val vao = GL30.glGenVertexArrays()
    val vbo = GL15.glGenBuffers()
    val white1x1Texture = textureManager.load1x1WhiteTexture()
    val textures = gameWAD.txtr.textures.map { this.textureManager.loadTexture(it.blob, GL11.GL_NEAREST) }

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

    override fun drawInstance(instance: Instance) {
        // Only attempt to render instances that DO have a sprite index
        if (instance.spriteIndex >= 0) {
            val sprite = runner.gameWAD.sprt.sprites[instance.spriteIndex.toInt()]
            drawSprite(
                sprite,
                instance.imageIndex.toInt(),
                instance.x,
                instance.y,
                instance.imageXScale,
                instance.imageYScale,
                instance.imageAngle,
                GMLColor(0xFFFFFF),
                instance.imageAlpha
            )
        } else {
            // println("Instance ${instance.objectName} has no sprite index!")
        }
    }

    override fun drawSprite(sprite: Sprite, subimg: Int, x: Float, y: Float, xscale: Float, yscale: Float, rot: Float, color: GMLColor, alpha: Float) {
        // Only attempt to render instances that DO have a sprite index
        // See GameMaker-HTML5's yySprite.js Draw, which wraps the subimage: _ind = (~ ~_ind) % this.numb
        val framePointer = sprite.frames[Math.floorMod(subimg, sprite.frames.size)]
        val tpag = runner.gameWAD.tpag.entries[framePointer]!!

        val texture = textures[tpag.textureIndex.toInt()]

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
            textures[tpag.textureIndex.toInt()],
            color,
            alpha
        )
    }

    override fun drawSpritePart(
        sprite: Sprite,
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
        val framePointer = sprite.frames[Math.floorMod(subimg, sprite.frames.size)]
        val tpag = runner.gameWAD.tpag.entries[framePointer]!!

        val texture = textures[tpag.textureIndex.toInt()]
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

    override fun drawBackground(texturePageEntry: TexturePageEntry, x: Float, y: Float, width: Float, height: Float, alpha: Float) {
        val texture = textures[texturePageEntry.textureIndex.toInt()]

        val tpag = texturePageEntry
        drawQuad(
            x + tpag.targetX.toFloat(),
            y + tpag.targetY.toFloat(),
            tpag.sourceX.toFloat() / texture.width,
            tpag.sourceY.toFloat() / texture.height,
            (tpag.sourceX.toFloat() + tpag.sourceWidth.toFloat()) / texture.width,
            (tpag.sourceY.toFloat() + tpag.sourceHeight.toFloat()) / texture.height,
            width,
            height,
            textures[tpag.textureIndex.toInt()],
            GMLColor(0xFFFFFF),
            alpha
        )
    }

    override fun drawTile(texturePageEntry: TexturePageEntry, tile: RunnerRoom.RunnerTile) {
        val texture = textures[texturePageEntry.textureIndex.toInt()]

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
            textures[tpag.textureIndex.toInt()],
            GMLColor(0xFFFFFF),
            1.0f
        )
    }

    override fun drawText(font: Font, x: Float, y: Float, text: String) {
        val tpag = requireNotNull(gameWAD.tpag.entries[font.texturePageEntryPointer])
        val texture = textures[tpag.textureIndex.toInt()]

        var x = x
        var y = y

        flushIfNeeded(texture)

        for (char in text) {
            // TODO: HashMap lookup for the glyphs would be pretty swell
            val glyph = font.glyphs.firstOrNull { it.character == char }

            if (glyph != null) {
                x += glyph.offset

                drawQuad(
                    x,
                    y,
                    (tpag.sourceX.toFloat() + glyph.x.toFloat()) / texture.width,
                    (tpag.sourceY.toFloat() + glyph.y.toFloat()) / texture.height,
                    (tpag.sourceX.toFloat() + glyph.x.toFloat() + glyph.width.toFloat()) / texture.width,
                    (tpag.sourceY.toFloat() + glyph.y.toFloat() + glyph.height.toFloat()) / texture.height,
                    glyph.width.toFloat(),
                    glyph.height.toFloat(),
                    texture,
                    runner.drawState.color,
                    1.0f
                )

                x += glyph.shift
            }
        }
    }

    override fun fillRectangle(x: Float, y: Float, width: Float, height: Float, color: GMLColor, alpha: Float) {
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
            }

            for (debugRectangle in runner.debugRectangles) {
                drawQuad(
                    debugRectangle.left,
                    debugRectangle.top,
                    0.0f,
                    0.0f,
                    1.0f,
                    1.0f,
                    debugRectangle.right - debugRectangle.left,
                    debugRectangle.bottom - debugRectangle.top,
                    white1x1Texture,
                    GMLColor(0x0000ff),
                    0.5f
                )
            }
        }

        runner.debugRectangles.clear()

        flush(runner)
    }

    fun flushIfNeeded(texture: Texture) {
        if (this.activeBatchTexture != texture) {
            flush(runner)
            return
        }

        if (this.currentIndex == batch.size) {
            flush(runner)
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
    ) = drawQuad(x, y, x + width, y, x + width, y + height, x, y + height, u1, v1, u2, v2, texture, blend, alpha)

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
        blend: GMLColor,
        alpha: Float
    ) {
        flushIfNeeded(texture)

        // top-left
        batch[currentIndex++] = x1; batch[currentIndex++] = y1;
        batch[currentIndex++] = u1; batch[currentIndex++] = v1;

        batch[currentIndex++] = blend.redF
        batch[currentIndex++] = blend.greenF
        batch[currentIndex++] = blend.blueF
        batch[currentIndex++] = alpha

        // bottom-right
        batch[currentIndex++] = x3; batch[currentIndex++] = y3;
        batch[currentIndex++] = u2; batch[currentIndex++] = v2;

        batch[currentIndex++] = blend.redF
        batch[currentIndex++] = blend.greenF
        batch[currentIndex++] = blend.blueF
        batch[currentIndex++] = alpha

        // bottom-left
        batch[currentIndex++] = x4; batch[currentIndex++] = y4;
        batch[currentIndex++] = u1; batch[currentIndex++] = v2;

        batch[currentIndex++] = blend.redF
        batch[currentIndex++] = blend.greenF
        batch[currentIndex++] = blend.blueF
        batch[currentIndex++] = alpha

        // top-left
        batch[currentIndex++] = x1; batch[currentIndex++] = y1;
        batch[currentIndex++] = u1; batch[currentIndex++] = v1;

        batch[currentIndex++] = blend.redF
        batch[currentIndex++] = blend.greenF
        batch[currentIndex++] = blend.blueF
        batch[currentIndex++] = alpha

        // top-right
        batch[currentIndex++] = x2; batch[currentIndex++] = y2;
        batch[currentIndex++] = u2; batch[currentIndex++] = v1;

        batch[currentIndex++] = blend.redF
        batch[currentIndex++] = blend.greenF
        batch[currentIndex++] = blend.blueF
        batch[currentIndex++] = alpha

        // bottom-right
        batch[currentIndex++] = x3; batch[currentIndex++] = y3;
        batch[currentIndex++] = u2; batch[currentIndex++] = v2;

        batch[currentIndex++] = blend.redF
        batch[currentIndex++] = blend.greenF
        batch[currentIndex++] = blend.blueF
        batch[currentIndex++] = alpha

        this.activeBatchTexture = texture
    }

    fun flush(runner: StarfaitRunner) {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo)
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, batch)

        // This is wrong, we should run ALL the draw events for EACH view
        val firstVisibleView = runner.currentRoom.views.firstOrNull { it.visible }

        if (firstVisibleView != null)
            minimalShader.uProjection.set(Matrix4f().ortho(firstVisibleView.viewX.toFloat(), firstVisibleView.viewX.toFloat() + firstVisibleView.viewWidth.toFloat(), firstVisibleView.viewY.toFloat() + firstVisibleView.viewHeight.toFloat(), firstVisibleView.viewY.toFloat(), 1.0f, -1.0f).get(FloatArray(16)))
        else
            minimalShader.uProjection.set(Matrix4f().ortho(0.0f, runner.currentRoom.width.toFloat(), runner.currentRoom.height.toFloat(), 0.0f, 1.0f, -1.0f).get(FloatArray(16)))

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