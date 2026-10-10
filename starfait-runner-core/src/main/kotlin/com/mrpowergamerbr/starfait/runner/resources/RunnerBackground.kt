package com.mrpowergamerbr.starfait.runner.resources

import com.mrpowergamerbr.starfait.renderer.TextureHandle
import com.mrpowergamerbr.starfait.wad.Background
import com.mrpowergamerbr.starfait.wad.GameWAD

class RunnerBackground private constructor(
    val name: String,
    val transparent: Boolean,
    val smooth: Boolean,
    val preload: Boolean,
    val texturePageEntry: RunnerTexturePageEntry?
) {
    companion object {
        fun fromWAD(gameWAD: GameWAD, background: Background, textureHandles: Map<Short, TextureHandle>) = RunnerBackground(
            gameWAD.strg.getString(background.name),
            background.transparent,
            background.smooth,
            background.preload,
            RunnerTexturePageEntry.fromWAD(gameWAD, background.texturePageEntryPointer, textureHandles),
        )
    }
}