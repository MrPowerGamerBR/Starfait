package com.mrpowergamerbr.starfait.desktop

import com.mrpowergamerbr.starfait.desktop.shaders.MinimalShader
import com.mrpowergamerbr.starfait.runner.Renderer
import com.mrpowergamerbr.starfait.runner.StarfaitRunner
import net.perfectdreams.harmony.gl.shaders.ShaderManager
import net.perfectdreams.harmony.gl.shaders.bind
import net.perfectdreams.harmony.gl.textures.Texture
import net.perfectdreams.harmony.gl.textures.TextureManager
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11.GL_FLOAT
import org.lwjgl.opengl.GL11.GL_TRIANGLES
import org.lwjgl.opengl.GL11.glDrawArrays
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

class OpenGLRenderer : Renderer {
    lateinit var runner: StarfaitRunner
    val textureManager = TextureManager()
    val shaderManager = ShaderManager()
    val minimalShader: MinimalShader = shaderManager.loadShader(
        StarfaitWindow::class.java.getResourceAsStream("/shaders/minimal.vsh").readAllBytes().toString(Charsets.UTF_8),
        StarfaitWindow::class.java.getResourceAsStream("/shaders/minimal.fsh").readAllBytes().toString(Charsets.UTF_8),
        ::MinimalShader
    )
    val batch = FloatArray(24 * 128)
    var activeBatchTexture: Texture? = null
    var currentIndex = 0
    val vao = glGenVertexArrays()
    val vbo = glGenBuffers()

    init {
        glBindVertexArray(vao)
        glBindBuffer(GL_ARRAY_BUFFER, vbo)

        glBufferData(GL_ARRAY_BUFFER, Float.SIZE_BYTES * 24L * 128L, GL_DYNAMIC_DRAW)

        // aPosition
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.SIZE_BYTES, 0L)
        glEnableVertexAttribArray(0)

        // aTexCoord
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.SIZE_BYTES, 2L * Float.SIZE_BYTES)
        glEnableVertexAttribArray(1)
    }

    fun drawQuad(x: Float, y: Float, u1: Float, v1: Float, u2: Float, v2: Float, width: Float, height: Float, texture: Texture) {
        if (this.activeBatchTexture != texture)
            flush(runner)

        // top-left
        batch[currentIndex++] = x; batch[currentIndex++] = y;
        batch[currentIndex++] = u1; batch[currentIndex++] = v1;

        // bottom-right
        batch[currentIndex++] = x + width; batch[currentIndex++] = y + height;
        batch[currentIndex++] = u2; batch[currentIndex++] = v2;

        // bottom-left
        batch[currentIndex++] = x; batch[currentIndex++] = y + height;
        batch[currentIndex++] = u1; batch[currentIndex++] = v2;

        // top-left
        batch[currentIndex++] = x; batch[currentIndex++] = y;
        batch[currentIndex++] = u1; batch[currentIndex++] = v1;

        // top-right
        batch[currentIndex++] = x + width; batch[currentIndex++] = y;
        batch[currentIndex++] = u2; batch[currentIndex++] = v1;

        // bottom-right
        batch[currentIndex++] = x + width; batch[currentIndex++] = y + height;
        batch[currentIndex++] = u2; batch[currentIndex++] = v2;

        this.activeBatchTexture = texture
    }

    fun flush(runner: StarfaitRunner) {
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferSubData(GL_ARRAY_BUFFER, 0, batch)

        // This is wrong
        val firstVisibleView = runner.currentRoom.views.firstOrNull { it.visible }

        if (firstVisibleView != null)
            minimalShader.uProjection.set(Matrix4f().ortho(firstVisibleView.viewX.toFloat(), firstVisibleView.viewWidth.toFloat(), firstVisibleView.viewHeight.toFloat(), firstVisibleView.viewY.toFloat(), 1.0f, -1.0f).get(FloatArray(16)))
        else
            minimalShader.uProjection.set(Matrix4f().ortho(0.0f, runner.currentRoom.width.toFloat(), runner.currentRoom.height.toFloat(), 0.0f, 1.0f, -1.0f).get(FloatArray(16)))

        val activeBatchTexture = this.activeBatchTexture
        if (activeBatchTexture != null)
            minimalShader.uTexture.set(0, activeBatchTexture)

        minimalShader.bind {
            glBindVertexArray(vao)
            glDrawArrays(GL_TRIANGLES, 0, currentIndex / 4)
        }

        this.currentIndex = 0
        // We don't need to clear the current batch even though it has TRASH in it
    }
}