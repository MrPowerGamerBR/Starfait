package com.mrpowergamerbr.starfait.renderer.opengl

import org.lwjgl.stb.STBImage
import org.lwjgl.stb.STBImage.stbi_set_flip_vertically_on_load
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil

class STBImageDecoder : ImageDecoder {
    override suspend fun decodeRGBA(data: ByteArray): DecodedImage {
        stbi_set_flip_vertically_on_load(false)

        val imageBuffer = MemoryUtil.memAlloc(data.size)
        imageBuffer.put(data)
        imageBuffer.flip()

        try {
            MemoryStack.stackPush().use { stack ->
                val w = stack.mallocInt(1)
                val h = stack.mallocInt(1)
                val channels = stack.mallocInt(1)

                // Decode the image
                val decodedImage = STBImage.stbi_load_from_memory(
                    imageBuffer,
                    w,
                    h,
                    channels,
                    4 // Force 4 channels (RGBA)
                ) ?: throw RuntimeException("Failed to load image: " + STBImage.stbi_failure_reason())

                val pixels = ByteArray(decodedImage.remaining())
                // Absolute get, because stbi_image_free frees memAddress(buffer), which is offset by the buffer position
                decodedImage.get(0, pixels)
                STBImage.stbi_image_free(decodedImage)

                return RawPixelsDecodedImage(w.get(), h.get(), pixels)
            }
        } finally {
            MemoryUtil.memFree(imageBuffer)
        }
    }
}
