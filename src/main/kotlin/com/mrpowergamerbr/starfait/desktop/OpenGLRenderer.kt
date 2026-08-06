package com.mrpowergamerbr.starfait.desktop

import com.mrpowergamerbr.starfait.desktop.shaders.MinimalShader
import com.mrpowergamerbr.starfait.runner.Renderer
import net.perfectdreams.harmony.gl.shaders.ShaderManager
import net.perfectdreams.harmony.gl.textures.TextureManager

class OpenGLRenderer : Renderer {
    val textureManager = TextureManager()
    val shaderManager = ShaderManager()
    val minimalShader: MinimalShader = shaderManager.loadShader(
        StarfaitWindow::class.java.getResourceAsStream("/shaders/minimal.vsh").readAllBytes().toString(Charsets.UTF_8),
        StarfaitWindow::class.java.getResourceAsStream("/shaders/minimal.fsh").readAllBytes().toString(Charsets.UTF_8),
        ::MinimalShader
    )
}