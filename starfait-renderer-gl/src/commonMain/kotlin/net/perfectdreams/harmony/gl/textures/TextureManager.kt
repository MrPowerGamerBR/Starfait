package net.perfectdreams.harmony.gl.textures

import com.mrpowergamerbr.starfait.renderer.opengl.ImageDecoder
import com.mrpowergamerbr.starfait.renderer.opengl.StarfaitGL
import com.mrpowergamerbr.starfait.renderer.opengl.StarfaitGLConstants
import com.mrpowergamerbr.starfait.renderer.opengl.StarfaitGLConstants.GL_NEAREST

class TextureManager(val gl: StarfaitGL, val imageDecoder: ImageDecoder) {
    fun load1x1WhiteTexture(): Texture {
        // 1. Generate and bind a new texture ID
        val textureId = gl.genTextures()
        gl.bindTexture(StarfaitGLConstants.GL_TEXTURE_2D, textureId)

        // 2. Tell OpenGL how to unpack the RGBA bytes.
        // Each component is 1 byte size
        gl.pixelStorei(StarfaitGLConstants.GL_UNPACK_ALIGNMENT, 1)

        // 3. Set texture parameters
        // Repeat image in both directions
        gl.texParameteri(StarfaitGLConstants.GL_TEXTURE_2D, StarfaitGLConstants.GL_TEXTURE_WRAP_S, StarfaitGLConstants.GL_REPEAT)
        gl.texParameteri(StarfaitGLConstants.GL_TEXTURE_2D, StarfaitGLConstants.GL_TEXTURE_WRAP_T, StarfaitGLConstants.GL_REPEAT)
        // When stretching the image, pixelate
        gl.texParameteri(StarfaitGLConstants.GL_TEXTURE_2D, StarfaitGLConstants.GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        // When shrinking the image, pixelate
        gl.texParameteri(StarfaitGLConstants.GL_TEXTURE_2D, StarfaitGLConstants.GL_TEXTURE_MAG_FILTER, GL_NEAREST)

        // 4. Upload the texture data
        gl.texImage2D(
            StarfaitGLConstants.GL_TEXTURE_2D,
            0,
            StarfaitGLConstants.GL_RGBA8,
            1,
            1,
            0,
            StarfaitGLConstants.GL_RGBA,
            StarfaitGLConstants.GL_UNSIGNED_BYTE,
            byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
        )

        return Texture(textureId, 1, 1)
    }

    fun loadTexture(data: ByteArray, textureInterpolationType: Int): Texture {
        val decodedImage = imageDecoder.decodeRGBA(data)
        val width = decodedImage.width
        val height = decodedImage.height

        // --- OpenGL Texture Creation ---

        // 1. Generate and bind a new texture ID
        val textureId = gl.genTextures()
        gl.bindTexture(StarfaitGLConstants.GL_TEXTURE_2D, textureId)

        // 2. Tell OpenGL how to unpack the RGBA bytes.
        // Each component is 1 byte size
        gl.pixelStorei(StarfaitGLConstants.GL_UNPACK_ALIGNMENT, 1)

        // 3. Set texture parameters
        // Repeat image in both directions
        gl.texParameteri(StarfaitGLConstants.GL_TEXTURE_2D, StarfaitGLConstants.GL_TEXTURE_WRAP_S, StarfaitGLConstants.GL_REPEAT)
        gl.texParameteri(StarfaitGLConstants.GL_TEXTURE_2D, StarfaitGLConstants.GL_TEXTURE_WRAP_T, StarfaitGLConstants.GL_REPEAT)
        // When stretching the image, pixelate
        gl.texParameteri(StarfaitGLConstants.GL_TEXTURE_2D, StarfaitGLConstants.GL_TEXTURE_MIN_FILTER, textureInterpolationType)
        // When shrinking the image, pixelate
        gl.texParameteri(StarfaitGLConstants.GL_TEXTURE_2D, StarfaitGLConstants.GL_TEXTURE_MAG_FILTER, textureInterpolationType)

        // 4. Upload the texture data
        gl.texImage2D(
            StarfaitGLConstants.GL_TEXTURE_2D,
            0,
            StarfaitGLConstants.GL_RGBA,
            width,
            height,
            0,
            StarfaitGLConstants.GL_RGBA,
            StarfaitGLConstants.GL_UNSIGNED_BYTE,
            decodedImage.pixels
        )

        // 5. Generate Mipmaps
        gl.generateMipmap(StarfaitGLConstants.GL_TEXTURE_2D)

        return Texture(textureId, width, height)
    }
}
