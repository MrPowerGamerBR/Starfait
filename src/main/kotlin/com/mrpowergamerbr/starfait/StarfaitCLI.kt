package com.mrpowergamerbr.starfait

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.mrpowergamerbr.starfait.vm.StarfaitVM
import com.mrpowergamerbr.starfait.wad.Background
import com.mrpowergamerbr.starfait.wad.Code
import com.mrpowergamerbr.starfait.wad.Font
import com.mrpowergamerbr.starfait.wad.GameObject
import com.mrpowergamerbr.starfait.wad.GameWAD
import com.mrpowergamerbr.starfait.wad.Room
import com.mrpowergamerbr.starfait.wad.Sound
import com.mrpowergamerbr.starfait.wad.StringPointer
import com.mrpowergamerbr.starfait.wad.chunks.OBJT
import java.io.File
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1

class StarfaitCLI : CliktCommand() {
    val wadPath by argument()
    val printGameObjects by option()
    val printBackgrounds by option()
    val printSounds by option()
    val printFonts by option()
    val printRooms by option()
    val printCodes by option()

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
        val byteReader = StarfaitByteReader(File(wadPath).readBytes())
        val gameWAD = GameWAD.read(byteReader)

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

        if (printCodes != null) {
            dump(gameWAD, "Code", gameWAD.code.codes, Code::name, printCodes!!)
            return
        }

        val vm = StarfaitVM(gameWAD)

        vm.executeLoop(gameWAD.code.codes.first { gameWAD.strg.getString(it.name) == "gml_Script_attention_hackerz_no_2" })
    }
}

fun main(args: Array<String>) = StarfaitCLI().main(args)