package com.mrpowergamerbr.starfait

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.mrpowergamerbr.starfait.runner.StarfaitRunner
import com.mrpowergamerbr.starfait.vm.StarfaitVM
import com.mrpowergamerbr.starfait.wad.Background
import com.mrpowergamerbr.starfait.wad.Code
import com.mrpowergamerbr.starfait.wad.Font
import com.mrpowergamerbr.starfait.wad.Function
import com.mrpowergamerbr.starfait.wad.GameObject
import com.mrpowergamerbr.starfait.wad.GameWAD
import com.mrpowergamerbr.starfait.wad.Room
import com.mrpowergamerbr.starfait.wad.Script
import com.mrpowergamerbr.starfait.wad.Sound
import com.mrpowergamerbr.starfait.wad.StringPointer
import com.mrpowergamerbr.starfait.wad.Variable
import com.mrpowergamerbr.starfait.wad.chunks.SCPT
import java.io.File
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
    val traceOpcodes by option()

    fun <T : Any> dump(gameWAD: GameWAD, type: String, list: List<T>, nameSource: KProperty1<T, StringPointer>, match: String) {
        for ((index, element) in list.withIndex()) {
            val name = gameWAD.strg.getString(nameSource.invoke(element))

            if (match == name || match == "*") {
                println("$type #$index:")
                StarfaitUtils.dump(gameWAD, element, 1)

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

        val vm = StarfaitVM(
            gameWAD,
            gmlScriptsOpcodesToBeTraced = traceOpcodes?.split(",")?.toSet().orEmpty()
        )
        vm.remapReferences()

        val runner = StarfaitRunner(gameWAD, vm)
        vm.runner = runner
        runner.start()
        runner.step()
        runner.step()
        // vm.executeLoop(gameWAD.code.codes.first { gameWAD.strg.getString(it.name) == "gml_Script_attention_hackerz_no_2" })
    }
}

fun main(args: Array<String>) = StarfaitCLI().main(args)