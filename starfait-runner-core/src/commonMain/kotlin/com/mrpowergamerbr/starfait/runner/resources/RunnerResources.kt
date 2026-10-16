package com.mrpowergamerbr.starfait.runner.resources

import com.mrpowergamerbr.starfait.renderer.Renderer
import com.mrpowergamerbr.starfait.wad.Code
import com.mrpowergamerbr.starfait.wad.GameWAD
import com.mrpowergamerbr.starfait.wad.chunks.CODE
import com.mrpowergamerbr.starfait.wad.chunks.OBJT
import com.mrpowergamerbr.starfait.wad.chunks.ROOM
import com.mrpowergamerbr.starfait.wad.chunks.SOND
import com.mrpowergamerbr.starfait.wad.chunks.STRG

/**
 * Creates a "RunnerGame" from a [GameWAD] state, to avoid mutating the original [GameWAD]
 *
 * Essentially it is just the [GameWAD] data in a more "friendly" format
 */
class RunnerResources(
    val maxRoomInstanceId: Int,
    val roomOrder: List<Int>,
    val paths: MutableList<RunnerPath>,
    val sprites: MutableList<RunnerSprite>,
    val fonts: MutableList<RunnerFont>,
    val backgrounds: MutableList<RunnerBackground>,
    val strg: STRG,
    val room: ROOM,
    val code: CODE,
    val objt: OBJT,
    val sond: SOND
) {
    companion object {
        suspend fun create(gameWAD: GameWAD, renderer: Renderer): RunnerResources {
            // Look: This royally sucks, because we essentially duplicate the WAD versions of the classes with our own classes
            // But it is the only way we can do things like *dynamic* sprites in a way that doesn't want me to cry
            // TODO: Maybe intern the strings?
            // TODO: A lot of the fields in the resources could be dropped
            val textureHandles = gameWAD.txtr.textures.mapIndexed { index, texture -> index.toShort() to renderer.uploadTexture(texture) }.toMap()
            val paths = gameWAD.path.paths.map { RunnerPath(it) }
            val sprites = gameWAD.sprt.sprites.map { RunnerSprite.fromWAD(gameWAD, it, textureHandles) }.toMutableList()
            val fonts = gameWAD.font.fonts.map { RunnerFont.fromWAD(gameWAD, it, textureHandles) }
            val backgrounds = gameWAD.bgnd.backgrounds.map { RunnerBackground.fromWAD(gameWAD, it, textureHandles) }

            return RunnerResources(
                gameWAD.gen8.maxRoomInstanceId,
                gameWAD.gen8.roomOrder,
                paths.toMutableList(),
                sprites.toMutableList(),
                fonts.toMutableList(),
                backgrounds.toMutableList(),
                gameWAD.strg,
                gameWAD.room,
                gameWAD.code,
                gameWAD.objt,
                gameWAD.sond
            )
        }
    }
}