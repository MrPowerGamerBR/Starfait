package com.mrpowergamerbr.starfait

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import com.mrpowergamerbr.starfait.desktop.StarfaitWindow
import com.mrpowergamerbr.starfait.disasm.StarfaitDisassembler
import com.mrpowergamerbr.starfait.vm.StarfaitVM
import com.mrpowergamerbr.starfait.wad.*
import com.mrpowergamerbr.starfait.wad.Function
import java.io.File
import java.nio.ByteBuffer
import javax.imageio.ImageIO
import kotlin.collections.map
import kotlin.let
import kotlin.reflect.KProperty1

class StarfaitCLI : CliktCommand() {
    val wadPath by argument()
    val printGameObjects by option()
    val printBackgrounds by option()
    val printSounds by option()
    val printFonts by option()
    val printRooms by option()
    val printScripts by option()
    val printCodes by option()
    val printVariables by option()
    val printFunctions by option()
    val printTexturePages by option()
    val printSprites by option()
    val dumpTiles by option()
    val traceOpcodes by option()
    val traceFunctionCalls by option()
    val traceAlarms by option()
    val traceEvents by option()
    val traceVariableReads by option()
    val traceVariableWrites by option()
    val traceFrameDuration by option().multiple()
    val traceCodeExecution by option()
    val enableCollisionRendering by option().flag(default = false)
    val recordInputs by option().file()
    val playbackInputs by option().file()
    val screenshot by option().file()
    val screenshotAtFrame by option().int().multiple()
    val exitAtFrame by option().int()
    val pauseAtFrame by option().int().multiple()
    val headless by option().flag(default = false)
    val replaceRoom by option().multiple()
    val dumpState by option().file()
    val dumpStateAtFrame by option().int().multiple()
    val disassemble by option().multiple()
    val seed by option().long().defaultLazy { System.currentTimeMillis() }

    fun <T : Any> dump(gameWAD: GameWAD, type: String, list: List<T>, nameSource: KProperty1<T, StringPointer>?, match: String) {
        for ((index, element) in list.withIndex()) {
            val name = nameSource?.invoke(element)?.let { gameWAD.strg.getString(it) }

            if (match == name || match == "*") {
                println("$type #$index:")
                StarfaitUtils.dump(gameWAD, element, 1, null)

                println()
            }
        }
    }

