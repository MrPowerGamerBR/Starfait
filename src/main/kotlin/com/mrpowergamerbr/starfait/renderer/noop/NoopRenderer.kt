package com.mrpowergamerbr.starfait.renderer.noop

import com.mrpowergamerbr.starfait.GMLColor
import com.mrpowergamerbr.starfait.runner.Instance
import com.mrpowergamerbr.starfait.runner.Renderer
import com.mrpowergamerbr.starfait.runner.RunnerRoom
import com.mrpowergamerbr.starfait.wad.Font
import com.mrpowergamerbr.starfait.wad.Sprite
import com.mrpowergamerbr.starfait.wad.TexturePageEntry

object NoopRenderer : Renderer {
    override fun drawInstance(instance: Instance) {}
    override fun drawSprite(sprite: Sprite, subimg: Int, x: Float, y: Float, xscale: Float, yscale: Float, alpha: Float) {}
    override fun drawBackground(texturePageEntry: TexturePageEntry, width: Float, height: Float) {}
    override fun drawTile(texturePageEntry: TexturePageEntry, tile: RunnerRoom.RunnerTile) {}
    override fun drawText(font: Font, x: Float, y: Float, text: String) {}
    override fun fillRectangle(x: Float, y: Float, width: Float, height: Float, color: GMLColor) {}
    override fun frame(block: () -> Unit) {}
    override fun setDrawColor(color: GMLColor) {}
}