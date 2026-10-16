package com.mrpowergamerbr.starfait.renderer.opengl

import org.lwjgl.opengl.GL11.glBindTexture
import org.lwjgl.opengl.GL11.glBlendFunc
import org.lwjgl.opengl.GL11.glClear
import org.lwjgl.opengl.GL11.glClearColor
import org.lwjgl.opengl.GL11.glDeleteTextures
import org.lwjgl.opengl.GL11.glDisable
import org.lwjgl.opengl.GL11.glDrawArrays
import org.lwjgl.opengl.GL11.glEnable
import org.lwjgl.opengl.GL11.glGenTextures
import org.lwjgl.opengl.GL11.glPixelStorei
import org.lwjgl.opengl.GL11.glReadPixels
import org.lwjgl.opengl.GL11.glScissor
import org.lwjgl.opengl.GL11.glTexImage2D
import org.lwjgl.opengl.GL11.glTexParameteri
import org.lwjgl.opengl.GL11.glViewport
import org.lwjgl.opengl.GL13.glActiveTexture
import org.lwjgl.opengl.GL15.glBindBuffer
import org.lwjgl.opengl.GL15.glBufferData
import org.lwjgl.opengl.GL15.glBufferSubData
import org.lwjgl.opengl.GL15.glGenBuffers
import org.lwjgl.opengl.GL20.glAttachShader
import org.lwjgl.opengl.GL20.glCompileShader
import org.lwjgl.opengl.GL20.glCreateProgram
import org.lwjgl.opengl.GL20.glCreateShader
import org.lwjgl.opengl.GL20.glDeleteShader
import org.lwjgl.opengl.GL20.glDetachShader
import org.lwjgl.opengl.GL20.glEnableVertexAttribArray
import org.lwjgl.opengl.GL20.glGetProgramInfoLog
import org.lwjgl.opengl.GL20.glGetProgrami
import org.lwjgl.opengl.GL20.glGetShaderInfoLog
import org.lwjgl.opengl.GL20.glGetShaderi
import org.lwjgl.opengl.GL20.glGetUniformLocation
import org.lwjgl.opengl.GL20.glLinkProgram
import org.lwjgl.opengl.GL20.glShaderSource
import org.lwjgl.opengl.GL20.glUseProgram
import org.lwjgl.opengl.GL20.glVertexAttribPointer
import org.lwjgl.opengl.GL30.glBindFramebuffer
import org.lwjgl.opengl.GL30.glBindVertexArray
import org.lwjgl.opengl.GL30.glGenVertexArrays
import org.lwjgl.opengl.GL30.glGenerateMipmap
import org.lwjgl.opengl.GL41.glProgramUniform1f
import org.lwjgl.opengl.GL41.glProgramUniform1i
import org.lwjgl.opengl.GL41.glProgramUniform2f
import org.lwjgl.opengl.GL41.glProgramUniform3f
import org.lwjgl.opengl.GL41.glProgramUniform4f
import org.lwjgl.opengl.GL41.glProgramUniformMatrix4fv
import org.lwjgl.system.MemoryStack
import java.nio.ByteBuffer

