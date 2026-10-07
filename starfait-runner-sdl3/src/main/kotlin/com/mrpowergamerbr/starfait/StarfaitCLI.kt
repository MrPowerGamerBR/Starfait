package com.mrpowergamerbr.starfait

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.double
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.float
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import com.mrpowergamerbr.starfait.desktop.StarfaitWindow
import com.mrpowergamerbr.starfait.desktop.StarfaitWindow.SaveFolderKind
import com.mrpowergamerbr.starfait.disasm.StarfaitDisassembler
import com.mrpowergamerbr.starfait.vm.StarfaitVM
import com.mrpowergamerbr.starfait.wad.*
import com.mrpowergamerbr.starfait.wad.Function
import kotlinx.serialization.json.Json
import org.lwjgl.system.MemoryStack
import org.lwjgl.util.tinyfd.TinyFileDialogs
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.nio.ByteBuffer
import javax.imageio.ImageIO
import kotlin.collections.map
import kotlin.let
import kotlin.reflect.KProperty1

class StarfaitCLI : CliktCommand() {
    companion object {
        private val WAD_FILTERS = listOf(
            "*.win"
        )
    }

    val wadFile by argument().file(mustExist = true).optional()
    val printGameObjects by option()
    val printBackgrounds by option()
    val printSounds by option()
    val printFonts by option()
    val printRooms by option()
    val printScripts by option()
    val printCodes by option()
    val printVariables by option()
    val printFunctions by option()
    val printUnimplementedFunctions by option().flag(default = false)
    val printTexturePages by option()
    val printSprites by option()
    val printObjectTree by option()
    val dumpSprites by option()
    val dumpSpriteMasks by option()
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
    val drawInvisibleInstances by option().flag(default = false)
    val recordInputs by option().file()
    val playbackInputs by option().file()
    val screenshot by option().file()
    val screenshotAtFrame by option().int().multiple()
    val exitAtFrame by option().int()
    val exitWhenPlaybackEnds by option().flag(default = false)
    val speed by option().double().default(1.0)
    val runFullSpeedUntilFrame by option().int()
    val pauseAtFrame by option().int().multiple()
    val headless by option().flag(default = false)
    val replaceRoom by option().multiple()
    val dumpState by option().file()
    val dumpStateAtFrame by option().int().multiple()
    val disassemble by option().multiple()
    val masterVolume by option().float().default(1.0f)
    val seed by option().long().defaultLazy { System.currentTimeMillis() }
    val saveFolder by option()

    fun <T : Any> dump(gameWAD: GameWAD, type: String, list: List<T>, nameSource: KProperty1<T, StringPointer>?, match: Regex) {
        for ((index, element) in list.withIndex()) {
            val name = nameSource?.invoke(element)?.let { gameWAD.strg.getString(it) }

            if (name?.matches(match) == true || index.toString().matches(match)) {
                println("$type #$index:")
                StarfaitUtils.dump(gameWAD, element, 1, null)

                println()
            }
        }
    }

