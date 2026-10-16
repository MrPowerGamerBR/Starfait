package net.perfectdreams.harmony.gl.shaders

import com.mrpowergamerbr.starfait.renderer.opengl.StarfaitGL
import com.mrpowergamerbr.starfait.renderer.opengl.StarfaitGLConstants

class ShaderManager(val gl: StarfaitGL) {
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
        val vertexShaderId = gl.createShader(StarfaitGLConstants.GL_VERTEX_SHADER)
        val fragmentShaderId = gl.createShader(StarfaitGLConstants.GL_FRAGMENT_SHADER)

        // Compile Vertex Shader
        checkAndCompile(vertexShaderId, vertexShaderCode)

        // Compile Fragment Shader
        checkAndCompile(fragmentShaderId, fragmentShaderCode)

        val programId = gl.createProgram()
        gl.attachShader(programId, vertexShaderId)
        gl.attachShader(programId, fragmentShaderId)
        gl.linkProgram(programId)

        // Check the program
        val result = gl.getProgrami(programId, StarfaitGLConstants.GL_LINK_STATUS)
        val infoLog = gl.getProgramInfoLog(programId, StarfaitGLConstants.GL_INFO_LOG_LENGTH)

        // YES DON'T FORGET THAT WE NEED TO USE StarfaitGLConstants.GL_TRUE!!
        // I was checking using == 0 and of course that doesn't work because that means FALSE (i think)
        if (result != StarfaitGLConstants.GL_TRUE) {
            error("Something went wrong while linking shader! Status: $result; Info: $infoLog")
        }

        gl.detachShader(programId, vertexShaderId)
        gl.detachShader(programId, fragmentShaderId)

        gl.deleteShader(vertexShaderId)
        gl.deleteShader(fragmentShaderId)

        return programId
    }

    private fun checkAndCompile(shaderId: Int, code: String) {
        // Compile Shader
        gl.shaderSource(shaderId, code)
        gl.compileShader(shaderId)

        // Check Shader
        val result = gl.getShaderi(shaderId, StarfaitGLConstants.GL_COMPILE_STATUS)
        val infoLog = gl.getShaderInfoLog(shaderId, StarfaitGLConstants.GL_INFO_LOG_LENGTH)

        // YES DON'T FORGET THAT WE NEED TO USE StarfaitGLConstants.GL_TRUE!!
        // I was checking using == 0 and of course that doesn't work because that means FALSE (i think)
        if (result != StarfaitGLConstants.GL_TRUE) {
            error("Something went wrong while compiling shader $shaderId! Status: $result; Info: $infoLog")
        }
    }
}