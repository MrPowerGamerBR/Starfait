package com.mrpowergamerbr.starfait.renderer

import com.mrpowergamerbr.starfait.GMLColor
import com.mrpowergamerbr.starfait.runner.Instance
import com.mrpowergamerbr.starfait.runner.RunnerRoom
import com.mrpowergamerbr.starfait.runner.resources.RunnerFont
import com.mrpowergamerbr.starfait.runner.resources.RunnerSprite
import com.mrpowergamerbr.starfait.runner.resources.RunnerTexturePageEntry
import com.mrpowergamerbr.starfait.runner.text.TextHorizontalAlign
import com.mrpowergamerbr.starfait.wad.Texture

abstract class Renderer {
    var doSwap = true

    /**
     * Uploads a [texture] to the renderer.
     *
     * @param texture the texture to upload
     * @return a texture handle specific to the renderer
     */
    abstract fun uploadTexture(texture: Texture): TextureHandle

    abstract fun drawSprite(sprite: RunnerSprite, tpag: RunnerTexturePageEntry, x: Float, y: Float, xscale: Float, yscale: Float, rot: Float, color: GMLColor, alpha: Float)
    abstract fun drawSpritePart(sprite: RunnerSprite, tpag: RunnerTexturePageEntry, left: Float, top: Float, width: Float, height: Float, x: Float, y: Float, xscale: Float, yscale: Float, color: GMLColor, alpha: Float)
    abstract fun drawBackground(texturePageEntry: RunnerTexturePageEntry, x: Float, y: Float, width: Float, height: Float, tileX: Boolean, tileY: Boolean, roomWidth: Int, roomHeight: Int, color: GMLColor, alpha: Float)
    abstract fun drawTile(texturePageEntry: RunnerTexturePageEntry, tile: RunnerRoom.RunnerTile)
    abstract fun drawText(font: RunnerFont, x: Float, y: Float, xscale: Float, yscale: Float, rot: Float, horizontalAlign: TextHorizontalAlign, text: String)
    abstract fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, thickness: Float, color1: GMLColor, color2: GMLColor, alpha: Float)
    abstract fun drawRectangle(x: Float, y: Float, width: Float, height: Float, color: GMLColor, alpha: Float, outline: Boolean)
    abstract fun drawTriangle(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, blend1: GMLColor, blend2: GMLColor, blend3: GMLColor, alpha: Float, outline: Boolean)
    abstract fun drawCircle(x: Float, y: Float, radius: Float, circlePrecision: Int, col1: GMLColor, col2: GMLColor, alpha: Float, outline: Boolean)
    abstract fun createTextureFromSurface(surfaceId: Int, x: Int, y: Int, width: Int, height: Int, removeback: Boolean, smooth: Boolean): TextureHandle
    abstract fun deleteTextures(textures: List<TextureHandle>)
    abstract fun withView(view: RunnerRoom.RunnerView?, block: () -> (Unit))
    abstract fun frame(block: () -> (Unit))
}