    override fun run() {
        val wadFile = if (wadFile == null) {
            val selectedFile = MemoryStack.stackPush().use { stack ->
                val filters = stack.mallocPointer(WAD_FILTERS.size)
                WAD_FILTERS.forEach { filters.put(stack.UTF8(it)) }
                filters.flip()

                return@use TinyFileDialogs.tinyfd_openFileDialog(
                    "Select GameMaker: Studio WAD File",
                    null,
                    filters,
                    null,
                    false
                )
            }

            if (selectedFile == null) {
                println("You need to select a file to be able to use Starfait!")
                return
            }

            File(selectedFile)
        } else
            wadFile!!

        val byteReader = StarfaitByteBuffer.fromByteArray(wadFile.readBytes())
        val gameWAD = GameWAD.read(byteReader)
        println("Loaded \"${gameWAD.strg.getString(gameWAD.gen8.gameDisplayName)}\" successfully! [WAD Version ${gameWAD.gen8.wadVersion}]")

        if (printGameObjects != null) {
            dump(gameWAD, "Object", gameWAD.objt.objects, GameObject::name, Regex(printGameObjects!!))
            return
        }

        if (printBackgrounds != null) {
            dump(gameWAD, "Background", gameWAD.bgnd.backgrounds, Background::name, Regex(printBackgrounds!!))
            return
        }

        if (printSounds != null) {
            dump(gameWAD, "Sound", gameWAD.sond.sounds, Sound::name, Regex(printSounds!!))
            return
        }

        if (printFonts != null) {
            dump(gameWAD, "Font", gameWAD.font.fonts, Font::name, Regex(printFonts!!))
            return
        }

        if (printRooms != null) {
            dump(gameWAD, "Room", gameWAD.room.rooms, Room::name, Regex(printRooms!!))
            return
        }

        if (printScripts != null) {
            dump(gameWAD, "Script", gameWAD.scpt.scripts, Script::name, Regex(printScripts!!))
            return
        }

        if (printCodes != null) {
            dump(gameWAD, "Code", gameWAD.code.codes, Code::name, Regex(printCodes!!))
            return
        }

        if (printVariables != null) {
            dump(gameWAD, "Variable", gameWAD.vari.variables, Variable::name, Regex(printVariables!!))
            return
        }

        if (printFunctions != null) {
            dump(gameWAD, "Function", gameWAD.func.functions, Function::name, Regex(printFunctions!!))
            return
        }

        if (printTexturePages != null) {
            dump(gameWAD, "Texture Page Entry", gameWAD.tpag.entries.values.toList(), null, Regex(printTexturePages!!))
            return
        }

        if (printSprites != null) {
            dump(gameWAD, "Sprite", gameWAD.sprt.sprites, Sprite::name, Regex(printSprites!!))
            return
        }

        if (printUnimplementedFunctions) {
            val vm = StarfaitVM(gameWAD, setOf(), setOf(), setOf(), setOf(), setOf(), setOf(), setOf())
            val implementedFunctions = mutableSetOf<String>()
            val unimplementedFunctions = mutableSetOf<String>()
            val scriptFunctions = mutableSetOf<String>()

            for (func in gameWAD.func.functions) {
                val functionName = func.name.let { gameWAD.strg.getString(it) }

                val handler = vm.builtins.registeredFunctions[functionName]

                if (handler != null) {
                    implementedFunctions.add(functionName)
                } else {
                    val scpt = gameWAD.scpt.scripts.firstOrNull { gameWAD.strg.getString(it.name) == functionName }

                    if (scpt != null) {
                        scriptFunctions.add(functionName)
                    } else {
                        unimplementedFunctions.add(functionName)
                    }
                }
            }

            println("Overview (${implementedFunctions.size} / ${unimplementedFunctions.size} / ${scriptFunctions.size}):")
            for (functionName in unimplementedFunctions) {
                println("UNIMPLEMENTED $functionName")
            }

            for (functionName in implementedFunctions) {
                println("IMPLEMENTED $functionName")
            }

            for (functionName in scriptFunctions) {
                println("SCRIPT $functionName")
            }
            return
        }

        if (printObjectTree != null) {
            val split = printObjectTree!!.split(",")
            for (obj in gameWAD.objt.objects) {
                val name = obj.name.let { gameWAD.strg.getString(it) }

                if (split.contains("*") || split.contains(name)) {
                    val objectTree = mutableListOf<GameObject>()
                    var currentObject = obj

                    while (true) {
                        objectTree.add(currentObject)

                        val parentId = currentObject.parentId
                        if (parentId == WADUtils.HAS_NO_PARENT_ID)
                            break

                        currentObject = gameWAD.objt.objects[parentId]
                    }

                    var isFirst = true
                    for (entry in objectTree) {
                        if (!isFirst) {
                            print(" -> ")
                        }
                        print(gameWAD.strg.getString(entry.name))
                        isFirst = false
                    }
                    println()
                }
            }
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

        if (dumpSprites != null) {
            val outputFolder = File(dumpSprites!!)
            outputFolder.mkdirs()

            // Preload all atlas so that we don't need to keep recreating them
            val textureAtlasImages = gameWAD.txtr.textures.map { ImageIO.read(it.blob.inputStream()) }
            for (sprite in gameWAD.sprt.sprites) {
                for ((index, frame) in sprite.frames.withIndex()) {
                    val texturePageEntry = gameWAD.tpag.entries[frame]
                    // val textureAtlas = gameWAD.txtr.textures[texturePageEntry!!.textureIndex.toInt()]
                    val textureAtlasImage = textureAtlasImages[texturePageEntry!!.textureIndex.toInt()]
                    val spriteImage = textureAtlasImage.getSubimage(
                        texturePageEntry.sourceX.toInt(),
                        texturePageEntry.sourceY.toInt(),
                        texturePageEntry.sourceWidth.toInt(),
                        texturePageEntry.sourceHeight.toInt()
                    )

                    ImageIO.write(spriteImage, "png", File(outputFolder, "${gameWAD.strg.getString(sprite.name)}.$index.png"))
                }
            }
            return
        }

        if (dumpSpriteMasks != null) {
            val outputFolder = File(dumpSpriteMasks!!)
            outputFolder.mkdirs()

            for (sprite in gameWAD.sprt.sprites) {
                if (sprite.hasCollisionMask) {
                    for ((collisionMaskIndex, collisionMask) in sprite.collisionMasks.withIndex()) {
                        File(outputFolder, "${gameWAD.strg.getString(sprite.name)}.$collisionMaskIndex.bin").writeBytes(collisionMask.data)

                        val maskWidth = sprite.width
                        val maskHeight = sprite.height

                        val image = BufferedImage(maskWidth, maskHeight, BufferedImage.TYPE_INT_ARGB)

                        for (y in 0 until maskHeight) {
                            for (x in 0 until maskWidth) {
                                val isSet = collisionMask.isPixelSolid(sprite, x, y)
                                image.setRGB(x, y, if (isSet) Color.WHITE.rgb else Color.BLACK.rgb)
                            }
                        }

                        ImageIO.write(image, "png", File(outputFolder, "${gameWAD.strg.getString(sprite.name)}.$collisionMaskIndex.png"))
                    }
                }
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

        var exitAtFrame = exitAtFrame
        val playbackInputFrames: Map<Int, RecordedFrame>?

        if (playbackInputs != null) {
            val frames = playbackInputs!!.readLines().mapIndexed { it, string -> it to Json.decodeFromString<RecordedFrame>(string) }.toMap()
            exitAtFrame = if (exitWhenPlaybackEnds) frames.keys.size else null
            playbackInputFrames = frames
        } else {
            playbackInputFrames = null
        }

        var saveFolderKind: SaveFolderKind = SaveFolderKind.Memory

        if (this.saveFolder != "memory" || this.saveFolder == null) {
            saveFolderKind = SaveFolderKind.Folder(File(this.saveFolder ?: "./saves/${gameWAD.strg.getString(gameWAD.gen8.gameTitle)}/").also { it.mkdirs() })
        }

        val window = StarfaitWindow(
            wadFile,
            saveFolderKind,
            vm,
            seed,
            enableCollisionRendering,
            drawInvisibleInstances,
            recordInputs,
            playbackInputFrames,
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
            traceFrameDuration.map { if (it == "*") 0..Int.MAX_VALUE else it.split("..").let { it[0].toInt()..it[1].toInt() } },
            speed,
            runFullSpeedUntilFrame,
            masterVolume
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