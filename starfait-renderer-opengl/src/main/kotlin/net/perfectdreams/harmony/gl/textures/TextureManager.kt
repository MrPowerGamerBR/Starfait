package net.perfectdreams.harmony.gl.textures

import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.GL_NEAREST
import org.lwjgl.opengl.GL30
import org.lwjgl.stb.STBImage
import org.lwjgl.stb.STBImage.stbi_set_flip_vertically_on_load
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

class TextureManager {
    fun load1x1WhiteTexture(): Texture {
        // 1. Generate and bind a new texture ID
        val textureId = GL11.glGenTextures()
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId)

        // 2. Tell OpenGL how to unpack the RGBA bytes.
        // Each component is 1 byte size
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1)

        // 3. Set texture parameters
        // Repeat image in both directions
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT)
        // When stretching the image, pixelate
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        // When shrinking the image, pixelate
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL_NEAREST)

        // 4. Upload the texture data
        MemoryStack.stackPush().use { stack ->
            val pixel = stack.malloc(4)
                .put(0, 0xFF.toByte())
                .put(1, 0xFF.toByte())
                .put(2, 0xFF.toByte())
                .put(3, 0xFF.toByte())

            GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                GL11.GL_RGBA8,
                1,
                1,
                0,
                GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE,
                pixel
            )
        }

        return Texture(textureId, 1, 1)
    }

    fun loadTexture(data: ByteArray, textureInterpolationType: Int): Texture {
        stbi_set_flip_vertically_on_load(false)

        val imageBuffer = MemoryUtil.memAlloc(data.size)
        imageBuffer.put(data)
        imageBuffer.flip()

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
            )
            if (decodedImage == null) {
                throw RuntimeException("Failed to load image: " + STBImage.stbi_failure_reason())
            }

            val width = w.get()
            val height = h.get()

            // --- OpenGL Texture Creation ---

            // 1. Generate and bind a new texture ID
            val textureId = GL11.glGenTextures()
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId)

            // 2. Tell OpenGL how to unpack the RGBA bytes.
            // Each component is 1 byte size
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1)

            // 3. Set texture parameters
            // Repeat image in both directions
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT)
            // When stretching the image, pixelate
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, textureInterpolationType)
            // When shrinking the image, pixelate
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, textureInterpolationType)

            // 4. Upload the texture data
            GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                GL11.GL_RGBA,
                width,
                height,
                0,
                GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE,
                decodedImage
            )

            // 5. Generate Mipmaps
            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D)

            // --- Cleanup ---
            STBImage.stbi_image_free(decodedImage) // Free the decoded image memory
            MemoryUtil.memFree(imageBuffer) // Free the raw file buffer
            return Texture(textureId, width, height)
        }
    }
}