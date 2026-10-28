package com.mrpowergamerbr.starfait.web.renderer

import com.mrpowergamerbr.starfait.renderer.opengl.ImageDecoder
import js.array.toJsArray
import js.objects.unsafeJso
import net.perfectdreams.harmony.gl.webgl2.BrowserImageBitmapTextureSource
import net.perfectdreams.harmony.wasm.toArrayBufferFast
import web.blob.Blob
import web.images.createImageBitmap

class BrowserImageDecoder : ImageDecoder {
    @OptIn(ExperimentalWasmJsInterop::class)
    override suspend fun decode(data: ByteArray): BrowserImageBitmapTextureSource {
        val blob = Blob(
            listOf(data.toArrayBufferFast()).toJsArray(),
            unsafeJso {
                type = "application/octet-stream"
            }
        )

        val image = createImageBitmap(blob)

        return BrowserImageBitmapTextureSource(image)
    }
}