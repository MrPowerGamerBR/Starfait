package com.mrpowergamerbr.starfait.renderer

import com.mrpowergamerbr.starfait.GMLColor
import com.mrpowergamerbr.starfait.runner.Instance
import com.mrpowergamerbr.starfait.runner.RunnerRoom
import com.mrpowergamerbr.starfait.wad.Font
import com.mrpowergamerbr.starfait.wad.Sprite
import com.mrpowergamerbr.starfait.wad.TexturePageEntry

interface Renderer {
    fun drawInstance(instance: Instance)
    fun drawSprite(sprite: Sprite, subimg: Int, x: Float, y: Float, xscale: Float, yscale: Float, alpha: Float)
    fun drawSpritePart(sprite: Sprite, subimg: Int, left: Float, top: Float, width: Float, height: Float, x: Float, y: Float, xscale: Float, yscale: Float, color: GMLColor, alpha: Float)
    fun drawBackground(texturePageEntry: TexturePageEntry, x: Float, y: Float, width: Float, height: Float)
    fun drawTile(texturePageEntry: TexturePageEntry, tile: RunnerRoom.RunnerTile)
    fun drawText(font: Font, x: Float, y: Float, text: String)
    fun fillRectangle(x: Float, y: Float, width: Float, height: Float, color: GMLColor)
    fun frame(block: () -> (Unit))
}