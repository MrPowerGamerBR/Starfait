package com.mrpowergamerbr.starfait.desktop

import com.mrpowergamerbr.starfait.GMLColor
import com.mrpowergamerbr.starfait.desktop.shaders.MinimalShader
import com.mrpowergamerbr.starfait.runner.Instance
import com.mrpowergamerbr.starfait.runner.Renderer
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
import org.lwjgl.opengl.GL11.GL_BLEND
import org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT
import org.lwjgl.opengl.GL11.GL_DEPTH_TEST
import org.lwjgl.opengl.GL11.GL_FLOAT
import org.lwjgl.opengl.GL11.GL_NEAREST
import org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA
import org.lwjgl.opengl.GL11.GL_SRC_ALPHA
import org.lwjgl.opengl.GL11.GL_TRIANGLES
import org.lwjgl.opengl.GL11.glBlendFunc
import org.lwjgl.opengl.GL11.glClear
import org.lwjgl.opengl.GL11.glClearColor
import org.lwjgl.opengl.GL11.glDisable
import org.lwjgl.opengl.GL11.glDrawArrays
import org.lwjgl.opengl.GL11.glEnable
import org.lwjgl.opengl.GL13.GL_TEXTURE0
import org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER
import org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW
import org.lwjgl.opengl.GL15.glBindBuffer
import org.lwjgl.opengl.GL15.glBufferData
import org.lwjgl.opengl.GL15.glBufferSubData
import org.lwjgl.opengl.GL15.glGenBuffers
import org.lwjgl.opengl.GL20.glEnableVertexAttribArray
import org.lwjgl.opengl.GL20.glVertexAttribPointer
import org.lwjgl.opengl.GL30.glBindVertexArray
import org.lwjgl.opengl.GL30.glGenVertexArrays

class OpenGLRenderer(val gameWAD: GameWAD) : Renderer {
    companion object {
        private const val MAX_BATCH_ELEMENTS = 512
        private const val FIELDS_PER_ELEMENT = 42
        private const val TOTAL_ATTRIBUTE_SIZE_PER_ELEMENT = 7
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
    val vao = glGenVertexArrays()
    val vbo = glGenBuffers()
    val textures = gameWAD.txtr.textures.map { this.textureManager.loadTexture(it.blob, GL_NEAREST) }

    init {
        // For transparency in sprites
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        glBindVertexArray(vao)
        glBindBuffer(GL_ARRAY_BUFFER, vbo)

        glBufferData(GL_ARRAY_BUFFER, Float.SIZE_BYTES * FIELDS_PER_ELEMENT.toLong() * MAX_BATCH_ELEMENTS, GL_DYNAMIC_DRAW)

        // aPosition
        // stride is from the START of the current "section"!
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 7 * Float.SIZE_BYTES, 0L)
        glEnableVertexAttribArray(0)

        // aTexCoord
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 7 * Float.SIZE_BYTES, 2L * Float.SIZE_BYTES)
        glEnableVertexAttribArray(1)

