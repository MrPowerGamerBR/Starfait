package com.mrpowergamerbr.starfait.desktop.shaders

import net.perfectdreams.harmony.gl.shaders.GameShader

class MinimalShader(programId: Int) : GameShader(programId) {
    val uProjection = uniformMatrix4fv("uProjection")
    val uTexture = uniform1i("uTexture")
}