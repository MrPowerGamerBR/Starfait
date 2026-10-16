package com.mrpowergamerbr.starfait.renderer.noop

import com.mrpowergamerbr.starfait.GMLColor
import com.mrpowergamerbr.starfait.runner.Instance
import com.mrpowergamerbr.starfait.renderer.Renderer
import com.mrpowergamerbr.starfait.renderer.TextureHandle
import com.mrpowergamerbr.starfait.runner.RunnerRoom
import com.mrpowergamerbr.starfait.runner.resources.RunnerFont
import com.mrpowergamerbr.starfait.runner.resources.RunnerSprite
import com.mrpowergamerbr.starfait.runner.resources.RunnerTexturePageEntry
import com.mrpowergamerbr.starfait.runner.text.TextHorizontalAlign
import com.mrpowergamerbr.starfait.wad.Texture

object NoopRenderer : Renderer() {
    override suspend fun uploadTexture(texture: Texture) = TextureHandle(0)

    override fun drawSprite(sprite: RunnerSprite, tpag: RunnerTexturePageEntry, x: Float, y: Float, xscale: Float, yscale: Float, rot: Float, color: GMLColor, alpha: Float) {}
    override fun drawSpritePart(sprite: RunnerSprite, tpag: RunnerTexturePageEntry, left: Float, top: Float, width: Float, height: Float, x: Float, y: Float, xscale: Float, yscale: Float, color: GMLColor, alpha: Float) {}
    override fun drawBackground(texturePageEntry: RunnerTexturePageEntry, x: Float, y: Float, width: Float, height: Float, tileX: Boolean, tileY: Boolean, roomWidth: Int, roomHeight: Int, color: GMLColor, alpha: Float) {}
    override fun drawTile(texturePageEntry: RunnerTexturePageEntry, tile: RunnerRoom.RunnerTile) {}
    override fun drawText(font: RunnerFont, x: Float, y: Float, xscale: Float, yscale: Float, rot: Float, horizontalAlign: TextHorizontalAlign, text: String) {}
    override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, thickness: Float, color1: GMLColor, color2: GMLColor, alpha: Float) {}
    override fun drawRectangle(x: Float, y: Float, width: Float, height: Float, color: GMLColor, alpha: Float, outline: Boolean) {}
    override fun drawTriangle(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, blend1: GMLColor, blend2: GMLColor, blend3: GMLColor, alpha: Float, outline: Boolean) {}
    override fun drawCircle(x: Float, y: Float, radius: Float, circlePrecision: Int, col1: GMLColor, col2: GMLColor, alpha: Float, outline: Boolean) {}
    override fun createTextureFromSurface(surfaceId: Int, x: Int, y: Int, width: Int, height: Int, removeback: Boolean, smooth: Boolean) = TextureHandle(0)
    override fun deleteTextures(textures: List<TextureHandle>) {}

    override fun withView(view: RunnerRoom.RunnerView?, block: () -> (Unit)) {}
    override fun frame(block: () -> Unit) {}
}