        // aBlendColor
        glVertexAttribPointer(2, 3, GL_FLOAT, false, 7 * Float.SIZE_BYTES, 4L * Float.SIZE_BYTES)
        glEnableVertexAttribArray(2)
    }

    override fun drawInstance(instance: Instance) {
        // Only attempt to render instances that DO have a sprite index
        if (instance.spriteIndex >= 0) {
            val sprite = runner.gameWAD.sprt.sprites[instance.spriteIndex.toInt()]
            drawSprite(
                sprite,
                instance.imageIndex.toInt(),
                instance.x,
                instance.y
            )
        } else {
            // println("Instance ${instance.objectName} has no sprite index!")
        }
    }

    override fun drawSprite(sprite: Sprite, subimg: Int, x: Float, y: Float) {
        // Only attempt to render instances that DO have a sprite index
        val framePointer = sprite.frames[subimg]
        val tpag = runner.gameWAD.tpag.entries[framePointer]!!

        val texture = textures[tpag.textureIndex.toInt()]
        drawQuad(
            x + tpag.targetX.toFloat(),
            y + tpag.targetY.toFloat(),
            tpag.sourceX.toFloat() / texture.width,
            tpag.sourceY.toFloat() / texture.height,
            (tpag.sourceX.toFloat() + tpag.sourceWidth.toFloat()) / texture.width,
            (tpag.sourceY.toFloat() + tpag.sourceHeight.toFloat()) / texture.height,
            sprite.width.toFloat() - tpag.targetX.toFloat(),
            sprite.height.toFloat() - tpag.targetY.toFloat(),
            textures[tpag.textureIndex.toInt()],
            GMLColor(0xFFFFFF)
        )
    }

    override fun drawBackground(texturePageEntry: TexturePageEntry, width: Float, height: Float) {
        val texture = textures[texturePageEntry.textureIndex.toInt()]

        val tpag = texturePageEntry
        drawQuad(
            tpag.targetX.toFloat(),
            tpag.targetY.toFloat(),
            tpag.sourceX.toFloat() / texture.width,
            tpag.sourceY.toFloat() / texture.height,
            (tpag.sourceX.toFloat() + tpag.sourceWidth.toFloat()) / texture.width,
            (tpag.sourceY.toFloat() + tpag.sourceHeight.toFloat()) / texture.height,
            width - tpag.targetX.toFloat(),
            height - tpag.targetY.toFloat(),
            textures[tpag.textureIndex.toInt()],
            GMLColor(0xFFFFFF)
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
            GMLColor(0xFFFFFF)
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
                    runner.drawSetColor
                )

                x += glyph.shift
            }
        }
    }

    override fun frame(block: () -> Unit) {
        // TODO: Respect the room's drawBackgroundColor
        val color = runner.currentRoom.backgroundColor
        glClearColor(color.redF, color.greenF, color.blueF, 1.0f)
        glClear(GL_COLOR_BUFFER_BIT)

        block()
        flush(runner)
    }

    override fun setDrawColor(color: GMLColor) {
        // TODO: Remove this
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
        blend: GMLColor
    ) {
        flushIfNeeded(texture)

        // top-left
        batch[currentIndex++] = x; batch[currentIndex++] = y;
        batch[currentIndex++] = u1; batch[currentIndex++] = v1;

        batch[currentIndex++] = blend.redF
        batch[currentIndex++] = blend.greenF
        batch[currentIndex++] = blend.blueF

        // bottom-right
        batch[currentIndex++] = x + width; batch[currentIndex++] = y + height;
        batch[currentIndex++] = u2; batch[currentIndex++] = v2;

        batch[currentIndex++] = blend.redF
        batch[currentIndex++] = blend.greenF
        batch[currentIndex++] = blend.blueF

        // bottom-left
        batch[currentIndex++] = x; batch[currentIndex++] = y + height;
        batch[currentIndex++] = u1; batch[currentIndex++] = v2;

        batch[currentIndex++] = blend.redF
        batch[currentIndex++] = blend.greenF
        batch[currentIndex++] = blend.blueF

        // top-left
        batch[currentIndex++] = x; batch[currentIndex++] = y;
        batch[currentIndex++] = u1; batch[currentIndex++] = v1;

        batch[currentIndex++] = blend.redF
        batch[currentIndex++] = blend.greenF
        batch[currentIndex++] = blend.blueF

        // top-right
        batch[currentIndex++] = x + width; batch[currentIndex++] = y;
        batch[currentIndex++] = u2; batch[currentIndex++] = v1;

        batch[currentIndex++] = blend.redF
        batch[currentIndex++] = blend.greenF
        batch[currentIndex++] = blend.blueF

        // bottom-right
        batch[currentIndex++] = x + width; batch[currentIndex++] = y + height;
        batch[currentIndex++] = u2; batch[currentIndex++] = v2;

        batch[currentIndex++] = blend.redF
        batch[currentIndex++] = blend.greenF
        batch[currentIndex++] = blend.blueF

        this.activeBatchTexture = texture
    }

    fun flush(runner: StarfaitRunner) {
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferSubData(GL_ARRAY_BUFFER, 0, batch)

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
            glBindVertexArray(vao)
            glDrawArrays(GL_TRIANGLES, 0, currentIndex / TOTAL_ATTRIBUTE_SIZE_PER_ELEMENT)
        }

        this.currentIndex = 0
        // We don't need to clear the current batch even though it has TRASH in it
    }
}