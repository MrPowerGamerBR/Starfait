package net.perfectdreams.harmony.gl.textures

import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30
import org.lwjgl.stb.STBImage
import org.lwjgl.stb.STBImage.stbi_set_flip_vertically_on_load
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.use

class TextureManager {
    fun loadTexture(
        filePath: String,
        textureInterpolationType: Int
    ): Texture {
        stbi_set_flip_vertically_on_load(true)

        var imageBuffer: ByteBuffer?
        // Load image file into a ByteBuffer
        val path = Paths.get(filePath)
        if (Files.exists(path)) {
            Files.newByteChannel(path).use { fc ->
                imageBuffer = MemoryUtil.memAlloc(fc.size().toInt() + 1)
                while (fc.read(imageBuffer) != -1) {
                    // Loop until end of file
                }
            }
        } else {
            // Fallback for resources inside a JAR
            Texture::class.java.getResourceAsStream("/" + filePath).use { source ->
                Channels.newChannel(source).use { rbc ->
                    imageBuffer = MemoryUtil.memAlloc(8192) // 8KB initial buffer
                    while (true) {
                        val bytes = rbc.read(imageBuffer)
                        if (bytes == -1) {
                            break
                        }
                        if (imageBuffer!!.remaining() == 0) {
                            imageBuffer = MemoryUtil.memRealloc(
                                imageBuffer,
                                imageBuffer!!.capacity() * 2
                            )
                        }
                    }
                }
            }
        }
        imageBuffer!!.flip()

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
                throw kotlin.RuntimeException(
                    "Failed to load image: " + STBImage.stbi_failure_reason()
                )
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