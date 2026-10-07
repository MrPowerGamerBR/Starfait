package com.mrpowergamerbr.starfait.renderer

import com.mrpowergamerbr.starfait.GMLColor
import com.mrpowergamerbr.starfait.runner.Instance
import com.mrpowergamerbr.starfait.runner.RunnerRoom
import com.mrpowergamerbr.starfait.wad.Font
import com.mrpowergamerbr.starfait.wad.Sprite
import com.mrpowergamerbr.starfait.wad.TexturePageEntry

abstract class Renderer {
    var doSwap = true

    abstract fun drawInstance(instance: Instance)
    abstract fun drawSprite(sprite: Sprite, subimg: Int, x: Float, y: Float, xscale: Float, yscale: Float, rot: Float, color: GMLColor, alpha: Float)
    abstract fun drawSpritePart(sprite: Sprite, subimg: Int, left: Float, top: Float, width: Float, height: Float, x: Float, y: Float, xscale: Float, yscale: Float, color: GMLColor, alpha: Float)
    abstract fun drawBackground(texturePageEntry: TexturePageEntry, x: Float, y: Float, width: Float, height: Float, tileX: Boolean, tileY: Boolean, roomWidth: Int, roomHeight: Int, alpha: Float)
    abstract fun drawTile(texturePageEntry: TexturePageEntry, tile: RunnerRoom.RunnerTile)
    abstract fun drawText(font: Font, x: Float, y: Float, xscale: Float, yscale: Float, rot: Float, text: String)
    abstract fun fillRectangle(x: Float, y: Float, width: Float, height: Float, color: GMLColor, alpha: Float, outline: Boolean)
    abstract fun withView(view: RunnerRoom.RunnerView?, block: () -> (Unit))
    abstract fun frame(block: () -> (Unit))
}