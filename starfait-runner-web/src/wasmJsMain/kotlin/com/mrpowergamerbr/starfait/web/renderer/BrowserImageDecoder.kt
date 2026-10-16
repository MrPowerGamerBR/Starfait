package com.mrpowergamerbr.starfait.web.renderer

import com.mrpowergamerbr.starfait.renderer.opengl.ImageDecoder
import js.array.toJsArray
import js.objects.unsafeJso
import js.typedarrays.toUint8Array
import web.blob.Blob
import web.blob.BlobPart
import web.blob.BlobPropertyBag
import web.images.ImageBitmapOptions
import web.images.ImageOrientation
import web.images.createImageBitmap
import web.images.flipY

class BrowserImageDecoder : ImageDecoder {
    override suspend fun decodeRGBA(data: ByteArray): BrowserDecodedImage {
        val blob = Blob(
            listOf<BlobPart>(data.toUint8Array()).toJsArray(),
            unsafeJso<BlobPropertyBag> {
                type = "application/octet-stream"
            }
        )

        val image = createImageBitmap(
            blob,
            unsafeJso<ImageBitmapOptions> {
                // this.imageOrientation = ImageOrientation.flipY
            }
        )

        return BrowserDecodedImage(image.width, image.height, image)
    }
}