package net.perfectdreams.harmony.gl.shaders

import org.lwjgl.opengl.GL11.GL_TRUE
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL20.GL_COMPILE_STATUS
import org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER
import org.lwjgl.opengl.GL20.GL_INFO_LOG_LENGTH
import org.lwjgl.opengl.GL20.GL_LINK_STATUS
import org.lwjgl.opengl.GL20.GL_VERTEX_SHADER
import org.lwjgl.opengl.GL20.glAttachShader
import org.lwjgl.opengl.GL20.glCompileShader
import org.lwjgl.opengl.GL20.glCreateProgram
import org.lwjgl.opengl.GL20.glCreateShader
import org.lwjgl.opengl.GL20.glDeleteShader
import org.lwjgl.opengl.GL20.glDetachShader
import org.lwjgl.opengl.GL20.glGetProgramInfoLog
import org.lwjgl.opengl.GL20.glGetProgrami
import org.lwjgl.opengl.GL20.glLinkProgram
import org.lwjgl.opengl.GL20.glShaderSource

class ShaderManager {
    /**
     * Loads the vertex shader and fragment shader by their file name from the application's resources
     */
    fun <T : GameShader> loadShader(vertexShaderCode: String, fragmentShaderCode: String, createGameShader: (Int) -> (T)): T {
        val programId = loadShader(vertexShaderCode, fragmentShaderCode)
        return createGameShader.invoke(programId)
    }

    /**
     * Loads the vertex shader and fragment shader
     */
    fun loadShader(vertexShaderCode: String, fragmentShaderCode: String): Int {
        val vertexShaderId = glCreateShader(GL_VERTEX_SHADER)
        val fragmentShaderId = glCreateShader(GL_FRAGMENT_SHADER)

        // Compile Vertex Shader
        checkAndCompile(vertexShaderId, vertexShaderCode)

        // Compile Fragment Shader
        checkAndCompile(fragmentShaderId, fragmentShaderCode)

        val programId = glCreateProgram()
        glAttachShader(programId, vertexShaderId)
        glAttachShader(programId, fragmentShaderId)
        glLinkProgram(programId)

        // Check the program
        val result = glGetProgrami(programId, GL_LINK_STATUS)
        val infoLog = glGetProgramInfoLog(programId, GL_INFO_LOG_LENGTH)

        // YES DON'T FORGET THAT WE NEED TO USE GL_TRUE!!
        // I was checking using == 0 and of course that doesn't work because that means FALSE (i think)
        if (result != GL_TRUE) {
            error("Something went wrong while linking shader! Status: $result; Info: $infoLog")
        }

        glDetachShader(programId, vertexShaderId)
        glDetachShader(programId, fragmentShaderId)

        glDeleteShader(vertexShaderId)
        glDeleteShader(fragmentShaderId)

        return programId
    }

    private fun checkAndCompile(shaderId: Int, code: String) {
        // Compile Shader
        glShaderSource(shaderId, code)
        glCompileShader(shaderId)

        // Check Shader
        val result = GL20.glGetShaderi(shaderId, GL_COMPILE_STATUS)
        val infoLog = GL20.glGetShaderInfoLog(shaderId, GL_INFO_LOG_LENGTH)

        // YES DON'T FORGET THAT WE NEED TO USE GL_TRUE!!
        // I was checking using == 0 and of course that doesn't work because that means FALSE (i think)
        if (result != GL_TRUE) {
            error("Something went wrong while compiling shader $shaderId! Status: $result; Info: $infoLog")
        }
    }
}