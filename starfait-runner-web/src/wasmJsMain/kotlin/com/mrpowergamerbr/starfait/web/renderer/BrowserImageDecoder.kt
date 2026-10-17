package com.mrpowergamerbr.starfait.web.renderer

import com.mrpowergamerbr.starfait.renderer.opengl.ImageDecoder
import com.mrpowergamerbr.starfait.web.toBlobFast
import web.images.createImageBitmap

class BrowserImageDecoder : ImageDecoder {
    override suspend fun decodeRGBA(data: ByteArray): BrowserDecodedImage {
        val blob = data.toBlobFast("application/octet-stream")

        val image = createImageBitmap(blob)

        return BrowserDecodedImage(image.width, image.height, image)
    }
}