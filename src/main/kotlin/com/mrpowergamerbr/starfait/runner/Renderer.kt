package com.mrpowergamerbr.starfait.runner

import com.mrpowergamerbr.starfait.GMLColor
import com.mrpowergamerbr.starfait.wad.Font
import com.mrpowergamerbr.starfait.wad.Room
import com.mrpowergamerbr.starfait.wad.Sprite
import com.mrpowergamerbr.starfait.wad.TexturePageEntry

interface Renderer {
    fun drawInstance(instance: Instance)
    fun drawSprite(sprite: Sprite, subimg: Int, x: Float, y: Float, xscale: Float, yscale: Float, alpha: Float)
    fun drawSpritePart(sprite: Sprite, subimg: Int, left: Float, top: Float, width: Float, height: Float, x: Float, y: Float, alpha: Float)
    fun drawBackground(texturePageEntry: TexturePageEntry, width: Float, height: Float)
    fun drawTile(texturePageEntry: TexturePageEntry, tile: RunnerRoom.RunnerTile)
    fun drawText(font: Font, x: Float, y: Float, text: String)
    fun fillRectangle(x: Float, y: Float, width: Float, height: Float, color: GMLColor)
    fun frame(block: () -> (Unit))
    fun setDrawColor(color: GMLColor)
}