package com.mrpowergamerbr.starfait.renderer.noop

import com.mrpowergamerbr.starfait.GMLColor
import com.mrpowergamerbr.starfait.runner.Instance
import com.mrpowergamerbr.starfait.renderer.Renderer
import com.mrpowergamerbr.starfait.runner.RunnerRoom
import com.mrpowergamerbr.starfait.wad.Font
import com.mrpowergamerbr.starfait.wad.Sprite
import com.mrpowergamerbr.starfait.wad.TexturePageEntry

object NoopRenderer : Renderer() {
    override fun drawInstance(instance: Instance) {}
    override fun drawSprite(sprite: Sprite, subimg: Int, x: Float, y: Float, xscale: Float, yscale: Float, rot: Float, color: GMLColor, alpha: Float) {}
    override fun drawSpritePart(sprite: Sprite, subimg: Int, left: Float, top: Float, width: Float, height: Float, x: Float, y: Float, xscale: Float, yscale: Float, color: GMLColor, alpha: Float) {}
    override fun drawBackground(texturePageEntry: TexturePageEntry, x: Float, y: Float, width: Float, height: Float, tileX: Boolean, tileY: Boolean, roomWidth: Int, roomHeight: Int, alpha: Float) {}
    override fun drawTile(texturePageEntry: TexturePageEntry, tile: RunnerRoom.RunnerTile) {}
    override fun drawText(font: Font, x: Float, y: Float, xscale: Float, yscale: Float, rot: Float, text: String) {}
    override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, thickness: Float, color1: GMLColor, color2: GMLColor, alpha: Float) {}
    override fun drawRectangle(x: Float, y: Float, width: Float, height: Float, color: GMLColor, alpha: Float, outline: Boolean) {}
    override fun withView(view: RunnerRoom.RunnerView?, block: () -> (Unit)) {}
    override fun frame(block: () -> Unit) {}
}