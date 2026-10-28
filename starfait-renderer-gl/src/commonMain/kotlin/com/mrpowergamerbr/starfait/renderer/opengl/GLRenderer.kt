package com.mrpowergamerbr.starfait.renderer.opengl

import com.mrpowergamerbr.starfait.GMLColor
import com.mrpowergamerbr.starfait.MathUtils
import com.mrpowergamerbr.starfait.Matrix3x2f
import com.mrpowergamerbr.starfait.Matrix4f
import com.mrpowergamerbr.starfait.Vector2f
import com.mrpowergamerbr.starfait.Vector3f
import com.mrpowergamerbr.starfait.runner.text.TextUtils
import com.mrpowergamerbr.starfait.renderer.Renderer
import com.mrpowergamerbr.starfait.renderer.TextureHandle
import com.mrpowergamerbr.starfait.renderer.opengl.shaders.MinimalShader
import com.mrpowergamerbr.starfait.runner.RunnerRoom
import com.mrpowergamerbr.starfait.runner.StarfaitRunner
import com.mrpowergamerbr.starfait.runner.resources.RunnerFont
import com.mrpowergamerbr.starfait.runner.resources.RunnerSprite
import com.mrpowergamerbr.starfait.runner.resources.RunnerTexturePageEntry
import com.mrpowergamerbr.starfait.runner.text.TextHorizontalAlign
import net.perfectdreams.harmony.gl.ByteArrayTextureSource
import net.perfectdreams.harmony.gl.GLConstants
import net.perfectdreams.harmony.gl.GLContext
import net.perfectdreams.harmony.gl.GPUResources
import net.perfectdreams.harmony.gl.textures.Texture
import net.perfectdreams.harmony.gl.vertex.buildVertexAttributes
import net.perfectdreams.harmony.graphics.batcher.SpriteBatcher
import net.perfectdreams.harmony.graphics.batcher.withSpritePipeline
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class GLRenderer(
    val gl: GLContext,
    vertexShaderSource: String,
    fragmentShaderSource: String,
    val imageDecoder: ImageDecoder
) : Renderer() {
    companion object {
        // TODO: This shouldn't be hardcoded!
        private const val VIEWPORT_WIDTH = 640
        private const val VIEWPORT_HEIGHT = 480

        private val vertexAttributes = buildVertexAttributes {
            vec2(0) // aPosition
            vec2(1) // aTexCoord
            vec4(2) // aBlendColor
        }
    }

    lateinit var runner: StarfaitRunner

    val gpu = GPUResources(gl, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
    val minimalShader: MinimalShader = gpu.createShader(
        vertexShaderSource,
        fragmentShaderSource,
        ::MinimalShader
    )
    val batcher = SpriteBatcher(gl)
    val batcherScope = batcher.createDrawingScope(minimalShader)
    val textures = mutableMapOf<TextureHandle, Texture>()
    var currentTextureId = 0

    init {
        // For transparency in sprites
        gl.enable(GLConstants.GL_BLEND)
        gl.blendFunc(GLConstants.GL_SRC_ALPHA, GLConstants.GL_ONE_MINUS_SRC_ALPHA)

        vertexAttributes.apply(gl)
    }

    private fun getTextureFromStarfaitHandle(handle: TextureHandle): Texture {
        return this.textures[handle]!!
    }

    override suspend fun uploadTexture(texture: com.mrpowergamerbr.starfait.wad.Texture): TextureHandle {
        val uploadedTexture = this.gpu.createTexture(imageDecoder.decode(texture.blob), GLConstants.GL_CLAMP_TO_BORDER, GLConstants.GL_CLAMP_TO_BORDER, GLConstants.GL_NEAREST, GLConstants.GL_NEAREST)
        return allocateAndStoreTextureHandle(uploadedTexture)
    }

    private fun allocateAndStoreTextureHandle(texture: Texture): TextureHandle {
        val textureHandle = TextureHandle(this.currentTextureId++)
        this.textures[textureHandle] = texture
        return textureHandle
    }

    override fun drawSprite(sprite: RunnerSprite, tpag: RunnerTexturePageEntry, x: Float, y: Float, xscale: Float, yscale: Float, rot: Float, color: GMLColor, alpha: Float) {
        val texture = this.textures[tpag.textureIndex]!!

        val rad = MathUtils.toRadians(rot.toDouble())

        val matrix4f = Matrix4f()
            .translate(x, y, 0f)
            .rotateZ(-rad.toFloat())

        // Think of this like if it was a sprite, and the sprite may had included transparent borders, so the values below are in relation to that "canvas"
        // targetX, targetY = where the first non-opaque pixel is

        // Those are relative to "local"
        val left = ((tpag.targetX.toFloat() - sprite.originX) * xscale)
        val right = (((tpag.targetX.toFloat() - sprite.originX + tpag.targetWidth.toInt()) * xscale))
        val top = ((tpag.targetY.toFloat() - sprite.originY) * yscale)
        val bottom = (((tpag.targetY.toFloat() - sprite.originY + tpag.targetHeight.toInt()) * yscale))

        val x1y1Transformed = matrix4f.transformPosition(Vector3f(left, top, 0f), Vector3f())
        val x2y2Transformed = matrix4f.transformPosition(Vector3f(right, top, 0f), Vector3f())
        val x3y3Transformed = matrix4f.transformPosition(Vector3f(right, bottom, 0f), Vector3f())
        val x4y4Transformed = matrix4f.transformPosition(Vector3f(left, bottom, 0f), Vector3f())

        drawQuad(
            x1y1Transformed.x,
            x1y1Transformed.y,
            x2y2Transformed.x,
            x2y2Transformed.y,
            x3y3Transformed.x,
            x3y3Transformed.y,
            x4y4Transformed.x,
            x4y4Transformed.y,
            tpag.sourceX.toFloat() / texture.width,
            tpag.sourceY.toFloat() / texture.height,
            (tpag.sourceX.toFloat() + tpag.sourceWidth.toFloat()) / texture.width,
            (tpag.sourceY.toFloat() + tpag.sourceHeight.toFloat()) / texture.height,
            texture,
            color,
            color,
            color,
            color,
            alpha
        )
    }

    override fun drawSpritePart(
        sprite: RunnerSprite,
        tpag: RunnerTexturePageEntry,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        x: Float,
        y: Float,
        xscale: Float,
        yscale: Float,
        color: GMLColor,
        alpha: Float
    ) {
        val texture = textures[tpag.textureIndex]!!
        // See GameMaker-HTML5's yyWebGL.js WebGL_DrawPart_RELEASE
        var partLeft = left
        var partTop = top
        var partWidth = width
        var partHeight = height
        var partX = x
        var partY = y

        val xOffset = tpag.targetX.toFloat()
        val yOffset = tpag.targetY.toFloat()

        // CLIP the drawing area.
        if (xOffset > partLeft) {
            val off = xOffset - partLeft
            partX += off * xscale
            partWidth -= off
            partLeft = 0f
        } else {
            partLeft -= xOffset
        }

        if (yOffset > partTop) {
            val off = yOffset - partTop
            partY += off * yscale
            partHeight -= off
            partTop = 0f
        } else {
            partTop -= yOffset
        }

        val cropWidth = tpag.sourceWidth.toFloat()
        val cropHeight = tpag.sourceHeight.toFloat()
        if (partWidth > cropWidth - partLeft) partWidth = cropWidth - partLeft
        if (partHeight > cropHeight - partTop) partHeight = cropHeight - partTop
        if (0 >= partWidth || 0 >= partHeight) return

        drawQuad(
            partX,
            partY,
            (tpag.sourceX.toFloat() + partLeft) / texture.width,
            (tpag.sourceY.toFloat() + partTop) / texture.height,
            (tpag.sourceX.toFloat() + partLeft + partWidth) / texture.width,
            (tpag.sourceY.toFloat() + partTop + partHeight) / texture.height,
            partWidth * xscale,
            partHeight * yscale,
            texture,
            color,
            alpha
        )
    }

    override fun drawBackground(texturePageEntry: RunnerTexturePageEntry, x: Float, y: Float, width: Float, height: Float, tileX: Boolean, tileY: Boolean, roomWidth: Int, roomHeight: Int, color: GMLColor, alpha: Float) {
        val texture = textures[texturePageEntry.textureIndex]!!

        val tpag = texturePageEntry

        val howManyTilesX = if (tileX) ceil((roomWidth - x) / width).toInt() else 1
        val howManyTilesY = if (tileY) ceil((roomHeight - y) / height).toInt() else 1

        // println("X tiles: $howManyTilesX, Y tiles: $howManyTilesY, $width, $height")
        repeat(howManyTilesY) { cellY ->
            repeat(howManyTilesX) { cellX ->
                val drawX = (x + tpag.targetX.toInt()) + (cellX * width)
                val drawY = (y + tpag.targetY.toInt()) + (cellY * height)

                // println("$drawX, $drawY")
                drawQuad(
                    drawX,
                    drawY,
                    tpag.sourceX.toFloat() / texture.width,
                    tpag.sourceY.toFloat() / texture.height,
                    (tpag.sourceX.toFloat() + tpag.sourceWidth.toFloat()) / texture.width,
                    (tpag.sourceY.toFloat() + tpag.sourceHeight.toFloat()) / texture.height,
                    width,
                    height,
                    textures[tpag.textureIndex]!!,
                    color,
                    alpha
                )
            }
        }
    }

    override fun drawTile(texturePageEntry: RunnerTexturePageEntry, tile: RunnerRoom.RunnerTile) {
        val texture = textures[texturePageEntry.textureIndex]!!

        val tpag = texturePageEntry

        val entryX = tpag.sourceX.toInt() + tile.sourceX
        val entryY = tpag.sourceY.toInt() + tile.sourceY
        val entryWidth = tile.width
        val entryHeight = tile.height

        // This is painful as hell, but we need to do this because a tile can be LARGER than its texture page entry
        // What we need to think is that the "top left" of the sprite is the reference point, but if a tile is larger than its texture page entry, it should be "transparent"
        val tileMaxWidth = min(entryWidth.toFloat(), tpag.targetWidth.toFloat())
        val tileMaxHeight = min(entryHeight.toFloat(), tpag.targetHeight.toFloat())

        val diffWidth = tileMaxWidth - entryWidth.toFloat()
        val diffHeight = tileMaxHeight - entryHeight.toFloat()

        drawQuad(
            tile.x + tpag.targetX.toFloat(),
            tile.y + tpag.targetY.toFloat(),
            entryX.toFloat() / texture.width,
            entryY.toFloat() / texture.height,
            (entryX + tileMaxWidth) / texture.width,
            (entryY + tileMaxHeight) / texture.height,
            tile.width + diffWidth,
            tile.height + diffHeight,
            textures[tpag.textureIndex]!!,
            GMLColor(0xFFFFFF),
            1.0f
        )
    }

    override fun drawText(font: RunnerFont, x: Float, y: Float, xscale: Float, yscale: Float, rot: Float, horizontalAlign: TextHorizontalAlign, text: String) {
        var x = x
        var y = y

        val tpag = font.texturePageEntry
        val texture = getTextureFromStarfaitHandle(tpag.textureIndex)

        val rad = MathUtils.toRadians(rot.toDouble())

        val textMetrics = TextUtils.getTextMetrics(font, text, xscale, yscale)

        // When testing, it seems that the alignment changes from where the string will be rotated
        // 1. left = rotates around the top left
        // 2. center = rotates around the center
        // 3. right = rotates around the top right
        val pivotX: Float
        val pivotY: Float
        val translateX: Float
        val translateY: Float

        when (horizontalAlign) {
            TextHorizontalAlign.LEFT -> {
                pivotX = x
                pivotY = y
                translateX = 0f
                translateY = 0f
            }
            TextHorizontalAlign.CENTER -> {
                pivotX = x + textMetrics.width / 2f
                pivotY = y
                translateX = -(textMetrics.width / 2f)
                translateY = 0f
            }
            TextHorizontalAlign.RIGHT -> {
                pivotX = x + textMetrics.width
                pivotY = y
                translateX = -(textMetrics.width)
                translateY = 0f
            }
        }

        // Render around center (backwards!)
        val matrix = Matrix3x2f()
            .translate(translateX, translateY)
            // Translate it back to the pivot
            .translate(pivotX, pivotY)
            // Rotate it
            .rotate(-rad.toFloat())
            // Translate it to the world's origin
            .translate(-pivotX, -pivotY)

        // Now we ACTUALLY draw it
        for (lineMetrics in textMetrics.lines) {
            for (char in lineMetrics.characters) {
                val glyph = char.glyph

                val width = (glyph.width.toFloat() * xscale)
                val height = (glyph.height.toFloat() * yscale)

                val xy1 = matrix.transformPosition(Vector2f(char.x + x, char.y + y), Vector2f())
                val xy2 = matrix.transformPosition(Vector2f(char.x + x + width, char.y + y), Vector2f())
                val xy3 = matrix.transformPosition(Vector2f(char.x + x + width, char.y + y + height), Vector2f())
                val xy4 = matrix.transformPosition(Vector2f(char.x + x, char.y + y + height), Vector2f())

                drawQuad(
                    xy1.x,
                    xy1.y,
                    xy2.x,
                    xy2.y,
                    xy3.x,
                    xy3.y,
                    xy4.x,
                    xy4.y,
                    (tpag.sourceX.toFloat() + glyph.x.toFloat()) / texture.width,
                    (tpag.sourceY.toFloat() + glyph.y.toFloat()) / texture.height,
                    (tpag.sourceX.toFloat() + glyph.x.toFloat() + glyph.width.toFloat()) / texture.width,
                    (tpag.sourceY.toFloat() + glyph.y.toFloat() + glyph.height.toFloat()) / texture.height,
                    texture,
                    runner.drawState.color,
                    runner.drawState.color,
                    runner.drawState.color,
                    runner.drawState.color,
                    runner.drawState.alpha
                )
            }
        }
    }

    override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, thickness: Float, color1: GMLColor, color2: GMLColor, alpha: Float) {
        // Essentially to draw a line you get the perpendicular vector to the line, and then you "inflate" the coordinates to get the point of each of the quad's corner
        val p1 = Vector2f(x1, y1)
        val p2 = Vector2f(x2, y2)
        val dir = Vector2f(p2).sub(p1).normalize()
        val perpendicular = Vector2f(-dir.y, dir.x).mul(thickness * 0.5f)

        // These follow the same convention as the drawQuad (top left, top right, bottom right, bottom left)
        val c1 = Vector2f(p1).add(perpendicular)
        val c2 = Vector2f(p1).sub(perpendicular)
        val c3 = Vector2f(p2).sub(perpendicular)
        val c4 = Vector2f(p2).add(perpendicular)

        drawQuad(
            c1.x,
            c1.y,
            c2.x,
            c2.y,
            c3.x,
            c3.y,
            c4.x,
            c4.y,
            0f,
            0f,
            1f,
            1f,
            gpu.white1x1Texture,
            color1,
            color1,
            color2,
            color2,
            1.0f
        )
    }

    override fun drawRectangle(x: Float, y: Float, width: Float, height: Float, color: GMLColor, alpha: Float, outline: Boolean) {
        if (outline) {
            // top
            drawQuad(
                x,
                y,
                0.0f,
                0.0f,
                1.0f,
                1.0f,
                width,
                1f,
                gpu.white1x1Texture,
                color,
                alpha
            )

            // bottom
            drawQuad(
                x,
                y + height - 1f,
                0.0f,
                0.0f,
                1.0f,
                1.0f,
                width,
                1f,
                gpu.white1x1Texture,
                color,
                alpha
            )

            // left
            drawQuad(
                x,
                y,
                0.0f,
                0.0f,
                1.0f,
                1.0f,
                1f,
                height,
                gpu.white1x1Texture,
                color,
                alpha
            )

            // right
            drawQuad(
                x + width - 1f,
                y,
                0.0f,
                0.0f,
                1.0f,
                1.0f,
                1f,
                height,
                gpu.white1x1Texture,
                color,
                alpha
            )
        } else {
            drawQuad(
                x,
                y,
                0.0f,
                0.0f,
                1.0f,
                1.0f,
                width,
                height,
                gpu.white1x1Texture,
                color,
                alpha
            )
        }
    }

    override fun drawTriangle(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float,
        blend1: GMLColor,
        blend2: GMLColor,
        blend3: GMLColor,
        alpha: Float,
        outline: Boolean
    ) {
        if (outline) {
            // TODO: The lines are being rendered off screen if they are at 0, 0?
            drawLine(x1, y1, x2, y2, 1f, blend1, blend2, alpha)
            drawLine(x2, y2, x3, y3, 1f, blend2, blend3, alpha)
            drawLine(x3, y3, x1, y1, 1f, blend3, blend1, alpha)
        } else {
            val texture = this.gpu.white1x1Texture

            batcherScope.drawVertexGroup(3, texture) {
                // vertex #1
                drawVertex(x1, y1, 0.0f, 0.0f, blend1.redF, blend1.greenF, blend1.blueF, alpha)

                // vertex #2
                drawVertex(x2, y2, 1.0f, 0.0f, blend2.redF, blend2.greenF, blend2.blueF, alpha)

                // vertex #3
                drawVertex(x3, y3, 1.0f, 1.0f, blend3.redF, blend3.greenF, blend3.blueF, alpha)
            }
        }
    }

    override fun drawCircle(x: Float, y: Float, radius: Float, circlePrecision: Int, col1: GMLColor, col2: GMLColor, alpha: Float, outline: Boolean) {
        val steps = circlePrecision

        data class Point(val x: Float, val y: Float)

        val points = mutableListOf<Point>()

        repeat(steps) {
            val degrees = (it / steps.toFloat()) * 360f
            val rads = MathUtils.toRadians(degrees.toDouble()).toFloat()
            val x = cos(rads) * radius
            val y = sin(rads) * radius

            // TODO: Is this really correct? Because it feels very wonky that we need to add +1 to the coordinate
            //  And the drawLine has a similar issue too...?
            points.add(Point(x + 1f, y + 1f))
        }

        if (outline) {
            repeat(steps) {
                val point1 = points[it]
                val point2 = points[(it + 1) % steps]

                // TODO: This looks very hacky
                // "Whether the circle is an outline (true) or not (false). If true, col1 is irrelevant."
                drawLine(point1.x + x, point1.y + y, point2.x + x, point2.y + y, 0.93f, col2, col2, alpha)
            }
        } else {
            val texture = this.gpu.white1x1Texture

            repeat(steps) {
                val point1 = points[it]
                val point2 = points[(it + 1) % steps]

                batcherScope.drawVertexGroup(3, texture) {
                    // vertex #1
                    drawVertex(x, y, 0.0f, 0.0f, col1.redF, col1.greenF, col1.blueF, alpha)

                    // vertex #2
                    drawVertex(x + point1.x, y + point1.y, 1.0f, 0.0f, col2.redF, col2.greenF, col2.blueF, alpha)

                    // vertex #3
                    drawVertex(x + point2.x, y + point2.y, 1.0f, 1.0f, col2.redF, col2.greenF, col2.blueF, alpha)
                }
            }
        }
    }

    override fun createTextureFromSurface(surfaceId: Int, x: Int, y: Int, width: Int, height: Int, removeback: Boolean, smooth: Boolean): TextureHandle {
        // This is a bit painful but it is what it is

        // We flush anything pending so that we actually COPY the screen with anything that hasn't been flushed recently
        batcherScope.flush()

        // TODO: Other surfaces

        // This sadly very much sucks quite a bit
        // TODO: Maybe replace this with a framebuffer? We draw directly to the fb and flip it on the GPU
        val flippedByteArray = gl.readPixels(x, VIEWPORT_HEIGHT - y - height, width, height, GLConstants.GL_RGBA, GLConstants.GL_UNSIGNED_BYTE)

        val byteArray = ByteArray(flippedByteArray.size)
        for (i in (height - 1) downTo 0) {
            flippedByteArray.copyInto(byteArray, (height - 1 - i) * width * 4, i * width * 4, (i + 1) * width * 4)
        }

        // 1. Generate and bind a new texture ID
        val textureId = gl.genTextures()
        gl.bindTexture(GLConstants.GL_TEXTURE_2D, textureId)

        // 2. Tell OpenGL how to unpack the RGBA bytes.
        // Each component is 1 byte size
        gl.pixelStorei(GLConstants.GL_UNPACK_ALIGNMENT, 1)

        // Create the texture first
        gl.texImage2D(
            GLConstants.GL_TEXTURE_2D,
            0,
            GLConstants.GL_RGBA,
            0,
            GLConstants.GL_RGBA,
            GLConstants.GL_UNSIGNED_BYTE,
            ByteArrayTextureSource(
                width,
                height,
                byteArray
            )
        )

        // 3. Set texture parameters
        // Repeat image in both directions
        gl.texParameteri(GLConstants.GL_TEXTURE_2D, GLConstants.GL_TEXTURE_WRAP_S, GLConstants.GL_REPEAT)
        gl.texParameteri(GLConstants.GL_TEXTURE_2D, GLConstants.GL_TEXTURE_WRAP_T, GLConstants.GL_REPEAT)
        // When stretching the image, pixelate
        gl.texParameteri(GLConstants.GL_TEXTURE_2D, GLConstants.GL_TEXTURE_MIN_FILTER, GLConstants.GL_NEAREST)
        // When shrinking the image, pixelate
        gl.texParameteri(GLConstants.GL_TEXTURE_2D, GLConstants.GL_TEXTURE_MAG_FILTER, GLConstants.GL_NEAREST)

        gl.bindFramebuffer(GLConstants.GL_READ_FRAMEBUFFER, 0)

        return allocateAndStoreTextureHandle(Texture(gpu.gl, textureId, width, height))
    }

    override fun deleteTextures(textures: List<TextureHandle>) = gl.deleteTextures(textures.mapNotNull { this.textures.remove(it)?.textureId })

    override fun withView(view: RunnerRoom.RunnerView?, block: () -> Unit) {
        gpu.onRenderTarget(gpu.windowRenderTarget) {
            withSpritePipeline(batcher, minimalShader) {
                gl.enable(GLConstants.GL_SCISSOR_TEST)

                val projection: Matrix4f

                if (view != null) {
                    val halfWidth = view.viewWidth.toFloat() / 2f
                    val halfHeight = view.viewHeight.toFloat() / 2f

                    // portX/portY = where it will be rendered in the screen
                    // We do - for the Y because OpenGL is y up
                    gl.viewport(view.portX, VIEWPORT_HEIGHT - (view.portY + view.portHeight), view.portWidth, view.portHeight)
                    gl.scissor(view.portX, VIEWPORT_HEIGHT - (view.portY + view.portHeight), view.portWidth, view.portHeight)

                    projection = Matrix4f()
                        .ortho(
                            view.viewX.toFloat(),
                            view.viewX.toFloat() + view.viewWidth.toFloat(),
                            view.viewY.toFloat() + view.viewHeight.toFloat(),
                            view.viewY.toFloat(),
                            1.0f,
                            -1.0f
                        )
                        // Essentially what we do here is that we want to move the center of the camera to (0, 0), then we apply the rotation, then we move it back to where it is supposed to be
                        .translate(halfWidth + view.viewX.toFloat(), halfHeight + view.viewY.toFloat(), 0f)
                        .rotateZ(MathUtils.toRadians(-view.angle.toDouble()).toFloat())
                        .translate(-halfWidth - view.viewX.toFloat(), -halfHeight - view.viewY.toFloat(), 0f)
                } else {
                    gl.viewport(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
                    gl.scissor(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)

                    projection = Matrix4f()
                        .ortho(0.0f, runner.currentRoom.width.toFloat(), runner.currentRoom.height.toFloat(), 0.0f, 1.0f, -1.0f)
                }

                this.shader.uProjection.set(projection.get(FloatArray(16)))

                block()

                batcherScope.flush()

                gl.disable(GLConstants.GL_SCISSOR_TEST)
            }
        }
    }

    override fun frame(block: () -> Unit) {
        // TODO: Respect the room's drawBackgroundColor
        val color = runner.currentRoom.backgroundColor
        gl.clearColor(color.redF, color.greenF, color.blueF, 1.0f)
        gl.clear(GLConstants.GL_COLOR_BUFFER_BIT)

        block()

        gpu.onRenderTarget(gpu.windowRenderTarget) {
            withSpritePipeline(batcher, minimalShader) {
                batcher.drawBatch(this) {
                    if (runner.enableCollisionRendering) {
                        for (instance in runner.currentRoom.instances) {
                            val bbox = instance.getBoundingBox()

                            drawQuad(
                                bbox.left,
                                bbox.top,
                                0.0f,
                                0.0f,
                                1.0f,
                                1.0f,
                                bbox.right - bbox.left,
                                bbox.bottom - bbox.top,
                                gpu.white1x1Texture,
                                GMLColor(0x00ff00),
                                0.25f
                            )

                            if (instance.spriteIndex >= 0f) {
                                val collisionSprite = instance.getCollisionSprite()
                                if (collisionSprite != null && collisionSprite.hasCollisionMask) {
                                    // This is very much NOT optimized
                                    // I think the best way would be to convert the mask into a image, then blit it to a texture
                                    for (y in 0 until (collisionSprite.height * instance.imageYScale).toInt()) {
                                        for (x in 0 until (collisionSprite.width * instance.imageXScale).toInt()) {
                                            if (collisionSprite.isPixelSolidOnCollisionMask(
                                                    instance.imageIndex.toInt(),
                                                    (x / instance.imageXScale).toInt(),
                                                    (y / instance.imageYScale).toInt(),
                                                    runner.collisionCompatibilityMode
                                                )
                                            ) {
                                                drawQuad(
                                                    x + instance.x,
                                                    y + instance.y,
                                                    0.0f,
                                                    0.0f,
                                                    1.0f,
                                                    1.0f,
                                                    1f,
                                                    1f,
                                                    gpu.white1x1Texture,
                                                    GMLColor(0x00ffff),
                                                    0.25f
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        for (debugRectangle in runner.debugRectangles) {
                            drawQuad(
                                debugRectangle.bbox.left,
                                debugRectangle.bbox.top,
                                0.0f,
                                0.0f,
                                1.0f,
                                1.0f,
                                debugRectangle.bbox.right - debugRectangle.bbox.left,
                                debugRectangle.bbox.bottom - debugRectangle.bbox.top,
                                gpu.white1x1Texture,
                                debugRectangle.color,
                                0.5f
                            )
                        }

                        for (debugLine in runner.debugLines) {
                            drawLine(
                                debugLine.x1,
                                debugLine.y1,
                                debugLine.x2,
                                debugLine.y2,
                                1f,
                                debugLine.color,
                                debugLine.color,
                                1.0f
                            )
                        }
                    }

                    runner.debugRectangles.clear()
                    runner.debugLines.clear()
                }
            }
        }
    }

    fun drawQuad(
        x: Float,
        y: Float,
        u1: Float,
        v1: Float,
        u2: Float,
        v2: Float,
        width: Float,
        height: Float,
        texture: Texture,
        blend: GMLColor,
        alpha: Float
    ) = drawQuad(x, y, x + width, y, x + width, y + height, x, y + height, u1, v1, u2, v2, texture, blend, blend, blend, blend, alpha)

    // Clockwise pattern for the coordinates
    fun drawQuad(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float,
        x4: Float,
        y4: Float,
        u1: Float,
        v1: Float,
        u2: Float,
        v2: Float,
        texture: Texture,
        blend1: GMLColor,
        blend2: GMLColor,
        blend3: GMLColor,
        blend4: GMLColor,
        alpha: Float
    ) {
        batcherScope.drawQuad(
            x1,
            y1,
            x2,
            y2,
            x3,
            y3,
            x4,
            y4,
            u1,
            v1,
            u2,
            v2,
            texture,
            blend1.redF,
            blend1.greenF,
            blend1.blueF,
            alpha,
            blend2.redF,
            blend2.greenF,
            blend2.blueF,
            alpha,
            blend3.redF,
            blend3.greenF,
            blend3.blueF,
            alpha,
            blend4.redF,
            blend4.greenF,
            blend4.blueF,
            alpha
        )
    }
}