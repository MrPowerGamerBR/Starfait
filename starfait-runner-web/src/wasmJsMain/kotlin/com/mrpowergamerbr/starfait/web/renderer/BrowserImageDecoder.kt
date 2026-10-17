package com.mrpowergamerbr.starfait.web.renderer

import com.mrpowergamerbr.starfait.renderer.opengl.ImageDecoder
import com.mrpowergamerbr.starfait.web.toArrayBufferFast
import js.array.toJsArray
import js.objects.unsafeJso
import web.blob.Blob
import web.blob.BlobPropertyBag
import web.images.createImageBitmap

class BrowserImageDecoder : ImageDecoder {
    @OptIn(ExperimentalWasmJsInterop::class)
    override suspend fun decodeRGBA(data: ByteArray): BrowserDecodedImage {
        val blob = Blob(
            listOf(data.toArrayBufferFast()).toJsArray(),
            unsafeJso {
                type = "application/octet-stream"
            }
        )

        val image = createImageBitmap(blob)

        return BrowserDecodedImage(image.width, image.height, image)
    }
}