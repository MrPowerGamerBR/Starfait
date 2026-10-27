package com.mrpowergamerbr.starfait.renderer.opengl

import net.perfectdreams.harmony.gl.TextureSource

/**
 * Decodes the encoded images (PNG, etc) into a [net.perfectdreams.harmony.gl.TextureSource]
 */
interface ImageDecoder {
    suspend fun decode(data: ByteArray): TextureSource
}
