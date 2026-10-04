package com.mrpowergamerbr.starfait.runner

import com.mrpowergamerbr.starfait.GMLColor
import com.mrpowergamerbr.starfait.wad.GameWAD

class DrawState(val gameWAD: GameWAD) {
    var alpha = 1.0f
    var color = GMLColor(0xFFFFFF)
    var font = gameWAD.font.fonts.firstOrNull()
}