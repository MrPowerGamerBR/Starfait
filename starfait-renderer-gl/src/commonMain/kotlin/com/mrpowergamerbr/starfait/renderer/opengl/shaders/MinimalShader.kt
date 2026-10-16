package com.mrpowergamerbr.starfait.renderer.opengl.shaders

import com.mrpowergamerbr.starfait.renderer.opengl.StarfaitGL
import net.perfectdreams.harmony.gl.shaders.GameShader

class MinimalShader(gl: StarfaitGL, programId: Int) : GameShader(gl, programId) {
    val uProjection = uniformMatrix4fv("uProjection")
    val uTexture = uniform1Sampler2D("uTexture")
}