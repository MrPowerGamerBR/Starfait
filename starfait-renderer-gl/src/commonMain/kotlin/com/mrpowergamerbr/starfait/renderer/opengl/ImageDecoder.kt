package com.mrpowergamerbr.starfait.renderer.opengl

/**
 * Decodes encoded images (PNG, etc) into RGBA pixels
 */
interface ImageDecoder {
    suspend fun decodeRGBA(data: ByteArray): DecodedImage
}
