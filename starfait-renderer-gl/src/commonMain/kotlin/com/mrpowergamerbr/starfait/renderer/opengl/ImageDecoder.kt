package com.mrpowergamerbr.starfait.renderer.opengl

/**
 * Decodes encoded images (PNG, etc) into RGBA pixels
 */
interface ImageDecoder {
    fun decodeRGBA(data: ByteArray): DecodedImage

    class DecodedImage(val width: Int, val height: Int, val pixels: ByteArray)
}