    override fun run() {
        val byteReader = StarfaitByteBuffer.fromByteArray(File(wadPath).readBytes())
        val gameWAD = GameWAD.read(byteReader)
        println("Loaded \"${gameWAD.strg.getString(gameWAD.gen8.gameDisplayName)}\" successfully! [WAD Version ${gameWAD.gen8.wadVersion}]")

        if (printGameObjects != null) {
            dump(gameWAD, "Object", gameWAD.objt.objects, GameObject::name, printGameObjects!!)
            return
        }

        if (printBackgrounds != null) {
            dump(gameWAD, "Background", gameWAD.bgnd.backgrounds, Background::name, printBackgrounds!!)
            return
        }

        if (printSounds != null) {
            dump(gameWAD, "Sound", gameWAD.sond.sounds, Sound::name, printSounds!!)
            return
        }

        if (printFonts != null) {
            dump(gameWAD, "Font", gameWAD.font.fonts, Font::name, printFonts!!)
            return
        }

        if (printRooms != null) {
            dump(gameWAD, "Room", gameWAD.room.rooms, Room::name, printRooms!!)
            return
        }

        if (printScripts != null) {
            dump(gameWAD, "Script", gameWAD.scpt.scripts, Script::name, printScripts!!)
            return
        }

        if (printCodes != null) {
            dump(gameWAD, "Code", gameWAD.code.codes, Code::name, printCodes!!)
            return
        }

        if (printVariables != null) {
            dump(gameWAD, "Variable", gameWAD.vari.variables, Variable::name, printVariables!!)
            return
        }

        if (printFunctions != null) {
            dump(gameWAD, "Function", gameWAD.func.functions, Function::name, printFunctions!!)
            return
        }

        if (printTexturePages != null) {
            dump(gameWAD, "Texture Page Entry", gameWAD.tpag.entries.values.toList(), null, printTexturePages!!)
            return
        }

        if (printSprites != null) {
            dump(gameWAD, "Sprite", gameWAD.sprt.sprites, Sprite::name, printSprites!!)
            return
        }

        if (dumpTiles != null) {
            val outputFolder = File(dumpTiles!!)
            outputFolder.mkdirs()

            // TODO: Allow customizing this somehow?
            val room = gameWAD.room.rooms.first { gameWAD.strg.getString(it.name) == "room_area1" }

            for (tile in room.tiles) {
                val background = gameWAD.bgnd.backgrounds[tile.backgroundIndex]
                val tpag = gameWAD.tpag.entries[background.texturePageEntryPointer]!!
                val txtr = gameWAD.txtr.textures[tpag.textureIndex.toInt()]
                // TODO: We could reuse the blob
                val txtrBlob = ImageIO.read(txtr.blob.inputStream())

                println(tile.sourceX.toString() + ", " + tile.sourceY)
                // val graphics2d = txtrBlob.createGraphics()

                val entryX = tpag.sourceX.toInt() + tile.sourceX
                val entryY = tpag.sourceY.toInt() + tile.sourceY
                val entryWidth = tile.width
                val entryHeight = tile.height

                // graphics2d.color = Color.RED
                // graphics2d.stroke = BasicStroke(3f)
                // graphics2d.drawRect(entryX, entryY, entryWidth, entryHeight)

                val tileImage = txtrBlob.getSubimage(
                    entryX,
                    entryY,
                    entryWidth,
                    entryHeight
                )

                ImageIO.write(tileImage, "png", File(outputFolder, "${gameWAD.strg.getString(room.name)}_${tile.x}_${tile.y}.png"))
            }
            return
        }

        if (disassemble.isNotEmpty()) {
            val disassembler = StarfaitDisassembler(gameWAD)
            StarfaitUtils.remapReferences(gameWAD)

            val codeToBeDisassembled = gameWAD.code.codes.filter { disassemble.contains("*") || gameWAD.strg.getString(it.name) in disassemble }

            for (code in codeToBeDisassembled) {
                println("CODE ${gameWAD.strg.getString(code.name)}:")
                disassembler.disassembleAndPrint(
                    ByteBuffer.wrap(
                        gameWAD.code.bytecode,
                        // We do + on the offset because the offset is negative
                        code.offset + (code.bytecodeRelativeOffsetFieldPosition + code.bytecodeRelativeOffset) - gameWAD.code.postAddressPosition,
                        code.length.toInt() - code.offset
                    )
                )
            }
            return
        }

        val vm = StarfaitVM(
            gameWAD,
            gmlScriptsOpcodesToBeTraced = traceOpcodes?.split(",")?.toSet().orEmpty(),
            functionCallsToBeTraced = traceFunctionCalls?.split(",")?.toSet().orEmpty(),
            alarmsToBeTraced = traceAlarms?.split(",")?.toSet().orEmpty(),
            eventsToBeTraced = traceEvents?.split(",")?.toSet().orEmpty(),
            traceVariableReads = traceVariableReads?.split(",")?.toSet().orEmpty(),
            traceVariableWrites = traceVariableWrites?.split(",")?.toSet().orEmpty(),
            traceCodeExecution = traceCodeExecution?.split(",")?.toSet().orEmpty()
        )
        vm.associateBuiltins(StarfaitUtils.remapReferences(gameWAD))

        val window = StarfaitWindow(
            vm,
            seed,
            enableCollisionRendering,
            recordInputs,
            playbackInputs,
            screenshot,
            screenshotAtFrame.toSet(),
            exitAtFrame,
            pauseAtFrame.toSet(),
            headless,
            replaceRoom.map {
                it.split("=").let { (room1NameOrId, room2NameOrId) ->
                    var room1Id: Int? = null
                    var room2Id: Int? = null

                    for ((index, room) in gameWAD.room.rooms.withIndex()) {
                        if (room1NameOrId.toIntOrNull() == index || room1NameOrId == gameWAD.strg.getString(room.name)) {
                            room1Id = index
                            break
                        }
                    }

                    for ((index, room) in gameWAD.room.rooms.withIndex()) {
                        if (room2NameOrId.toIntOrNull() == index || room2NameOrId == gameWAD.strg.getString(room.name)) {
                            room2Id = index
                            break
                        }
                    }

                    requireNotNull(room1Id)
                    requireNotNull(room2Id)

                    Pair(room1Id, room2Id)
                }
            }.toMap(),
            dumpState,
            dumpStateAtFrame.toSet(),
            traceFrameDuration.map { if (it == "*") 0..Int.MAX_VALUE else it.split("..").let { it[0].toInt()..it[1].toInt() } }
        )
        window.start()

        // runner.start()
        // repeat(200) {
        //     runner.step()
        // }
        // vm.executeLoop(gameWAD.code.codes.first { gameWAD.strg.getString(it.name) == "gml_Script_attention_hackerz_no_2" })
    }
}

fun main(args: Array<String>) = StarfaitCLI().main(args)