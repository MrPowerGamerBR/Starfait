package com.mrpowergamerbr.starfait.runner

import com.mrpowergamerbr.starfait.wad.Font
import com.mrpowergamerbr.starfait.wad.Sprite
import com.mrpowergamerbr.starfait.wad.TexturePageEntry

interface Renderer {
    fun drawInstance(instance: Instance)
    fun drawSprite(sprite: Sprite, subimg: Int, x: Float, y: Float)
    fun drawBackground(texturePageEntry: TexturePageEntry, width: Float, height: Float)
    fun drawText(font: Font, x: Float, y: Float, text: String)
}