class StarfaitOpenGL : StarfaitGL() {
    override fun enable(target: Int) = glEnable(target)
    override fun disable(target: Int) = glDisable(target)
    override fun clearColor(red: Float, green: Float, blue: Float, alpha: Float) = glClearColor(red, green, blue, alpha)
    override fun clear(mask: Int) = glClear(mask)
    override fun viewport(x: Int, y: Int, width: Int, height: Int) = glViewport(x, y, width, height)
    override fun scissor(x: Int, y: Int, width: Int, height: Int) = glScissor(x, y, width, height)
    override fun bindBuffer(target: Int, buffer: Int) = glBindBuffer(target, buffer)
    override fun bufferData(target: Int, size: Long, usage: Int) = glBufferData(target, size, usage)
    override fun bufferSubData(target: Int, offset: Long, buffer: FloatArray) = glBufferSubData(target, offset, buffer)
    override fun bindVertexArray(array: Int) = glBindVertexArray(array)
    override fun drawArrays(mode: Int, first: Int, count: Int) = glDrawArrays(mode, first, count)
    override fun genTextures() = glGenTextures()
    override fun bindTexture(target: Int, texture: Int) = glBindTexture(target, texture)
    override fun activeTexture(texture: Int) = glActiveTexture(texture)
    override fun deleteTextures(texturesToBeDeleted: List<Int>) {
        if (texturesToBeDeleted.isNotEmpty()) {
            MemoryStack.stackPush().use {
                val textures = it.ints(*texturesToBeDeleted.toIntArray())
                glDeleteTextures(textures)
            }
        }
    }
    override fun genVertexArrays() = glGenVertexArrays()
    override fun genBuffers() = glGenBuffers()
    override fun blendFunc(src: Int, dst: Int) = glBlendFunc(src, dst)
    override fun vertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, offset: Long) = glVertexAttribPointer(index, size, type, normalized, stride, offset)
    override fun enableVertexAttribArray(index: Int) = glEnableVertexAttribArray(index)
    override fun readPixels(x: Int, y: Int, width: Int, height: Int, format: Int, type: Int): ByteArray {
        val byteBuffer = ByteBuffer.allocateDirect(width * height * 4)
        glReadPixels(x, y, width, height, format, type, byteBuffer)

        // We can't use ".array()" directly because it is a ooo allocateDirect
        val byteArray = ByteArray(width * height * 4)
        byteBuffer.get(byteArray)

        return byteArray
    }

    override fun pixelStorei(pname: Int, param: Int) = glPixelStorei(pname, param)
    override fun texImage2D(
        target: Int,
        level: Int,
        internalFormat: Int,
        width: Int,
        height: Int,
        border: Int,
        format: Int,
        type: Int,
        pixels: ByteArray
    ) {
        val byteBuffer = ByteBuffer.allocateDirect(pixels.size)
        byteBuffer.put(pixels)
        byteBuffer.position(0)
        glTexImage2D(target, level, internalFormat, width, height, border, format, type, byteBuffer)
    }

    override fun texParameteri(target: Int, pname: Int, param: Int) = glTexParameteri(target, pname, param)
    override fun generateMipmap(target: Int) = glGenerateMipmap(target)
    override fun bindFramebuffer(target: Int, framebuffer: Int) = glBindFramebuffer(target, framebuffer)

    override fun createShader(type: Int) = glCreateShader(type)
    override fun shaderSource(shader: Int, source: String) = glShaderSource(shader, source)
    override fun compileShader(shader: Int) = glCompileShader(shader)
    override fun getShaderi(shader: Int, pname: Int) = glGetShaderi(shader, pname)
    override fun getShaderInfoLog(shader: Int, maxLength: Int): String = glGetShaderInfoLog(shader, maxLength)
    override fun deleteShader(shader: Int) = glDeleteShader(shader)
    override fun createProgram() = glCreateProgram()
    override fun attachShader(program: Int, shader: Int) = glAttachShader(program, shader)
    override fun detachShader(program: Int, shader: Int) = glDetachShader(program, shader)
    override fun linkProgram(program: Int) = glLinkProgram(program)
    override fun getProgrami(program: Int, pname: Int) = glGetProgrami(program, pname)
    override fun getProgramInfoLog(program: Int, maxLength: Int): String = glGetProgramInfoLog(program, maxLength)
    override fun useProgram(program: Int) = glUseProgram(program)

    override fun getUniformLocation(program: Int, name: String) = glGetUniformLocation(program, name)
    override fun programUniform1i(program: Int, location: Int, x: Int) = glProgramUniform1i(program, location, x)
    override fun programUniform1f(program: Int, location: Int, x: Float) = glProgramUniform1f(program, location, x)
    override fun programUniform2f(program: Int, location: Int, x: Float, y: Float) = glProgramUniform2f(program, location, x, y)
    override fun programUniform3f(program: Int, location: Int, x: Float, y: Float, z: Float) = glProgramUniform3f(program, location, x, y, z)
    override fun programUniform4f(program: Int, location: Int, x: Float, y: Float, z: Float, w: Float) = glProgramUniform4f(program, location, x, y, z, w)
    override fun programUniformMatrix4fv(program: Int, location: Int, transpose: Boolean, value: FloatArray) = glProgramUniformMatrix4fv(program, location, transpose, value)
}