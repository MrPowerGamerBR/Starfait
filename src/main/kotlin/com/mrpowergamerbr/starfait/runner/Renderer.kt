package com.mrpowergamerbr.starfait.runner

import com.mrpowergamerbr.starfait.wad.Font

interface Renderer {
    fun drawInstance(instance: Instance)

    fun drawText(font: Font, x: Float, y: Float, text: String)
}