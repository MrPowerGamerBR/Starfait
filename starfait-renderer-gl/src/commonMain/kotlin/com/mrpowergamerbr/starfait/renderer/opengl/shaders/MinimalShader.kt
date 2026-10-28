package com.mrpowergamerbr.starfait.renderer.opengl.shaders

import net.perfectdreams.harmony.gl.GLContext
import net.perfectdreams.harmony.gl.shaders.ShaderProgram
import net.perfectdreams.harmony.graphics.batcher.SpriteShader

class MinimalShader(gl: GLContext, programId: Int) : SpriteShader(gl, programId) {
    companion object {
        val VERTEX_SHADER_CODE = """
            // For batching, we store the position of each quad directly on the position, so we don't actually *need* to have a uModel
            layout(location = 0) in vec2 aPosition;
            layout(location = 1) in vec2 aTexCoord;
            layout(location = 2) in vec4 aBlendColor;
            
            uniform vec4 uColor;
            uniform mat4 uProjection;
            
            out vec2 vTexCoord;
            out vec4 vBlendColor;
            
            void main() {
                vTexCoord = aTexCoord;
                vBlendColor = aBlendColor;
                gl_Position =  uProjection * vec4(aPosition, 0.0, 1.0);
            }
        """.trimIndent()

        val FRAGMENT_SHADER_CODE = """
            in vec2 vTexCoord;
            in vec4 vBlendColor;
            
            uniform sampler2D uTexture;
            
            out vec4 fragColor;
            
            void main() {
                fragColor = texture(uTexture, vTexCoord) * vBlendColor;
            }
        """
    }
    val uProjection = uniformMatrix4fv("uProjection")
    override val uTexture = uniform1Sampler2D("uTexture")
}